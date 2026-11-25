package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-13 (Gestionar citas).
 * Técnicas: clases de equivalencia sobre cada dato obligatorio (válido / nulo / en blanco),
 * control de acceso por rol (permitido / denegado), criterio each use entre parámetros y
 * conjetura de errores sobre los conflictos de franja (mismo médico y fecha, otro médico,
 * otra fecha, reprogramar a la propia franja) y sobre el estado CANCELADA.
 */
class GestionCitasServiceImplTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 10);
    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1234";

    private GestionCitasServiceImpl servicio;
    private List<String> eventosNotificados;

    @BeforeEach
    void setUp() {
        eventosNotificados = new ArrayList<>();
        servicio = new GestionCitasServiceImpl((pacienteId, citaId, tipoEvento, detalle) ->
                eventosNotificados.add(tipoEvento + ":" + pacienteId + ":" + citaId));
    }

    private String ultimoEvento() {
        return eventosNotificados.get(eventosNotificados.size() - 1);
    }

    // ---------------------------------------------------------------- constructores

    @Test
    @DisplayName("El constructor rechaza un notificador nulo")
    void constructorConNotificadorNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GestionCitasServiceImpl(null));

        assertEquals("El notificador de citas no puede ser nulo", e.getMessage());
    }

    @Test
    @DisplayName("El constructor rechaza un gestor de roles nulo")
    void constructorConGestorDeRolesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GestionCitasServiceImpl((p, c, t, d) -> {}, null, "tok"));

        assertEquals("El servicio de gestión de roles no puede ser nulo", e.getMessage());
    }

    @Test
    @DisplayName("El constructor por defecto crea un servicio que programa citas")
    void constructorPorDefectoPermiteProgramarCitas() {
        GestionCitasServiceImpl porDefecto = new GestionCitasServiceImpl();

        CitaDTO cita = porDefecto.programarCita("PAC-1", "MED-1", "RX-TORAX", LocalDate.of(2026, 12, 1), "09:00-09:15");

        assertEquals("PROGRAMADA", cita.getEstado());
    }

    // ---------------------------------------------------------------- permisos

    @ParameterizedTest(name = "el token {0} puede gestionar citas")
    @ValueSource(strings = {"token-sso-adm1-ADMISION-1", "token-sso-med1-MEDICO-1"})
    @DisplayName("Los roles ADMISION y MEDICO pueden programar citas")
    void programarCitaConRolPermitidoLaCrea(String token) {
        GestionCitasServiceImpl conRol =
                new GestionCitasServiceImpl((p, c, t, d) -> {}, new GestorRolesServiceImpl(), token);

        assertEquals("PROGRAMADA", conRol.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00").getEstado());
    }

    @ParameterizedTest(name = "el token [{0}] no puede gestionar citas")
    @NullAndEmptySource
    @ValueSource(strings = {TOKEN_PACIENTE, "sin-rol-aqui"})
    @DisplayName("Sin rol ADMISION ni MEDICO se deniega programar, reprogramar y cancelar")
    void gestionarCitasSinPermisoLanzaSecurityException(String token) {
        GestionCitasServiceImpl sinPermiso =
                new GestionCitasServiceImpl((p, c, t, d) -> {}, new GestorRolesServiceImpl(), token);

        assertAll(
                () -> assertEquals("Acceso denegado: Se requiere rol ADMISION o MEDICO.",
                        assertThrows(SecurityException.class,
                                () -> sinPermiso.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00")).getMessage()),
                () -> assertThrows(SecurityException.class, () -> sinPermiso.reprogramarCita("CIT-1", FECHA, "11:00")),
                () -> assertThrows(SecurityException.class, () -> sinPermiso.cancelarCita("CIT-1", "Motivo")));
    }

    @Test
    @DisplayName("Una operación denegada queda registrada en la auditoría")
    void gestionarCitasDenegadaRegistraEventoEnAuditoria() {
        AuditServiceImpl auditoria = new AuditServiceImpl();
        GestionCitasServiceImpl sinPermiso = new GestionCitasServiceImpl(
                (p, c, t, d) -> {}, new GestorRolesServiceImpl(), TOKEN_PACIENTE, auditoria);

        assertThrows(SecurityException.class,
                () -> sinPermiso.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00"));

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("ACCESO_DENEGADO_GESTION_CITAS", traza.getAccion()),
                () -> assertFalse(traza.isExito()));
    }

    @ParameterizedTest(name = "token [{0}] no puede consultar ni listar")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Consultar o listar citas sin token de sesión lanza SecurityException")
    void consultarYListarSinTokenLanzaSecurityException(String token) {
        GestionCitasServiceImpl sinToken =
                new GestionCitasServiceImpl((p, c, t, d) -> {}, new GestorRolesServiceImpl(), token);

        assertAll(
                () -> assertEquals("Token de sesión obligatorio para consultar citas",
                        assertThrows(SecurityException.class, () -> sinToken.consultarCita("CIT-1")).getMessage()),
                () -> assertEquals("Token de sesión obligatorio para listar citas",
                        assertThrows(SecurityException.class, () -> sinToken.listarCitasPaciente("PAC-01")).getMessage()));
    }

    // ---------------------------------------------------------------- programarCita

    @Test
    @DisplayName("Programar una cita válida la crea PROGRAMADA y notifica al paciente")
    void programarCitaValidaLaCreaYNotifica() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "tac-abdo", FECHA, "10:00-10:30");

        assertAll(
                () -> assertTrue(cita.getCitaId().startsWith("CIT-")),
                () -> assertEquals("TAC-ABDO", cita.getCodigoPrueba()),
                () -> assertEquals("PROGRAMADA", cita.getEstado()),
                () -> assertNotNull(cita.getFechaUltimaActualizacion()),
                () -> assertEquals(1, eventosNotificados.size()),
                () -> assertTrue(ultimoEvento().startsWith("PROGRAMADA:PAC-01:")));
    }

    @Test
    @DisplayName("Programar en una franja ocupada del mismo médico y fecha lanza excepción")
    void programarCitaEnFranjaOcupadaLanzaExcepcion() {
        servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "10:00-10:30");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.programarCita("PAC-02", "MED-01", "RX-TORAX", FECHA, "10:00-10:30"));

        assertEquals("La franja horaria ya está ocupada para ese médico y fecha", e.getMessage());
    }

    @Test
    @DisplayName("La misma franja se puede usar con otro médico")
    void programarCitaMismaFranjaConOtroMedicoEsValida() {
        servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "10:00-10:30");

        assertDoesNotThrow(() -> servicio.programarCita("PAC-02", "MED-02", "RX-TORAX", FECHA, "10:00-10:30"));
    }

    @Test
    @DisplayName("La misma franja se puede usar el día siguiente con el mismo médico")
    void programarCitaMismaFranjaEnOtraFechaEsValida() {
        servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "10:00-10:30");

        assertDoesNotThrow(() -> servicio.programarCita("PAC-02", "MED-01", "RX-TORAX", FECHA.plusDays(1), "10:00-10:30"));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Programar con paciente nulo, vacío o en blanco lanza excepción")
    void programarCitaConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.programarCita(paciente, "MED-1", "RX", FECHA, "09:00"));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Programar con médico nulo, vacío o en blanco lanza excepción")
    void programarCitaConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.programarCita("PAC-1", medico, "RX", FECHA, "09:00"));

        assertEquals("El identificador del médico es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "código de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Programar con código de prueba nulo, vacío o en blanco lanza excepción")
    void programarCitaConCodigoPruebaInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.programarCita("PAC-1", "MED-1", codigo, FECHA, "09:00"));

        assertEquals("El código de la prueba diagnóstica es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Programar sin fecha lanza excepción")
    void programarCitaSinFechaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.programarCita("PAC-1", "MED-1", "RX", null, "09:00"));

        assertEquals("La fecha de la cita es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "franja [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Programar con franja nula, vacía o en blanco lanza excepción")
    void programarCitaConFranjaInvalidaLanzaExcepcion(String franja) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.programarCita("PAC-1", "MED-1", "RX", FECHA, franja));

        assertEquals("La franja horaria es obligatoria", e.getMessage());
    }

    // ---------------------------------------------------------------- consultar y listar

    @Test
    @DisplayName("Consultar una cita existente devuelve esa cita")
    void consultarCitaExistenteLaDevuelve() {
        CitaDTO creada = servicio.programarCita("PAC-01", "MED-01", "RX-1", FECHA, "09:00-09:15");

        assertEquals(creada.getCitaId(), servicio.consultarCita(creada.getCitaId()).getCitaId());
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Consultar con identificador nulo, vacío o en blanco lanza excepción")
    void consultarCitaConIdInvalidoLanzaExcepcion(String citaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarCita(citaId));

        assertEquals("El identificador de la cita es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar una cita inexistente lanza excepción")
    void consultarCitaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarCita("CIT-INEXISTENTE"));

        assertEquals("No existe ninguna cita con ID: CIT-INEXISTENTE", e.getMessage());
    }

    @Test
    @DisplayName("Listar las citas de un paciente devuelve solo las suyas")
    void listarCitasPacienteDevuelveSoloLasSuyas() {
        servicio.programarCita("PAC-01", "MED-01", "RX-1", FECHA, "09:00-09:15");
        servicio.programarCita("PAC-01", "MED-02", "ECO-1", FECHA, "11:00-11:20");
        servicio.programarCita("PAC-02", "MED-02", "ECO-1", FECHA, "12:00-12:20");

        List<CitaDTO> citas = servicio.listarCitasPaciente("PAC-01");

        assertEquals(2, citas.size());
        citas.forEach(c -> assertEquals("PAC-01", c.getPacienteId()));
    }

    @Test
    @DisplayName("Listar un paciente sin citas devuelve una lista vacía")
    void listarCitasPacienteSinCitasDevuelveListaVacia() {
        assertTrue(servicio.listarCitasPaciente("PAC-SIN-CITAS").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarCitasPacienteConIdInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarCitasPaciente(paciente));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- reprogramarCita

    @Test
    @DisplayName("Reprogramar una cita actualiza fecha, franja y estado y notifica")
    void reprogramarCitaValidaActualizaLaCita() {
        LocalDate nuevaFecha = FECHA.plusDays(1);
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "RM-CRANEO", FECHA, "10:00-10:45");

        CitaDTO reprogramada = servicio.reprogramarCita(cita.getCitaId(), nuevaFecha, "16:00-16:45");

        assertAll(
                () -> assertEquals("REPROGRAMADA", reprogramada.getEstado()),
                () -> assertEquals(nuevaFecha, reprogramada.getFecha()),
                () -> assertEquals("16:00-16:45", reprogramada.getFranjaHoraria()),
                () -> assertTrue(ultimoEvento().startsWith("REPROGRAMADA:PAC-01:")));
    }

    @Test
    @DisplayName("Reprogramar a una franja ocupada por otra cita lanza excepción")
    void reprogramarCitaAFranjaOcupadaLanzaExcepcion() {
        LocalDate fecha2 = FECHA.plusDays(1);
        CitaDTO c1 = servicio.programarCita("PAC-01", "MED-01", "RM-CRANEO", FECHA, "10:00-10:45");
        servicio.programarCita("PAC-02", "MED-01", "RM-CRANEO", fecha2, "12:00-12:45");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.reprogramarCita(c1.getCitaId(), fecha2, "12:00-12:45"));

        assertEquals("La nueva franja horaria no está disponible", e.getMessage());
    }

    @Test
    @DisplayName("Reprogramar una cita a su propia franja no cuenta como conflicto")
    void reprogramarCitaASuPropiaFranjaEsValida() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "RM-CRANEO", FECHA, "10:00-10:45");

        assertDoesNotThrow(() -> servicio.reprogramarCita(cita.getCitaId(), FECHA, "10:00-10:45"));
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Reprogramar con identificador nulo, vacío o en blanco lanza excepción")
    void reprogramarCitaConIdInvalidoLanzaExcepcion(String citaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.reprogramarCita(citaId, FECHA, "17:00"));

        assertEquals("El identificador de la cita es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Reprogramar sin nueva fecha lanza excepción")
    void reprogramarCitaSinFechaLanzaExcepcion() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00-10:15");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.reprogramarCita(cita.getCitaId(), null, "17:00"));

        assertEquals("La nueva fecha es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "franja [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Reprogramar con franja nula, vacía o en blanco lanza excepción")
    void reprogramarCitaConFranjaInvalidaLanzaExcepcion(String franja) {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00-10:15");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.reprogramarCita(cita.getCitaId(), FECHA, franja));

        assertEquals("La nueva franja horaria es obligatoria", e.getMessage());
    }

    @Test
    @DisplayName("Reprogramar una cita cancelada lanza excepción")
    void reprogramarCitaCanceladaLanzaExcepcion() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "RX", FECHA, "10:00-10:15");
        servicio.cancelarCita(cita.getCitaId(), "Motivo");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.reprogramarCita(cita.getCitaId(), FECHA, "11:00-11:15"));

        assertEquals("No se puede reprogramar una cita cancelada", e.getMessage());
    }

    // ---------------------------------------------------------------- cancelarCita

    @Test
    @DisplayName("Cancelar una cita la pasa a CANCELADA, guarda el motivo y notifica")
    void cancelarCitaValidaCambiaEstadoYNotifica() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "09:00-09:30");

        CitaDTO cancelada = servicio.cancelarCita(cita.getCitaId(), "Solicitud del paciente");

        assertAll(
                () -> assertEquals("CANCELADA", cancelada.getEstado()),
                () -> assertEquals("Solicitud del paciente", cancelada.getMotivoCancelacion()),
                () -> assertTrue(ultimoEvento().startsWith("CANCELADA:PAC-01:")));
    }

    @Test
    @DisplayName("Cancelar una cita libera su franja para otra cita")
    void cancelarCitaLiberaLaFranja() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "09:00-09:30");
        servicio.cancelarCita(cita.getCitaId(), "Solicitud del paciente");

        CitaDTO nueva = servicio.programarCita("PAC-03", "MED-01", "TAC-ABDO", FECHA, "09:00-09:30");

        assertEquals("PROGRAMADA", nueva.getEstado());
    }

    @Test
    @DisplayName("Cancelar dos veces la misma cita lanza excepción")
    void cancelarCitaYaCanceladaLanzaExcepcion() {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "09:00-09:30");
        servicio.cancelarCita(cita.getCitaId(), "Primer motivo");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.cancelarCita(cita.getCitaId(), "Otro motivo"));

        assertEquals("La cita ya se encuentra cancelada", e.getMessage());
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Cancelar con identificador nulo, vacío o en blanco lanza excepción")
    void cancelarCitaConIdInvalidoLanzaExcepcion(String citaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarCita(citaId, "Motivo"));

        assertEquals("El identificador de la cita es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Cancelar con motivo nulo, vacío o en blanco lanza excepción")
    void cancelarCitaConMotivoInvalidoLanzaExcepcion(String motivo) {
        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", FECHA, "09:00-09:30");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarCita(cita.getCitaId(), motivo));

        assertEquals("El motivo de cancelación es obligatorio", e.getMessage());
    }
}
