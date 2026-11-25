package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.analitica.EstadisticasSolicitudesDTO;
import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.solicitudes.IGestorSolicitudesMedicoService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-11 (Consultar estadísticas de sus solicitudes).
 * Técnicas: clases de equivalencia (estados y prioridades de solicitud, solicitud con / sin cita,
 * cita cancelada / de otra prueba / anterior a la solicitud, revisión futura / vencida / realizada),
 * valores límite (periodo y revisión con fecha de hoy) y conjetura de errores (fuentes nulas,
 * pruebas inexistentes, errores de los colaboradores). Todos los colaboradores son stubs (ADR-012).
 */
class EstadisticasSolicitudesServiceImplTest {

    private static final String TOKEN_MEDICO = "token-sso-med10-MEDICO-1";
    private static final String TOKEN_PACIENTE = "token-sso-pac01-PACIENTE-1";
    private static final String MEDICO = "MED-010";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    private List<SolicitudPruebaDTO> solicitudes;
    private Map<String, List<CitaDTO>> citasPorPaciente;
    private Map<String, CitaDTO> citasPorId;
    private Map<String, TipoPruebaDTO> catalogo;
    private List<PrescripcionSeguimientoDTO> seguimientos;
    private Map<String, List<RegistroPruebaDTO>> registrosPorPaciente;
    private List<AuditLogDTO> registrados;
    private List<Object> argumentosSolicitudes;
    private List<String> medicosSeguimiento;
    private EstadisticasSolicitudesServiceImpl servicio;

    @BeforeEach
    void setUp() {
        registrados = new ArrayList<>();
        argumentosSolicitudes = new ArrayList<>();
        medicosSeguimiento = new ArrayList<>();

        solicitudes = new ArrayList<>();
        solicitudes.add(solicitud("SOL-1", "PAC-1", "RX-TORAX", "URGENTE", "PENDIENTE", LocalDateTime.of(2026, 11, 2, 10, 0)));
        solicitudes.add(solicitud("SOL-2", "PAC-1", "RX-TORAX", "ORDINARIA", "CITADA", LocalDateTime.of(2026, 11, 3, 9, 0)));
        solicitudes.add(solicitud("SOL-3", "PAC-2", "TAC-ABDO", "PREFERENTE", "PENDIENTE", LocalDateTime.of(2026, 11, 10, 12, 0)));
        solicitudes.add(solicitud("SOL-4", "PAC-3", "RM-CRANEO", "ORDINARIA", "PENDIENTE", LocalDateTime.of(2026, 11, 21, 8, 0)));
        solicitudes.add(solicitud("SOL-5", "PAC-4", "XX-INEXISTENTE", null, "EN_REALIZACION", LocalDateTime.of(2026, 11, 26, 8, 0)));
        solicitudes.add(solicitud("SOL-6", "PAC-5", "ECO-ABDO", "URGENTE", "CANCELADA", LocalDateTime.of(2026, 11, 15, 8, 0)));
        solicitudes.add(solicitud("SOL-7", "PAC-6", "RX-TORAX", "PREFERENTE", "REALIZADA", LocalDateTime.of(2026, 10, 15, 8, 0)));
        solicitudes.add(solicitud("SOL-8", "PAC-7", "RX-TORAX", "ORDINARIA", "PENDIENTE", null));
        solicitudes.add(null);

        citasPorPaciente = new HashMap<>();
        citasPorPaciente.put("PAC-1", List.of(
                cita("C-0", "PAC-1", "RX-TORAX", LocalDate.of(2026, 10, 30), "PROGRAMADA"),
                cita("C-1", "PAC-1", "RX-TORAX", LocalDate.of(2026, 11, 4), "PROGRAMADA"),
                cita("C-X", "PAC-1", "TAC-ABDO", LocalDate.of(2026, 11, 5), "PROGRAMADA"),
                cita("C-2", "PAC-1", "RX-TORAX", LocalDate.of(2026, 11, 13), "PROGRAMADA")));
        citasPorPaciente.put("PAC-2", Arrays.asList(
                cita("C-3", "PAC-2", "TAC-ABDO", LocalDate.of(2026, 11, 20), "CANCELADA"),
                null,
                cita("C-N", "PAC-2", null, LocalDate.of(2026, 11, 12), "PROGRAMADA"),
                cita("C-F", "PAC-2", "TAC-ABDO", null, "PROGRAMADA"),
                cita("C-4", "PAC-2", "TAC-ABDO", LocalDate.of(2026, 11, 25), "REPROGRAMADA")));
        citasPorPaciente.put("PAC-6", List.of(cita("C-7", "PAC-6", "RX-TORAX", LocalDate.of(2026, 10, 18), "PROGRAMADA")));

        catalogo = new HashMap<>();
        catalogo.put("RX-TORAX", prueba("RX-TORAX", "RX", "Sala RX-01", 15));
        catalogo.put("TAC-ABDO", prueba("TAC-ABDO", "TAC", "Sala TAC-02", 30));
        catalogo.put("RM-CRANEO", prueba("RM-CRANEO", "RM", "Sala RM-01", 45));
        catalogo.put("ECO-ABDO", prueba("ECO-ABDO", "ECO", "Sala ECO-03", 20));

        citasPorId = new HashMap<>();
        citasPorId.put("CS-1", cita("CS-1", "PAC-1", "RX-TORAX", LocalDate.of(2026, 11, 10), "PROGRAMADA"));
        citasPorId.put("CS-2", cita("CS-2", "PAC-1", "RX-TORAX", LocalDate.of(2026, 11, 24), "PROGRAMADA"));
        citasPorId.put("CS-3", cita("CS-3", "PAC-1", "RX-TORAX", LocalDate.of(2026, 12, 15), "PROGRAMADA"));
        citasPorId.put("CS-4", cita("CS-4", "PAC-2", "TAC-ABDO", LocalDate.of(2026, 11, 15), "CANCELADA"));
        citasPorId.put("CS-5", cita("CS-5", "PAC-3", "RM-CRANEO", LocalDate.of(2026, 12, 1), "PROGRAMADA"));
        citasPorId.put("CS-6", cita("CS-6", "PAC-3", "RM-CRANEO", null, "PROGRAMADA"));

        seguimientos = new ArrayList<>();
        seguimientos.add(seguimiento("SEG-1", "PAC-1", "ACTIVO", List.of("CS-1", "CS-2", "CS-3")));
        seguimientos.add(seguimiento("SEG-2", "PAC-2", "CANCELADO", List.of("CS-4")));
        seguimientos.add(seguimiento("SEG-3", "PAC-3", "ACTIVO", List.of("CS-5", "CS-NOEXISTE", "CS-6")));
        seguimientos.add(null);
        seguimientos.add(seguimiento("SEG-4", "PAC-8", "ACTIVO", null));
        seguimientos.add(seguimiento("SEG-5", "PAC-9", "FINALIZADO", List.of()));

        registrosPorPaciente = new HashMap<>();
        registrosPorPaciente.put("PAC-1", Arrays.asList(
                registro("R-1", "CS-1", "PAC-1", true),
                registro("R-2", "CS-2", "PAC-1", false),
                null,
                registro("R-3", null, "PAC-1", true)));
        registrosPorPaciente.put("PAC-3", List.of(registro("R-5", "CS-5", "PAC-3", true)));

        servicio = crearServicio(RELOJ);
    }

    private EstadisticasSolicitudesServiceImpl crearServicio(Clock reloj) {
        return new EstadisticasSolicitudesServiceImpl(new GestorRolesServiceImpl(), stubSolicitudes(), stubCitas(),
                stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria(), reloj);
    }

    private static SolicitudPruebaDTO solicitud(String id, String pacienteId, String codigo, String nivel,
                                                String estado, LocalDateTime fecha) {
        return new SolicitudPruebaDTO(id, pacienteId, MEDICO, codigo, "Prueba " + codigo, 3, nivel,
                "Indicación clínica", estado, null, fecha);
    }

    private static CitaDTO cita(String id, String pacienteId, String codigo, LocalDate fecha, String estado) {
        return new CitaDTO(id, pacienteId, "MED-REALIZADOR", codigo, fecha, "09:00-09:30", estado, null,
                LocalDateTime.of(2026, 11, 1, 8, 0));
    }

    private static TipoPruebaDTO prueba(String codigo, String modalidad, String sala, int minutos) {
        return new TipoPruebaDTO(codigo, "Prueba " + codigo, modalidad, sala, minutos, "", false, false, "", true);
    }

    private static PrescripcionSeguimientoDTO seguimiento(String id, String pacienteId, String estado, List<String> citas) {
        return new PrescripcionSeguimientoDTO(id, pacienteId, MEDICO, "RX-TORAX", "Prueba RX-TORAX", 30, 3,
                LocalDate.of(2026, 11, 10), "Control periódico", estado, null, citas, LocalDateTime.of(2026, 11, 1, 8, 0));
    }

    private static RegistroPruebaDTO registro(String id, String citaId, String pacienteId, boolean realizada) {
        return new RegistroPruebaDTO(id, citaId, pacienteId, "MED-REALIZADOR", "RX-TORAX", "", "", realizada,
                LocalDateTime.of(2026, 11, 10, 10, 0));
    }

    private IGestorSolicitudesMedicoService stubSolicitudes() {
        return new IGestorSolicitudesMedicoService() {
            @Override
            public SolicitudPruebaDTO registrarSolicitudMedico(String pacienteId, String medicoId, String codigoPrueba,
                                                               int prioridad, String justificacionClinica, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<SolicitudPruebaDTO> consultarSolicitudesMedico(String medicoId, String filtroEstado, String tokenSSO) {
                argumentosSolicitudes.add(medicoId);
                argumentosSolicitudes.add(filtroEstado);
                argumentosSolicitudes.add(tokenSSO);
                return solicitudes;
            }

            @Override
            public SolicitudPruebaDTO actualizarEstadoSolicitud(String solicitudId, String nuevoEstado, String motivo,
                                                                String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AgendaDiariaDTO consultarAgendaRealizacion(String medicoRealizadorId, LocalDate fecha, String tokenSSO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<TramoHorarioDTO> consultarHuecosDisponiblesRealizacion(String medicoRealizadorId, LocalDate fecha,
                                                                              String tokenSSO) {
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
                CitaDTO cita = citasPorId.get(citaId);
                if (cita == null) {
                    throw new IllegalStateException("No existe ninguna cita con ID: " + citaId);
                }
                return cita;
            }

            @Override
            public List<CitaDTO> listarCitasPaciente(String pacienteId) {
                if (pacienteId == null || "PAC-4".equals(pacienteId)) {
                    throw new IllegalArgumentException("Paciente no válido");
                }
                if ("PAC-SEC".equals(pacienteId)) {
                    throw new SecurityException("Token de sesión obligatorio para listar citas");
                }
                return citasPorPaciente.get(pacienteId);
            }

            @Override
            public CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CitaDTO cancelarCita(String citaId, String motivo) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private ICatalogoPruebasService stubCatalogo() {
        return new ICatalogoPruebasService() {
            @Override
            public List<TipoPruebaDTO> listarCatalogoPruebas() {
                return new ArrayList<>(catalogo.values());
            }

            @Override
            public TipoPruebaDTO consultarDetallePrueba(String codigoPrueba) {
                if (codigoPrueba == null || codigoPrueba.isBlank()) {
                    throw new IllegalArgumentException("El código de la prueba es obligatorio");
                }
                TipoPruebaDTO prueba = catalogo.get(codigoPrueba);
                if (prueba == null) {
                    throw new IllegalStateException("No existe ninguna prueba activa con código: " + codigoPrueba);
                }
                return prueba;
            }

            @Override
            public List<TipoPruebaDTO> filtrarPorModalidad(String modalidad) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private IPrescripcionSeguimientoService stubSeguimientos() {
        return new IPrescripcionSeguimientoService() {
            @Override
            public PrescripcionSeguimientoDTO prescribirSeguimiento(String pacienteId, String medicoId, String codigoPrueba,
                                                                    int periodicidadDias, int numeroRevisiones,
                                                                    LocalDate fechaInicio, String indicacionesClinicas) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrescripcionSeguimientoDTO consultarSeguimiento(String seguimientoId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String pacienteId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrescripcionSeguimientoDTO cancelarSeguimiento(String seguimientoId, String motivo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<PrescripcionSeguimientoDTO> listarSeguimientosMedico(String medicoId) {
                medicosSeguimiento.add(medicoId);
                return seguimientos;
            }
        };
    }

    private IRegistroPruebaService stubRegistros() {
        return new IRegistroPruebaService() {
            @Override
            public RegistroPruebaDTO registrarRealizacionPrueba(String citaId, String pacienteId, String medicoId,
                                                                String tipoPrueba, String observaciones, String incidencias) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RegistroPruebaDTO consultarRegistroPrueba(String registroId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<RegistroPruebaDTO> listarRegistrosPaciente(String pacienteId) {
                if (pacienteId == null) {
                    throw new IllegalArgumentException("El ID del paciente es obligatorio");
                }
                return registrosPorPaciente.get(pacienteId);
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
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(null, stubSolicitudes(), stubCitas(), stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, null, stubCitas(), stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), null, stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), stubCitas(), null, stubSeguimientos(), stubRegistros(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), stubCitas(), stubCatalogo(), null, stubRegistros(), stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), stubCitas(), stubCatalogo(), stubSeguimientos(), null, stubAuditoria()));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), stubCitas(), stubCatalogo(), stubSeguimientos(), stubRegistros(), null));
        assertThrows(IllegalArgumentException.class, () -> new EstadisticasSolicitudesServiceImpl(roles, stubSolicitudes(), stubCitas(), stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria(), null));
    }

    @Test
    @DisplayName("Cuenta las solicitudes por estado y prioridad")
    void cuentaEstadosYPrioridades() {
        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(8, e.getTotalSolicitudes());
        assertEquals(4, e.getSolicitudesPendientes());
        assertEquals(2, e.getSolicitudesEnCurso());
        assertEquals(1, e.getSolicitudesRealizadas());
        assertEquals(1, e.getSolicitudesCanceladas());
        assertEquals(List.of("URGENTE", "PREFERENTE", "ORDINARIA", "SIN_PRIORIDAD"),
                new ArrayList<>(e.getSolicitudesPorPrioridad().keySet()));
        assertEquals(3, e.getSolicitudesPorPrioridad().get("ORDINARIA"));
        assertEquals(1, e.getSolicitudesPorPrioridad().get("SIN_PRIORIDAD"));
    }

    @Test
    @DisplayName("Calcula los días hasta la primera cita válida y la espera de las solicitudes sin cita")
    void calculaTiemposDeEspera() {
        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(4, e.getSolicitudesConCita());
        assertEquals(7.5, e.getTiempoMedioHastaCitaDias());
        assertEquals(15, e.getTiempoMaximoHastaCitaDias());
        assertEquals(List.of("URGENTE", "PREFERENTE", "ORDINARIA"),
                new ArrayList<>(e.getTiempoMedioHastaCitaPorPrioridad().keySet()));
        assertEquals(2.0, e.getTiempoMedioHastaCitaPorPrioridad().get("URGENTE"));
        assertEquals(9.0, e.getTiempoMedioHastaCitaPorPrioridad().get("PREFERENTE"));
        assertEquals(10.0, e.getTiempoMedioHastaCitaPorPrioridad().get("ORDINARIA"));
        assertEquals(2, e.getSolicitudesSinCita());
        assertEquals(7.5, e.getTiempoMedioSinCitaDias());
    }

    @Test
    @DisplayName("Agrega el uso de recursos por modalidad, minutos estimados y equipo o sala")
    void calculaUsoDeRecursos() {
        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(List.of("RX", "DESCONOCIDA", "RM", "TAC"), new ArrayList<>(e.getSolicitudesPorModalidad().keySet()));
        assertEquals(4, e.getSolicitudesPorModalidad().get("RX"));
        assertEquals(List.of("RX", "RM", "TAC", "DESCONOCIDA"), new ArrayList<>(e.getMinutosEstimadosPorModalidad().keySet()));
        assertEquals(60, e.getMinutosEstimadosPorModalidad().get("RX"));
        assertEquals(0, e.getMinutosEstimadosPorModalidad().get("DESCONOCIDA"));
        assertEquals(List.of("Sala RX-01", "DESCONOCIDO", "Sala RM-01", "Sala TAC-02"),
                new ArrayList<>(e.getSolicitudesPorEquipoSala().keySet()));
        assertFalse(e.getSolicitudesPorModalidad().containsKey("ECO"));
    }

    @Test
    @DisplayName("La adherencia compara revisiones vencidas (hasta hoy incluido) con las realizadas")
    void calculaAdherencia() {
        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(3, e.getSeguimientosActivos());
        assertEquals(1, e.getSeguimientosCancelados());
        assertEquals(4, e.getRevisionesProgramadas());
        assertEquals(3, e.getRevisionesVencidas());
        assertEquals(2, e.getRevisionesRealizadas());
        assertEquals(66.67, e.getPorcentajeAdherencia());
    }

    @Test
    @DisplayName("Registra la consulta y delega en los servicios con el médico normalizado")
    void registraLaConsultaYDelega() {
        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes("  MED-010 ", null, null, TOKEN_MEDICO);

        assertEquals(MEDICO, e.getMedicoId());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), e.getFechaGeneracion());
        assertEquals(Arrays.asList(MEDICO, null, TOKEN_MEDICO), argumentosSolicitudes);
        assertEquals(List.of(MEDICO), medicosSeguimiento);
        assertEquals(1, registrados.size());
        assertEquals(MEDICO, registrados.get(0).getUsuario());
        assertEquals("CONSULTAR_ESTADISTICAS_SOLICITUDES", registrados.get(0).getAccion());
        assertTrue(registrados.get(0).isExito());
    }

    @Test
    @DisplayName("El periodo filtra por fecha de solicitud con los extremos incluidos")
    void periodoFiltraPorFechaDeSolicitud() {
        LocalDate dia2 = LocalDate.of(2026, 11, 2);
        LocalDate dia10 = LocalDate.of(2026, 11, 10);

        EstadisticasSolicitudesDTO conExtremos = servicio.consultarEstadisticasSolicitudes(MEDICO, dia2, dia10, TOKEN_MEDICO);

        assertEquals(3, conExtremos.getTotalSolicitudes());
        assertEquals(dia2, conExtremos.getDesde());
        assertEquals(dia10, conExtremos.getHasta());
        assertEquals(2, servicio.consultarEstadisticasSolicitudes(MEDICO, dia2, dia10.minusDays(1), TOKEN_MEDICO).getTotalSolicitudes());
        assertEquals(2, servicio.consultarEstadisticasSolicitudes(MEDICO, LocalDate.of(2026, 11, 21), null, TOKEN_MEDICO).getTotalSolicitudes());
        assertEquals(1, servicio.consultarEstadisticasSolicitudes(MEDICO, null, LocalDate.of(2026, 10, 31), TOKEN_MEDICO).getTotalSolicitudes());
    }

    @Test
    @DisplayName("Sin revisiones vencidas la adherencia se considera completa")
    void sinRevisionesVencidasAdherenciaCompleta() {
        seguimientos = new ArrayList<>(List.of(seguimiento("SEG-9", "PAC-1", "ACTIVO", List.of("CS-3"))));

        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(1, e.getRevisionesProgramadas());
        assertEquals(0, e.getRevisionesVencidas());
        assertEquals(100.0, e.getPorcentajeAdherencia());
    }

    @Test
    @DisplayName("Si las fuentes devuelven null las estadísticas quedan a cero")
    void fuentesNulas() {
        solicitudes = null;
        seguimientos = null;

        EstadisticasSolicitudesDTO e = servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertEquals(0, e.getTotalSolicitudes());
        assertEquals(0, e.getSolicitudesPorPrioridad().get("URGENTE"));
        assertEquals(0.0, e.getTiempoMedioHastaCitaDias());
        assertTrue(e.getSolicitudesPorModalidad().isEmpty());
        assertEquals(0, e.getSeguimientosActivos());
        assertEquals(100.0, e.getPorcentajeAdherencia());
    }

    @Test
    @DisplayName("Un periodo con inicio posterior al fin se rechaza")
    void periodoInvertidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarEstadisticasSolicitudes(
                MEDICO, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), TOKEN_MEDICO));
        assertTrue(argumentosSolicitudes.isEmpty());
    }

    @Test
    @DisplayName("Solo el rol MEDICO consulta sus estadísticas; los intentos denegados se registran")
    void soloMedicoConsulta() {
        assertThrows(SecurityException.class, () -> servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_PACIENTE));

        assertEquals(1, registrados.size());
        assertEquals("ACCESO_DENEGADO_ESTADISTICAS_SOLICITUDES", registrados.get(0).getAccion());
        assertFalse(registrados.get(0).isExito());
        assertTrue(argumentosSolicitudes.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin token SSO no se consultan estadísticas")
    void tokenVacioDeniega(String token) {
        assertThrows(SecurityException.class, () -> servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("El identificador del médico es obligatorio")
    void medicoObligatorio(String medicoId) {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarEstadisticasSolicitudes(medicoId, null, null, TOKEN_MEDICO));
        assertTrue(registrados.isEmpty());
    }

    @Test
    @DisplayName("Los errores de seguridad de los colaboradores no se ocultan")
    void erroresDeSeguridadSePropagan() {
        solicitudes.add(solicitud("SOL-9", "PAC-SEC", "RX-TORAX", "URGENTE", "PENDIENTE", LocalDateTime.of(2026, 11, 28, 8, 0)));

        assertThrows(SecurityException.class, () -> servicio.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO));
    }

    @Test
    @DisplayName("El constructor sin reloj usa el reloj del sistema")
    void constructorSinRelojUsaElDelSistema() {
        EstadisticasSolicitudesServiceImpl conRelojSistema = new EstadisticasSolicitudesServiceImpl(new GestorRolesServiceImpl(),
                stubSolicitudes(), stubCitas(), stubCatalogo(), stubSeguimientos(), stubRegistros(), stubAuditoria());

        EstadisticasSolicitudesDTO e = conRelojSistema.consultarEstadisticasSolicitudes(MEDICO, null, null, TOKEN_MEDICO);

        assertNotNull(e.getFechaGeneracion());
        assertEquals(8, e.getTotalSolicitudes());
    }
}
