package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-01 (Gestionar citas propias del paciente).
 * Técnicas: clases de equivalencia (token válido / nulo / de otro rol, cita propia / ajena /
 * inexistente, filtro de estado válido / no válido), criterio each use entre parámetros y
 * conjetura de errores (mayúsculas del identificador, cita ya cancelada, agenda cerrada).
 * Los servicios colaboradores se reutilizan reales; el PACIENTE solo actúa sobre sus citas.
 */
class GestorCitasPacienteServiceImplTest {

    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1234";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-9999";
    private static final String TOKEN_ADMIN = "token-sso-adm01-ADMISION-0001";
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 10);

    private GestorRolesServiceImpl gestorRoles;
    private GestionCitasServiceImpl gestionCitas;
    private GestorAgendasServiceImpl gestorAgendas;
    private GestorCitasPacienteServiceImpl servicioPaciente;

    @BeforeEach
    void setUp() {
        gestorRoles = new GestorRolesServiceImpl();
        gestionCitas = new GestionCitasServiceImpl((p, c, t, d) -> {}, gestorRoles, TOKEN_ADMIN);
        gestorAgendas = new GestorAgendasServiceImpl(gestorRoles);
        servicioPaciente = new GestorCitasPacienteServiceImpl(gestorRoles, gestionCitas, gestorAgendas);
    }

    private CitaDTO citaDe(String paciente, String prueba, String franja) {
        return gestionCitas.programarCita(paciente, "MED-01", prueba, FECHA, franja);
    }

    // ---------------------------------------------------------------- constructores

    @Test
    @DisplayName("El constructor por defecto crea un servicio válido")
    void constructorPorDefectoNoLanzaExcepcion() {
        assertDoesNotThrow(() -> new GestorCitasPacienteServiceImpl());
    }

    @ParameterizedTest(name = "sin roles={0}, sin citación={1}, sin agendas={2}")
    @CsvSource({
            "true,  false, false",
            "false, true,  false",
            "false, false, true"
    })
    @DisplayName("El constructor rechaza cada servicio colaborador nulo (criterio each use)")
    void constructorConColaboradorNuloLanzaExcepcion(boolean sinRoles, boolean sinCitas, boolean sinAgendas) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new GestorCitasPacienteServiceImpl(
                        sinRoles ? null : gestorRoles,
                        sinCitas ? null : gestionCitas,
                        sinAgendas ? null : gestorAgendas));

        assertEquals("Los servicios de roles, citación y agendas son obligatorios", e.getMessage());
    }

    // ---------------------------------------------------------------- seguridad

    @ParameterizedTest(name = "token [{0}] no está informado")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Sin token SSO se deniega el acceso")
    void consultarMisCitasSinTokenLanzaSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicioPaciente.consultarMisCitas("PAC-01", null, token));

        assertEquals("Token SSO obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} no es de un paciente")
    @ValueSource(strings = {TOKEN_MEDICO, TOKEN_ADMIN, "sin-rol-aqui"})
    @DisplayName("Un token que no es de paciente se deniega")
    void consultarMisCitasConTokenDeOtroRolLanzaSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicioPaciente.consultarMisCitas("PAC-01", null, token));

        assertEquals("Acceso denegado: se requiere rol PACIENTE", e.getMessage());
    }

    @Test
    @DisplayName("Todas las operaciones del paciente exigen rol PACIENTE")
    void todasLasOperacionesExigenRolPaciente() {
        CitaDTO cita = citaDe("PAC-01", "RX-TORAX", "09:00-09:15");

        assertAll(
                () -> assertThrows(SecurityException.class,
                        () -> servicioPaciente.consultarMisCitas("PAC-01", null, TOKEN_MEDICO)),
                () -> assertThrows(SecurityException.class,
                        () -> servicioPaciente.consultarDetalleCitaPropia("PAC-01", cita.getCitaId(), TOKEN_MEDICO)),
                () -> assertThrows(SecurityException.class,
                        () -> servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", FECHA, TOKEN_MEDICO)),
                () -> assertThrows(SecurityException.class,
                        () -> servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), FECHA.plusDays(1), "10:00-10:15", TOKEN_MEDICO)),
                () -> assertThrows(SecurityException.class,
                        () -> servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Motivo", TOKEN_MEDICO)));
    }

    // ---------------------------------------------------------------- consultarMisCitas

    @ParameterizedTest(name = "filtro [{0}] devuelve {1} cita(s)")
    @CsvSource({
            "PROGRAMADA, 1",
            "cancelada,  1",
            "Cancelada,  1"
    })
    @DisplayName("El filtro de estado se aplica sin distinguir mayúsculas")
    void consultarMisCitasConFiltroDevuelveLasDeEseEstado(String filtro, int esperadas) {
        citaDe("PAC-01", "RX-TORAX", "09:00-09:15");
        CitaDTO aCancelar = citaDe("PAC-01", "TAC-ABDO", "10:00-10:30");
        gestionCitas.cancelarCita(aCancelar.getCitaId(), "Motivo prueba");

        List<CitaDTO> citas = servicioPaciente.consultarMisCitas("PAC-01", filtro, TOKEN_PACIENTE);

        assertEquals(esperadas, citas.size());
    }

    @ParameterizedTest(name = "filtro [{0}] no filtra")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin filtro de estado se devuelven todas las citas del paciente")
    void consultarMisCitasSinFiltroDevuelveTodas(String filtro) {
        citaDe("PAC-01", "RX-TORAX", "09:00-09:15");
        CitaDTO aCancelar = citaDe("PAC-01", "TAC-ABDO", "10:00-10:30");
        gestionCitas.cancelarCita(aCancelar.getCitaId(), "Motivo prueba");

        assertEquals(2, servicioPaciente.consultarMisCitas("PAC-01", filtro, TOKEN_PACIENTE).size());
    }

    @Test
    @DisplayName("Un filtro de estado no válido lanza excepción")
    void consultarMisCitasConFiltroNoValidoLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.consultarMisCitas("PAC-01", "INVENTADO", TOKEN_PACIENTE));

        assertEquals("Estado de cita no válido: INVENTADO", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con paciente nulo, vacío o en blanco lanza excepción")
    void consultarMisCitasConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.consultarMisCitas(paciente, null, TOKEN_PACIENTE));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarDetalleCitaPropia

    @Test
    @DisplayName("Consultar el detalle de una cita propia la devuelve")
    void consultarDetalleCitaPropiaLaDevuelve() {
        CitaDTO cita = citaDe("PAC-01", "RX-TORAX", "09:00-09:15");

        CitaDTO detalle = servicioPaciente.consultarDetalleCitaPropia("PAC-01", cita.getCitaId(), TOKEN_PACIENTE);

        assertEquals(cita.getCitaId(), detalle.getCitaId());
    }

    @Test
    @DisplayName("El identificador del paciente se compara sin distinguir mayúsculas")
    void consultarDetalleCitaPropiaIgnoraMayusculasDelPaciente() {
        CitaDTO cita = citaDe("PAC-01", "RX-TORAX", "09:00-09:15");

        CitaDTO detalle = servicioPaciente.consultarDetalleCitaPropia("pac-01", cita.getCitaId(), TOKEN_PACIENTE);

        assertEquals(cita.getCitaId(), detalle.getCitaId());
    }

    @Test
    @DisplayName("Consultar una cita de otro paciente lanza SecurityException")
    void consultarDetalleCitaAjenaLanzaSecurityException() {
        CitaDTO ajena = citaDe("PAC-99", "RM-CRANEO", "11:00-11:30");

        SecurityException e = assertThrows(SecurityException.class,
                () -> servicioPaciente.consultarDetalleCitaPropia("PAC-01", ajena.getCitaId(), TOKEN_PACIENTE));

        assertEquals("Acceso denegado: la cita no pertenece al paciente autenticado", e.getMessage());
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con identificador de cita nulo, vacío o en blanco lanza excepción")
    void consultarDetalleCitaConIdInvalidoLanzaExcepcion(String citaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.consultarDetalleCitaPropia("PAC-01", citaId, TOKEN_PACIENTE));

        assertEquals("El identificador de la cita es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar una cita inexistente lanza excepción")
    void consultarDetalleCitaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicioPaciente.consultarDetalleCitaPropia("PAC-01", "CIT-NO-EXISTE", TOKEN_PACIENTE));

        assertEquals("No existe ninguna cita con ID: CIT-NO-EXISTE", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarHuecosDisponiblesCambio

    @Test
    @DisplayName("Una agenda abierta devuelve sus huecos libres")
    void consultarHuecosConAgendaAbiertaDevuelveLosLibres() {
        gestorAgendas.configurarAgendaDiaria("MED-01", FECHA, LocalTime.of(10, 0), LocalTime.of(11, 0), 30, TOKEN_MEDICO);

        List<TramoHorarioDTO> libres = servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", FECHA, TOKEN_PACIENTE);

        assertAll(
                () -> assertEquals(2, libres.size()),
                () -> assertTrue(libres.get(0).isDisponible()));
    }

    @Test
    @DisplayName("Una agenda cerrada no devuelve huecos")
    void consultarHuecosConAgendaCerradaDevuelveListaVacia() {
        AgendaDiariaDTO agenda = gestorAgendas.configurarAgendaDiaria(
                "MED-01", FECHA, LocalTime.of(10, 0), LocalTime.of(11, 0), 30, TOKEN_MEDICO);
        gestorAgendas.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMIN);

        assertTrue(servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", FECHA, TOKEN_PACIENTE).isEmpty());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar huecos con médico nulo, vacío o en blanco lanza excepción")
    void consultarHuecosConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.consultarHuecosDisponiblesCambio(medico, FECHA, TOKEN_PACIENTE));

        assertEquals("El identificador del médico y la fecha son obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Consultar huecos sin fecha lanza excepción")
    void consultarHuecosSinFechaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", null, TOKEN_PACIENTE));

        assertEquals("El identificador del médico y la fecha son obligatorios", e.getMessage());
    }

    // ---------------------------------------------------------------- reprogramarCitaPropia

    @Test
    @DisplayName("Reprogramar una cita propia la deja REPROGRAMADA en la nueva franja")
    void reprogramarCitaPropiaLaDejaReprogramada() {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");

        CitaDTO reprogramada = servicioPaciente.reprogramarCitaPropia(
                "PAC-01", cita.getCitaId(), FECHA.plusDays(1), "10:00-10:20", TOKEN_PACIENTE);

        assertAll(
                () -> assertEquals("REPROGRAMADA", reprogramada.getEstado()),
                () -> assertEquals(FECHA.plusDays(1), reprogramada.getFecha()),
                () -> assertEquals("10:00-10:20", reprogramada.getFranjaHoraria()));
    }

    @Test
    @DisplayName("Reprogramar una cita de otro paciente lanza SecurityException")
    void reprogramarCitaAjenaLanzaSecurityException() {
        CitaDTO ajena = citaDe("PAC-99", "RM-CRANEO", "11:00-11:30");

        assertThrows(SecurityException.class, () -> servicioPaciente.reprogramarCitaPropia(
                "PAC-01", ajena.getCitaId(), FECHA.plusDays(1), "12:00-12:30", TOKEN_PACIENTE));
    }

    @Test
    @DisplayName("Reprogramar sin nueva fecha lanza excepción")
    void reprogramarCitaPropiaSinFechaLanzaExcepcion() {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), null, "10:00-10:20", TOKEN_PACIENTE));

        assertEquals("La nueva fecha y la nueva franja horaria son obligatorias", e.getMessage());
    }

    @ParameterizedTest(name = "franja [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Reprogramar con franja nula, vacía o en blanco lanza excepción")
    void reprogramarCitaPropiaConFranjaInvalidaLanzaExcepcion(String franja) {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), FECHA.plusDays(1), franja, TOKEN_PACIENTE));

        assertEquals("La nueva fecha y la nueva franja horaria son obligatorias", e.getMessage());
    }

    @Test
    @DisplayName("Reprogramar una cita ya cancelada lanza excepción")
    void reprogramarCitaPropiaCanceladaLanzaExcepcion() {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");
        servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Motivo personal", TOKEN_PACIENTE);

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), FECHA.plusDays(2), "11:00-11:20", TOKEN_PACIENTE));

        assertEquals("No se puede reprogramar una cita ya cancelada", e.getMessage());
    }

    // ---------------------------------------------------------------- cancelarCitaPropia

    @Test
    @DisplayName("Cancelar una cita propia la deja CANCELADA")
    void cancelarCitaPropiaLaDejaCancelada() {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");

        CitaDTO cancelada = servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Motivo personal", TOKEN_PACIENTE);

        assertAll(
                () -> assertEquals("CANCELADA", cancelada.getEstado()),
                () -> assertEquals("Motivo personal", cancelada.getMotivoCancelacion()));
    }

    @Test
    @DisplayName("Cancelar una cita de otro paciente lanza SecurityException")
    void cancelarCitaAjenaLanzaSecurityException() {
        CitaDTO ajena = citaDe("PAC-99", "RM-CRANEO", "11:00-11:30");

        assertThrows(SecurityException.class,
                () -> servicioPaciente.cancelarCitaPropia("PAC-01", ajena.getCitaId(), "No es mía", TOKEN_PACIENTE));
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Cancelar con motivo nulo, vacío o en blanco lanza excepción")
    void cancelarCitaPropiaConMotivoInvalidoLanzaExcepcion(String motivo) {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), motivo, TOKEN_PACIENTE));

        assertEquals("El motivo de cancelación es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Cancelar una cita ya cancelada lanza excepción")
    void cancelarCitaPropiaYaCanceladaLanzaExcepcion() {
        CitaDTO cita = citaDe("PAC-01", "ECO-ABDO", "09:00-09:20");
        servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Motivo personal", TOKEN_PACIENTE);

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Otro motivo", TOKEN_PACIENTE));

        assertEquals("La cita ya se encuentra cancelada", e.getMessage());
    }
}
