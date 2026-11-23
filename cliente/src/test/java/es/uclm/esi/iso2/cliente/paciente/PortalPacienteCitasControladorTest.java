package es.uclm.esi.iso2.cliente.paciente;

import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestorCitasPacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortalPacienteCitasControladorTest {

    private List<CitaDTO> citasSimuladas;
    private PortalPacienteCitasControlador controlador;

    @BeforeEach
    void setUp() {
        citasSimuladas = new ArrayList<>();
        CitaDTO c1 = new CitaDTO("CIT-1", "PAC-01", "MED-01", "RX-TORAX",
                LocalDate.of(2026, 12, 10), "09:00-09:15", "PROGRAMADA", null, LocalDateTime.now());
        citasSimuladas.add(c1);

        IGestorCitasPacienteService stubServicio = new IGestorCitasPacienteService() {
            @Override
            public List<CitaDTO> consultarMisCitas(String pacienteId, String filtroEstado, String tokenSSO) {
                if (filtroEstado == null) return new ArrayList<>(citasSimuladas);
                return citasSimuladas.stream().filter(c -> filtroEstado.equalsIgnoreCase(c.getEstado())).toList();
            }

            @Override
            public CitaDTO consultarDetalleCitaPropia(String pacienteId, String citaId, String tokenSSO) {
                return citasSimuladas.stream().filter(c -> c.getCitaId().equals(citaId)).findFirst().orElseThrow();
            }

            @Override
            public List<TramoHorarioDTO> consultarHuecosDisponiblesCambio(String medicoId, LocalDate fecha, String tokenSSO) {
                return List.of(new TramoHorarioDTO(LocalTime.of(10, 0), LocalTime.of(10, 15), true, null));
            }

            @Override
            public CitaDTO reprogramarCitaPropia(String pacienteId, String citaId, LocalDate nuevaFecha, String nuevaFranja, String tokenSSO) {
                CitaDTO cita = consultarDetalleCitaPropia(pacienteId, citaId, tokenSSO);
                cita.setFecha(nuevaFecha);
                cita.setFranjaHoraria(nuevaFranja);
                cita.setEstado("REPROGRAMADA");
                return cita;
            }

            @Override
            public CitaDTO cancelarCitaPropia(String pacienteId, String citaId, String motivo, String tokenSSO) {
                CitaDTO cita = consultarDetalleCitaPropia(pacienteId, citaId, tokenSSO);
                cita.setEstado("CANCELADA");
                cita.setMotivoCancelacion(motivo);
                return cita;
            }
        };

        controlador = new PortalPacienteCitasControlador(stubServicio, "PAC-01", "token-sso-pac01-PACIENTE-1234");
    }

    @Test
    @DisplayName("Validación de parámetros del constructor del controlador")
    void validarConstructorControlador() {
        assertThrows(IllegalArgumentException.class, () -> new PortalPacienteCitasControlador(null, "PAC-01", "tok"));
        assertThrows(IllegalArgumentException.class, () -> new PortalPacienteCitasControlador(
                new IGestorCitasPacienteService() {
                    @Override public List<CitaDTO> consultarMisCitas(String p, String f, String t) { return List.of(); }
                    @Override public CitaDTO consultarDetalleCitaPropia(String p, String c, String t) { return null; }
                    @Override public List<TramoHorarioDTO> consultarHuecosDisponiblesCambio(String m, LocalDate f, String t) { return List.of(); }
                    @Override public CitaDTO reprogramarCitaPropia(String p, String c, LocalDate nf, String nfr, String t) { return null; }
                    @Override public CitaDTO cancelarCitaPropia(String p, String c, String m, String t) { return null; }
                }, " ", "tok"));
        assertThrows(IllegalArgumentException.class, () -> new PortalPacienteCitasControlador(
                new IGestorCitasPacienteService() {
                    @Override public List<CitaDTO> consultarMisCitas(String p, String f, String t) { return List.of(); }
                    @Override public CitaDTO consultarDetalleCitaPropia(String p, String c, String t) { return null; }
                    @Override public List<TramoHorarioDTO> consultarHuecosDisponiblesCambio(String m, LocalDate f, String t) { return List.of(); }
                    @Override public CitaDTO reprogramarCitaPropia(String p, String c, LocalDate nf, String nfr, String t) { return null; }
                    @Override public CitaDTO cancelarCitaPropia(String p, String c, String m, String t) { return null; }
                }, "PAC-01", ""));
    }

    @Test
    @DisplayName("Operaciones del portal del paciente funcionan y formatean salida")
    void operacionesControladorPaciente() {
        assertEquals(1, controlador.obtenerTodasMisCitas().size());
        assertEquals(1, controlador.obtenerMisCitasPorEstado("PROGRAMADA").size());
        assertEquals("CIT-1", controlador.verDetalleCita("CIT-1").getCitaId());
        assertEquals(1, controlador.buscarHuecosParaCambio("MED-01", LocalDate.of(2026, 12, 11)).size());
        assertTrue(controlador.formatearResumenCitas().contains("CIT-1"));

        CitaDTO rep = controlador.solicitarReprogramacion("CIT-1", LocalDate.of(2026, 12, 12), "11:00-11:15");
        assertEquals("REPROGRAMADA", rep.getEstado());

        CitaDTO canc = controlador.solicitarCancelacion("CIT-1", "Ya no puedo asistir");
        assertEquals("CANCELADA", canc.getEstado());

        citasSimuladas.clear();
        assertEquals("No tienes citas radiológicas registradas actualmente.", controlador.formatearResumenCitas());
    }
}