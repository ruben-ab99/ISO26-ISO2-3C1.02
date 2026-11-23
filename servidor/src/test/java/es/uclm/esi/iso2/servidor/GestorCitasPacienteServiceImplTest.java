package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;

class GestorCitasPacienteServiceImplTest {

    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1234";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-9999";
    private static final String TOKEN_ADMIN = "token-sso-adm01-ADMISION-0001";

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

    @Test
    @DisplayName("Validación de constructores")
    void validarConstructores() {
        assertDoesNotThrow(() -> new GestorCitasPacienteServiceImpl());
        assertThrows(IllegalArgumentException.class, () -> new GestorCitasPacienteServiceImpl(null, gestionCitas, gestorAgendas));
        assertThrows(IllegalArgumentException.class, () -> new GestorCitasPacienteServiceImpl(gestorRoles, null, gestorAgendas));
        assertThrows(IllegalArgumentException.class, () -> new GestorCitasPacienteServiceImpl(gestorRoles, gestionCitas, null));
    }

    @Test
    @DisplayName("Seguridad: rechaza tokens vacíos o de otros roles")
    void seguridadRolesYTokens() {
        assertThrows(SecurityException.class, () -> servicioPaciente.consultarMisCitas("PAC-01", null, null));
        assertThrows(SecurityException.class, () -> servicioPaciente.consultarMisCitas("PAC-01", null, " "));
        assertThrows(SecurityException.class, () -> servicioPaciente.consultarMisCitas("PAC-01", null, TOKEN_MEDICO));
    }

    @Test
    @DisplayName("Consultar citas propias con y sin filtro de estado")
    void consultarMisCitasConYFiltro() {
        LocalDate fecha = LocalDate.of(2026, 12, 10);
        CitaDTO c1 = gestionCitas.programarCita("PAC-01", "MED-01", "RX-TORAX", fecha, "09:00-09:15");
        CitaDTO c2 = gestionCitas.programarCita("PAC-01", "MED-01", "TAC-ABDO", fecha, "10:00-10:30");
        gestionCitas.cancelarCita(c2.getCitaId(), "Motivo prueba");

        assertEquals(2, servicioPaciente.consultarMisCitas("PAC-01", null, TOKEN_PACIENTE).size());
        assertEquals(1, servicioPaciente.consultarMisCitas("PAC-01", "PROGRAMADA", TOKEN_PACIENTE).size());
        assertEquals(1, servicioPaciente.consultarMisCitas("PAC-01", "cancelada", TOKEN_PACIENTE).size());
        assertEquals(c1.getCitaId(), servicioPaciente.consultarDetalleCitaPropia("PAC-01", c1.getCitaId(), TOKEN_PACIENTE).getCitaId());

        assertThrows(IllegalArgumentException.class, () -> servicioPaciente.consultarMisCitas("", null, TOKEN_PACIENTE));
        assertThrows(IllegalArgumentException.class, () -> servicioPaciente.consultarMisCitas("PAC-01", "INVENTADO", TOKEN_PACIENTE));
    }

    @Test
    @DisplayName("Seguridad: impide acceder o modificar citas de otro paciente")
    void seguridadPropiedadCita() {
        LocalDate fecha = LocalDate.of(2026, 12, 11);
        CitaDTO citaAjena = gestionCitas.programarCita("PAC-99", "MED-01", "RM-CRANEO", fecha, "11:00-11:30");

        assertThrows(SecurityException.class, () ->
                servicioPaciente.consultarDetalleCitaPropia("PAC-01", citaAjena.getCitaId(), TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", citaAjena.getCitaId(), fecha.plusDays(1), "12:00-12:30", TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () ->
                servicioPaciente.cancelarCitaPropia("PAC-01", citaAjena.getCitaId(), "No es mía", TOKEN_PACIENTE));
        assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.consultarDetalleCitaPropia("PAC-01", " ", TOKEN_PACIENTE));
    }

    @Test
    @DisplayName("Consulta de huecos disponibles para cambio de cita")
    void consultarHuecosDisponibles() {
        LocalDate fecha = LocalDate.of(2026, 12, 12);
        AgendaDiariaDTO agenda = gestorAgendas.configurarAgendaDiaria(
                "MED-01", fecha, LocalTime.of(10, 0), LocalTime.of(11, 0), 30, TOKEN_MEDICO
        );

        List<TramoHorarioDTO> libres = servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", fecha, TOKEN_PACIENTE);
        assertEquals(2, libres.size());
        assertTrue(libres.get(0).isDisponible());

        gestorAgendas.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMIN);
        assertTrue(servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", fecha, TOKEN_PACIENTE).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> servicioPaciente.consultarHuecosDisponiblesCambio(null, fecha, TOKEN_PACIENTE));
        assertThrows(IllegalArgumentException.class, () -> servicioPaciente.consultarHuecosDisponiblesCambio("MED-01", null, TOKEN_PACIENTE));
    }

    @Test
    @DisplayName("Reprogramar y cancelar cita propia cumple ciclo de vida")
    void reprogramarYCancelarCitaPropia() {
        LocalDate fecha = LocalDate.of(2026, 12, 15);
        CitaDTO cita = gestionCitas.programarCita("PAC-01", "MED-01", "ECO-ABDO", fecha, "09:00-09:20");

        assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), null, "10:00-10:20", TOKEN_PACIENTE));
        assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), fecha.plusDays(1), " ", TOKEN_PACIENTE));

        CitaDTO reprogramada = servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), fecha.plusDays(1), "10:00-10:20", TOKEN_PACIENTE);
        assertEquals("REPROGRAMADA", reprogramada.getEstado());

        assertThrows(IllegalArgumentException.class, () ->
                servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), " ", TOKEN_PACIENTE));

        CitaDTO cancelada = servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Motivo personal", TOKEN_PACIENTE);
        assertEquals("CANCELADA", cancelada.getEstado());

        assertThrows(IllegalStateException.class, () ->
                servicioPaciente.cancelarCitaPropia("PAC-01", cita.getCitaId(), "Otro motivo", TOKEN_PACIENTE));
        assertThrows(IllegalStateException.class, () ->
                servicioPaciente.reprogramarCitaPropia("PAC-01", cita.getCitaId(), fecha.plusDays(2), "11:00-11:20", TOKEN_PACIENTE));
    }
}