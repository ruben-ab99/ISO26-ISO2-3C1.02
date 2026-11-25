package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.NotificacionDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
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
 * Pruebas de CU-02 (Recibir notificaciones y recordatorios).
 * Técnicas: clases de equivalencia sobre cada dato obligatorio y sobre el tipo de aviso
 * (soportado / no soportado), control de acceso por rol (permitido / denegado),
 * criterio each use entre parámetros y conjetura de errores (mayúsculas, paciente ajeno).
 */
class NotificacionesServiceImplTest {

    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1234";
    private static final String MSG_PACIENTE = "El identificador del paciente es obligatorio";

    private NotificacionesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionesServiceImpl();
    }

    // ---------------------------------------------------------------- constructor y permisos

    @Test
    @DisplayName("El constructor rechaza un gestor de roles nulo")
    void constructorConGestorDeRolesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new NotificacionesServiceImpl(null, "tok"));

        assertEquals("El servicio de gestión de roles no puede ser nulo", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} puede enviar notificaciones")
    @ValueSource(strings = {"token-sso-adm1-ADMISION-1", "token-sso-med1-MEDICO-1"})
    @DisplayName("Los roles ADMISION y MEDICO pueden enviar notificaciones")
    void enviarNotificacionConRolPermitidoLaCrea(String token) {
        NotificacionesServiceImpl conRol = new NotificacionesServiceImpl(new GestorRolesServiceImpl(), token);

        NotificacionDTO aviso = conRol.enviarNotificacion("PAC-01", "CITA", "Aviso", "Detalle", "C-1");

        assertNotNull(aviso);
    }

    @ParameterizedTest(name = "el token [{0}] no puede enviar notificaciones")
    @NullAndEmptySource
    @ValueSource(strings = {TOKEN_PACIENTE, "sin-rol-aqui"})
    @DisplayName("Sin rol ADMISION ni MEDICO se deniega el envío con SecurityException")
    void enviarNotificacionSinPermisoLanzaSecurityException(String token) {
        NotificacionesServiceImpl sinPermiso = new NotificacionesServiceImpl(new GestorRolesServiceImpl(), token);

        SecurityException e = assertThrows(SecurityException.class,
                () -> sinPermiso.enviarNotificacion("PAC-01", "CITA", "Aviso", "Detalle", "C-1"));

        assertEquals("Acceso denegado: Se requiere rol ADMISION o MEDICO para enviar notificaciones.", e.getMessage());
    }

    @Test
    @DisplayName("Un envío denegado queda registrado en la auditoría")
    void enviarNotificacionDenegadoRegistraEventoEnAuditoria() {
        AuditServiceImpl auditoria = new AuditServiceImpl();
        NotificacionesServiceImpl sinPermiso =
                new NotificacionesServiceImpl(new GestorRolesServiceImpl(), TOKEN_PACIENTE, auditoria);

        assertThrows(SecurityException.class,
                () -> sinPermiso.enviarNotificacion("PAC-01", "CITA", "Aviso", "Detalle", "C-1"));

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("ACCESO_DENEGADO_NOTIFICACIONES", traza.getAccion()),
                () -> assertFalse(traza.isExito()));
    }

    @ParameterizedTest(name = "token [{0}] no puede listar")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar sin token de sesión lanza SecurityException")
    void listarNotificacionesSinTokenLanzaSecurityException(String token) {
        NotificacionesServiceImpl sinToken = new NotificacionesServiceImpl(new GestorRolesServiceImpl(), token);

        SecurityException e = assertThrows(SecurityException.class,
                () -> sinToken.listarNotificacionesPaciente("PAC-01"));

        assertEquals("Token de sesión obligatorio para consultar notificaciones", e.getMessage());
    }

    @ParameterizedTest(name = "token [{0}] no puede marcar como leída")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Marcar como leída sin token de sesión lanza SecurityException")
    void marcarComoLeidaSinTokenLanzaSecurityException(String token) {
        NotificacionesServiceImpl sinToken = new NotificacionesServiceImpl(new GestorRolesServiceImpl(), token);

        SecurityException e = assertThrows(SecurityException.class,
                () -> sinToken.marcarComoLeida("NOT-1", "PAC-01"));

        assertEquals("Token de sesión obligatorio para gestionar notificaciones", e.getMessage());
    }

    // ---------------------------------------------------------------- enviarNotificacion

    @ParameterizedTest(name = "tipo {0} se guarda como {1}")
    @CsvSource({
            "cita,               CITA",
            "PREPARACION_PREVIA, PREPARACION_PREVIA",
            "informe_disponible, INFORME_DISPONIBLE"
    })
    @DisplayName("Cada tipo de aviso soportado se acepta y se normaliza a mayúsculas")
    void enviarNotificacionNormalizaElTipoDeAviso(String tipo, String tipoEsperado) {
        NotificacionDTO aviso = servicio.enviarNotificacion("PAC-01", tipo, "Título", "Mensaje", "R-1");

        assertEquals(tipoEsperado, aviso.getTipoAviso());
    }

    @Test
    @DisplayName("Una notificación nueva queda sin leer y con fecha de envío")
    void enviarNotificacionValidaQuedaSinLeer() {
        NotificacionDTO aviso = servicio.enviarNotificacion(
                "PAC-01", "cita", "Nueva cita programada", "Tiene cita el lunes a las 10:00", "CITA-100");

        assertAll(
                () -> assertTrue(aviso.getId().startsWith("NOT-")),
                () -> assertEquals("CITA-100", aviso.getReferenciaId()),
                () -> assertFalse(aviso.isLeida()),
                () -> assertNotNull(aviso.getFechaEnvio()));
    }

    @Test
    @DisplayName("Sin referencia, la notificación guarda una referencia vacía")
    void enviarNotificacionSinReferenciaGuardaCadenaVacia() {
        NotificacionDTO aviso = servicio.enviarNotificacion(
                "PAC-02", "INFORME_DISPONIBLE", "Informe listo", "Ya puede consultar su informe", null);

        assertEquals("", aviso.getReferenciaId());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Enviar con paciente nulo, vacío o en blanco lanza excepción")
    void enviarNotificacionConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarNotificacion(paciente, "CITA", "T", "M", "R"));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @ParameterizedTest(name = "tipo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Enviar con tipo de aviso nulo, vacío o en blanco lanza excepción")
    void enviarNotificacionConTipoVacioLanzaExcepcion(String tipo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarNotificacion("PAC-1", tipo, "T", "M", "R"));

        assertEquals("El tipo de aviso es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Enviar con un tipo de aviso no soportado lanza excepción")
    void enviarNotificacionConTipoNoSoportadoLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarNotificacion("PAC-1", "TIPO_FALSO", "T", "M", "R"));

        assertEquals("Tipo de aviso no soportado: TIPO_FALSO", e.getMessage());
    }

    @ParameterizedTest(name = "título [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Enviar con título nulo, vacío o en blanco lanza excepción")
    void enviarNotificacionConTituloInvalidoLanzaExcepcion(String titulo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarNotificacion("PAC-1", "CITA", titulo, "M", "R"));

        assertEquals("El título de la notificación es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "mensaje [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Enviar con mensaje nulo, vacío o en blanco lanza excepción")
    void enviarNotificacionConMensajeInvalidoLanzaExcepcion(String mensaje) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviarNotificacion("PAC-1", "CITA", "T", mensaje, "R"));

        assertEquals("El mensaje de la notificación es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- listar

    @Test
    @DisplayName("Listar devuelve únicamente las notificaciones del paciente solicitado")
    void listarNotificacionesPacienteDevuelveSoloLasSuyas() {
        servicio.enviarNotificacion("PAC-01", "CITA", "Cita 1", "Mensaje 1", "C-1");
        servicio.enviarNotificacion("PAC-01", "PREPARACION_PREVIA", "Prep 1", "Mensaje 2", "C-1");
        servicio.enviarNotificacion("PAC-02", "INFORME_DISPONIBLE", "Inf 1", "Mensaje 3", "I-1");

        List<NotificacionDTO> lista = servicio.listarNotificacionesPaciente("PAC-01");

        assertEquals(2, lista.size());
    }

    @Test
    @DisplayName("Listar un paciente sin avisos devuelve una lista vacía")
    void listarNotificacionesPacienteSinAvisosDevuelveListaVacia() {
        assertTrue(servicio.listarNotificacionesPaciente("PAC-99").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarNotificacionesPacienteConIdInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarNotificacionesPaciente(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    // ---------------------------------------------------------------- marcar como leída

    @Test
    @DisplayName("Marcar como leída cambia el estado de la notificación")
    void marcarComoLeidaCambiaElEstado() {
        NotificacionDTO creada = servicio.enviarNotificacion("PAC-01", "CITA", "Cita", "Detalle", "C-10");

        NotificacionDTO leida = servicio.marcarComoLeida(creada.getId(), "PAC-01");

        assertTrue(leida.isLeida());
    }

    @ParameterizedTest(name = "notificación [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Marcar con identificador de notificación nulo, vacío o en blanco lanza excepción")
    void marcarComoLeidaConIdNotificacionInvalidoLanzaExcepcion(String notificacionId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.marcarComoLeida(notificacionId, "PAC-01"));

        assertEquals("El identificador de la notificación es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Marcar con paciente nulo, vacío o en blanco lanza excepción")
    void marcarComoLeidaConPacienteInvalidoLanzaExcepcion(String paciente) {
        NotificacionDTO creada = servicio.enviarNotificacion("PAC-01", "CITA", "Cita", "Detalle", "C-10");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.marcarComoLeida(creada.getId(), paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @Test
    @DisplayName("Marcar una notificación inexistente lanza excepción")
    void marcarComoLeidaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.marcarComoLeida("NOT-NO-EXISTE", "PAC-01"));

        assertEquals("No existe ninguna notificación con ID: NOT-NO-EXISTE", e.getMessage());
    }

    @Test
    @DisplayName("Un paciente no puede marcar como leída la notificación de otro")
    void marcarComoLeidaDeOtroPacienteLanzaExcepcion() {
        NotificacionDTO creada = servicio.enviarNotificacion("PAC-01", "CITA", "Cita", "Detalle", "C-10");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.marcarComoLeida(creada.getId(), "OTRO-PACIENTE"));

        assertEquals("La notificación no pertenece al paciente indicado", e.getMessage());
        assertFalse(creada.isLeida());
    }
}
