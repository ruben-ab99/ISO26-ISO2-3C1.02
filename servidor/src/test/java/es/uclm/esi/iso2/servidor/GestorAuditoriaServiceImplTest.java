package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestorAuditoriaServiceImplTest {

    private static final String TOKEN_ADMIN = "token-sso-adm01-ADMINISTRADOR-0001";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-1111";
    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-2222";
    private static final String TOKEN_ADMISION = "token-sso-adm02-ADMISION-3333";
    private static final String TOKEN_SIN_ROL = "token-sso-invitado-SIN_ROL-9999";

    private GestorRolesServiceImpl gestorRoles;
    private AuditServiceImpl auditBase;
    private GestorAuditoriaServiceImpl gestorAuditoria;

    @BeforeEach
    void setUp() {
        gestorRoles = new GestorRolesServiceImpl();
        auditBase = new AuditServiceImpl();
        gestorAuditoria = new GestorAuditoriaServiceImpl(gestorRoles, auditBase);
    }

    @Test
    @DisplayName("CU-19: Constructores validan dependencias obligatorias")
    void validarConstructores() {
        assertDoesNotThrow(() -> new GestorAuditoriaServiceImpl());
        assertThrows(IllegalArgumentException.class, () -> new GestorAuditoriaServiceImpl(null, auditBase));
        assertThrows(IllegalArgumentException.class, () -> new GestorAuditoriaServiceImpl(gestorRoles, null));
    }

    @Test
    @DisplayName("CU-19: Registro y consulta completa de trazas por ADMINISTRADOR")
    void registrarYConsultarHistorialCompleto() {
        gestorAuditoria.registrarActividad("med01", "login_sso", true, "10.10.1.15", TOKEN_MEDICO);
        gestorAuditoria.registrarActividad("pac01", "consulta_citas", true, "192.168.1.20", TOKEN_PACIENTE);
        gestorAuditoria.registrarActividad("usr99", "intento_acceso", false, null, TOKEN_ADMIN);

        List<AuditLogDTO> historial = gestorAuditoria.consultarHistorialCompleto(TOKEN_ADMIN);
        assertEquals(3, historial.size());
        assertEquals("LOGIN_SSO", historial.get(0).getAccion());
        assertEquals("0.0.0.0", historial.get(2).getIp());

        assertThrows(IllegalArgumentException.class, () ->
                gestorAuditoria.registrarActividad(" ", "LOGIN", true, "1.1.1.1", TOKEN_ADMIN));
        assertThrows(IllegalArgumentException.class, () ->
                gestorAuditoria.registrarActividad("usr01", " ", true, "1.1.1.1", TOKEN_ADMIN));
    }

    @Test
    @DisplayName("CU-19: Búsqueda filtrada de trazas y detección de intentos fallidos")
    void buscarTrazasYConsultarFallidos() {
        LocalDateTime antes = LocalDateTime.now().minusMinutes(1);
        gestorAuditoria.registrarActividad("med01", "PRESCRIPCION_PRUEBA", true, "10.0.0.1", TOKEN_MEDICO);
        gestorAuditoria.registrarActividad("med01", "ACCESO_DENEGADO_ADMIN", false, "10.0.0.1", TOKEN_MEDICO);
        gestorAuditoria.registrarActividad("pac01", "CANCELACION_CITA", true, "10.0.0.2", TOKEN_PACIENTE);
        LocalDateTime despues = LocalDateTime.now().plusMinutes(1);

        assertEquals(2, gestorAuditoria.buscarTrazas("med01", null, null, antes, despues, TOKEN_ADMIN).size());
        assertEquals(1, gestorAuditoria.buscarTrazas("med01", "PRESCRIPCION", true, antes, despues, TOKEN_ADMIN).size());
        assertEquals(1, gestorAuditoria.consultarIntentosFallidos(TOKEN_ADMIN).size());

        assertThrows(IllegalArgumentException.class, () ->
                gestorAuditoria.buscarTrazas(null, null, null, despues, antes, TOKEN_ADMIN));
    }

    @Test
    @DisplayName("CU-25 Seguridad: Bloqueo de consulta de auditoría a roles no administradores con registro automático de intrusión")
    void seguridadAccesoAuditoriaSoloAdmin() {
        assertThrows(SecurityException.class, () -> gestorAuditoria.consultarHistorialCompleto(null));
        assertThrows(SecurityException.class, () -> gestorAuditoria.consultarHistorialCompleto(TOKEN_SIN_ROL));
        assertThrows(SecurityException.class, () -> gestorAuditoria.consultarHistorialCompleto(TOKEN_MEDICO));
        assertThrows(SecurityException.class, () -> gestorAuditoria.buscarTrazas(null, null, null, null, null, TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () -> gestorAuditoria.consultarIntentosFallidos(TOKEN_ADMISION));

        List<AuditLogDTO> alertas = gestorAuditoria.consultarIntentosFallidos(TOKEN_ADMIN);
        assertEquals(5, alertas.size());
    }

    @Test
    @DisplayName("CU-25 Seguridad: Validación transversal RBAC y aislamiento entre módulos críticos")
    void pruebasSeguridadTransversalesSistema() {
        assertTrue(gestorRoles.tieneAcceso(TOKEN_ADMIN, Rol.ADMINISTRADOR));
        assertFalse(gestorRoles.tieneAcceso(TOKEN_PACIENTE, Rol.MEDICO));
        assertFalse(gestorRoles.tieneAcceso(TOKEN_MEDICO, Rol.PACIENTE));
        assertFalse(gestorRoles.tieneAcceso("token-manipulado-FALSO-123", Rol.ADMINISTRADOR));
        assertFalse(gestorRoles.asignarRol("usr01", Rol.ADMINISTRADOR, TOKEN_PACIENTE));
        assertTrue(gestorRoles.asignarRol("usr01", Rol.MEDICO, TOKEN_ADMIN));
        assertEquals(Rol.MEDICO, gestorRoles.obtenerRolUsuario("usr01"));

        GestionCitasServiceImpl citas = new GestionCitasServiceImpl((p, c, t, d) -> {}, gestorRoles, TOKEN_ADMISION);
        GestorCitasPacienteServiceImpl portalPaciente = new GestorCitasPacienteServiceImpl(
                gestorRoles, citas, new GestorAgendasServiceImpl(gestorRoles)
        );
        CitaDTO citaPac02 = citas.programarCita("PAC-02", "MED-01", "RX-TORAX", LocalDate.of(2026, 12, 20), "10:00-10:15");

        assertThrows(SecurityException.class, () ->
                portalPaciente.consultarDetalleCitaPropia("PAC-01", citaPac02.getCitaId(), TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () ->
                portalPaciente.cancelarCitaPropia("PAC-01", citaPac02.getCitaId(), "Intento ajeno", TOKEN_PACIENTE));
    }
}