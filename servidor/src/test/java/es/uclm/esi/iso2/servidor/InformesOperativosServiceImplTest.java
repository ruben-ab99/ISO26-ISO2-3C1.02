package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.analitica.InformeOperativoDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.listasespera.IGestorListasEsperaService;
import es.uclm.sescam.contratos.listasespera.PrioridadEspera;
import es.uclm.sescam.contratos.listasespera.SolicitudEsperaDTO;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-16 (Generar informes operativos).
 * Técnicas: clases de equivalencia (tramos disponibles / no disponibles, solicitudes por prioridad,
 * citas programadas / reprogramadas / canceladas, datos nulos), valores límite en los días de espera
 * (registro hoy, en el futuro o sin fecha) y conjetura de errores (fuentes que devuelven null).
 * Agendas, lista de espera y citas se sustituyen por stubs (ADR-012).
 */
class InformesOperativosServiceImplTest {

    private static final String TOKEN_ADMISION = "token-sso-adm01-ADMISION-1";
    private static final String TOKEN_MEDICO = "token-sso-med01-MEDICO-1";
    private static final String TOKEN_ADMIN = "token-sso-admin01-ADMINISTRADOR-1";
    private static final String USUARIO = "ADM-01";
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 23);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 29);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    private List<AgendaDiariaDTO> agendas;
    private List<SolicitudEsperaDTO> pendientes;
    private List<CitaDTO> citas;
    private List<AuditLogDTO> registrados;
    private List<Object> argumentosAgendas;
    private List<String> tiposConsultadosEspera;
    private InformesOperativosServiceImpl servicio;

    @BeforeEach
    void setUp() {
        registrados = new ArrayList<>();
        argumentosAgendas = new ArrayList<>();
        tiposConsultadosEspera = new ArrayList<>();

        agendas = new ArrayList<>();
        agendas.add(agenda("MED-A", LocalDate.of(2026, 11, 24), false, false, false, true));
        agendas.add(agenda("MED-A", LocalDate.of(2026, 11, 25), true, true, true, false));
        agendas.add(agenda("MED-B", LocalDate.of(2026, 11, 24), true, true));
        agendas.add(null);
        AgendaDiariaDTO sinTramos = new AgendaDiariaDTO() {
            @Override
            public List<TramoHorarioDTO> getTramos() {
                return null;
            }
        };
        sinTramos.setFecha(LocalDate.of(2026, 11, 26));
        agendas.add(sinTramos);

        pendientes = new ArrayList<>();
        pendientes.add(espera("S1", PrioridadEspera.URGENTE, LocalDateTime.of(2026, 11, 29, 10, 0), false));
        pendientes.add(espera("S2", PrioridadEspera.ORDINARIA, LocalDateTime.of(2026, 11, 1, 8, 0), false));
        pendientes.add(espera("S3", PrioridadEspera.ORDINARIA, LocalDateTime.of(2026, 11, 21, 8, 0), false));
        pendientes.add(espera("S4", PrioridadEspera.PREFERENTE, null, false));
        pendientes.add(espera("S5", PrioridadEspera.URGENTE, LocalDateTime.of(2026, 10, 1, 8, 0), true));
        pendientes.add(espera("S6", null, LocalDateTime.of(2026, 11, 30, 8, 0), false));
        pendientes.add(espera("S7", PrioridadEspera.PREFERENTE, LocalDateTime.of(2026, 12, 5, 8, 0), false));
        pendientes.add(null);

        citas = new ArrayList<>();
        citas.add(cita("C1", "MED-A", "PROGRAMADA"));
        citas.add(cita("C2", "MED-A", "CANCELADA"));
        citas.add(cita("C3", "MED-B", "CANCELADA"));
        citas.add(cita("C4", "MED-A", "REPROGRAMADA"));
        citas.add(cita("C5", "MED-A", "cancelada"));
        citas.add(cita("C6", null, "CANCELADA"));
        citas.add(cita("C7", "MED-B", null));
        citas.add(null);

        servicio = new InformesOperativosServiceImpl(new GestorRolesServiceImpl(), stubAgendas(), stubListasEspera(),
                stubCitas(), stubAuditoria(), RELOJ);
    }

    private static AgendaDiariaDTO agenda(String medicoId, LocalDate fecha, boolean... disponibles) {
        List<TramoHorarioDTO> tramos = new ArrayList<>();
        LocalTime inicio = LocalTime.of(9, 0);
        for (boolean disponible : disponibles) {
            tramos.add(new TramoHorarioDTO(inicio, inicio.plusMinutes(15), disponible, disponible ? null : "Ocupado"));
            inicio = inicio.plusMinutes(15);
        }
        return new AgendaDiariaDTO("AGD-" + medicoId + fecha, medicoId, fecha, LocalTime.of(9, 0), inicio, 15, true, tramos);
    }

    private static SolicitudEsperaDTO espera(String id, PrioridadEspera prioridad, LocalDateTime registro, boolean atendida) {
        return new SolicitudEsperaDTO(id, "PAC-" + id, "RX-TORAX", prioridad, registro, atendida);
    }

    private static CitaDTO cita(String id, String medicoId, String estado) {
        return new CitaDTO(id, "PAC-" + id, medicoId, "RX-TORAX", LocalDate.of(2026, 11, 25), "09:00-09:30",
                estado, null, LocalDateTime.of(2026, 11, 20, 8, 0));
    }

    private IGestorAgendasService stubAgendas() {
        return new IGestorAgendasService() {
            @Override
            public AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                                          LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicioTramo, LocalTime horaFinTramo,
                                               String motivoBloqueo, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean cerrarAgendaDiaria(String agendaId, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<AgendaDiariaDTO> listarAgendasEntreFechas(LocalDate desde, LocalDate hasta, String tokenSSO) {
                argumentosAgendas.add(desde);
                argumentosAgendas.add(hasta);
                argumentosAgendas.add(tokenSSO);
                return agendas;
            }
        };
    }

    private IGestorListasEsperaService stubListasEspera() {
        return new IGestorListasEsperaService() {
            @Override
            public SolicitudEsperaDTO registrarEnListaEspera(String pacienteId, String tipoPrueba,
                                                             PrioridadEspera prioridad, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<SolicitudEsperaDTO> consultarListaEspera(String tipoPrueba, String tokenSSO) {
                tiposConsultadosEspera.add(tipoPrueba);
                return pendientes;
            }

            @Override
            public boolean actualizarPrioridad(String solicitudId, PrioridadEspera nuevaPrioridad, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int contarHuecosLibres(String medicoId, LocalDate fecha, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SolicitudEsperaDTO asignarHuecoLibre(String solicitudId, String medicoId, LocalDate fecha,
                                                        LocalTime horaInicio, LocalTime horaFin, String tokenSSO) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private IGestionCitasService stubCitas() {
        return new IGestionCitasService() {
            @Override
            public CitaDTO programarCita(String pacienteId, String medicoId, String codigoPrueba, LocalDate fecha,
                                         String franjaHoraria) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CitaDTO consultarCita(String citaId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CitaDTO> listarCitasPaciente(String pacienteId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CitaDTO cancelarCita(String citaId, String motivo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CitaDTO> listarCitasEntreFechas(LocalDate desde, LocalDate hasta) {
                return citas;
            }
        };
    }

    private IAuditService stubAuditoria() {
        return new IAuditService() {
            @Override
            public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
                registrados.add(new AuditLogDTO(usuario, accion, exito, ip));
            }

            @Override
            public List<AuditLogDTO> obtenerTrazas() {
                return new ArrayList<>(registrados);
            }
        };
    }

    @Test
    @DisplayName("El constructor rechaza dependencias nulas")
    void constructorRechazaDependenciasNulas() {
        GestorRolesServiceImpl roles = new GestorRolesServiceImpl();
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(null, stubAgendas(), stubListasEspera(), stubCitas(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(roles, null, stubListasEspera(), stubCitas(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(roles, stubAgendas(), null, stubCitas(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(roles, stubAgendas(), stubListasEspera(), null, stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(roles, stubAgendas(), stubListasEspera(), stubCitas(), null));
        assertThrows(IllegalArgumentException.class, () -> new InformesOperativosServiceImpl(roles, stubAgendas(), stubListasEspera(), stubCitas(), stubAuditoria(), null));
    }

    @Test
    @DisplayName("Calcula la ocupación de agendas del periodo, global y por médico")
    void calculaOcupacion() {
        InformeOperativoDTO informe = servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertEquals(4, informe.getAgendasAnalizadas());
        assertEquals(10, informe.getTotalTramos());
        assertEquals(4, informe.getTramosNoDisponibles());
        assertEquals(40.0, informe.getPorcentajeOcupacion());
        assertEquals(List.of("DESCONOCIDO", "MED-A", "MED-B"), new ArrayList<>(informe.getOcupacionPorMedico().keySet()));
        assertEquals(50.0, informe.getOcupacionPorMedico().get("MED-A"));
        assertEquals(0.0, informe.getOcupacionPorMedico().get("MED-B"));
        assertEquals(0.0, informe.getOcupacionPorMedico().get("DESCONOCIDO"));
        assertEquals(List.of(DESDE, HASTA, TOKEN_ADMISION), argumentosAgendas);
    }

    @Test
    @DisplayName("Calcula los tiempos de espera de la lista pendiente a fecha de generación")
    void calculaTiemposDeEspera() {
        InformeOperativoDTO informe = servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertEquals(6, informe.getSolicitudesEnEspera());
        assertEquals(7.17, informe.getTiempoMedioEsperaDias());
        assertEquals(30, informe.getTiempoMaximoEsperaDias());
        assertEquals(List.of("URGENTE", "PREFERENTE", "ORDINARIA", "SIN_PRIORIDAD"),
                new ArrayList<>(informe.getSolicitudesEnEsperaPorPrioridad().keySet()));
        assertEquals(2, informe.getSolicitudesEnEsperaPorPrioridad().get("PREFERENTE"));
        assertEquals(2.0, informe.getTiempoMedioEsperaPorPrioridad().get("URGENTE"));
        assertEquals(0.0, informe.getTiempoMedioEsperaPorPrioridad().get("PREFERENTE"));
        assertEquals(20.0, informe.getTiempoMedioEsperaPorPrioridad().get("ORDINARIA"));
        assertEquals(1.0, informe.getTiempoMedioEsperaPorPrioridad().get("SIN_PRIORIDAD"));
        assertEquals(1, tiposConsultadosEspera.size());
        assertNull(tiposConsultadosEspera.get(0));
    }

    @Test
    @DisplayName("Calcula cancelaciones y reprogramaciones del periodo y registra el informe")
    void calculaCancelacionesYRegistraElInforme() {
        InformeOperativoDTO informe = servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertEquals(7, informe.getTotalCitas());
        assertEquals(4, informe.getCitasCanceladas());
        assertEquals(1, informe.getCitasReprogramadas());
        assertEquals(57.14, informe.getPorcentajeCancelacion());
        assertEquals(List.of("DESCONOCIDO", "MED-A", "MED-B"), new ArrayList<>(informe.getCancelacionesPorMedico().keySet()));
        assertEquals(2, informe.getCancelacionesPorMedico().get("MED-A"));
        assertEquals(DESDE, informe.getDesde());
        assertEquals(HASTA, informe.getHasta());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), informe.getFechaGeneracion());

        assertEquals(1, registrados.size());
        assertEquals(USUARIO, registrados.get(0).getUsuario());
        assertEquals("GENERAR_INFORME_OPERATIVO:2026-11-23_2026-11-29", registrados.get(0).getAccion());
        assertTrue(registrados.get(0).isExito());
    }

    @Test
    @DisplayName("Sin datos el informe devuelve ceros y mantiene todas las prioridades")
    void informeSinDatos() {
        agendas = new ArrayList<>();
        pendientes = new ArrayList<>();
        citas = new ArrayList<>();

        InformeOperativoDTO informe = servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertEquals(0.0, informe.getPorcentajeOcupacion());
        assertEquals(0, informe.getSolicitudesEnEspera());
        assertEquals(0.0, informe.getTiempoMedioEsperaDias());
        assertEquals(List.of("URGENTE", "PREFERENTE", "ORDINARIA"),
                new ArrayList<>(informe.getSolicitudesEnEsperaPorPrioridad().keySet()));
        assertEquals(0.0, informe.getTiempoMedioEsperaPorPrioridad().get("URGENTE"));
        assertEquals(0.0, informe.getPorcentajeCancelacion());
        assertTrue(informe.getCancelacionesPorMedico().isEmpty());
    }

    @Test
    @DisplayName("Si las fuentes devuelven null se tratan como vacías")
    void fuentesNulas() {
        agendas = null;
        pendientes = null;
        citas = null;

        InformeOperativoDTO informe = servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertEquals(0, informe.getAgendasAnalizadas());
        assertEquals(0, informe.getSolicitudesEnEspera());
        assertEquals(0, informe.getTotalCitas());
    }

    @Test
    @DisplayName("El periodo es obligatorio y su inicio no puede ser posterior al fin")
    void periodoObligatorioYOrdenado() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarInformeOperativo(null, HASTA, USUARIO, TOKEN_ADMISION));
        assertThrows(IllegalArgumentException.class, () -> servicio.generarInformeOperativo(DESDE, null, USUARIO, TOKEN_ADMISION));
        assertThrows(IllegalArgumentException.class, () -> servicio.generarInformeOperativo(HASTA, DESDE, USUARIO, TOKEN_ADMISION));
        assertTrue(argumentosAgendas.isEmpty());
    }

    @Test
    @DisplayName("Solo el rol ADMISION puede generar el informe; los intentos denegados se registran")
    void soloAdmisionGeneraInforme() {
        assertThrows(SecurityException.class, () -> servicio.generarInformeOperativo(DESDE, HASTA, "MED-010", TOKEN_MEDICO));
        assertThrows(SecurityException.class, () -> servicio.generarInformeOperativo(DESDE, HASTA, "ADMIN-1", TOKEN_ADMIN));

        assertEquals(2, registrados.size());
        assertEquals("ACCESO_DENEGADO_INFORMES_OPERATIVOS", registrados.get(0).getAccion());
        assertFalse(registrados.get(0).isExito());
        assertTrue(argumentosAgendas.isEmpty());
        assertTrue(tiposConsultadosEspera.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin token SSO no se genera el informe")
    void tokenVacioDeniega(String token) {
        assertThrows(SecurityException.class, () -> servicio.generarInformeOperativo(DESDE, HASTA, USUARIO, token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("El identificador del usuario de admisión es obligatorio")
    void usuarioObligatorio(String usuario) {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarInformeOperativo(DESDE, HASTA, usuario, TOKEN_ADMISION));
        assertTrue(registrados.isEmpty());
    }

    @Test
    @DisplayName("El constructor sin reloj usa el reloj del sistema")
    void constructorSinRelojUsaElDelSistema() {
        InformesOperativosServiceImpl conRelojSistema = new InformesOperativosServiceImpl(new GestorRolesServiceImpl(),
                stubAgendas(), stubListasEspera(), stubCitas(), stubAuditoria());

        InformeOperativoDTO informe = conRelojSistema.generarInformeOperativo(DESDE, HASTA, USUARIO, TOKEN_ADMISION);

        assertNotNull(informe.getFechaGeneracion());
        assertEquals(10, informe.getTotalTramos());
    }
}
