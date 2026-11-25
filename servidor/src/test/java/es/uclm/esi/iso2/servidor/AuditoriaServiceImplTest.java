package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.auditoria.ResumenAuditoriaDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-19 (Auditar actividad y accesos a datos clínicos).
 * Técnicas: clases de equivalencia (administrador / otro rol / token vacío, con y sin filtros,
 * trazas exitosas / fallidas / con campos nulos), valores límite en los extremos del periodo
 * y criterio each use al combinar filtros. El servicio de trazas se sustituye por un stub (ADR-012).
 */
class AuditoriaServiceImplTest {

    private static final String TOKEN_ADMIN = "token-sso-admin01-ADMINISTRADOR-1";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-1";
    private static final String ADMIN_ID = "ADM-01";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    private List<AuditLogDTO> trazas;
    private List<AuditLogDTO> registrados;
    private AuditoriaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        trazas = new ArrayList<>();
        registrados = new ArrayList<>();
        trazas.add(traza("PAC-001", "CONSULTAR_SEGUIMIENTOS_PACIENTE", true, LocalDateTime.of(2026, 11, 20, 9, 0)));
        trazas.add(traza("PAC-001", "ACCESO_DENEGADO_SEGUIMIENTO:SEG-201", false, LocalDateTime.of(2026, 11, 20, 9, 5)));
        trazas.add(traza("MED-010", "CONSULTAR_HISTORIAL:PAC-001", true, LocalDateTime.of(2026, 11, 21, 12, 0)));
        trazas.add(traza("SISTEMA", "ACCESO_DENEGADO_GESTION_CITAS", false, LocalDateTime.of(2026, 11, 22, 8, 30)));
        trazas.add(traza("MED-010", "CONSULTAR_HISTORIAL:PAC-002", true, LocalDateTime.of(2026, 11, 23, 18, 0)));

        servicio = new AuditoriaServiceImpl(stubAuditoria(), new GestorRolesServiceImpl(), RELOJ);
    }

    private IAuditService stubAuditoria() {
        return new IAuditService() {
            @Override
            public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
                registrados.add(new AuditLogDTO(usuario, accion, exito, ip));
            }

            @Override
            public List<AuditLogDTO> obtenerTrazas() {
                return new ArrayList<>(trazas);
            }
        };
    }

    private static AuditLogDTO traza(String usuario, String accion, boolean exito, LocalDateTime momento) {
        return new AuditLogDTO(usuario, accion, exito, "10.0.0.1") {
            @Override
            public LocalDateTime getTimestamp() {
                return momento;
            }
        };
    }

    @Test
    @DisplayName("El constructor rechaza dependencias nulas")
    void constructorRechazaDependenciasNulas() {
        GestorRolesServiceImpl roles = new GestorRolesServiceImpl();
        assertThrows(IllegalArgumentException.class, () -> new AuditoriaServiceImpl(null, roles));
        assertThrows(IllegalArgumentException.class, () -> new AuditoriaServiceImpl(new AuditServiceImpl(), null));
        assertThrows(IllegalArgumentException.class, () -> new AuditoriaServiceImpl(new AuditServiceImpl(), roles, null));
    }

    @Test
    @DisplayName("Sin filtros devuelve todas las trazas y registra la consulta del administrador")
    void consultarTrazasSinFiltrosDevuelveTodasYRegistraLaConsulta() {
        List<AuditLogDTO> resultado = servicio.consultarTrazas(null, null, null, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(5, resultado.size());
        assertEquals(1, registrados.size());
        assertEquals(ADMIN_ID, registrados.get(0).getUsuario());
        assertEquals("CONSULTAR_AUDITORIA:TRAZAS", registrados.get(0).getAccion());
        assertTrue(registrados.get(0).isExito());
    }

    @Test
    @DisplayName("El filtro por usuario no distingue mayúsculas")
    void filtroPorUsuarioIgnoraMayusculas() {
        List<AuditLogDTO> resultado = servicio.consultarTrazas("  med-010 ", null, null, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(t -> "MED-010".equals(t.getUsuario())));
    }

    @Test
    @DisplayName("El filtro por acción busca fragmentos sin distinguir mayúsculas")
    void filtroPorAccionBuscaFragmentos() {
        assertEquals(2, servicio.consultarTrazas(null, "historial", null, null, ADMIN_ID, TOKEN_ADMIN).size());
        assertEquals(2, servicio.consultarTrazas(null, "acceso_denegado", null, null, ADMIN_ID, TOKEN_ADMIN).size());
        assertTrue(servicio.consultarTrazas(null, "BORRAR", null, null, ADMIN_ID, TOKEN_ADMIN).isEmpty());
    }

    @Test
    @DisplayName("Los filtros de usuario y acción se combinan")
    void filtrosCombinados() {
        List<AuditLogDTO> resultado = servicio.consultarTrazas("PAC-001", "DENEGADO", null, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(1, resultado.size());
        assertEquals("ACCESO_DENEGADO_SEGUIMIENTO:SEG-201", resultado.get(0).getAccion());
    }

    @Test
    @DisplayName("Los extremos del periodo están incluidos")
    void extremosDelPeriodoIncluidos() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 20, 9, 5);
        LocalDateTime hasta = LocalDateTime.of(2026, 11, 22, 8, 30);

        assertEquals(3, servicio.consultarTrazas(null, null, desde, hasta, ADMIN_ID, TOKEN_ADMIN).size());
        assertEquals(2, servicio.consultarTrazas(null, null, desde.plusSeconds(1), hasta, ADMIN_ID, TOKEN_ADMIN).size());
        assertEquals(2, servicio.consultarTrazas(null, null, desde, hasta.minusSeconds(1), ADMIN_ID, TOKEN_ADMIN).size());
    }

    @Test
    @DisplayName("Un periodo con inicio posterior al fin se rechaza")
    void periodoInvertidoSeRechaza() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 23, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 11, 20, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarTrazas(null, null, desde, hasta, ADMIN_ID, TOKEN_ADMIN));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarAccesosDenegados(desde, hasta, ADMIN_ID, TOKEN_ADMIN));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarResumenAuditoria(desde, hasta, ADMIN_ID, TOKEN_ADMIN));
    }

    @Test
    @DisplayName("Un rol distinto de ADMINISTRADOR recibe acceso denegado y queda registrado")
    void rolNoAdministradorDeniegaYRegistra() {
        assertThrows(SecurityException.class,
                () -> servicio.consultarTrazas(null, null, null, null, "MED-010", TOKEN_MEDICO));

        assertEquals(1, registrados.size());
        assertEquals("MED-010", registrados.get(0).getUsuario());
        assertEquals("ACCESO_DENEGADO_AUDITORIA:CONSULTAR_TRAZAS", registrados.get(0).getAccion());
        assertFalse(registrados.get(0).isExito());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin token SSO no se accede a la auditoría")
    void tokenVacioDeniega(String token) {
        assertThrows(SecurityException.class,
                () -> servicio.consultarAccesosDenegados(null, null, ADMIN_ID, token));
        assertThrows(SecurityException.class,
                () -> servicio.generarResumenAuditoria(null, null, ADMIN_ID, token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("El identificador del administrador es obligatorio")
    void adminIdObligatorio(String adminId) {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarTrazas(null, null, null, null, adminId, TOKEN_ADMIN));
        assertTrue(registrados.isEmpty());
    }

    @Test
    @DisplayName("Los accesos denegados solo incluyen eventos fallidos del periodo")
    void accesosDenegadosSoloFallidos() {
        List<AuditLogDTO> todos = servicio.consultarAccesosDenegados(null, null, ADMIN_ID, TOKEN_ADMIN);
        List<AuditLogDTO> desdeEl21 = servicio.consultarAccesosDenegados(
                LocalDateTime.of(2026, 11, 21, 0, 0), null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(2, todos.size());
        assertTrue(todos.stream().noneMatch(AuditLogDTO::isExito));
        assertEquals(1, desdeEl21.size());
        assertEquals("SISTEMA", desdeEl21.get(0).getUsuario());
        assertEquals("CONSULTAR_AUDITORIA:ACCESOS_DENEGADOS", registrados.get(0).getAccion());
    }

    @Test
    @DisplayName("El resumen agrega por usuario, tipo de acción y accesos denegados")
    void resumenAgregaCorrectamente() {
        ResumenAuditoriaDTO resumen = servicio.generarResumenAuditoria(null, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(5, resumen.getTotalEventos());
        assertEquals(3, resumen.getEventosExitosos());
        assertEquals(2, resumen.getEventosFallidos());
        assertEquals(40.0, resumen.getPorcentajeEventosFallidos());
        assertEquals(3, resumen.getUsuariosDistintos());
        assertEquals(List.of("MED-010", "PAC-001", "SISTEMA"), new ArrayList<>(resumen.getEventosPorUsuario().keySet()));
        assertEquals(2, resumen.getEventosPorUsuario().get("MED-010"));
        assertEquals(List.of("CONSULTAR_HISTORIAL", "ACCESO_DENEGADO_GESTION_CITAS",
                        "ACCESO_DENEGADO_SEGUIMIENTO", "CONSULTAR_SEGUIMIENTOS_PACIENTE"),
                new ArrayList<>(resumen.getEventosPorTipoAccion().keySet()));
        assertEquals(1, resumen.getAccesosDenegadosPorUsuario().get("PAC-001"));
        assertEquals(1, resumen.getAccesosDenegadosPorUsuario().get("SISTEMA"));
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), resumen.getFechaGeneracion());
        assertNull(resumen.getDesde());
        assertEquals("CONSULTAR_AUDITORIA:RESUMEN", registrados.get(0).getAccion());
    }

    @Test
    @DisplayName("Sin trazas el resumen devuelve ceros")
    void resumenSinTrazas() {
        trazas.clear();
        LocalDateTime desde = LocalDateTime.of(2026, 11, 1, 0, 0);

        ResumenAuditoriaDTO resumen = servicio.generarResumenAuditoria(desde, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(0, resumen.getTotalEventos());
        assertEquals(0.0, resumen.getPorcentajeEventosFallidos());
        assertTrue(resumen.getEventosPorUsuario().isEmpty());
        assertTrue(resumen.getAccesosDenegadosPorUsuario().isEmpty());
        assertEquals(desde, resumen.getDesde());
    }

    @Test
    @DisplayName("Si el servicio de trazas devuelve null se trata como lista vacía")
    void trazasNulasSeTratanComoVacias() {
        IAuditService sinTrazas = new IAuditService() {
            @Override
            public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
                registrados.add(new AuditLogDTO(usuario, accion, exito, ip));
            }

            @Override
            public List<AuditLogDTO> obtenerTrazas() {
                return null;
            }
        };
        AuditoriaServiceImpl servicioSinTrazas = new AuditoriaServiceImpl(sinTrazas, new GestorRolesServiceImpl(), RELOJ);

        assertTrue(servicioSinTrazas.consultarTrazas(null, null, null, null, ADMIN_ID, TOKEN_ADMIN).isEmpty());
    }

    @Test
    @DisplayName("Las trazas con campos nulos se agrupan como DESCONOCIDO / SIN_ACCION y sin fecha solo cuentan sin periodo")
    void trazasConCamposNulos() {
        trazas.clear();
        trazas.add(null);
        trazas.add(traza(null, null, true, null));
        trazas.add(traza(" ", " ", false, LocalDateTime.of(2026, 11, 24, 10, 0)));

        ResumenAuditoriaDTO sinPeriodo = servicio.generarResumenAuditoria(null, null, ADMIN_ID, TOKEN_ADMIN);
        ResumenAuditoriaDTO conPeriodo = servicio.generarResumenAuditoria(
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 0, 0), ADMIN_ID, TOKEN_ADMIN);

        assertEquals(2, sinPeriodo.getTotalEventos());
        assertEquals(2, sinPeriodo.getEventosPorUsuario().get("DESCONOCIDO"));
        assertEquals(2, sinPeriodo.getEventosPorTipoAccion().get("SIN_ACCION"));
        assertEquals(1, conPeriodo.getTotalEventos());
        assertEquals(1, conPeriodo.getAccesosDenegadosPorUsuario().get("DESCONOCIDO"));
    }

    @Test
    @DisplayName("La propia consulta no aparece en su resultado, pero sí en la siguiente")
    void laConsultaSeRegistraDespuesDeCalcularElResultado() {
        AuditServiceImpl auditoriaReal = new AuditServiceImpl();
        auditoriaReal.registrarEvento("PAC-001", "CONSULTAR_HISTORIAL:PAC-001", true, "127.0.0.1");
        AuditoriaServiceImpl servicioReal = new AuditoriaServiceImpl(auditoriaReal, new GestorRolesServiceImpl());

        List<AuditLogDTO> primera = servicioReal.consultarTrazas(null, null, null, null, ADMIN_ID, TOKEN_ADMIN);
        List<AuditLogDTO> segunda = servicioReal.consultarTrazas(null, null, null, null, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(1, primera.size());
        assertEquals(2, segunda.size());
        assertEquals("CONSULTAR_AUDITORIA:TRAZAS", segunda.get(1).getAccion());
    }
}
