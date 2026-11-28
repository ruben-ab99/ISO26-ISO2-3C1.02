package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-05 (Consultar pruebas de seguimiento y revisiones periódicas).
 * Técnicas: clases de equivalencia (paciente con / sin seguimientos, seguimiento propio / ajeno /
 * inexistente, estado ACTIVO / CANCELADO) y criterio each use entre parámetros.
 * El servicio de prescripciones se sustituye por un stub (ADR-012).
 */
class ConsultaSeguimientoPacienteServiceImplTest {

    private Map<String, PrescripcionSeguimientoDTO> datosStub;
    private AuditServiceImpl auditoria;
    private ConsultaSeguimientoPacienteServiceImpl servicio;

    @BeforeEach
    void setUp() {
        auditoria = new AuditServiceImpl();
        datosStub = new LinkedHashMap<>();

        PrescripcionSeguimientoDTO segActivo = new PrescripcionSeguimientoDTO(
                "SEG-101", "PAC-001", "MED-010", "TAC-ABDO", "TAC Abdominal con Contraste IV",
                90, 3, LocalDate.of(2026, 12, 1), "Control trimestral de nódulo",
                "ACTIVO", "", List.of("CIT-001", "CIT-002", "CIT-003"), LocalDateTime.now()
        );
        PrescripcionSeguimientoDTO segCancelado = new PrescripcionSeguimientoDTO(
                "SEG-102", "PAC-001", "MED-010", "RX-TORAX", "Radiografía de Tórax PA y Lateral",
                180, 2, LocalDate.of(2026, 10, 1), "Revisión semestral",
                "CANCELADO", "Alta clínica", List.of("CIT-004"), LocalDateTime.now()
        );
        PrescripcionSeguimientoDTO segOtroPaciente = new PrescripcionSeguimientoDTO(
                "SEG-201", "PAC-999", "MED-020", "RM-CRANEO", "Resonancia Magnética Craneal",
                60, 2, LocalDate.of(2026, 12, 15), "Cribado neurológico",
                "ACTIVO", "", List.of("CIT-005"), LocalDateTime.now()
        );

        datosStub.put(segActivo.getSeguimientoId(), segActivo);
        datosStub.put(segCancelado.getSeguimientoId(), segCancelado);
        datosStub.put(segOtroPaciente.getSeguimientoId(), segOtroPaciente);

        IPrescripcionSeguimientoService stubPrescripciones = new IPrescripcionSeguimientoService() {
            @Override
            public PrescripcionSeguimientoDTO prescribirSeguimiento(String pacienteId, String medicoId,
                                                                    String codigoPrueba, int periodicidadDias,
                                                                    int numeroRevisiones, LocalDate fechaInicio,
                                                                    String indicacionesClinicas) {
                return null;
            }

            @Override
            public PrescripcionSeguimientoDTO consultarSeguimiento(String seguimientoId) {
                PrescripcionSeguimientoDTO encontrado = datosStub.get(seguimientoId);
                if (encontrado == null) {
                    throw new IllegalStateException("No existe seguimiento con ID: " + seguimientoId);
                }
                return encontrado;
            }

            @Override
            public List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String pacienteId) {
                List<PrescripcionSeguimientoDTO> lista = new ArrayList<>();
                for (PrescripcionSeguimientoDTO s : datosStub.values()) {
                    if (s.getPacienteId().equals(pacienteId)) {
                        lista.add(s);
                    }
                }
                return lista;
            }

            @Override
            public PrescripcionSeguimientoDTO cancelarSeguimiento(String seguimientoId, String motivo) {
                return null;
            }
        };

        servicio = new ConsultaSeguimientoPacienteServiceImpl(stubPrescripciones, auditoria);
    }

    private static final String MSG_PACIENTE = "El identificador del paciente es obligatorio";

    @Test
    @DisplayName("El constructor rechaza un servicio de prescripciones nulo")
    void constructorConPrescripcionesNulasLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ConsultaSeguimientoPacienteServiceImpl(null));

        assertEquals("El servicio de prescripciones de seguimiento es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- listarMisSeguimientos

    @Test
    @DisplayName("Listar mis seguimientos devuelve los activos y los cancelados del paciente")
    void listarMisSeguimientosDevuelveTodosLosDelPaciente() {
        List<PrescripcionSeguimientoDTO> resultado = servicio.listarMisSeguimientos("PAC-001");

        assertEquals(2, resultado.size());
        resultado.forEach(s -> assertEquals("PAC-001", s.getPacienteId()));
    }

    @Test
    @DisplayName("Listar los seguimientos de un paciente sin seguimientos devuelve una lista vacía")
    void listarMisSeguimientosSinSeguimientosDevuelveListaVacia() {
        assertTrue(servicio.listarMisSeguimientos("PAC-SIN-SEGUIMIENTOS").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar seguimientos con paciente nulo, vacío o en blanco lanza excepción")
    void listarMisSeguimientosConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarMisSeguimientos(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    // ---------------------------------------------------------------- listarMisSeguimientosActivos

    @Test
    @DisplayName("Listar mis seguimientos activos filtra solo los que están en estado ACTIVO")
    void listarMisSeguimientosActivosExcluyeLosCancelados() {
        List<PrescripcionSeguimientoDTO> activos = servicio.listarMisSeguimientosActivos("PAC-001");

        assertAll(
                () -> assertEquals(1, activos.size()),
                () -> assertEquals("SEG-101", activos.get(0).getSeguimientoId()),
                () -> assertEquals("ACTIVO", activos.get(0).getEstado()));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar activos con paciente nulo, vacío o en blanco lanza excepción")
    void listarMisSeguimientosActivosConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarMisSeguimientosActivos(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    // ---------------------------------------------------------------- consultarDetalleMiSeguimiento

    @Test
    @DisplayName("Consultar el detalle de un seguimiento propio devuelve la pauta y sus citas")
    void consultarDetalleMiSeguimientoPropioDevuelveLaPauta() {
        PrescripcionSeguimientoDTO detalle = servicio.consultarDetalleMiSeguimiento("SEG-101", "PAC-001");

        assertAll(
                () -> assertNotNull(detalle),
                () -> assertEquals("TAC-ABDO", detalle.getCodigoPrueba()),
                () -> assertEquals(3, detalle.getCitasGeneradasIds().size()));
    }

    @Test
    @DisplayName("Consultar el detalle de un seguimiento propio se registra en la auditoría")
    void consultarDetalleMiSeguimientoPropioSeAudita() {
        servicio.consultarDetalleMiSeguimiento("SEG-101", "PAC-001");

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("CONSULTAR_DETALLE_SEGUIMIENTO:SEG-101", traza.getAccion()),
                () -> assertTrue(traza.isExito()));
    }

    @Test
    @DisplayName("Consultar el seguimiento de otro paciente lanza SecurityException y se audita como denegado")
    void consultarDetalleSeguimientoAjenoDeniegaAccesoYAudita() {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.consultarDetalleMiSeguimiento("SEG-201", "PAC-001"));

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("Acceso denegado: la prescripción de seguimiento no pertenece al paciente", e.getMessage()),
                () -> assertEquals("ACCESO_DENEGADO_SEGUIMIENTO:SEG-201", traza.getAccion()),
                () -> assertFalse(traza.isExito()));
    }

    @ParameterizedTest(name = "seguimiento [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con identificador de seguimiento nulo, vacío o en blanco lanza excepción")
    void consultarDetalleConSeguimientoInvalidoLanzaExcepcion(String seguimientoId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDetalleMiSeguimiento(seguimientoId, "PAC-001"));

        assertEquals("El identificador del seguimiento es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con paciente nulo, vacío o en blanco lanza excepción")
    void consultarDetalleConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDetalleMiSeguimiento("SEG-101", paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @Test
    @DisplayName("Consultar un seguimiento inexistente lanza excepción")
    void consultarDetalleSeguimientoInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarDetalleMiSeguimiento("SEG-NO-EXISTE", "PAC-001"));

        assertEquals("No existe seguimiento con ID: SEG-NO-EXISTE", e.getMessage());
    }
}
