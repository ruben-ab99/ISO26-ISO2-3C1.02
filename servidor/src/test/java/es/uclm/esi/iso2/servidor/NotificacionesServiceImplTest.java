package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.NotificacionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificacionesServiceImplTest {

    private NotificacionesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionesServiceImpl();
    }

    @Test
    @DisplayName("Usuario sin rol ADMISION ni MEDICO recibe SecurityException al enviar notificaciones")
    void usuarioSinPermisoLanzaSecurityException() {
        GestorRolesServiceImpl roles = new GestorRolesServiceImpl();
        NotificacionesServiceImpl servicioPaciente = new NotificacionesServiceImpl(roles, "token-sso-pac01-PACIENTE-1234");
        NotificacionesServiceImpl servicioSinToken = new NotificacionesServiceImpl(roles, "");

        assertThrows(IllegalArgumentException.class, () -> new NotificacionesServiceImpl(null, "tok"));
        assertThrows(SecurityException.class, () ->
                servicioPaciente.enviarNotificacion("PAC-01", "CITA", "Aviso", "Detalle", "C-1"));
        assertThrows(SecurityException.class, () ->
                servicioSinToken.listarNotificacionesPaciente("PAC-01"));
        assertThrows(SecurityException.class, () ->
                servicioSinToken.marcarComoLeida("NOT-1", "PAC-01"));
    }

    @Test
    @DisplayName("Enviar notificaciones válidas de cita, preparación e informe las crea sin leer")
    void enviarNotificacionesValidas() {
        NotificacionDTO avisoCita = servicio.enviarNotificacion(
                "PAC-01", "cita", "Nueva cita programada", "Tiene cita el lunes a las 10:00", "CITA-100"
        );
        NotificacionDTO avisoPrep = servicio.enviarNotificacion(
                "PAC-01", "PREPARACION_PREVIA", "Recordatorio ayuno", "Acuda en ayunas de 6 horas", "CITA-100"
        );
        NotificacionDTO avisoInf = servicio.enviarNotificacion(
                "PAC-02", "INFORME_DISPONIBLE", "Informe listo", "Ya puede consultar su informe", null
        );

        assertNotNull(avisoCita);
        assertTrue(avisoCita.getId().startsWith("NOT-"));
        assertEquals("CITA", avisoCita.getTipoAviso());
        assertEquals("CITA-100", avisoCita.getReferenciaId());
        assertFalse(avisoCita.isLeida());
        assertNotNull(avisoCita.getFechaEnvio());

        assertEquals("PREPARACION_PREVIA", avisoPrep.getTipoAviso());
        assertEquals("", avisoInf.getReferenciaId());
    }

    @Test
    @DisplayName("Enviar notificación con parámetros nulos, vacíos o tipo no soportado lanza excepción")
    void enviarNotificacionInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion(null, "CITA", "T", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("   ", "CITA", "T", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", null, "T", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", " ", "T", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", "TIPO_FALSO", "T", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", "CITA", null, "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", "CITA", "", "M", "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", "CITA", "T", null, "R"));
        assertThrows(IllegalArgumentException.class, () -> servicio.enviarNotificacion("PAC-1", "CITA", "T", "  ", "R"));
    }

    @Test
    @DisplayName("Listar notificaciones devuelve únicamente las del paciente solicitado")
    void listarNotificacionesPaciente() {
        servicio.enviarNotificacion("PAC-01", "CITA", "Cita 1", "Mensaje 1", "C-1");
        servicio.enviarNotificacion("PAC-01", "PREPARACION_PREVIA", "Prep 1", "Mensaje 2", "C-1");
        servicio.enviarNotificacion("PAC-02", "INFORME_DISPONIBLE", "Inf 1", "Mensaje 3", "I-1");

        List<NotificacionDTO> listaPac1 = servicio.listarNotificacionesPaciente("PAC-01");
        List<NotificacionDTO> listaPacSinAvisos = servicio.listarNotificacionesPaciente("PAC-99");

        assertEquals(2, listaPac1.size());
        assertTrue(listaPacSinAvisos.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> servicio.listarNotificacionesPaciente(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarNotificacionesPaciente("   "));
    }

    @Test
    @DisplayName("Marcar como leída actualiza el estado y valida pertenencia y existencia")
    void marcarComoLeidaValidaEInvalida() {
        NotificacionDTO creada = servicio.enviarNotificacion("PAC-01", "CITA", "Cita", "Detalle", "C-10");

        NotificacionDTO leida = servicio.marcarComoLeida(creada.getId(), "PAC-01");
        assertTrue(leida.isLeida());

        assertThrows(IllegalArgumentException.class, () -> servicio.marcarComoLeida(null, "PAC-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.marcarComoLeida(" ", "PAC-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.marcarComoLeida(creada.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> servicio.marcarComoLeida(creada.getId(), ""));
        assertThrows(IllegalStateException.class, () -> servicio.marcarComoLeida("NOT-NO-EXISTE", "PAC-01"));
        assertThrows(IllegalStateException.class, () -> servicio.marcarComoLeida(creada.getId(), "OTRO-PACIENTE"));
    }
}