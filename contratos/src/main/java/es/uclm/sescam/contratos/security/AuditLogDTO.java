package es.uclm.sescam.contratos.security;

import java.time.LocalDateTime;

public class AuditLogDTO {
    private String usuario;
    private String accion;
    private boolean exito;
    private String ip;
    private LocalDateTime timestamp;

    public AuditLogDTO() {}

    public AuditLogDTO(String usuario, String accion, boolean exito, String ip) {
        this.usuario = usuario;
        this.accion = accion;
        this.exito = exito;
        this.ip = ip;
        this.timestamp = LocalDateTime.now();
    }

    public String getUsuario() { return usuario; }
    public String getAccion() { return accion; }
    public boolean isExito() { return exito; }
    public String getIp() { return ip; }
    public LocalDateTime getTimestamp() { return timestamp; }
}