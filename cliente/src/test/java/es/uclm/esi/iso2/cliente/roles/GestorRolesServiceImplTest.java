package es.uclm.esi.iso2.cliente.roles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GestorRolesServiceImplTest {

    private final IGestorRolesService rolesService = new GestorRolesServiceImpl();

    @Test
    public void testExtraerRolValido() {
        String tokenAdmin = "token-sso-user1-ADMINISTRADOR";
        assertEquals(Rol.ADMINISTRADOR, rolesService.extraerRolDeToken(tokenAdmin));
    }

    @Test
    public void testBloqueoDeInterfazPorRol() {
        String tokenPaciente = "token-sso-paciente1-PACIENTE";
        
        // n paciente no deb
        assertFalse(rolesService.tieneAcceso(tokenPaciente, Rol.MEDICO));
        
    
        assertTrue(rolesService.tieneAcceso(tokenPaciente, Rol.PACIENTE));
    }

    @Test
    public void testAsignarRolExitosoSiendoAdmin() {
        String tokenAdmin = "token-sso-admin1-ADMINISTRADOR";
        assertTrue(rolesService.asignarRol("user99", Rol.MEDICO, tokenAdmin));
    }

    @Test
    public void testAsignarRolDenegadoSinPrivilegios() {
        String tokenMedico = "token-sso-medico1-MEDICO";
        assertFalse(rolesService.asignarRol("user99", Rol.ADMISION, tokenMedico));
    }
}