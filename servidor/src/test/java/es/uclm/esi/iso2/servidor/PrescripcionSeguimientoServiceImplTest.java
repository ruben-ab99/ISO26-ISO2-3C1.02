package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-07 (Prescribir pruebas de seguimiento con periodicidad).
 * Técnicas: clases de equivalencia (datos nulos o en blanco, prueba existente / inexistente),
 * valores límite (periodicidad 0 y 1; número de revisiones 0, 1, 24 y 25), criterio each use
 * entre parámetros y conjetura de errores (franja ocupada, cancelación en cascada, cancelación
 * repetida). Se usan los servicios reales de catálogo y citación (relación include con CU-13).
 */
class PrescripcionSeguimientoServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);

    private IGestionCitasService gestionCitasService;
    private PrescripcionSeguimientoServiceImpl servicio;

    @BeforeEach
    void setUp() {
        gestionCitasService = new GestionCitasServiceImpl();
        servicio = new PrescripcionSeguimientoServiceImpl(
                new CatalogoPruebasServiceImpl(), gestionCitasService, new AuditServiceImpl());
    }

    // ---------------------------------------------------------------- constructores

    @Test
    @DisplayName("El constructor rechaza un catálogo de pruebas nulo")
    void constructorConCatalogoNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new PrescripcionSeguimientoServiceImpl(null, gestionCitasService));

        assertEquals("El servicio de catálogo de pruebas es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("El constructor rechaza un servicio de citas nulo")
    void constructorConGestionDeCitasNulaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new PrescripcionSeguimientoServiceImpl(new CatalogoPruebasServiceImpl(), null));

        assertEquals("El servicio de gestión de citas es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- prescribirSeguimiento

    @Test
    @DisplayName("Prescribir un seguimiento válido crea la pauta y genera las citas periódicas")
    void prescribirSeguimientoValidoGeneraCitasPeriodicas() {
        // Preparar y ejecutar
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-001", "MED-010", "tac-abdo", 30, 3, HOY, "Control periódico de nódulo pulmonar");

        // Comprobar
        List<CitaDTO> citas = gestionCitasService.listarCitasPaciente("PAC-001");
        assertAll(
                () -> assertNotNull(seg),
                () -> assertTrue(seg.getSeguimientoId().startsWith("SEG-")),
                () -> assertEquals("TAC-ABDO", seg.getCodigoPrueba()),
                () -> assertEquals("ACTIVO", seg.getEstado()),
                () -> assertEquals(3, seg.getCitasGeneradasIds().size()),
                () -> assertEquals(3, citas.size()),
                () -> assertEquals(LocalDate.of(2026, 12, 1), citas.get(0).getFecha()),
                () -> assertEquals(LocalDate.of(2026, 12, 31), citas.get(1).getFecha()),
                () -> assertEquals(LocalDate.of(2027, 1, 30), citas.get(2).getFecha()));
    }

    @Test
    @DisplayName("Los datos de texto se guardan sin espacios sobrantes")
    void prescribirSeguimientoRecortaEspaciosDeLosTextos() {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "  PAC-005 ", " MED-010  ", "RX-TORAX", 30, 1, HOY, "  Control anual  ");

        assertAll(
                () -> assertEquals("PAC-005", seg.getPacienteId()),
                () -> assertEquals("MED-010", seg.getMedicoId()),
                () -> assertEquals("Control anual", seg.getIndicacionesClinicas()));
    }

    @Test
    @DisplayName("Si la primera franja está ocupada se usa una franja alternativa")
    void prescribirSeguimientoUsaFranjaAlternativaSiHayConflicto() {
        // Preparar: se ocupa 09:00-09:30 para ese médico y fecha
        LocalDate fecha = LocalDate.of(2026, 12, 10);
        gestionCitasService.programarCita("PAC-999", "MED-010", "RX-TORAX", fecha, "09:00-09:30");

        // Ejecutar
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-002", "MED-010", "RX-TORAX", 90, 1, fecha, "Revisión trimestral");

        // Comprobar
        CitaDTO citaGenerada = gestionCitasService.consultarCita(seg.getCitasGeneradasIds().get(0));
        assertEquals("10:00-10:30", citaGenerada.getFranjaHoraria());
    }

    @ParameterizedTest(name = "{0} revisión(es) generan {0} cita(s)")
    @ValueSource(ints = {1, 2, 24})
    @DisplayName("El número de revisiones válido (límites 1 y 24) genera tantas citas como revisiones")
    void prescribirSeguimientoGeneraTantasCitasComoRevisiones(int revisiones) {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-030", "MED-010", "RX-TORAX", 7, revisiones, HOY, "Control");

        assertEquals(revisiones, seg.getCitasGeneradasIds().size());
    }

    @ParameterizedTest(name = "{0} revisiones no es válido")
    @ValueSource(ints = {0, -1, 25, 100})
    @DisplayName("Un número de revisiones fuera de 1 a 24 (límites 0 y 25) lanza excepción")
    void prescribirSeguimientoConRevisionesFueraDeRangoLanzaExcepcion(int revisiones) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, revisiones, HOY, "Indicación"));

        assertEquals("El número de revisiones debe estar comprendido entre 1 y 24", e.getMessage());
    }

    @ParameterizedTest(name = "periodicidad {0} día(s) es válida")
    @ValueSource(ints = {1, 7, 365})
    @DisplayName("Una periodicidad de 1 día o más (límite 1) se acepta")
    void prescribirSeguimientoConPeriodicidadValidaLaAcepta(int dias) {
        assertDoesNotThrow(() ->
                servicio.prescribirSeguimiento("PAC-040", "MED-010", "RX-TORAX", dias, 2, HOY, "Control"));
    }

    @ParameterizedTest(name = "periodicidad {0} día(s) no es válida")
    @ValueSource(ints = {0, -1, -30})
    @DisplayName("Una periodicidad de 0 o menos días (límite 0) lanza excepción")
    void prescribirSeguimientoConPeriodicidadInvalidaLanzaExcepcion(int dias) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", dias, 2, HOY, "Indicación"));

        assertEquals("La periodicidad en días debe ser mayor que cero", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Prescribir con paciente nulo, vacío o en blanco lanza excepción")
    void prescribirSeguimientoConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento(paciente, "MED-1", "RX-TORAX", 30, 2, HOY, "Indicación"));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Prescribir con médico nulo, vacío o en blanco lanza excepción")
    void prescribirSeguimientoConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", medico, "RX-TORAX", 30, 2, HOY, "Indicación"));

        assertEquals("El identificador del médico es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "código de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Prescribir con código de prueba nulo, vacío o en blanco lanza excepción")
    void prescribirSeguimientoConCodigoInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", codigo, 30, 2, HOY, "Indicación"));

        assertEquals("El código de la prueba diagnóstica es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Prescribir sin fecha de inicio lanza excepción")
    void prescribirSeguimientoSinFechaDeInicioLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 2, null, "Indicación"));

        assertEquals("La fecha de inicio del seguimiento es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "indicaciones [{0}] no son válidas")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Prescribir con indicaciones nulas, vacías o en blanco lanza excepción")
    void prescribirSeguimientoConIndicacionesInvalidasLanzaExcepcion(String indicaciones) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 2, HOY, indicaciones));

        assertEquals("Las indicaciones clínicas son obligatorias", e.getMessage());
    }

    @Test
    @DisplayName("Prescribir una prueba inexistente en el catálogo lanza excepción y no crea citas")
    void prescribirSeguimientoConPruebaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.prescribirSeguimiento("PAC-1", "MED-1", "NO-EXISTE", 30, 2, HOY, "Indicación"));

        assertAll(
                () -> assertEquals("No existe ninguna prueba activa con código: NO-EXISTE", e.getMessage()),
                () -> assertTrue(gestionCitasService.listarCitasPaciente("PAC-1").isEmpty()));
    }

    // ---------------------------------------------------------------- consultar y listar

    @Test
    @DisplayName("Consultar un seguimiento existente lo devuelve")
    void consultarSeguimientoExistenteLoDevuelve() {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-010", "MED-020", "ECO-ABDO", 60, 2, LocalDate.of(2026, 12, 5), "Seguimiento hepático");

        assertEquals(seg.getSeguimientoId(), servicio.consultarSeguimiento(seg.getSeguimientoId()).getSeguimientoId());
    }

    @ParameterizedTest(name = "seguimiento [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con identificador nulo, vacío o en blanco lanza excepción")
    void consultarSeguimientoConIdInvalidoLanzaExcepcion(String seguimientoId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarSeguimiento(seguimientoId));

        assertEquals("El identificador del seguimiento es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar un seguimiento inexistente lanza excepción")
    void consultarSeguimientoInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarSeguimiento("SEG-INEXISTENTE"));

        assertEquals("No existe ninguna prescripción de seguimiento con ID: SEG-INEXISTENTE", e.getMessage());
    }

    @Test
    @DisplayName("Listar los seguimientos de un paciente devuelve solo los suyos")
    void listarSeguimientosPacienteDevuelveSoloLosSuyos() {
        servicio.prescribirSeguimiento("PAC-010", "MED-020", "ECO-ABDO", 60, 2, LocalDate.of(2026, 12, 5), "Hepático");
        servicio.prescribirSeguimiento("PAC-011", "MED-020", "ECO-ABDO", 60, 2, LocalDate.of(2026, 12, 6), "Renal");

        assertEquals(1, servicio.listarSeguimientosPaciente("PAC-010").size());
    }

    @Test
    @DisplayName("Listar un paciente sin seguimientos devuelve una lista vacía")
    void listarSeguimientosPacienteSinDatosDevuelveListaVacia() {
        assertTrue(servicio.listarSeguimientosPaciente("PAC-SIN-DATOS").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarSeguimientosPacienteConIdInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarSeguimientosPaciente(paciente));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- cancelarSeguimiento

    @Test
    @DisplayName("Cancelar un seguimiento lo anula y cancela en cascada sus citas")
    void cancelarSeguimientoCancelaCitasAsociadas() {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-020", "MED-030", "RM-CRANEO", 180, 2, LocalDate.of(2026, 12, 15), "Control post-quirúrgico");

        PrescripcionSeguimientoDTO cancelado = servicio.cancelarSeguimiento(seg.getSeguimientoId(), "Alta médica del paciente");

        assertAll(
                () -> assertEquals("CANCELADO", cancelado.getEstado()),
                () -> assertEquals("Alta médica del paciente", cancelado.getMotivoCancelacion()),
                () -> assertEquals(2, cancelado.getCitasGeneradasIds().size()),
                () -> cancelado.getCitasGeneradasIds().forEach(
                        id -> assertEquals("CANCELADA", gestionCitasService.consultarCita(id).getEstado())));
    }

    @Test
    @DisplayName("Cancelar dos veces el mismo seguimiento lanza excepción")
    void cancelarSeguimientoYaCanceladoLanzaExcepcion() {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-020", "MED-030", "RM-CRANEO", 180, 2, LocalDate.of(2026, 12, 15), "Control");
        servicio.cancelarSeguimiento(seg.getSeguimientoId(), "Primer motivo");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.cancelarSeguimiento(seg.getSeguimientoId(), "Duplicado"));

        assertEquals("La prescripción de seguimiento ya se encuentra cancelada", e.getMessage());
    }

    @ParameterizedTest(name = "seguimiento [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Cancelar con identificador nulo, vacío o en blanco lanza excepción")
    void cancelarSeguimientoConIdInvalidoLanzaExcepcion(String seguimientoId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarSeguimiento(seguimientoId, "Motivo"));

        assertEquals("El identificador del seguimiento es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Cancelar con motivo nulo, vacío o en blanco lanza excepción")
    void cancelarSeguimientoConMotivoInvalidoLanzaExcepcion(String motivo) {
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-020", "MED-030", "RM-CRANEO", 180, 2, LocalDate.of(2026, 12, 15), "Control");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cancelarSeguimiento(seg.getSeguimientoId(), motivo));

        assertEquals("El motivo de cancelación es obligatorio", e.getMessage());
    }
}
