package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
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

    @Test
    void testAuditoriaConectadaEnServicios() {
        assertEquals(0, auditService.obtenerTrazas().size());

        // 1. Login SSO
        AutenticacionServiceImpl authService = new AutenticacionServiceImpl(new GestorRolesServiceImpl(), auditService);
        assertTrue(authService.autenticar("dr.house", "clave123"));
        assertEquals(1, auditService.obtenerTrazas().size());

        // 2. Programar cita
        GestionCitasServiceImpl citasService = new GestionCitasServiceImpl(
                (p, c, t, d) -> {}, new GestorRolesServiceImpl(), "token-sso-adm-ADMISION-1", auditService
        );
        citasService.programarCita("PAC-01", "MED-01", "RX-TORAX", LocalDate.of(2026, 12, 10), "10:00-10:15");
        assertEquals(2, auditService.obtenerTrazas().size());

        // 3. Crear informe radiológico
        InformesImagenesServiceImpl informesService = new InformesImagenesServiceImpl(
                new ISistemaExternoRisPacs() {
                    @Override
                    public List<EstudioImagenDTO> obtenerEstudiosDesdePacs(String pacienteId) {
                        return Collections.emptyList();
                    }

                    @Override
                    public void sincronizarInformeConHce(String informeId, String pacienteId, String contenidoClinico) {
                    }
                },
                auditService
        );
        informesService.crearInforme("SOL-1", "PAC-01", "MED-01", "Sin hallazgos patológicos");
        assertEquals(3, auditService.obtenerTrazas().size());

        // 4. Guardar parámetro del sistema
        ConfiguracionSistemaServiceImpl configService = new ConfiguracionSistemaServiceImpl(
                new CatalogoPruebasServiceImpl(), auditService
        );
        configService.guardarParametro("SLA_URGENTE", "12", "PRIORIDADES", "ADM-01");
        assertEquals(4, auditService.obtenerTrazas().size());
    }
}