package es.uclm.esi.iso2.cliente.auditoria;

import es.uclm.sescam.contratos.security.AuditLogDTO;
import es.uclm.sescam.contratos.security.IGestorAuditoriaService;

import java.time.LocalDateTime;
import java.util.List;

public class AuditoriaAdminControlador {

    private final IGestorAuditoriaService gestorAuditoria;
    private final String tokenSesionAdmin;

    public AuditoriaAdminControlador(IGestorAuditoriaService gestorAuditoria, String tokenSesionAdmin) {
        if (gestorAuditoria == null) {
            throw new IllegalArgumentException("El servicio de auditoría es obligatorio");
        }
        if (tokenSesionAdmin == null || tokenSesionAdmin.isBlank()) {
            throw new IllegalArgumentException("El token de sesión del administrador es obligatorio");
        }
        this.gestorAuditoria = gestorAuditoria;
        this.tokenSesionAdmin = tokenSesionAdmin.trim();
    }

    public void notificarEventoSistema(String usuario, String accion, boolean exito, String ip) {
        gestorAuditoria.registrarActividad(usuario, accion, exito, ip, tokenSesionAdmin);
    }

    public List<AuditLogDTO> obtenerTodasLasTrazas() {
        return gestorAuditoria.consultarHistorialCompleto(tokenSesionAdmin);
    }

    public List<AuditLogDTO> filtrarPorUsuario(String usuario) {
        return gestorAuditoria.buscarTrazas(usuario, null, null, null, null, tokenSesionAdmin);
    }

    public List<AuditLogDTO> filtrarPorAccionYRango(String accion, LocalDateTime desde, LocalDateTime hasta) {
        return gestorAuditoria.buscarTrazas(null, accion, null, desde, hasta, tokenSesionAdmin);
    }

    public List<AuditLogDTO> obtenerAlertasSeguridad() {
        return gestorAuditoria.consultarIntentosFallidos(tokenSesionAdmin);
    }

    public String generarInformeAuditoriaTexto() {
        List<AuditLogDTO> trazas = obtenerTodasLasTrazas();
        if (trazas.isEmpty()) {
            return "No existen registros de auditoría en el sistema.";
        }
        StringBuilder sb = new StringBuilder("=== REGISTRO DE AUDITORÍA SESCAM ===\n");
        for (AuditLogDTO log : trazas) {
            sb.append(String.format("[%s] Usuario: %s | Acción: %s | Resultado: %s | IP: %s%n",
                    log.getTimestamp(),
                    log.getUsuario(),
                    log.getAccion(),
                    log.isExito() ? "OK" : "DENEGADO/ERROR",
                    log.getIp()));
        }
        return sb.toString();
    }
}