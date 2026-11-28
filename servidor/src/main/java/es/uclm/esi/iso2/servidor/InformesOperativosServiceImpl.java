package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.analitica.IInformesOperativosService;
import es.uclm.sescam.contratos.analitica.InformeOperativoDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.listasespera.IGestorListasEsperaService;
import es.uclm.sescam.contratos.listasespera.PrioridadEspera;
import es.uclm.sescam.contratos.listasespera.SolicitudEsperaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class InformesOperativosServiceImpl implements IInformesOperativosService {

    private static final String IP_LOCAL = "127.0.0.1";

    private final IGestorRolesService gestorRoles;
    private final IGestorAgendasService gestorAgendas;
    private final IGestorListasEsperaService listasEspera;
    private final IGestionCitasService gestionCitas;
    private final IAuditService auditService;
    private final Clock reloj;

    public InformesOperativosServiceImpl(IGestorRolesService gestorRoles, IGestorAgendasService gestorAgendas,
                                         IGestorListasEsperaService listasEspera, IGestionCitasService gestionCitas,
                                         IAuditService auditService) {
        this(gestorRoles, gestorAgendas, listasEspera, gestionCitas, auditService, Clock.systemDefaultZone());
    }

    public InformesOperativosServiceImpl(IGestorRolesService gestorRoles, IGestorAgendasService gestorAgendas,
                                         IGestorListasEsperaService listasEspera, IGestionCitasService gestionCitas,
                                         IAuditService auditService, Clock reloj) {
        if (gestorRoles == null || gestorAgendas == null || listasEspera == null || gestionCitas == null
                || auditService == null || reloj == null) {
            throw new IllegalArgumentException("Los servicios de roles, agendas, listas de espera, citas y auditoría y el reloj son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.gestorAgendas = gestorAgendas;
        this.listasEspera = listasEspera;
        this.gestionCitas = gestionCitas;
        this.auditService = auditService;
        this.reloj = reloj;
    }

    @Override
    public InformeOperativoDTO generarInformeOperativo(LocalDate desde, LocalDate hasta,
                                                       String usuarioAdmisionId, String tokenSSO) {
        String usuario = verificarAdmision(usuarioAdmisionId, tokenSSO);
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin del periodo son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }

        InformeOperativoDTO informe = new InformeOperativoDTO();
        informe.setDesde(desde);
        informe.setHasta(hasta);
        calcularOcupacion(informe, gestorAgendas.listarAgendasEntreFechas(desde, hasta, tokenSSO));
        calcularEspera(informe, listasEspera.consultarListaEspera(null, tokenSSO));
        calcularCancelaciones(informe, gestionCitas.listarCitasEntreFechas(desde, hasta));
        informe.setFechaGeneracion(LocalDateTime.now(reloj));

        auditService.registrarEvento(usuario, "GENERAR_INFORME_OPERATIVO:" + desde + "_" + hasta, true, IP_LOCAL);
        return informe;
    }

    private String verificarAdmision(String usuarioAdmisionId, String tokenSSO) {
        if (usuarioAdmisionId == null || usuarioAdmisionId.isBlank()) {
            throw new IllegalArgumentException("El identificador del usuario de admisión es obligatorio");
        }
        String usuario = usuarioAdmisionId.trim();
        if (tokenSSO == null || tokenSSO.isBlank() || !gestorRoles.tieneAcceso(tokenSSO, Rol.ADMISION)) {
            auditService.registrarEvento(usuario, "ACCESO_DENEGADO_INFORMES_OPERATIVOS", false, IP_LOCAL);
            throw new SecurityException("Acceso denegado: se requiere rol ADMISION");
        }
        return usuario;
    }

    private void calcularOcupacion(InformeOperativoDTO informe, List<AgendaDiariaDTO> agendas) {
        Map<String, int[]> tramosPorMedico = new TreeMap<>();
        int analizadas = 0;
        int total = 0;
        int noDisponibles = 0;

        if (agendas != null) {
            for (AgendaDiariaDTO agenda : agendas) {
                if (agenda == null) {
                    continue;
                }
                analizadas++;
                int[] acumulado = tramosPorMedico.computeIfAbsent(nombre(agenda.getMedicoId()), m -> new int[2]);
                List<TramoHorarioDTO> tramos = agenda.getTramos();
                if (tramos == null) {
                    continue;
                }
                for (TramoHorarioDTO tramo : tramos) {
                    total++;
                    acumulado[0]++;
                    if (!tramo.isDisponible()) {
                        noDisponibles++;
                        acumulado[1]++;
                    }
                }
            }
        }

        Map<String, Double> ocupacionPorMedico = new LinkedHashMap<>();
        tramosPorMedico.forEach((medico, valores) -> ocupacionPorMedico.put(medico, porcentaje(valores[1], valores[0])));

        informe.setAgendasAnalizadas(analizadas);
        informe.setTotalTramos(total);
        informe.setTramosNoDisponibles(noDisponibles);
        informe.setPorcentajeOcupacion(porcentaje(noDisponibles, total));
        informe.setOcupacionPorMedico(ocupacionPorMedico);
    }

    private void calcularEspera(InformeOperativoDTO informe, List<SolicitudEsperaDTO> pendientes) {
        Map<String, Integer> solicitudesPorPrioridad = new LinkedHashMap<>();
        Map<String, Long> diasPorPrioridad = new LinkedHashMap<>();
        for (PrioridadEspera prioridad : PrioridadEspera.values()) {
            solicitudesPorPrioridad.put(prioridad.name(), 0);
            diasPorPrioridad.put(prioridad.name(), 0L);
        }

        LocalDate hoy = LocalDate.now(reloj);
        int enEspera = 0;
        long sumaDias = 0;
        long maximoDias = 0;

        if (pendientes != null) {
            for (SolicitudEsperaDTO solicitud : pendientes) {
                if (solicitud == null || solicitud.isAtendida()) {
                    continue;
                }
                long dias = (solicitud.getFechaRegistro() == null) ? 0
                        : Math.max(0, ChronoUnit.DAYS.between(solicitud.getFechaRegistro().toLocalDate(), hoy));
                String prioridad = (solicitud.getPrioridad() != null) ? solicitud.getPrioridad().name() : "SIN_PRIORIDAD";
                solicitudesPorPrioridad.merge(prioridad, 1, Integer::sum);
                diasPorPrioridad.merge(prioridad, dias, Long::sum);
                enEspera++;
                sumaDias += dias;
                maximoDias = Math.max(maximoDias, dias);
            }
        }

        Map<String, Double> mediaPorPrioridad = new LinkedHashMap<>();
        solicitudesPorPrioridad.forEach((prioridad, cantidad) ->
                mediaPorPrioridad.put(prioridad, media(diasPorPrioridad.get(prioridad), cantidad)));

        informe.setSolicitudesEnEspera(enEspera);
        informe.setTiempoMedioEsperaDias(media(sumaDias, enEspera));
        informe.setTiempoMaximoEsperaDias(maximoDias);
        informe.setSolicitudesEnEsperaPorPrioridad(solicitudesPorPrioridad);
        informe.setTiempoMedioEsperaPorPrioridad(mediaPorPrioridad);
    }

    private void calcularCancelaciones(InformeOperativoDTO informe, List<CitaDTO> citas) {
        Map<String, Integer> cancelacionesPorMedico = new TreeMap<>();
        int total = 0;
        int canceladas = 0;
        int reprogramadas = 0;

        if (citas != null) {
            for (CitaDTO cita : citas) {
                if (cita == null) {
                    continue;
                }
                total++;
                if ("CANCELADA".equalsIgnoreCase(cita.getEstado())) {
                    canceladas++;
                    cancelacionesPorMedico.merge(nombre(cita.getMedicoId()), 1, Integer::sum);
                } else if ("REPROGRAMADA".equalsIgnoreCase(cita.getEstado())) {
                    reprogramadas++;
                }
            }
        }

        informe.setTotalCitas(total);
        informe.setCitasCanceladas(canceladas);
        informe.setCitasReprogramadas(reprogramadas);
        informe.setPorcentajeCancelacion(porcentaje(canceladas, total));
        informe.setCancelacionesPorMedico(new LinkedHashMap<>(cancelacionesPorMedico));
    }

    private String nombre(String identificador) {
        return (identificador != null && !identificador.isBlank()) ? identificador.trim() : "DESCONOCIDO";
    }

    private double porcentaje(int parte, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(parte * 10000.0 / total) / 100.0;
    }

    private double media(long suma, int cantidad) {
        if (cantidad == 0) {
            return 0.0;
        }
        return Math.round(suma * 100.0 / cantidad) / 100.0;
    }
}
