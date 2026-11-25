package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.analitica.EstadisticasGlobalesDTO;
import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-21 (Generar estadísticas globales y reportes de uso).
 * Técnicas: clases de equivalencia (administrador / otro rol / token vacío, periodo con y sin actividad,
 * pruebas activas / inactivas / sin modalidad) y valores límite en el primer y último día del periodo.
 * Las fuentes de datos (trazas y catálogo) se sustituyen por stubs (ADR-012).
 */
class EstadisticasGlobalesServiceImplTest {

    private static final String TOKEN_ADMIN = "token-sso-admin01-ADMINISTRADOR-1";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-1";
    private static final String ADMIN_ID = "ADM-01";
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 10);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 20);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    private List<AuditLogDTO> trazas;
    private List<AuditLogDTO> registrados;
    private List<TipoPruebaDTO> catalogo;
    private EstadisticasGlobalesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        trazas = new ArrayList<>();
        registrados = new ArrayList<>();
        catalogo = new ArrayList<>();

        trazas.add(traza("PAC-003", "CONSULTAR_HISTORIAL:PAC-003", true, LocalDateTime.of(2026, 11, 9, 23, 59)));
        trazas.add(traza("PAC-001", "CONSULTAR_HISTORIAL:PAC-001", true, LocalDateTime.of(2026, 11, 10, 9, 0)));
        trazas.add(traza("MED-010", "SOLICITAR_PRUEBA", true, LocalDateTime.of(2026, 11, 15, 10, 0)));
        trazas.add(traza("MED-010", "SOLICITAR_PRUEBA", false, LocalDateTime.of(2026, 11, 15, 11, 0)));
        trazas.add(traza("ADM-02", "CONSULTAR_HISTORIAL:PAC-002", true, LocalDateTime.of(2026, 11, 20, 23, 59)));
        trazas.add(traza("PAC-004", "CONSULTAR_HISTORIAL:PAC-004", true, LocalDateTime.of(2026, 11, 21, 0, 0)));

        catalogo.add(prueba("RX-TORAX", "RX", true));
        catalogo.add(prueba("RX-COLUMNA", "rx", true));
        catalogo.add(prueba("TAC-ABDO", "TAC", true));
        catalogo.add(prueba("RM-OBSOLETA", "RM", false));
        catalogo.add(prueba("SIN-MOD", null, true));

        servicio = new EstadisticasGlobalesServiceImpl(stubAuditoria(), stubCatalogo(), new GestorRolesServiceImpl(), RELOJ);
    }

    private IAuditService stubAuditoria() {
        return new IAuditService() {
            @Override
            public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
                registrados.add(new AuditLogDTO(usuario, accion, exito, ip));
            }

            @Override
            public List<AuditLogDTO> obtenerTrazas() {
                return trazas;
            }
        };
    }

    private ICatalogoPruebasService stubCatalogo() {
        return new ICatalogoPruebasService() {
            @Override
            public List<TipoPruebaDTO> listarCatalogoPruebas() {
                return catalogo;
            }

            @Override
            public TipoPruebaDTO consultarDetallePrueba(String codigoPrueba) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<TipoPruebaDTO> filtrarPorModalidad(String modalidad) {
                throw new UnsupportedOperationException();
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

    private static TipoPruebaDTO prueba(String codigo, String modalidad, boolean activa) {
        return new TipoPruebaDTO(codigo, "Prueba " + codigo, modalidad, "Sala 1", 20, "", false, false, "", activa);
    }

    @Test
    @DisplayName("El constructor rechaza dependencias nulas")
    void constructorRechazaDependenciasNulas() {
        GestorRolesServiceImpl roles = new GestorRolesServiceImpl();
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasGlobalesServiceImpl(null, stubCatalogo(), roles));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasGlobalesServiceImpl(stubAuditoria(), null, roles));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasGlobalesServiceImpl(stubAuditoria(), stubCatalogo(), null));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasGlobalesServiceImpl(stubAuditoria(), stubCatalogo(), roles, null));
    }

    @Test
    @DisplayName("Calcula el uso del periodo con los extremos incluidos y registra la generación")
    void calculaElUsoDelPeriodo() {
        EstadisticasGlobalesDTO e = servicio.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(4, e.getTotalOperaciones());
        assertEquals(1, e.getOperacionesFallidas());
        assertEquals(25.0, e.getPorcentajeOperacionesFallidas());
        assertEquals(3, e.getUsuariosActivos());
        assertEquals(List.of(DESDE, LocalDate.of(2026, 11, 15), HASTA), new ArrayList<>(e.getOperacionesPorDia().keySet()));
        assertEquals(2, e.getOperacionesPorDia().get(LocalDate.of(2026, 11, 15)));
        assertEquals(LocalDate.of(2026, 11, 15), e.getDiaMayorActividad());
        assertEquals(List.of("CONSULTAR_HISTORIAL", "SOLICITAR_PRUEBA"), new ArrayList<>(e.getOperacionesPorTipo().keySet()));
        assertEquals(4, e.getPruebasCatalogoActivas());
        assertEquals(List.of("RX", "SIN_MODALIDAD", "TAC"), new ArrayList<>(e.getPruebasActivasPorModalidad().keySet()));
        assertEquals(2, e.getPruebasActivasPorModalidad().get("RX"));
        assertEquals(DESDE, e.getDesde());
        assertEquals(HASTA, e.getHasta());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), e.getFechaGeneracion());

        assertEquals(1, registrados.size());
        assertEquals(ADMIN_ID, registrados.get(0).getUsuario());
        assertEquals("GENERAR_ESTADISTICAS_GLOBALES:2026-11-10_2026-11-20", registrados.get(0).getAccion());
        assertTrue(registrados.get(0).isExito());
    }

    @Test
    @DisplayName("Un periodo sin actividad devuelve ceros pero mantiene el catálogo")
    void periodoSinActividad() {
        EstadisticasGlobalesDTO e = servicio.generarEstadisticasGlobales(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ADMIN_ID, TOKEN_ADMIN);

        assertEquals(0, e.getTotalOperaciones());
        assertEquals(0.0, e.getPorcentajeOperacionesFallidas());
        assertNull(e.getDiaMayorActividad());
        assertTrue(e.getOperacionesPorDia().isEmpty());
        assertEquals(4, e.getPruebasCatalogoActivas());
    }

    @Test
    @DisplayName("Si dos días empatan en actividad se toma el primero")
    void empateDiaMayorActividad() {
        trazas.add(traza("PAC-001", "CONSULTAR_HISTORIAL:PAC-001", true, LocalDateTime.of(2026, 11, 20, 8, 0)));

        EstadisticasGlobalesDTO e = servicio.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(2, e.getOperacionesPorDia().get(HASTA));
        assertEquals(LocalDate.of(2026, 11, 15), e.getDiaMayorActividad());
    }

    @Test
    @DisplayName("El periodo es obligatorio y su inicio no puede ser posterior al fin")
    void periodoObligatorioYOrdenado() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarEstadisticasGlobales(null, HASTA, ADMIN_ID, TOKEN_ADMIN));
        assertThrows(IllegalArgumentException.class, () -> servicio.generarEstadisticasGlobales(DESDE, null, ADMIN_ID, TOKEN_ADMIN));
        assertThrows(IllegalArgumentException.class, () -> servicio.exportarInformeUsoCsv(HASTA, DESDE, ADMIN_ID, TOKEN_ADMIN));
    }

    @Test
    @DisplayName("Un rol distinto de ADMINISTRADOR recibe acceso denegado y queda registrado")
    void rolNoAdministradorDeniega() {
        assertThrows(SecurityException.class, () -> servicio.generarEstadisticasGlobales(DESDE, HASTA, "MED-010", TOKEN_MEDICO));
        assertThrows(SecurityException.class, () -> servicio.exportarInformeUsoCsv(DESDE, HASTA, "MED-010", TOKEN_MEDICO));

        assertEquals(2, registrados.size());
        assertEquals("ACCESO_DENEGADO_ESTADISTICAS_GLOBALES", registrados.get(0).getAccion());
        assertFalse(registrados.get(0).isExito());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin token SSO no se generan estadísticas")
    void tokenVacioDeniega(String token) {
        assertThrows(SecurityException.class, () -> servicio.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("El identificador del administrador es obligatorio")
    void adminIdObligatorio(String adminId) {
        assertThrows(IllegalArgumentException.class, () -> servicio.exportarInformeUsoCsv(DESDE, HASTA, adminId, TOKEN_ADMIN));
        assertTrue(registrados.isEmpty());
    }

    @Test
    @DisplayName("El CSV incluye todas las secciones y la exportación se registra una sola vez")
    void exportarCsv() {
        String csv = servicio.exportarInformeUsoCsv(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertTrue(csv.startsWith("seccion;clave;valor\n"));
        assertTrue(csv.contains("resumen;periodo_desde;2026-11-10\n"));
        assertTrue(csv.contains("resumen;total_operaciones;4\n"));
        assertTrue(csv.contains("resumen;porcentaje_operaciones_fallidas;25.0\n"));
        assertTrue(csv.contains("resumen;dia_mayor_actividad;2026-11-15\n"));
        assertTrue(csv.contains("operaciones_por_dia;2026-11-15;2\n"));
        assertTrue(csv.contains("operaciones_por_tipo;SOLICITAR_PRUEBA;2\n"));
        assertTrue(csv.contains("catalogo_por_modalidad;RX;2\n"));
        assertEquals(1, registrados.size());
        assertEquals("EXPORTAR_ESTADISTICAS_GLOBALES:2026-11-10_2026-11-20", registrados.get(0).getAccion());
    }

    @Test
    @DisplayName("El CSV de un periodo sin actividad marca el día de mayor actividad con un guion")
    void exportarCsvSinActividad() {
        String csv = servicio.exportarInformeUsoCsv(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), ADMIN_ID, TOKEN_ADMIN);

        assertTrue(csv.contains("resumen;dia_mayor_actividad;-\n"));
        assertFalse(csv.contains("operaciones_por_dia;"));
    }

    @Test
    @DisplayName("Si las fuentes devuelven null se tratan como vacías")
    void fuentesNulasSeTratanComoVacias() {
        trazas = null;
        catalogo = null;

        EstadisticasGlobalesDTO e = servicio.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(0, e.getTotalOperaciones());
        assertEquals(0, e.getPruebasCatalogoActivas());
        assertTrue(e.getPruebasActivasPorModalidad().isEmpty());
    }

    @Test
    @DisplayName("Se ignoran trazas nulas o sin fecha y los campos vacíos se agrupan como DESCONOCIDO / SIN_ACCION")
    void trazasConCamposNulos() {
        trazas.clear();
        trazas.add(null);
        trazas.add(traza("PAC-001", "CONSULTAR_HISTORIAL", true, null));
        trazas.add(traza(" ", null, false, LocalDateTime.of(2026, 11, 12, 10, 0)));
        trazas.add(traza(null, "  ", true, LocalDateTime.of(2026, 11, 13, 10, 0)));
        catalogo.add(null);
        catalogo.add(prueba("SIN-MOD-2", "  ", true));

        EstadisticasGlobalesDTO e = servicio.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertEquals(2, e.getTotalOperaciones());
        assertEquals(1, e.getUsuariosActivos());
        assertEquals(2, e.getOperacionesPorTipo().get("SIN_ACCION"));
        assertEquals(50.0, e.getPorcentajeOperacionesFallidas());
        assertEquals(5, e.getPruebasCatalogoActivas());
        assertEquals(2, e.getPruebasActivasPorModalidad().get("SIN_MODALIDAD"));
    }

    @Test
    @DisplayName("El constructor sin reloj usa el reloj del sistema")
    void constructorSinRelojUsaElDelSistema() {
        EstadisticasGlobalesServiceImpl conRelojSistema =
                new EstadisticasGlobalesServiceImpl(stubAuditoria(), stubCatalogo(), new GestorRolesServiceImpl());

        EstadisticasGlobalesDTO e = conRelojSistema.generarEstadisticasGlobales(DESDE, HASTA, ADMIN_ID, TOKEN_ADMIN);

        assertNotNull(e.getFechaGeneracion());
        assertEquals(4, e.getTotalOperaciones());
    }
}
