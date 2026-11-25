package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-06 (Solicitar prueba diagnóstica).
 * Técnicas: clases de equivalencia (cada parámetro obligatorio: válido / nulo / vacío / en blanco),
 * valores límite de la prioridad (0 y 6 no válidos; 1 y 5 válidos), criterio each use para
 * combinar parámetros y conjetura de errores (espacios sobrantes, solicitud ya cancelada).
 */
class SolicitudPruebaServiceImplTest {

    private static final String MSG_PACIENTE = "El identificador del paciente es obligatorio";
    private static final String MSG_MEDICO = "El identificador del médico es obligatorio";
    private static final String MSG_CODIGO = "El código de la prueba diagnóstica es obligatorio";
    private static final String MSG_PRIORIDAD = "La prioridad debe estar comprendida entre 1 (urgente) y 5 (ordinaria)";
    private static final String MSG_JUSTIFICACION = "La justificación clínica es obligatoria";
    private static final String MSG_SOLICITUD = "El identificador de la solicitud es obligatorio";
    private static final String MSG_MOTIVO = "El motivo de cancelación es obligatorio";

    private SolicitudPruebaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new SolicitudPruebaServiceImpl();
    }

    // ---------------------------------------------------------------- constructor

    @Test
    @DisplayName("El constructor rechaza un servicio de catálogo nulo")
    void constructorConCatalogoNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SolicitudPruebaServiceImpl(null));

        assertEquals("El servicio de catálogo de pruebas es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- crear: caso válido

    @Test
    @DisplayName("Crear una solicitud válida recupera los datos del catálogo")
    void crearSolicitudValidaRecuperaDatosDelCatalogo() {
        // Ejecutar
        SolicitudPruebaDTO solicitud = servicio.crearSolicitudPrueba(
                "PAC-01", "MED-05", "tac-abdo", 1, "Sospecha de apendicitis aguda");

        // Comprobar
        assertAll(
                () -> assertNotNull(solicitud),
                () -> assertTrue(solicitud.getSolicitudId().startsWith("SOL-")),
                () -> assertEquals("TAC-ABDO", solicitud.getCodigoPrueba()),
                () -> assertEquals("TAC Abdominal con Contraste IV", solicitud.getNombrePrueba()),
                () -> assertEquals("PENDIENTE", solicitud.getEstado()),
                () -> assertNotNull(solicitud.getFechaSolicitud()));
    }

    @Test
    @DisplayName("Los datos de texto se guardan sin espacios sobrantes")
    void crearSolicitudRecortaEspaciosDeLosTextos() {
        SolicitudPruebaDTO solicitud = servicio.crearSolicitudPrueba(
                "  PAC-01 ", " MED-05  ", "RX-TORAX", 3, "  Tos persistente  ");

        assertAll(
                () -> assertEquals("PAC-01", solicitud.getPacienteId()),
                () -> assertEquals("MED-05", solicitud.getMedicoId()),
                () -> assertEquals("Tos persistente", solicitud.getJustificacionClinica()));
    }

    // ---------------------------------------------------------------- crear: prioridad

    @ParameterizedTest(name = "prioridad {0} produce nivel {1}")
    @CsvSource({
            "1, URGENTE",
            "2, PREFERENTE",
            "3, PREFERENTE",
            "4, ORDINARIA",
            "5, ORDINARIA"
    })
    @DisplayName("Cada prioridad válida (incluidos los límites 1 y 5) se traduce a su nivel")
    void crearSolicitudAsignaNivelSegunPrioridad(int prioridad, String nivelEsperado) {
        SolicitudPruebaDTO solicitud = servicio.crearSolicitudPrueba(
                "PAC-01", "MED-01", "RX-TORAX", prioridad, "Control");

        assertAll(
                () -> assertEquals(prioridad, solicitud.getPrioridad()),
                () -> assertEquals(nivelEsperado, solicitud.getNivelPrioridad()));
    }

    @ParameterizedTest(name = "prioridad {0} fuera de rango")
    @ValueSource(ints = {0, 6, -1, 100})
    @DisplayName("Una prioridad fuera de 1 a 5 (límites 0 y 6) lanza excepción")
    void crearSolicitudConPrioridadFueraDeRangoLanzaExcepcion(int prioridad) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", prioridad, "Justif"));

        assertEquals(MSG_PRIORIDAD, e.getMessage());
    }

    // ---------------------------------------------------------------- crear: parámetros obligatorios

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con paciente nulo, vacío o en blanco lanza excepción")
    void crearSolicitudConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.crearSolicitudPrueba(paciente, "MED-1", "RX-TORAX", 3, "Justif"));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con médico nulo, vacío o en blanco lanza excepción")
    void crearSolicitudConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.crearSolicitudPrueba("PAC-1", medico, "RX-TORAX", 3, "Justif"));

        assertEquals(MSG_MEDICO, e.getMessage());
    }

    @ParameterizedTest(name = "código de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con código de prueba nulo, vacío o en blanco lanza excepción")
    void crearSolicitudConCodigoPruebaInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.crearSolicitudPrueba("PAC-1", "MED-1", codigo, 3, "Justif"));

        assertEquals(MSG_CODIGO, e.getMessage());
    }

    @ParameterizedTest(name = "justificación [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con justificación clínica nula, vacía o en blanco lanza excepción")
    void crearSolicitudConJustificacionInvalidaLanzaExcepcion(String justificacion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", 3, justificacion));

        assertEquals(MSG_JUSTIFICACION, e.getMessage());
    }

    // ---------------------------------------------------------------- crear: catálogo (CU-24)

    @Test
    @DisplayName("Crear con una prueba inexistente en el catálogo lanza excepción")
    void crearSolicitudConPruebaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.crearSolicitudPrueba("PAC-01", "MED-01", "PRUEBA-INVENTADA", 3, "Justificación"));

        assertEquals("No existe ninguna prueba activa con código: PRUEBA-INVENTADA", e.getMessage());
    }

    @Test
    @DisplayName("Crear con una prueba dada de baja en el catálogo lanza excepción")
    void crearSolicitudConPruebaDadaDeBajaLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.crearSolicitudPrueba("PAC-01", "MED-01", "RX-OBSOLETA", 3, "Justificación"));

        assertEquals("No existe ninguna prueba activa con código: RX-OBSOLETA", e.getMessage());
    }

    // ---------------------------------------------------------------- consultar

    @Test
    @DisplayName("Consultar una solicitud existente devuelve la misma solicitud")
    void consultarSolicitudExistenteLaDevuelve() {
        SolicitudPruebaDTO creada = servicio.crearSolicitudPrueba("PAC-10", "MED-01", "ECO-ABDO", 4, "Dolor abdominal");

        SolicitudPruebaDTO consultada = servicio.consultarSolicitud(creada.getSolicitudId());

        assertEquals(creada.getSolicitudId(), consultada.getSolicitudId());
    }

    @ParameterizedTest(name = "identificador [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Consultar con identificador nulo, vacío o en blanco lanza excepción")
    void consultarSolicitudConIdInvalidoLanzaExcepcion(String solicitudId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarSolicitud(solicitudId));

        assertEquals(MSG_SOLICITUD, e.getMessage());
    }

    @Test
    @DisplayName("Consultar una solicitud inexistente lanza excepción")
    void consultarSolicitudInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarSolicitud("SOL-NO-EXISTE"));

        assertEquals("No existe ninguna solicitud de prueba con ID: SOL-NO-EXISTE", e.getMessage());
    }

    // ---------------------------------------------------------------- listar por paciente

    @Test
    @DisplayName("Listar devuelve solo las solicitudes del paciente indicado")
    void listarSolicitudesPacienteDevuelveSoloLasSuyas() {
        servicio.crearSolicitudPrueba("PAC-10", "MED-01", "ECO-ABDO", 4, "Dolor abdominal");
        servicio.crearSolicitudPrueba("PAC-10", "MED-02", "RX-TORAX", 2, "Revisión");
        servicio.crearSolicitudPrueba("PAC-11", "MED-02", "RX-TORAX", 2, "Revisión");

        List<SolicitudPruebaDTO> lista = servicio.listarSolicitudesPaciente("PAC-10");

        assertEquals(2, lista.size());
        lista.forEach(s -> assertEquals("PAC-10", s.getPacienteId()));
    }

    @Test
    @DisplayName("Listar un paciente sin solicitudes devuelve una lista vacía")
    void listarSolicitudesPacienteSinSolicitudesDevuelveListaVacia() {
        assertTrue(servicio.listarSolicitudesPaciente("PAC-99").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarSolicitudesPacienteConIdInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarSolicitudesPaciente(pacienteId));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    // ---------------------------------------------------------------- cancelar

    @Test
    @DisplayName("Cancelar una solicitud la pasa a CANCELADA y guarda el motivo")
    void cancelarSolicitudValidaCambiaEstadoYGuardaMotivo() {
        SolicitudPruebaDTO creada = servicio.crearSolicitudPrueba("PAC-20", "MED-01", "TAC-ABDO", 2, "Estudio digestivo");

        SolicitudPruebaDTO cancelada = servicio.cancelarSolicitud(creada.getSolicitudId(), "Duplicada por error clínico");

        assertAll(
                () -> assertEquals("CANCELADA", cancelada.getEstado()),
                () -> assertEquals("Duplicada por error clínico", cancelada.getMotivoCancelacion()));
    }

    @Test
    @DisplayName("Cancelar dos veces la misma solicitud lanza excepción")
    void cancelarSolicitudYaCanceladaLanzaExcepcion() {
        SolicitudPruebaDTO creada = servicio.crearSolicitudPrueba("PAC-20", "MED-01", "TAC-ABDO", 2, "Estudio digestivo");
        servicio.cancelarSolicitud(creada.getSolicitudId(), "Primer motivo");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.cancelarSolicitud(creada.getSolicitudId(), "Otro motivo"));

        assertEquals("La solicitud ya se encuentra cancelada", e.getMessage());
    }

    @Test
    @DisplayName("Cancelar una solicitud inexistente lanza excepción")
    void cancelarSolicitudInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.cancelarSolicitud("SOL-NO-EXISTE", "Motivo"));

        assertEquals("No existe ninguna solicitud de prueba con ID: SOL-NO-EXISTE", e.getMessage());
    }

    @ParameterizedTest(name = "identificador [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Cancelar con identificador nulo, vacío o en blanco lanza excepción")
    void cancelarSolicitudConIdInvalidoLanzaExcepcion(String solicitudId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarSolicitud(solicitudId, "Motivo"));

        assertEquals(MSG_SOLICITUD, e.getMessage());
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Cancelar con motivo nulo, vacío o en blanco lanza excepción")
    void cancelarSolicitudConMotivoInvalidoLanzaExcepcion(String motivo) {
        SolicitudPruebaDTO creada = servicio.crearSolicitudPrueba("PAC-20", "MED-01", "TAC-ABDO", 2, "Estudio digestivo");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarSolicitud(creada.getSolicitudId(), motivo));

        assertEquals(MSG_MOTIVO, e.getMessage());
    }
}
