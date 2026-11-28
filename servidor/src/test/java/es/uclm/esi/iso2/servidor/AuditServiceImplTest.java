package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-20 (Auditoría de accesos) / CU-25 (Seguridad).
 * Técnicas: clases de equivalencia sobre el usuario (válido / nulo / vacío / en blanco)
 * y prueba de integración de la traza entre servicios.
 */
class AuditServiceImplTest {

    private IAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl();
    }

    @Test
    @DisplayName("Un evento registrado conserva usuario, acción, resultado e IP")
    void registrarEventoExitosoConservaTodosSusDatos() {
        auditService.registrarEvento("raul.martinez", "LOGIN", true, "192.168.1.50");

        List<AuditLogDTO> trazas = auditService.obtenerTrazas();

        assertEquals(1, trazas.size());
        AuditLogDTO traza = trazas.get(0);
        assertAll(
                () -> assertEquals("raul.martinez", traza.getUsuario()),
                () -> assertEquals("LOGIN", traza.getAccion()),
                () -> assertTrue(traza.isExito()),
                () -> assertEquals("192.168.1.50", traza.getIp()),
                () -> assertNotNull(traza.getTimestamp()));
    }

    @Test
    @DisplayName("Un evento fallido queda registrado como no exitoso")
    void registrarEventoFallidoQuedaComoNoExitoso() {
        auditService.registrarEvento("intruso", "LOGIN", false, "10.0.0.9");

        assertFalse(auditService.obtenerTrazas().get(0).isExito());
    }

    @ParameterizedTest(name = "usuario [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar un evento con usuario nulo, vacío o en blanco lanza excepción y no deja traza")
    void registrarEventoConUsuarioInvalidoLanzaExcepcion(String usuario) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> auditService.registrarEvento(usuario, "LOGIN", false, "127.0.0.1"));

        assertEquals("El usuario no puede estar vacío", e.getMessage());
        assertTrue(auditService.obtenerTrazas().isEmpty());
    }

    @Test
    @DisplayName("Las trazas devueltas son una copia: modificarlas no altera el historial")
    void obtenerTrazasDevuelveUnaCopiaDelHistorial() {
        auditService.registrarEvento("ana", "LOGIN", true, "127.0.0.1");

        auditService.obtenerTrazas().clear();

        assertEquals(1, auditService.obtenerTrazas().size());
    }

    @Test
    @DisplayName("La auditoría registra eventos de login, citas, informes y configuración")
    void auditoriaRegistraEventosDeLosServiciosConectados() {
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
