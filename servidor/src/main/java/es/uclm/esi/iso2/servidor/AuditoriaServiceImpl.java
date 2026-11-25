package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.auditoria.IAuditoriaService;
import es.uclm.sescam.contratos.auditoria.ResumenAuditoriaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AuditoriaServiceImpl implements IAuditoriaService {

    private static final String IP_LOCAL = "127.0.0.1";

    private final IAuditService auditService;
    private final IGestorRolesService gestorRoles;
    private final Clock reloj;

    public AuditoriaServiceImpl(IAuditService auditService, IGestorRolesService gestorRoles) {
        this(auditService, gestorRoles, Clock.systemDefaultZone());
    }

    public AuditoriaServiceImpl(IAuditService auditService, IGestorRolesService gestorRoles, Clock reloj) {
        if (auditService == null || gestorRoles == null || reloj == null) {
            throw new IllegalArgumentException("Los servicios de auditoría y roles y el reloj son obligatorios");
        }
        this.auditService = auditService;
        this.gestorRoles = gestorRoles;
        this.reloj = reloj;
    }

    @Override
    public List<AuditLogDTO> consultarTrazas(String filtroUsuario, String filtroAccion, LocalDateTime desde,
                                             LocalDateTime hasta, String adminId, String tokenSSO) {
        String admin = verificarAdministrador(adminId, tokenSSO, "CONSULTAR_TRAZAS");
        validarPeriodo(desde, hasta);

        String usuarioBuscado = normalizar(filtroUsuario);
        String accionBuscada = normalizar(filtroAccion);

        List<AuditLogDTO> resultado = trazasDelPeriodo(desde, hasta).stream()
                .filter(t -> usuarioBuscado == null || usuarioBuscado.equalsIgnoreCase(t.getUsuario()))
                .filter(t -> accionBuscada == null || (t.getAccion() != null
                        && t.getAccion().toUpperCase().contains(accionBuscada.toUpperCase())))
                .collect(Collectors.toList());

        auditService.registrarEvento(admin, "CONSULTAR_AUDITORIA:TRAZAS", true, IP_LOCAL);
        return resultado;
    }

    @Override
    public List<AuditLogDTO> consultarAccesosDenegados(LocalDateTime desde, LocalDateTime hasta,
                                                       String adminId, String tokenSSO) {
        String admin = verificarAdministrador(adminId, tokenSSO, "CONSULTAR_ACCESOS_DENEGADOS");
        validarPeriodo(desde, hasta);

        List<AuditLogDTO> denegados = trazasDelPeriodo(desde, hasta).stream()
                .filter(t -> !t.isExito())
                .collect(Collectors.toList());

        auditService.registrarEvento(admin, "CONSULTAR_AUDITORIA:ACCESOS_DENEGADOS", true, IP_LOCAL);
        return denegados;
    }

    @Override
    public ResumenAuditoriaDTO generarResumenAuditoria(LocalDateTime desde, LocalDateTime hasta,
                                                       String adminId, String tokenSSO) {
        String admin = verificarAdministrador(adminId, tokenSSO, "GENERAR_RESUMEN");
        validarPeriodo(desde, hasta);

        List<AuditLogDTO> trazas = trazasDelPeriodo(desde, hasta);
        Map<String, Integer> porUsuario = new LinkedHashMap<>();
        Map<String, Integer> porTipoAccion = new LinkedHashMap<>();
        Map<String, Integer> denegadosPorUsuario = new LinkedHashMap<>();
        Set<String> usuarios = new HashSet<>();
        int exitosos = 0;

        for (AuditLogDTO traza : trazas) {
            String usuario = (traza.getUsuario() != null && !traza.getUsuario().isBlank())
                    ? traza.getUsuario().trim() : "DESCONOCIDO";
            usuarios.add(usuario);
            porUsuario.merge(usuario, 1, Integer::sum);
            porTipoAccion.merge(tipoAccion(traza.getAccion()), 1, Integer::sum);
            if (traza.isExito()) {
                exitosos++;
            } else {
                denegadosPorUsuario.merge(usuario, 1, Integer::sum);
            }
        }

        ResumenAuditoriaDTO resumen = new ResumenAuditoriaDTO();
        resumen.setDesde(desde);
        resumen.setHasta(hasta);
        resumen.setTotalEventos(trazas.size());
        resumen.setEventosExitosos(exitosos);
        resumen.setEventosFallidos(trazas.size() - exitosos);
        resumen.setPorcentajeEventosFallidos(porcentaje(trazas.size() - exitosos, trazas.size()));
        resumen.setUsuariosDistintos(usuarios.size());
        resumen.setEventosPorUsuario(ordenarPorFrecuencia(porUsuario));
        resumen.setEventosPorTipoAccion(ordenarPorFrecuencia(porTipoAccion));
        resumen.setAccesosDenegadosPorUsuario(ordenarPorFrecuencia(denegadosPorUsuario));
        resumen.setFechaGeneracion(LocalDateTime.now(reloj));

        auditService.registrarEvento(admin, "CONSULTAR_AUDITORIA:RESUMEN", true, IP_LOCAL);
        return resumen;
    }

    private String verificarAdministrador(String adminId, String tokenSSO, String operacion) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("El identificador del administrador es obligatorio");
        }
        String admin = adminId.trim();
        if (tokenSSO == null || tokenSSO.isBlank() || !gestorRoles.tieneAcceso(tokenSSO, Rol.ADMINISTRADOR)) {
            auditService.registrarEvento(admin, "ACCESO_DENEGADO_AUDITORIA:" + operacion, false, IP_LOCAL);
            throw new SecurityException("Acceso denegado: se requiere rol ADMINISTRADOR");
        }
        return admin;
    }

    private void validarPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }
    }

    private List<AuditLogDTO> trazasDelPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        List<AuditLogDTO> todas = auditService.obtenerTrazas();
        List<AuditLogDTO> resultado = new ArrayList<>();
        if (todas == null) {
            return resultado;
        }
        for (AuditLogDTO traza : todas) {
            if (traza != null && dentroDelPeriodo(traza.getTimestamp(), desde, hasta)) {
                resultado.add(traza);
            }
        }
        return resultado;
    }

    private boolean dentroDelPeriodo(LocalDateTime momento, LocalDateTime desde, LocalDateTime hasta) {
        if (momento == null) {
            return desde == null && hasta == null;
        }
        return (desde == null || !momento.isBefore(desde)) && (hasta == null || !momento.isAfter(hasta));
    }

    private String tipoAccion(String accion) {
        if (accion == null || accion.isBlank()) {
            return "SIN_ACCION";
        }
        int separador = accion.indexOf(':');
        String tipo = (separador >= 0) ? accion.substring(0, separador) : accion;
        return tipo.trim().toUpperCase();
    }

    private String normalizar(String filtro) {
        return (filtro == null || filtro.isBlank()) ? null : filtro.trim();
    }

    private double porcentaje(int parte, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(parte * 10000.0 / total) / 100.0;
    }

    private Map<String, Integer> ordenarPorFrecuencia(Map<String, Integer> conteos) {
        return conteos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<String, Integer>comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
