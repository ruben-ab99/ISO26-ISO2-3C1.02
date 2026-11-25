package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.analitica.EstadisticasSolicitudesDTO;
import es.uclm.sescam.contratos.analitica.IEstadisticasSolicitudesService;
import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.solicitudes.IGestorSolicitudesMedicoService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EstadisticasSolicitudesServiceImpl implements IEstadisticasSolicitudesService {

    private static final String IP_LOCAL = "127.0.0.1";
    private static final List<String> PRIORIDADES = List.of("URGENTE", "PREFERENTE", "ORDINARIA");

    private final IGestorRolesService gestorRoles;
    private final IGestorSolicitudesMedicoService gestorSolicitudes;
    private final IGestionCitasService gestionCitas;
    private final ICatalogoPruebasService catalogoService;
    private final IPrescripcionSeguimientoService prescripcionService;
    private final IRegistroPruebaService registroService;
    private final IAuditService auditService;
    private final Clock reloj;

    public EstadisticasSolicitudesServiceImpl(IGestorRolesService gestorRoles, IGestorSolicitudesMedicoService gestorSolicitudes,
                                              IGestionCitasService gestionCitas, ICatalogoPruebasService catalogoService,
                                              IPrescripcionSeguimientoService prescripcionService,
                                              IRegistroPruebaService registroService, IAuditService auditService) {
        this(gestorRoles, gestorSolicitudes, gestionCitas, catalogoService, prescripcionService, registroService,
                auditService, Clock.systemDefaultZone());
    }

    public EstadisticasSolicitudesServiceImpl(IGestorRolesService gestorRoles, IGestorSolicitudesMedicoService gestorSolicitudes,
                                              IGestionCitasService gestionCitas, ICatalogoPruebasService catalogoService,
                                              IPrescripcionSeguimientoService prescripcionService,
                                              IRegistroPruebaService registroService, IAuditService auditService,
                                              Clock reloj) {
        if (gestorRoles == null || gestorSolicitudes == null || gestionCitas == null || catalogoService == null
                || prescripcionService == null || registroService == null || auditService == null || reloj == null) {
            throw new IllegalArgumentException("Los servicios de roles, solicitudes, citas, catálogo, seguimientos, "
                    + "registros y auditoría y el reloj son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.gestorSolicitudes = gestorSolicitudes;
        this.gestionCitas = gestionCitas;
        this.catalogoService = catalogoService;
        this.prescripcionService = prescripcionService;
        this.registroService = registroService;
        this.auditService = auditService;
        this.reloj = reloj;
    }

    @Override
    public EstadisticasSolicitudesDTO consultarEstadisticasSolicitudes(String medicoId, LocalDate desde,
                                                                       LocalDate hasta, String tokenSSO) {
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        String medico = medicoId.trim();
        if (tokenSSO == null || tokenSSO.isBlank() || !gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO)) {
            auditService.registrarEvento(medico, "ACCESO_DENEGADO_ESTADISTICAS_SOLICITUDES", false, IP_LOCAL);
            throw new SecurityException("Acceso denegado: se requiere rol MEDICO");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }

        LocalDate hoy = LocalDate.now(reloj);
        List<SolicitudPruebaDTO> solicitudes = solicitudesDelPeriodo(medico, desde, hasta, tokenSSO);

        EstadisticasSolicitudesDTO estadisticas = new EstadisticasSolicitudesDTO();
        estadisticas.setMedicoId(medico);
        estadisticas.setDesde(desde);
        estadisticas.setHasta(hasta);
        contarEstados(estadisticas, solicitudes);
        calcularTiemposEspera(estadisticas, solicitudes, hoy);
        calcularUsoRecursos(estadisticas, solicitudes);
        calcularAdherencia(estadisticas, medico, hoy);
        estadisticas.setFechaGeneracion(LocalDateTime.now(reloj));

        auditService.registrarEvento(medico, "CONSULTAR_ESTADISTICAS_SOLICITUDES", true, IP_LOCAL);
        return estadisticas;
    }

    private List<SolicitudPruebaDTO> solicitudesDelPeriodo(String medico, LocalDate desde, LocalDate hasta, String tokenSSO) {
        List<SolicitudPruebaDTO> todas = gestorSolicitudes.consultarSolicitudesMedico(medico, null, tokenSSO);
        List<SolicitudPruebaDTO> resultado = new ArrayList<>();
        if (todas == null) {
            return resultado;
        }
        boolean sinPeriodo = desde == null && hasta == null;
        for (SolicitudPruebaDTO solicitud : todas) {
            if (solicitud == null) {
                continue;
            }
            if (sinPeriodo) {
                resultado.add(solicitud);
            } else if (solicitud.getFechaSolicitud() != null) {
                LocalDate dia = solicitud.getFechaSolicitud().toLocalDate();
                if ((desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta))) {
                    resultado.add(solicitud);
                }
            }
        }
        return resultado;
    }

    private void contarEstados(EstadisticasSolicitudesDTO estadisticas, List<SolicitudPruebaDTO> solicitudes) {
        Map<String, Integer> porPrioridad = new LinkedHashMap<>();
        PRIORIDADES.forEach(p -> porPrioridad.put(p, 0));
        int pendientes = 0;
        int enCurso = 0;
        int realizadas = 0;
        int canceladas = 0;

        for (SolicitudPruebaDTO solicitud : solicitudes) {
            String estado = (solicitud.getEstado() != null) ? solicitud.getEstado().trim().toUpperCase() : "";
            switch (estado) {
                case "CANCELADA" -> canceladas++;
                case "REALIZADA" -> realizadas++;
                case "CITADA", "EN_REALIZACION" -> enCurso++;
                default -> pendientes++;
            }
            porPrioridad.merge(prioridad(solicitud), 1, Integer::sum);
        }

        estadisticas.setTotalSolicitudes(solicitudes.size());
        estadisticas.setSolicitudesPendientes(pendientes);
        estadisticas.setSolicitudesEnCurso(enCurso);
        estadisticas.setSolicitudesRealizadas(realizadas);
        estadisticas.setSolicitudesCanceladas(canceladas);
        estadisticas.setSolicitudesPorPrioridad(porPrioridad);
    }

    private void calcularTiemposEspera(EstadisticasSolicitudesDTO estadisticas, List<SolicitudPruebaDTO> solicitudes,
                                       LocalDate hoy) {
        List<SolicitudPruebaDTO> vigentes = solicitudes.stream()
                .filter(s -> !esCancelada(s) && s.getFechaSolicitud() != null)
                .sorted(Comparator.comparing(SolicitudPruebaDTO::getFechaSolicitud))
                .collect(Collectors.toList());

        Map<String, List<CitaDTO>> citasPorPaciente = new HashMap<>();
        Set<String> citasAsignadas = new HashSet<>();
        Map<String, Long> diasPorPrioridad = new LinkedHashMap<>();
        Map<String, Integer> conCitaPorPrioridad = new LinkedHashMap<>();
        PRIORIDADES.forEach(p -> {
            diasPorPrioridad.put(p, 0L);
            conCitaPorPrioridad.put(p, 0);
        });
        int conCita = 0;
        long sumaConCita = 0;
        long maximoConCita = 0;
        int sinCita = 0;
        long sumaSinCita = 0;

        for (SolicitudPruebaDTO solicitud : vigentes) {
            LocalDate diaSolicitud = solicitud.getFechaSolicitud().toLocalDate();
            List<CitaDTO> citasPaciente = citasPorPaciente.computeIfAbsent(solicitud.getPacienteId(), this::citasDelPaciente);
            CitaDTO cita = primeraCitaPosterior(solicitud, diaSolicitud, citasPaciente, citasAsignadas);
            if (cita != null) {
                citasAsignadas.add(cita.getCitaId());
                long dias = ChronoUnit.DAYS.between(diaSolicitud, cita.getFecha());
                String prioridad = prioridad(solicitud);
                diasPorPrioridad.merge(prioridad, dias, Long::sum);
                conCitaPorPrioridad.merge(prioridad, 1, Integer::sum);
                conCita++;
                sumaConCita += dias;
                maximoConCita = Math.max(maximoConCita, dias);
            } else {
                sinCita++;
                sumaSinCita += Math.max(0, ChronoUnit.DAYS.between(diaSolicitud, hoy));
            }
        }

        Map<String, Double> mediaPorPrioridad = new LinkedHashMap<>();
        conCitaPorPrioridad.forEach((prioridad, cantidad) ->
                mediaPorPrioridad.put(prioridad, media(diasPorPrioridad.get(prioridad), cantidad)));

        estadisticas.setSolicitudesConCita(conCita);
        estadisticas.setTiempoMedioHastaCitaDias(media(sumaConCita, conCita));
        estadisticas.setTiempoMaximoHastaCitaDias(maximoConCita);
        estadisticas.setTiempoMedioHastaCitaPorPrioridad(mediaPorPrioridad);
        estadisticas.setSolicitudesSinCita(sinCita);
        estadisticas.setTiempoMedioSinCitaDias(media(sumaSinCita, sinCita));
    }

    private CitaDTO primeraCitaPosterior(SolicitudPruebaDTO solicitud, LocalDate diaSolicitud,
                                         List<CitaDTO> citasPaciente, Set<String> citasAsignadas) {
        CitaDTO elegida = null;
        for (CitaDTO cita : citasPaciente) {
            boolean candidata = cita != null
                    && !citasAsignadas.contains(cita.getCitaId())
                    && !"CANCELADA".equalsIgnoreCase(cita.getEstado())
                    && cita.getFecha() != null
                    && !cita.getFecha().isBefore(diaSolicitud)
                    && cita.getCodigoPrueba() != null
                    && cita.getCodigoPrueba().equalsIgnoreCase(solicitud.getCodigoPrueba());
            if (candidata && (elegida == null || cita.getFecha().isBefore(elegida.getFecha()))) {
                elegida = cita;
            }
        }
        return elegida;
    }

    private void calcularUsoRecursos(EstadisticasSolicitudesDTO estadisticas, List<SolicitudPruebaDTO> solicitudes) {
        Map<String, TipoPruebaDTO> catalogoConsultado = new HashMap<>();
        Map<String, Integer> porModalidad = new LinkedHashMap<>();
        Map<String, Integer> minutosPorModalidad = new LinkedHashMap<>();
        Map<String, Integer> porEquipoSala = new LinkedHashMap<>();

        for (SolicitudPruebaDTO solicitud : solicitudes) {
            if (esCancelada(solicitud)) {
                continue;
            }
            String codigo = solicitud.getCodigoPrueba();
            if (!catalogoConsultado.containsKey(codigo)) {
                catalogoConsultado.put(codigo, detallePrueba(codigo));
            }
            TipoPruebaDTO prueba = catalogoConsultado.get(codigo);
            String modalidad = (prueba != null && prueba.getModalidad() != null)
                    ? prueba.getModalidad().trim().toUpperCase() : "DESCONOCIDA";
            String equipoSala = (prueba != null && prueba.getEquipoSala() != null)
                    ? prueba.getEquipoSala().trim() : "DESCONOCIDO";
            porModalidad.merge(modalidad, 1, Integer::sum);
            minutosPorModalidad.merge(modalidad, (prueba != null) ? prueba.getDuracionEstimadaMinutos() : 0, Integer::sum);
            porEquipoSala.merge(equipoSala, 1, Integer::sum);
        }

        estadisticas.setSolicitudesPorModalidad(ordenarPorFrecuencia(porModalidad));
        estadisticas.setMinutosEstimadosPorModalidad(ordenarPorFrecuencia(minutosPorModalidad));
        estadisticas.setSolicitudesPorEquipoSala(ordenarPorFrecuencia(porEquipoSala));
    }

    private void calcularAdherencia(EstadisticasSolicitudesDTO estadisticas, String medico, LocalDate hoy) {
        List<PrescripcionSeguimientoDTO> seguimientos = prescripcionService.listarSeguimientosMedico(medico);
        Map<String, Set<String>> realizadasPorPaciente = new HashMap<>();
        int activos = 0;
        int cancelados = 0;
        int programadas = 0;
        int vencidas = 0;
        int realizadas = 0;

        if (seguimientos != null) {
            for (PrescripcionSeguimientoDTO seguimiento : seguimientos) {
                if (seguimiento == null) {
                    continue;
                }
                if ("ACTIVO".equalsIgnoreCase(seguimiento.getEstado())) {
                    activos++;
                } else if ("CANCELADO".equalsIgnoreCase(seguimiento.getEstado())) {
                    cancelados++;
                }
                if (seguimiento.getCitasGeneradasIds() == null) {
                    continue;
                }
                for (String citaId : seguimiento.getCitasGeneradasIds()) {
                    CitaDTO cita = consultarCita(citaId);
                    if (cita == null || cita.getFecha() == null || "CANCELADA".equalsIgnoreCase(cita.getEstado())) {
                        continue;
                    }
                    programadas++;
                    if (!cita.getFecha().isAfter(hoy)) {
                        vencidas++;
                        Set<String> realizadasPaciente = realizadasPorPaciente.computeIfAbsent(
                                seguimiento.getPacienteId(), this::citasRealizadasDelPaciente);
                        if (realizadasPaciente.contains(cita.getCitaId())) {
                            realizadas++;
                        }
                    }
                }
            }
        }

        estadisticas.setSeguimientosActivos(activos);
        estadisticas.setSeguimientosCancelados(cancelados);
        estadisticas.setRevisionesProgramadas(programadas);
        estadisticas.setRevisionesVencidas(vencidas);
        estadisticas.setRevisionesRealizadas(realizadas);
        // Sin revisiones vencidas no hay ninguna incumplida: la adherencia se considera completa (ADR-018).
        estadisticas.setPorcentajeAdherencia(vencidas == 0 ? 100.0 : porcentaje(realizadas, vencidas));
    }

    // Los errores de datos de los colaboradores no invalidan la estadística; los de seguridad sí se propagan.
    private List<CitaDTO> citasDelPaciente(String pacienteId) {
        try {
            List<CitaDTO> citas = gestionCitas.listarCitasPaciente(pacienteId);
            return (citas != null) ? citas : List.of();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return List.of();
        }
    }

    private CitaDTO consultarCita(String citaId) {
        try {
            return gestionCitas.consultarCita(citaId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    private TipoPruebaDTO detallePrueba(String codigoPrueba) {
        try {
            return catalogoService.consultarDetallePrueba(codigoPrueba);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    private Set<String> citasRealizadasDelPaciente(String pacienteId) {
        Set<String> realizadas = new HashSet<>();
        try {
            List<RegistroPruebaDTO> registros = registroService.listarRegistrosPaciente(pacienteId);
            if (registros != null) {
                for (RegistroPruebaDTO registro : registros) {
                    if (registro != null && registro.isRealizada() && registro.getCitaId() != null) {
                        realizadas.add(registro.getCitaId());
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return realizadas;
        }
        return realizadas;
    }

    private boolean esCancelada(SolicitudPruebaDTO solicitud) {
        return "CANCELADA".equalsIgnoreCase(solicitud.getEstado());
    }

    private String prioridad(SolicitudPruebaDTO solicitud) {
        String nivel = solicitud.getNivelPrioridad();
        return (nivel != null && !nivel.isBlank()) ? nivel.trim().toUpperCase() : "SIN_PRIORIDAD";
    }

    private double porcentaje(int parte, int total) {
        return Math.round(parte * 10000.0 / total) / 100.0;
    }

    private double media(long suma, int cantidad) {
        if (cantidad == 0) {
            return 0.0;
        }
        return Math.round(suma * 100.0 / cantidad) / 100.0;
    }

    private Map<String, Integer> ordenarPorFrecuencia(Map<String, Integer> conteos) {
        Map<String, Integer> ordenado = new LinkedHashMap<>();
        conteos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<String, Integer>comparingByKey()))
                .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
        return ordenado;
    }
}
