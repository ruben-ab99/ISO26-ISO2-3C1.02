package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las consultas de solo lectura por periodo añadidas en la It7 para CU-16 (ADR-017):
 * IGestionCitasService.listarCitasEntreFechas e IGestorAgendasService.listarAgendasEntreFechas.
 * Técnicas: valores límite en los extremos del periodo y clases de equivalencia de argumentos y roles.
 */
class ConsultasPeriodoCitasAgendasTest {

    private static final String TOKEN_ADMISION = "token-sso-adm01-ADMISION-1";
    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1";
    private static final LocalDate DIA_1 = LocalDate.of(2026, 12, 1);
    private static final LocalDate DIA_5 = LocalDate.of(2026, 12, 5);
    private static final LocalDate DIA_10 = LocalDate.of(2026, 12, 10);

    @Test
    @DisplayName("Las citas del periodo incluyen los dos extremos")
    void citasEntreFechasIncluyeExtremos() {
        GestionCitasServiceImpl citas = new GestionCitasServiceImpl();
        citas.programarCita("PAC-1", "MED-1", "RX-TORAX", DIA_1, "09:00-09:30");
        citas.programarCita("PAC-2", "MED-1", "RX-TORAX", DIA_5, "10:00-10:30");
        citas.programarCita("PAC-3", "MED-2", "TAC-ABDO", DIA_10, "11:00-11:30");

        assertEquals(2, citas.listarCitasEntreFechas(DIA_1, DIA_5).size());
        assertEquals(2, citas.listarCitasEntreFechas(DIA_5, DIA_10).size());
        assertTrue(citas.listarCitasEntreFechas(DIA_1.plusDays(1), DIA_5.minusDays(1)).isEmpty());
        assertEquals(1, citas.listarCitasEntreFechas(DIA_10, DIA_10).size());
    }

    @Test
    @DisplayName("Las citas del periodo exigen fechas válidas y token de sesión")
    void citasEntreFechasValidaArgumentosYToken() {
        GestionCitasServiceImpl citas = new GestionCitasServiceImpl();
        GestionCitasServiceImpl sinToken = new GestionCitasServiceImpl((p, c, t, d) -> {}, new GestorRolesServiceImpl(), "  ");

        assertThrows(IllegalArgumentException.class, () -> citas.listarCitasEntreFechas(null, DIA_5));
        assertThrows(IllegalArgumentException.class, () -> citas.listarCitasEntreFechas(DIA_1, null));
        assertThrows(IllegalArgumentException.class, () -> citas.listarCitasEntreFechas(DIA_5, DIA_1));
        assertThrows(SecurityException.class, () -> sinToken.listarCitasEntreFechas(DIA_1, DIA_5));
    }

    @Test
    @DisplayName("Las agendas del periodo se devuelven ordenadas por fecha y médico")
    void agendasEntreFechasOrdenadas() {
        GestorAgendasServiceImpl agendas = new GestorAgendasServiceImpl(new GestorRolesServiceImpl());
        agendas.configurarAgendaDiaria("MED-B", DIA_1.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 30, TOKEN_ADMISION);
        agendas.configurarAgendaDiaria("MED-A", DIA_1.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 30, TOKEN_ADMISION);
        agendas.configurarAgendaDiaria("MED-A", DIA_1, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, TOKEN_ADMISION);
        agendas.configurarAgendaDiaria("MED-A", DIA_10, LocalTime.of(9, 0), LocalTime.of(10, 0), 30, TOKEN_ADMISION);

        List<AgendaDiariaDTO> resultado = agendas.listarAgendasEntreFechas(DIA_1, DIA_1.plusDays(1), TOKEN_ADMISION);

        assertEquals(3, resultado.size());
        assertEquals(DIA_1, resultado.get(0).getFecha());
        assertEquals("MED-A", resultado.get(1).getMedicoId());
        assertEquals("MED-B", resultado.get(2).getMedicoId());
        assertEquals(2, resultado.get(0).getTramos().size());
    }

    @Test
    @DisplayName("Las agendas del periodo exigen rol de agenda y fechas válidas")
    void agendasEntreFechasValidaRolYArgumentos() {
        GestorAgendasServiceImpl agendas = new GestorAgendasServiceImpl(new GestorRolesServiceImpl());

        assertThrows(SecurityException.class, () -> agendas.listarAgendasEntreFechas(DIA_1, DIA_5, TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () -> agendas.listarAgendasEntreFechas(DIA_1, DIA_5, null));
        assertThrows(IllegalArgumentException.class, () -> agendas.listarAgendasEntreFechas(null, DIA_5, TOKEN_ADMISION));
        assertThrows(IllegalArgumentException.class, () -> agendas.listarAgendasEntreFechas(DIA_1, null, TOKEN_ADMISION));
        assertThrows(IllegalArgumentException.class, () -> agendas.listarAgendasEntreFechas(DIA_5, DIA_1, TOKEN_ADMISION));
    }

    @Test
    @DisplayName("Una implementación que no sobrescribe las consultas devuelve listas vacías (métodos default)")
    void metodosPorDefectoDevuelvenListasVacias() {
        IGestionCitasService citasMinimas = new IGestionCitasService() {
            @Override
            public CitaDTO programarCita(String pacienteId, String medicoId, String codigoPrueba, LocalDate fecha, String franjaHoraria) {
                return null;
            }

            @Override
            public CitaDTO consultarCita(String citaId) {
                return null;
            }

            @Override
            public List<CitaDTO> listarCitasPaciente(String pacienteId) {
                return List.of();
            }

            @Override
            public CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
                return null;
            }

            @Override
            public CitaDTO cancelarCita(String citaId, String motivo) {
                return null;
            }
        };
        IGestorAgendasService agendasMinimas = new IGestorAgendasService() {
            @Override
            public AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                                          LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO) {
                return null;
            }

            @Override
            public AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO) {
                return null;
            }

            @Override
            public boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicioTramo, LocalTime horaFinTramo,
                                               String motivoBloqueo, String tokenSSO) {
                return false;
            }

            @Override
            public boolean cerrarAgendaDiaria(String agendaId, String tokenSSO) {
                return false;
            }
        };

        assertTrue(citasMinimas.listarCitasEntreFechas(DIA_1, DIA_5).isEmpty());
        assertTrue(agendasMinimas.listarAgendasEntreFechas(DIA_1, DIA_5, TOKEN_ADMISION).isEmpty());
    }
}
