package es.uclm.esi.iso2.servidor;
import org.junit.jupiter.api.Test;

import es.uclm.esi.iso2.servidor.AutenticacionServiceImpl;
import es.uclm.esi.iso2.servidor.IAutenticacionService;

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
}