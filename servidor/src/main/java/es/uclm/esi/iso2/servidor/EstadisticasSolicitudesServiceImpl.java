package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.analitica.EstadisticasSolicitudesDTO;
import es.uclm.sescam.contratos.analitica.IEstadisticasSolicitudesService;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.ISolicitudPruebaService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EstadisticasSolicitudesServiceImpl implements IEstadisticasSolicitudesService {

    private final ISolicitudPruebaService solicitudPruebaService;
    private final IGestionCitasService gestionCitasService;
    private final IPrescripcionSeguimientoService prescripcionSeguimientoService;
    private final IRegistroPruebaService registroPruebaService;
    private final IGestorRolesService gestorRoles;
    private final IAuditService auditService;
    private final Clock clock;

    public EstadisticasSolicitudesServiceImpl(ISolicitudPruebaService solicitudPruebaService,
                                              IGestionCitasService gestionCitasService,
                                              IPrescripcionSeguimientoService prescripcionSeguimientoService,
                                              IRegistroPruebaService registroPruebaService,
                                              IGestorRolesService gestorRoles,
                                              IAuditService auditService) {
        this(solicitudPruebaService, gestionCitasService, prescripcionSeguimientoService,
                registroPruebaService, gestorRoles, auditService, Clock.systemDefaultZone());
    }

    public EstadisticasSolicitudesServiceImpl(ISolicitudPruebaService solicitudPruebaService,
                                              IGestionCitasService gestionCitasService,
                                              IPrescripcionSeguimientoService prescripcionSeguimientoService,
                                              IRegistroPruebaService registroPruebaService,
                                              IGestorRolesService gestorRoles,
                                              IAuditService auditService,
                                              Clock clock) {
        if (solicitudPruebaService == null || gestionCitasService == null
                || prescripcionSeguimientoService == null || registroPruebaService == null
                || gestorRoles == null) {
            throw new IllegalArgumentException("Todos los servicios colaboradores y de roles son obligatorios");
        }
        this.solicitudPruebaService = solicitudPruebaService;
        this.gestionCitasService = gestionCitasService;
        this.prescripcionSeguimientoService = prescripcionSeguimientoService;
        this.registroPruebaService = registroPruebaService;
        this.gestorRoles = gestorRoles;
        this.auditService = auditService;
        this.clock = (clock != null) ? clock : Clock.systemDefaultZone();
    }

    @Override
    public EstadisticasSolicitudesDTO consultarEstadisticasSolicitudes(String medicoId, LocalDate desde,
                                                                       LocalDate hasta, String tokenSSO) {
        verificarPermisoMedico(tokenSSO);

        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        String medicoLimpio = medicoId.trim();
        LocalDate hoy = LocalDate.now(clock);

        // 1. Obtener y filtrar solicitudes del médico en [desde, hasta]
        List<SolicitudPruebaDTO> todasSolicitudes = obtenerSolicitudesMedicoSeguras(medicoLimpio);
        List<SolicitudPruebaDTO> solicitudesPeriodo = new ArrayList<>();
        for (SolicitudPruebaDTO sol : todasSolicitudes) {
            if (sol == null || sol.getFechaSolicitud() == null) {
                continue;
            }
            LocalDate fechaSol = sol.getFechaSolicitud().toLocalDate();
            if (!fechaSol.isBefore(desde) && !fechaSol.isAfter(hasta)) {
                solicitudesPeriodo.add(sol);
            }
        }

        int totalSolicitudes = solicitudesPeriodo.size();
        int solicitudesCanceladas = 0;
        List<SolicitudPruebaDTO> solicitudesActivasLista = new ArrayList<>();

        for (SolicitudPruebaDTO sol : solicitudesPeriodo) {
            if ("CANCELADA".equalsIgnoreCase(sol.getEstado())) {
                solicitudesCanceladas++;
            } else {
                solicitudesActivasLista.add(sol);
            }
        }

        int solicitudesActivas = solicitudesActivasLista.size();

        // Ordenar solicitudes no canceladas por fecha de solicitud ascendente (ADR-018)
        solicitudesActivasLista.sort(Comparator.comparing(SolicitudPruebaDTO::getFechaSolicitud));

        // 2. Uso de recursos y tiempos de espera hasta cita (ADR-018)
        Map<String, Integer> usoPorTipoPrueba = new LinkedHashMap<>();
        Map<String, Integer> distribucionPorPrioridad = new LinkedHashMap<>();
        Map<String, List<CitaDTO>> citasPorPacienteCache = new HashMap<>();
        Set<String> citasAsignadasIds = new HashSet<>();

        int solicitudesConCita = 0;
        int solicitudesSinCita = 0;
        long sumaDiasEsperaConCita = 0L;
        long sumaDiasEsperaSinCita = 0L;

        for (SolicitudPruebaDTO sol : solicitudesActivasLista) {
            String codigoPrueba = (sol.getCodigoPrueba() != null && !sol.getCodigoPrueba().isBlank())
                    ? sol.getCodigoPrueba().trim().toUpperCase() : "SIN_CODIGO";
            usoPorTipoPrueba.put(codigoPrueba, usoPorTipoPrueba.getOrDefault(codigoPrueba, 0) + 1);

            String prioridad = (sol.getNivelPrioridad() != null && !sol.getNivelPrioridad().isBlank())
                    ? sol.getNivelPrioridad().trim().toUpperCase() : "SIN_PRIORIDAD";
            distribucionPorPrioridad.put(prioridad, distribucionPorPrioridad.getOrDefault(prioridad, 0) + 1);

            LocalDate fechaSol = sol.getFechaSolicitud().toLocalDate();
            CitaDTO citaEmparejada = buscarPrimeraCitaDisponible(
                    sol, fechaSol, citasPorPacienteCache, citasAsignadasIds
            );

            if (citaEmparejada != null) {
                solicitudesConCita++;
                long diasEspera = Math.max(0L, ChronoUnit.DAYS.between(fechaSol, citaEmparejada.getFecha()));
                sumaDiasEsperaConCita += diasEspera;
            } else {
                solicitudesSinCita++;
                long diasSinCita = Math.max(0L, ChronoUnit.DAYS.between(fechaSol, hoy));
                sumaDiasEsperaSinCita += diasSinCita;
            }
        }

        double tiempoMedioEsperaCitaDias = (solicitudesConCita > 0)
                ? redondear2Decimales((double) sumaDiasEsperaConCita / solicitudesConCita) : 0.0;
        double tiempoMedioEsperaSinCitaDias = (solicitudesSinCita > 0)
                ? redondear2Decimales((double) sumaDiasEsperaSinCita / solicitudesSinCita) : 0.0;

        // 3. Adherencia al seguimiento periódico (ADR-018)
        List<PrescripcionSeguimientoDTO> todosSeguimientos = obtenerSeguimientosMedicoSeguros(medicoLimpio);
        int totalSeguimientos = 0;
        int revisionesVencidas = 0;
        int revisionesRealizadas = 0;

        Map<String, Set<String>> citasRealizadasPorPacienteCache = new HashMap<>();

        for (PrescripcionSeguimientoDTO seg : todosSeguimientos) {
            if (seg == null || seg.getFechaInicio() == null) {
                continue;
            }
            if (seg.getFechaInicio().isBefore(desde) || seg.getFechaInicio().isAfter(hasta)) {
                continue;
            }
            if ("CANCELADO".equalsIgnoreCase(seg.getEstado())) {
                continue;
            }

            totalSeguimientos++;
            Set<String> citasRealizadasPaciente = obtenerCitasRealizadasPaciente(
                    seg.getPacienteId(), citasRealizadasPorPacienteCache
            );

            if (seg.getCitasGeneradasIds() != null) {
                for (String citaId : seg.getCitasGeneradasIds()) {
                    CitaDTO cita = consultarCitaSegura(citaId);
                    if (cita == null || cita.getFecha() == null) {
                        continue;
                    }
                    if ("CANCELADA".equalsIgnoreCase(cita.getEstado())) {
                        continue;
                    }
                    if (!cita.getFecha().isAfter(hoy)) {
                        revisionesVencidas++;
                        if (citasRealizadasPaciente.contains(cita.getCitaId())) {
                            revisionesRealizadas++;
                        }
                    }
                }
            }
        }

        double porcentajeAdherencia = (revisionesVencidas == 0)
                ? 100.0
                : redondear2Decimales((revisionesRealizadas * 100.0) / revisionesVencidas);

        registrarAuditoria(medicoLimpio, "CONSULTAR_ESTADISTICAS_SOLICITUDES", true);

        return new EstadisticasSolicitudesDTO(
                medicoLimpio,
                desde,
                hasta,
                totalSolicitudes,
                solicitudesActivas,
                solicitudesCanceladas,
                solicitudesConCita,
                solicitudesSinCita,
                tiempoMedioEsperaCitaDias,
                tiempoMedioEsperaSinCitaDias,
                usoPorTipoPrueba,
                distribucionPorPrioridad,
                totalSeguimientos,
                revisionesVencidas,
                revisionesRealizadas,
                porcentajeAdherencia
        );
    }

    private void verificarPermisoMedico(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            registrarAuditoria("DESCONOCIDO", "ACCESO_DENEGADO_ESTADISTICAS_SOLICITUDES", false);
            throw new SecurityException("Token SSO obligatorio para consultar estadísticas de solicitudes");
        }
        if (!gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO)) {
            registrarAuditoria(tokenSSO.trim(), "ACCESO_DENEGADO_ESTADISTICAS_SOLICITUDES", false);
            throw new SecurityException("Acceso denegado: se requiere rol MEDICO para consultar estadísticas");
        }
    }

    private CitaDTO buscarPrimeraCitaDisponible(SolicitudPruebaDTO solicitud, LocalDate fechaSolicitud,
                                                Map<String, List<CitaDTO>> cacheCitasPaciente,
                                                Set<String> citasAsignadasIds) {
        String pacienteId = solicitud.getPacienteId();
        if (pacienteId == null || pacienteId.isBlank()) {
            return null;
        }

        List<CitaDTO> citasPaciente = cacheCitasPaciente.computeIfAbsent(
                pacienteId.trim(), this::obtenerCitasPacienteOrdenadasSeguras
        );

        for (CitaDTO cita : citasPaciente) {
            if (cita.getCitaId() == null || citasAsignadasIds.contains(cita.getCitaId())) {
                continue;
            }
            if ("CANCELADA".equalsIgnoreCase(cita.getEstado()) || cita.getFecha() == null) {
                continue;
            }
            boolean mismaPrueba = solicitud.getCodigoPrueba() != null
                    && cita.getCodigoPrueba() != null
                    && solicitud.getCodigoPrueba().trim().equalsIgnoreCase(cita.getCodigoPrueba().trim());
            if (mismaPrueba && !cita.getFecha().isBefore(fechaSolicitud)) {
                citasAsignadasIds.add(cita.getCitaId());
                return cita;
            }
        }
        return null;
    }

    private List<SolicitudPruebaDTO> obtenerSolicitudesMedicoSeguras(String medicoId) {
        try {
            List<SolicitudPruebaDTO> lista = solicitudPruebaService.listarSolicitudesMedico(medicoId);
            return (lista != null) ? lista : List.of();
        } catch (SecurityException se) {
            throw se;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return List.of();
        }
    }

    private List<CitaDTO> obtenerCitasPacienteOrdenadasSeguras(String pacienteId) {
        try {
            List<CitaDTO> citas = gestionCitasService.listarCitasPaciente(pacienteId);
            if (citas == null) {
                return List.of();
            }
            List<CitaDTO> ordenadas = new ArrayList<>();
            for (CitaDTO c : citas) {
                if (c != null && c.getFecha() != null) {
                    ordenadas.add(c);
                }
            }
            ordenadas.sort(Comparator.comparing(CitaDTO::getFecha));
            return ordenadas;
        } catch (SecurityException se) {
            throw se;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return List.of();
        }
    }

    private CitaDTO consultarCitaSegura(String citaId) {
        if (citaId == null || citaId.isBlank()) {
            return null;
        }
        try {
            return gestionCitasService.consultarCita(citaId.trim());
        } catch (SecurityException se) {
            throw se;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return null;
        }
    }

    private List<PrescripcionSeguimientoDTO> obtenerSeguimientosMedicoSeguros(String medicoId) {
        try {
            List<PrescripcionSeguimientoDTO> lista = prescripcionSeguimientoService.listarSeguimientosMedico(medicoId);
            return (lista != null) ? lista : List.of();
        } catch (SecurityException se) {
            throw se;
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return List.of();
        }
    }

    private Set<String> obtenerCitasRealizadasPaciente(String pacienteId, Map<String, Set<String>> cache) {
        if (pacienteId == null || pacienteId.isBlank()) {
            return Set.of();
        }
        return cache.computeIfAbsent(pacienteId.trim(), id -> {
            try {
                List<RegistroPruebaDTO> registros = registroPruebaService.listarRegistrosPaciente(id);
                if (registros == null) {
                    return Set.of();
                }
                Set<String> ids = new HashSet<>();
                for (RegistroPruebaDTO reg : registros) {
                    if (reg != null && reg.isRealizada() && reg.getCitaId() != null) {
                        ids.add(reg.getCitaId().trim());
                    }
                }
                return ids;
            } catch (SecurityException se) {
                throw se;
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return Set.of();
            }
        });
    }

    private static double redondear2Decimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "MEDICO";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}