package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaSeguimientoPacienteServiceImplTest {

    private Map<String, PrescripcionSeguimientoDTO> datosStub;
    private ConsultaSeguimientoPacienteServiceImpl servicio;

    @BeforeEach
    void setUp() {
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

        servicio = new ConsultaSeguimientoPacienteServiceImpl(stubPrescripciones, new AuditServiceImpl());
    }

    @Test
    @DisplayName("Listar mis seguimientos devuelve tanto los activos como los cancelados del paciente")
    void listarMisSeguimientosDevuelveTodosLosDelPaciente() {
        // Ejecutar
        List<PrescripcionSeguimientoDTO> resultado = servicio.listarMisSeguimientos("PAC-001");

        // Comprobar
        assertEquals(2, resultado.size());
        assertTrue(servicio.listarMisSeguimientos("PAC-SIN-SEGUIMIENTOS").isEmpty());
    }

    @Test
    @DisplayName("Listar mis seguimientos activos filtra únicamente las prescripciones en estado ACTIVO")
    void listarMisSeguimientosActivosFiltraCancelados() {
        // Ejecutar
        List<PrescripcionSeguimientoDTO> activos = servicio.listarMisSeguimientosActivos("PAC-001");

        // Comprobar
        assertEquals(1, activos.size());
        assertEquals("SEG-101", activos.get(0).getSeguimientoId());
        assertEquals("ACTIVO", activos.get(0).getEstado());
    }

    @Test
    @DisplayName("Consultar detalle de seguimiento propio devuelve la pauta y sus citas asociadas")
    void consultarDetalleMiSeguimientoPropioFunciona() {
        // Ejecutar
        PrescripcionSeguimientoDTO detalle = servicio.consultarDetalleMiSeguimiento("SEG-101", "PAC-001");

        // Comprobar
        assertNotNull(detalle);
        assertEquals("TAC-ABDO", detalle.getCodigoPrueba());
        assertEquals(3, detalle.getCitasGeneradasIds().size());
    }

    @Test
    @DisplayName("Consultar detalle de seguimiento de otro paciente lanza SecurityException")
    void consultarDetalleSeguimientoAjenoDeniegaAcceso() {
        // Ejecutar y Comprobar
        assertThrows(SecurityException.class,
                () -> servicio.consultarDetalleMiSeguimiento("SEG-201", "PAC-001"));
    }

    @Test
    @DisplayName("Validación de parámetros inválidos, nulos e inexistentes lanza las excepciones esperadas")
    void validacionParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ConsultaSeguimientoPacienteServiceImpl(null));

        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisSeguimientos(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisSeguimientos(" "));

        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisSeguimientosActivos(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarMisSeguimientosActivos(" "));

        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetalleMiSeguimiento(null, "PAC-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetalleMiSeguimiento(" ", "PAC-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetalleMiSeguimiento("SEG-101", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetalleMiSeguimiento("SEG-101", " "));

        assertThrows(IllegalStateException.class, () -> servicio.consultarDetalleMiSeguimiento("SEG-NO-EXISTE", "PAC-001"));
    }
}