package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.analitica.EstadisticasSolicitudesDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.prescripciones.ISolicitudPruebaService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadisticasSolicitudesServiceImplTest {

    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-1234";
    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1234";
    private static final String TOKEN_ADMIN = "token-sso-adm01-ADMINISTRADOR-1234";

    private Clock relojFijo;
    private LocalDate hoyFijo;

    private CatalogoPruebasServiceImpl catalogoService;
    private SolicitudPruebaServiceImpl solicitudService;
    private GestionCitasServiceImpl citasService;
    private PrescripcionSeguimientoServiceImpl seguimientoService;
    private RegistroPruebaServiceImpl registroService;
    private GestorRolesServiceImpl gestorRoles;
    private AuditServiceImpl auditService;
    private EstadisticasSolicitudesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        relojFijo = Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC);
        hoyFijo = LocalDate.now(relojFijo);

        catalogoService = new CatalogoPruebasServiceImpl();
        auditService = new AuditServiceImpl();
        gestorRoles = new GestorRolesServiceImpl();
        solicitudService = new SolicitudPruebaServiceImpl(catalogoService);
        citasService = new GestionCitasServiceImpl();
        seguimientoService = new PrescripcionSeguimientoServiceImpl(catalogoService, citasService, auditService);
        registroService = new RegistroPruebaServiceImpl(new INotificadorRisHce() {
            @Override
            public void notificarRealizacionRis(String registroId, String citaId, String tipoPrueba) {}

            @Override
            public void registrarIncidenciaHce(String registroId, String pacienteId, String incidencias) {}
        });

        servicio = new EstadisticasSolicitudesServiceImpl(
                solicitudService,
                citasService,
                seguimientoService,
                registroService,
                gestorRoles,
                auditService,
                relojFijo
        );
    }

    @Test
    @DisplayName("Constructor rechaza servicios colaboradores o gestor de roles nulos")
    void constructorConDependenciasNulasLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(
                null, citasService, seguimientoService, registroService, gestorRoles, auditService));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(
                solicitudService, null, seguimientoService, registroService, gestorRoles, auditService));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(
                solicitudService, citasService, null, registroService, gestorRoles, auditService));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(
                solicitudService, citasService, seguimientoService, null, gestorRoles, auditService));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(
                solicitudService, citasService, seguimientoService, registroService, null, auditService));
    }

    @Test
    @DisplayName("Deniega acceso y audita intento si el token es nulo, vacío o no tiene rol MEDICO")
    void deniegaAccesoSiNoEsMedicoYRegistraAuditoria() {
        // Preparar
        LocalDate desde = hoyFijo.minusDays(7);

        // Ejecutar & Comprobar
        assertThrows(SecurityException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", desde, hoyFijo, null));
        assertThrows(SecurityException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", desde, hoyFijo, "   "));
        assertThrows(SecurityException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", desde, hoyFijo, TOKEN_PACIENTE));
        assertThrows(SecurityException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", desde, hoyFijo, TOKEN_ADMIN));

        List<AuditLogDTO> trazas = auditService.obtenerTrazas();
        assertEquals(4, trazas.size());
        assertTrue(trazas.stream().noneMatch(AuditLogDTO::isExito));
    }

    @Test
    @DisplayName("Valida parámetros obligatorios y rango de fechas en consultarEstadisticasSolicitudes")
    void validaParametrosDeEntrada() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.consultarEstadisticasSolicitudes(null, hoyFijo.minusDays(1), hoyFijo, TOKEN_MEDICO));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.consultarEstadisticasSolicitudes("  ", hoyFijo.minusDays(1), hoyFijo, TOKEN_MEDICO));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", null, hoyFijo, TOKEN_MEDICO));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", hoyFijo, null, TOKEN_MEDICO));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.consultarEstadisticasSolicitudes("MED-01", hoyFijo, hoyFijo.minusDays(1), TOKEN_MEDICO));
    }

    @Test
    @DisplayName("Devuelve ceros y 100% de adherencia cuando no hay solicitudes ni seguimientos en el periodo")
    void estadisticasSinDatosDevuelveCerosYAdherenciaCompleta() {
        // Ejecutar
        EstadisticasSolicitudesDTO dto = servicio.consultarEstadisticasSolicitudes(
                "MED-99", hoyFijo, hoyFijo, TOKEN_MEDICO
        );

        // Comprobar
        assertNotNull(dto);
        assertEquals("MED-99", dto.getMedicoId());
        assertEquals(0, dto.getTotalSolicitudes());
        assertEquals(0, dto.getSolicitudesActivas());
        assertEquals(0, dto.getSolicitudesCanceladas());
        assertEquals(0.0, dto.getTiempoMedioEsperaCitaDias());
        assertEquals(0.0, dto.getTiempoMedioEsperaSinCitaDias());
        assertTrue(dto.getUsoPorTipoPrueba().isEmpty());
        assertEquals(0, dto.getTotalSeguimientos());
        assertEquals(0, dto.getRevisionesVencidas());
        assertEquals(100.0, dto.getPorcentajeAdherenciaSeguimiento());
    }

    @Test
    @DisplayName("Calcula tiempos de espera emparejando citas sin reutilizarlas, uso de recursos y canceladas")
    void calculaEsperaUsoRecursosYCanceladasCorrectamente() {
        // Preparar: 2 solicitudes activas de RX-TORAX para PAC-01 (días -10 y -6), 1 sin cita (día -4), 1 cancelada y 1 fuera de rango
        SolicitudPruebaDTO s1 = solicitudService.crearSolicitudPrueba("PAC-01", "MED-01", "RX-TORAX", 1, "Urgente");
        s1.setFechaSolicitud(LocalDateTime.of(hoyFijo.minusDays(10), java.time.LocalTime.of(9, 0)));

        SolicitudPruebaDTO s2 = solicitudService.crearSolicitudPrueba("PAC-01", "MED-01", "RX-TORAX", 2, "Control");
        s2.setFechaSolicitud(LocalDateTime.of(hoyFijo.minusDays(6), java.time.LocalTime.of(10, 0)));

        SolicitudPruebaDTO s3SinCita = solicitudService.crearSolicitudPrueba("PAC-02", "MED-01", "TAC-ABDO", 4, "Estudio");
        s3SinCita.setFechaSolicitud(LocalDateTime.of(hoyFijo.minusDays(4), java.time.LocalTime.of(11, 0)));

        SolicitudPruebaDTO s4Cancelada = solicitudService.crearSolicitudPrueba("PAC-03", "MED-01", "RM-CRANEO", 5, "Duplicada");
        s4Cancelada.setFechaSolicitud(LocalDateTime.of(hoyFijo.minusDays(3), java.time.LocalTime.of(12, 0)));
        solicitudService.cancelarSolicitud(s4Cancelada.getSolicitudId(), "Error administrativo");

        SolicitudPruebaDTO s5FueraRango = solicitudService.crearSolicitudPrueba("PAC-04", "MED-01", "ECO-ABDO", 3, "Antigua");
        s5FueraRango.setFechaSolicitud(LocalDateTime.of(hoyFijo.minusDays(20), java.time.LocalTime.of(9, 0)));

        // Citas para PAC-01: una a los 3 días de s1 (día -7) y otra a los 5 días de s2 (día -1)
        citasService.programarCita("PAC-01", "MED-RAD", "RX-TORAX", hoyFijo.minusDays(7), "09:00-09:30");
        citasService.programarCita("PAC-01", "MED-RAD", "RX-TORAX", hoyFijo.minusDays(1), "10:00-10:30");

        // Ejecutar
        EstadisticasSolicitudesDTO stats = servicio.consultarEstadisticasSolicitudes(
                "MED-01", hoyFijo.minusDays(10), hoyFijo, TOKEN_MEDICO
        );

        // Comprobar
        assertEquals(4, stats.getTotalSolicitudes());
        assertEquals(3, stats.getSolicitudesActivas());
        assertEquals(1, stats.getSolicitudesCanceladas());
        assertEquals(2, stats.getSolicitudesConCita());
        assertEquals(1, stats.getSolicitudesSinCita());
        assertEquals(4.0, stats.getTiempoMedioEsperaCitaDias()); // (3 + 5) / 2 = 4.0 días
        assertEquals(4.0, stats.getTiempoMedioEsperaSinCitaDias()); // hoy - (hoy - 4) = 4.0 días

        assertEquals(2, stats.getUsoPorTipoPrueba().get("RX-TORAX"));
        assertEquals(1, stats.getUsoPorTipoPrueba().get("TAC-ABDO"));
        assertNull(stats.getUsoPorTipoPrueba().get("RM-CRANEO")); // cancelada no consume recurso

        assertEquals(1, stats.getDistribucionPorPrioridad().get("URGENTE"));
        assertEquals(1, stats.getDistribucionPorPrioridad().get("PREFERENTE"));
        assertEquals(1, stats.getDistribucionPorPrioridad().get("ORDINARIA"));
    }

    @Test
    @DisplayName("Calcula adherencia al seguimiento considerando solo revisiones vencidas no canceladas")
    void calculaAdherenciaSeguimientoPeriodico() {
        // Preparar: seguimiento con 3 revisiones cada 10 días empezando hace 15 días -> fechas: -15 (vencida), -5 (vencida), +5 (futura)
        PrescripcionSeguimientoDTO seg = seguimientoService.prescribirSeguimiento(
                "PAC-10", "MED-01", "ECO-ABDO", 10, 3, hoyFijo.minusDays(15), "Control nódulo"
        );
        List<String> citasIds = seg.getCitasGeneradasIds();
        assertEquals(3, citasIds.size());

        // Registrar realización solo de la primera revisión vencida
        CitaDTO primeraCita = citasService.consultarCita(citasIds.get(0));
        registroService.registrarRealizacionPrueba(
                primeraCita.getCitaId(), "PAC-10", "MED-01", "ECO-ABDO", "Sin cambios", null
        );

        // Ejecutar
        EstadisticasSolicitudesDTO stats = servicio.consultarEstadisticasSolicitudes(
                "MED-01", hoyFijo.minusDays(20), hoyFijo, TOKEN_MEDICO
        );

        // Comprobar: 2 revisiones vencidas (-15 y -5), 1 realizada -> 50.0%
        assertEquals(1, stats.getTotalSeguimientos());
        assertEquals(2, stats.getRevisionesVencidas());
        assertEquals(1, stats.getRevisionesRealizadas());
        assertEquals(50.0, stats.getPorcentajeAdherenciaSeguimiento());
    }

    @Test
    @DisplayName("Omite errores de datos en colaboradores pero propaga SecurityException")
    void toleraErroresDeDatosDeColaboradoresYPropagaSeguridad() {
        // Preparar: colaborador que lanza IllegalStateException en consultas de datos
        ISolicitudPruebaService colaboradorConFalloDatos = new ISolicitudPruebaService() {
            @Override
            public SolicitudPruebaDTO crearSolicitudPrueba(String p, String m, String c, int pr, String j) { return null; }

            @Override
            public SolicitudPruebaDTO consultarSolicitud(String id) { return null; }

            @Override
            public List<SolicitudPruebaDTO> listarSolicitudesPaciente(String p) { return List.of(); }

            @Override
            public SolicitudPruebaDTO cancelarSolicitud(String id, String m) { return null; }

            @Override
            public List<SolicitudPruebaDTO> listarSolicitudesMedico(String medicoId) {
                throw new IllegalStateException("Fallo temporal de consulta");
            }
        };

        EstadisticasSolicitudesServiceImpl servicioResiliente = new EstadisticasSolicitudesServiceImpl(
                colaboradorConFalloDatos, citasService, seguimientoService, registroService, gestorRoles, auditService, relojFijo
        );

        // Ejecutar & Comprobar: no rompe el informe ante IllegalStateException
        EstadisticasSolicitudesDTO res = servicioResiliente.consultarEstadisticasSolicitudes(
                "MED-01", hoyFijo.minusDays(5), hoyFijo, TOKEN_MEDICO
        );
        assertEquals(0, res.getTotalSolicitudes());

        // Preparar: colaborador que lanza SecurityException
        ISolicitudPruebaService colaboradorConFalloSeguridad = new ISolicitudPruebaService() {
            @Override
            public SolicitudPruebaDTO crearSolicitudPrueba(String p, String m, String c, int pr, String j) { return null; }

            @Override
            public SolicitudPruebaDTO consultarSolicitud(String id) { return null; }

            @Override
            public List<SolicitudPruebaDTO> listarSolicitudesPaciente(String p) { return List.of(); }

            @Override
            public SolicitudPruebaDTO cancelarSolicitud(String id, String m) { return null; }

            @Override
            public List<SolicitudPruebaDTO> listarSolicitudesMedico(String medicoId) {
                throw new SecurityException("Acceso denegado en repositorio");
            }
        };

        EstadisticasSolicitudesServiceImpl servicioSeguridad = new EstadisticasSolicitudesServiceImpl(
                colaboradorConFalloSeguridad, citasService, seguimientoService, registroService, gestorRoles, auditService, relojFijo
        );

        assertThrows(SecurityException.class, () ->
                servicioSeguridad.consultarEstadisticasSolicitudes("MED-01", hoyFijo.minusDays(5), hoyFijo, TOKEN_MEDICO));
    }
}