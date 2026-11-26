package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.security.IGestorAuditoriaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class GestorAuditoriaServiceImpl implements IGestorAuditoriaService {

    private final IGestorRolesService gestorRoles;
    private final IAuditService auditService;

    public GestorAuditoriaServiceImpl() {
        this(new GestorRolesServiceImpl(), new AuditServiceImpl());
    }

    public GestorAuditoriaServiceImpl(IGestorRolesService gestorRoles, IAuditService auditService) {
        if (gestorRoles == null || auditService == null) {
            throw new IllegalArgumentException("Los servicios de roles y auditoría base son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.auditService = auditService;
    }

    private void verificarTokenValido(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            auditService.registrarEvento("DESCONOCIDO", "ACCESO_SIN_TOKEN", false, "0.0.0.0");
            throw new SecurityException("Token SSO obligatorio");
        }
        Rol rol = gestorRoles.extraerRolDeToken(tokenSSO);
        if (rol == null || rol == Rol.SIN_ROL) {
            auditService.registrarEvento(tokenSSO.trim(), "TOKEN_INVALIDO", false, "0.0.0.0");
            throw new SecurityException("Token SSO no válido o sin rol asignado");
        }
    }

    private void verificarPermisoAdministrador(String tokenSSO, String accionSolicitada) {
        verificarTokenValido(tokenSSO);
        if (!gestorRoles.tieneAcceso(tokenSSO, Rol.ADMINISTRADOR)) {
            auditService.registrarEvento(tokenSSO.trim(), "ACCESO_DENEGADO_" + accionSolicitada, false, "0.0.0.0");
            throw new SecurityException("Acceso denegado: se requiere rol ADMINISTRADOR para auditar");
        }
    }

    @Override
    public void registrarActividad(String usuario, String accion, boolean exito, String ip, String tokenSSO) {
        verificarTokenValido(tokenSSO);
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario auditado es obligatorio");
        }
        if (accion == null || accion.isBlank()) {
            throw new IllegalArgumentException("La acción auditada es obligatoria");
        }
        String ipNormalizada = (ip == null || ip.isBlank()) ? "0.0.0.0" : ip.trim();
        auditService.registrarEvento(usuario.trim(), accion.trim().toUpperCase(), exito, ipNormalizada);
    }

    @Override
    public List<AuditLogDTO> consultarHistorialCompleto(String tokenSSO) {
        verificarPermisoAdministrador(tokenSSO, "CONSULTAR_HISTORIAL");
        return auditService.obtenerTrazas();
    }

    @Override
    public List<AuditLogDTO> buscarTrazas(String usuario, String accionContiene, Boolean soloExitosos,
                                          LocalDateTime desde, LocalDateTime hasta, String tokenSSO) {
        verificarPermisoAdministrador(tokenSSO, "BUSCAR_TRAZAS");

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        String usuarioFiltro = (usuario != null && !usuario.isBlank()) ? usuario.trim() : null;
        String accionFiltro = (accionContiene != null && !accionContiene.isBlank())
                ? accionContiene.trim().toUpperCase()
                : null;

        return auditService.obtenerTrazas().stream()
                .filter(log -> usuarioFiltro == null || (log.getUsuario() != null && log.getUsuario().equalsIgnoreCase(usuarioFiltro)))
                .filter(log -> accionFiltro == null || (log.getAccion() != null && log.getAccion().toUpperCase().contains(accionFiltro)))
                .filter(log -> soloExitosos == null || log.isExito() == soloExitosos)
                .filter(log -> desde == null || (log.getTimestamp() != null && !log.getTimestamp().isBefore(desde)))
                .filter(log -> hasta == null || (log.getTimestamp() != null && !log.getTimestamp().isAfter(hasta)))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDTO> consultarIntentosFallidos(String tokenSSO) {
        verificarPermisoAdministrador(tokenSSO, "CONSULTAR_FALLIDOS");
        return auditService.obtenerTrazas().stream()
                .filter(log -> !log.isExito())
                .collect(Collectors.toList());
    }
}