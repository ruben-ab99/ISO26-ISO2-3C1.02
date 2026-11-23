package es.uclm.esi.iso2.servidor;
import org.junit.jupiter.api.Test;

import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAutenticacionService;

import static org.junit.jupiter.api.Assertions.*;

public class AutenticacionServiceImplTest {

    private final IAutenticacionService service = new AutenticacionServiceImpl();

    @Test
    public void testAutenticarExitoso() {
        assertTrue(service.autenticar("admin", "1234"));
    }

    @Test
    public void testAutenticarFallido() {
        assertFalse(service.autenticar("", ""));
    }

    @Test
    public void testGenerarYValidarToken() {
        String token = service.generarToken("admin");
        assertNotNull(token);
        assertTrue(service.validarToken(token));
    }

    @Test
    void testGenerarTokenConRolAsignadoYValidacion() {
        GestorRolesServiceImpl gestorRoles = new GestorRolesServiceImpl();
        AutenticacionServiceImpl authService = new AutenticacionServiceImpl(gestorRoles);

        String tokenAdmin = "token-sso-root-ADMINISTRADOR-0001";
        assertTrue(gestorRoles.asignarRol("dr.house", Rol.MEDICO, tokenAdmin));

        String tokenMedico = authService.generarToken("dr.house");
        assertNotNull(tokenMedico);
        assertTrue(authService.validarToken(tokenMedico));
        assertEquals(Rol.MEDICO, gestorRoles.extraerRolDeToken(tokenMedico));
        assertTrue(gestorRoles.tieneAcceso(tokenMedico, Rol.MEDICO));
    }

    @Test
    void testRechazarTokenFalsificado() {
        AutenticacionServiceImpl authService = new AutenticacionServiceImpl();
        assertFalse(authService.validarToken("token-sso-hacker-ADMINISTRADOR-9999"));
    }
}