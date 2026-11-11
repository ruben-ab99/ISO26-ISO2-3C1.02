package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceImplTest {

    private IAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl();
    }

    @Test
    void testRegistrarEventoExitoso() {
        auditService.registrarEvento("raul.martinez", "LOGIN", true, "192.168.1.50");
        List<AuditLogDTO> trazas = auditService.obtenerTrazas();

        assertEquals(1, trazas.size());
        assertEquals("raul.martinez", trazas.get(0).getUsuario());
        assertTrue(trazas.get(0).isExito());
    }

    @Test
    void testRegistrarEventoUsuarioInvalido() {
        assertThrows(IllegalArgumentException.class, () -> {
            auditService.registrarEvento("", "LOGIN", false, "127.0.0.1");
        });
    }
}