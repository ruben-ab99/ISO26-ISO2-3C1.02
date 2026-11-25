package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.analitica.EstadisticasGlobalesDTO;
import es.uclm.sescam.contratos.analitica.IEstadisticasGlobalesService;
import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class EstadisticasGlobalesServiceImpl implements IEstadisticasGlobalesService {

    private static final String IP_LOCAL = "127.0.0.1";

    private final IAuditService auditService;
    private final ICatalogoPruebasService catalogoService;
    private final IGestorRolesService gestorRoles;
    private final Clock reloj;

    public EstadisticasGlobalesServiceImpl(IAuditService auditService, ICatalogoPruebasService catalogoService,
                                           IGestorRolesService gestorRoles) {
        this(auditService, catalogoService, gestorRoles, Clock.systemDefaultZone());
    }

    public EstadisticasGlobalesServiceImpl(IAuditService auditService, ICatalogoPruebasService catalogoService,
                                           IGestorRolesService gestorRoles, Clock reloj) {
        if (auditService == null || catalogoService == null || gestorRoles == null || reloj == null) {
            throw new IllegalArgumentException("Los servicios de auditoría, catálogo y roles y el reloj son obligatorios");
        }
        this.auditService = auditService;
        this.catalogoService = catalogoService;
        this.gestorRoles = gestorRoles;
        this.reloj = reloj;
    }

    @Override
    public EstadisticasGlobalesDTO generarEstadisticasGlobales(LocalDate desde, LocalDate hasta,
                                                               String adminId, String tokenSSO) {
        String admin = verificarAdministrador(adminId, tokenSSO);
        validarPeriodo(desde, hasta);

        EstadisticasGlobalesDTO estadisticas = calcular(desde, hasta);
        auditService.registrarEvento(admin, "GENERAR_ESTADISTICAS_GLOBALES:" + desde + "_" + hasta, true, IP_LOCAL);
        return estadisticas;
    }

    @Override
    public String exportarInformeUsoCsv(LocalDate desde, LocalDate hasta, String adminId, String tokenSSO) {
        String admin = verificarAdministrador(adminId, tokenSSO);
        validarPeriodo(desde, hasta);

        EstadisticasGlobalesDTO e = calcular(desde, hasta);
        StringBuilder csv = new StringBuilder("seccion;clave;valor\n");
        linea(csv, "resumen", "periodo_desde", e.getDesde());
        linea(csv, "resumen", "periodo_hasta", e.getHasta());
        linea(csv, "resumen", "total_operaciones", e.getTotalOperaciones());
        linea(csv, "resumen", "operaciones_fallidas", e.getOperacionesFallidas());
        linea(csv, "resumen", "porcentaje_operaciones_fallidas", e.getPorcentajeOperacionesFallidas());
        linea(csv, "resumen", "usuarios_activos", e.getUsuariosActivos());
        linea(csv, "resumen", "dia_mayor_actividad", e.getDiaMayorActividad() != null ? e.getDiaMayorActividad() : "-");
        linea(csv, "resumen", "pruebas_catalogo_activas", e.getPruebasCatalogoActivas());
        e.getOperacionesPorDia().forEach((dia, total) -> linea(csv, "operaciones_por_dia", dia, total));
        e.getOperacionesPorTipo().forEach((tipo, total) -> linea(csv, "operaciones_por_tipo", tipo, total));
        e.getPruebasActivasPorModalidad().forEach((modalidad, total) -> linea(csv, "catalogo_por_modalidad", modalidad, total));

        auditService.registrarEvento(admin, "EXPORTAR_ESTADISTICAS_GLOBALES:" + desde + "_" + hasta, true, IP_LOCAL);
        return csv.toString();
    }

    private EstadisticasGlobalesDTO calcular(LocalDate desde, LocalDate hasta) {
        Map<LocalDate, Integer> porDia = new TreeMap<>();
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        Set<String> usuarios = new HashSet<>();
        int total = 0;
        int fallidas = 0;

        List<AuditLogDTO> trazas = auditService.obtenerTrazas();
        if (trazas != null) {
            for (AuditLogDTO traza : trazas) {
                if (traza == null || traza.getTimestamp() == null) {
                    continue;
                }
                LocalDate dia = traza.getTimestamp().toLocalDate();
                if (dia.isBefore(desde) || dia.isAfter(hasta)) {
                    continue;
                }
                total++;
                if (!traza.isExito()) {
                    fallidas++;
                }
                usuarios.add((traza.getUsuario() != null && !traza.getUsuario().isBlank())
                        ? traza.getUsuario().trim() : "DESCONOCIDO");
                porDia.merge(dia, 1, Integer::sum);
                porTipo.merge(tipoAccion(traza.getAccion()), 1, Integer::sum);
            }
        }

        Map<String, Integer> porModalidad = new LinkedHashMap<>();
        int pruebasActivas = 0;
        List<TipoPruebaDTO> catalogo = catalogoService.listarCatalogoPruebas();
        if (catalogo != null) {
            for (TipoPruebaDTO prueba : catalogo) {
                if (prueba != null && prueba.isActiva()) {
                    pruebasActivas++;
                    String modalidad = (prueba.getModalidad() != null && !prueba.getModalidad().isBlank())
                            ? prueba.getModalidad().trim().toUpperCase() : "SIN_MODALIDAD";
                    porModalidad.merge(modalidad, 1, Integer::sum);
                }
            }
        }

        EstadisticasGlobalesDTO estadisticas = new EstadisticasGlobalesDTO();
        estadisticas.setDesde(desde);
        estadisticas.setHasta(hasta);
        estadisticas.setTotalOperaciones(total);
        estadisticas.setOperacionesFallidas(fallidas);
        estadisticas.setPorcentajeOperacionesFallidas(porcentaje(fallidas, total));
        estadisticas.setUsuariosActivos(usuarios.size());
        estadisticas.setDiaMayorActividad(diaConMasOperaciones(porDia));
        estadisticas.setOperacionesPorDia(new LinkedHashMap<>(porDia));
        estadisticas.setOperacionesPorTipo(ordenarPorFrecuencia(porTipo));
        estadisticas.setPruebasCatalogoActivas(pruebasActivas);
        estadisticas.setPruebasActivasPorModalidad(ordenarPorFrecuencia(porModalidad));
        estadisticas.setFechaGeneracion(LocalDateTime.now(reloj));
        return estadisticas;
    }

    private String verificarAdministrador(String adminId, String tokenSSO) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("El identificador del administrador es obligatorio");
        }
        String admin = adminId.trim();
        if (tokenSSO == null || tokenSSO.isBlank() || !gestorRoles.tieneAcceso(tokenSSO, Rol.ADMINISTRADOR)) {
            auditService.registrarEvento(admin, "ACCESO_DENEGADO_ESTADISTICAS_GLOBALES", false, IP_LOCAL);
            throw new SecurityException("Acceso denegado: se requiere rol ADMINISTRADOR");
        }
        return admin;
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin del periodo son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }
    }

    private LocalDate diaConMasOperaciones(Map<LocalDate, Integer> porDia) {
        LocalDate mejorDia = null;
        int maximo = 0;
        for (Map.Entry<LocalDate, Integer> entrada : porDia.entrySet()) {
            if (entrada.getValue() > maximo) {
                maximo = entrada.getValue();
                mejorDia = entrada.getKey();
            }
        }
        return mejorDia;
    }

    private String tipoAccion(String accion) {
        if (accion == null || accion.isBlank()) {
            return "SIN_ACCION";
        }
        int separador = accion.indexOf(':');
        String tipo = (separador >= 0) ? accion.substring(0, separador) : accion;
        return tipo.trim().toUpperCase();
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

    private void linea(StringBuilder csv, String seccion, Object clave, Object valor) {
        csv.append(seccion).append(';').append(clave).append(';').append(valor).append('\n');
    }
}
