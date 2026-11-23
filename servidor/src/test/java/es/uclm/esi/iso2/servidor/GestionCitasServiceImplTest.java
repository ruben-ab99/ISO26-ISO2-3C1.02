package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestionCitasServiceImplTest {

    private GestionCitasServiceImpl servicio;
    private List<String> eventosNotificados;

    @BeforeEach
    void setUp() {
        eventosNotificados = new ArrayList<>();
        servicio = new GestionCitasServiceImpl((pacienteId, citaId, tipoEvento, detalle) ->
                eventosNotificados.add(tipoEvento + ":" + pacienteId + ":" + citaId)
        );
    }

    @Test
    @DisplayName("Constructores validan notificador nulo y permiten instancia por defecto")
    void constructoresValidacion() {
        assertThrows(IllegalArgumentException.class, () -> new GestionCitasServiceImpl(null));
        assertThrows(IllegalArgumentException.class, () -> new GestionCitasServiceImpl((p, c, t, d) -> {}, null, "tok"));
        GestionCitasServiceImpl porDefecto = new GestionCitasServiceImpl();
        CitaDTO cita = porDefecto.programarCita("PAC-1", "MED-1", "RX-TORAX", LocalDate.of(2026, 12, 1), "09:00-09:15");
        assertEquals("PROGRAMADA", cita.getEstado());
    }

    @Test
    @DisplayName("Usuario sin rol ADMISION ni MEDICO recibe SecurityException en gestión de citas")
    void usuarioSinPermisoLanzaSecurityException() {
        GestorRolesServiceImpl roles = new GestorRolesServiceImpl();
        GestionCitasServiceImpl servicioSinPermiso = new GestionCitasServiceImpl(
                (p, c, t, d) -> {}, roles, "token-sso-pac01-PACIENTE-1234"
        );
        GestionCitasServiceImpl servicioSinToken = new GestionCitasServiceImpl(
                (p, c, t, d) -> {}, roles, ""
        );

        LocalDate fecha = LocalDate.of(2026, 12, 10);
        assertThrows(SecurityException.class, () ->
                servicioSinPermiso.programarCita("PAC-01", "MED-01", "RX", fecha, "10:00"));
        assertThrows(SecurityException.class, () ->
                servicioSinPermiso.reprogramarCita("CIT-1", fecha, "11:00"));
        assertThrows(SecurityException.class, () ->
                servicioSinPermiso.cancelarCita("CIT-1", "Motivo"));
        assertThrows(SecurityException.class, () ->
                servicioSinToken.consultarCita("CIT-1"));
        assertThrows(SecurityException.class, () ->
                servicioSinToken.listarCitasPaciente("PAC-01"));
    }

    @Test
    @DisplayName("Programar cita válida crea el registro, notifica y bloquea franja duplicada")
    void programarCitaValidaYConflictoHorario() {
        LocalDate fecha = LocalDate.of(2026, 12, 10);

        CitaDTO cita = servicio.programarCita("PAC-01", "MED-01", "tac-abdo", fecha, "10:00-10:30");

        assertNotNull(cita);
        assertTrue(cita.getCitaId().startsWith("CIT-"));
        assertEquals("TAC-ABDO", cita.getCodigoPrueba());
        assertEquals("PROGRAMADA", cita.getEstado());
        assertNotNull(cita.getFechaUltimaActualizacion());
        assertEquals(1, eventosNotificados.size());
        assertTrue(eventosNotificados.get(0).startsWith("PROGRAMADA:PAC-01:"));

        assertThrows(IllegalStateException.class, () ->
                servicio.programarCita("PAC-02", "MED-01", "RX-TORAX", fecha, "10:00-10:30"));
    }

    @Test
    @DisplayName("Programar cita con datos obligatorios nulos o vacíos lanza excepción")
    void programarCitaDatosInvalidos() {
        LocalDate fecha = LocalDate.of(2026, 12, 10);
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita(null, "MED-1", "RX", fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita(" ", "MED-1", "RX", fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", null, "RX", fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "", "RX", fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "MED-1", null, fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "MED-1", "  ", fecha, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "MED-1", "RX", null, "09:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "MED-1", "RX", fecha, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.programarCita("PAC-1", "MED-1", "RX", fecha, " "));
    }

    @Test
    @DisplayName("Consultar y listar citas de paciente devuelve resultados esperados y valida errores")
    void consultarYListarCitas() {
        LocalDate fecha = LocalDate.of(2026, 12, 12);
        CitaDTO c1 = servicio.programarCita("PAC-01", "MED-01", "RX-1", fecha, "09:00-09:15");
        servicio.programarCita("PAC-01", "MED-02", "ECO-1", fecha, "11:00-11:20");

        assertEquals(c1.getCitaId(), servicio.consultarCita(c1.getCitaId()).getCitaId());
        assertEquals(2, servicio.listarCitasPaciente("PAC-01").size());
        assertTrue(servicio.listarCitasPaciente("PAC-SIN-CITAS").isEmpty());

        assertThrows(IllegalArgumentException.class, () -> servicio.consultarCita(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarCita("  "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarCita("CIT-INEXISTENTE"));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarCitasPaciente(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarCitasPaciente(" "));
    }

    @Test
    @DisplayName("Reprogramar cita actualiza fecha, franja, estado y valida conflictos")
    void reprogramarCitaValidaEInvalida() {
        LocalDate fecha1 = LocalDate.of(2026, 12, 15);
        LocalDate fecha2 = LocalDate.of(2026, 12, 16);

        CitaDTO c1 = servicio.programarCita("PAC-01", "MED-01", "RM-CRANEO", fecha1, "10:00-10:45");
        servicio.programarCita("PAC-02", "MED-01", "RM-CRANEO", fecha2, "12:00-12:45");

        assertThrows(IllegalStateException.class, () ->
                servicio.reprogramarCita(c1.getCitaId(), fecha2, "12:00-12:45"));

        CitaDTO reprogramada = servicio.reprogramarCita(c1.getCitaId(), fecha2, "16:00-16:45");
        assertEquals("REPROGRAMADA", reprogramada.getEstado());
        assertEquals(fecha2, reprogramada.getFecha());
        assertEquals("16:00-16:45", reprogramada.getFranjaHoraria());
        assertTrue(eventosNotificados.get(eventosNotificados.size() - 1).startsWith("REPROGRAMADA:PAC-01:"));

        assertThrows(IllegalArgumentException.class, () -> servicio.reprogramarCita(null, fecha2, "17:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.reprogramarCita(c1.getCitaId(), null, "17:00"));
        assertThrows(IllegalArgumentException.class, () -> servicio.reprogramarCita(c1.getCitaId(), fecha2, " "));
    }

    @Test
    @DisplayName("Cancelar cita libera la franja horaria y bloquea modificaciones posteriores")
    void cancelarCitaValidaEInvalida() {
        LocalDate fecha = LocalDate.of(2026, 12, 20);
        CitaDTO c1 = servicio.programarCita("PAC-01", "MED-01", "TAC-ABDO", fecha, "09:00-09:30");

        CitaDTO cancelada = servicio.cancelarCita(c1.getCitaId(), "Solicitud del paciente");
        assertEquals("CANCELADA", cancelada.getEstado());
        assertEquals("Solicitud del paciente", cancelada.getMotivoCancelacion());
        assertTrue(eventosNotificados.get(eventosNotificados.size() - 1).startsWith("CANCELADA:PAC-01:"));

        assertThrows(IllegalStateException.class, () -> servicio.cancelarCita(c1.getCitaId(), "Otro motivo"));
        assertThrows(IllegalStateException.class, () -> servicio.reprogramarCita(c1.getCitaId(), fecha, "10:00-10:30"));

        CitaDTO nuevaEnFranjaLiberada = servicio.programarCita("PAC-03", "MED-01", "TAC-ABDO", fecha, "09:00-09:30");
        assertEquals("PROGRAMADA", nuevaEnFranjaLiberada.getEstado());

        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarCita(null, "Motivo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarCita(c1.getCitaId(), null));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarCita(c1.getCitaId(), "  "));
    }
}