package es.uclm.sescam.contratos.citacion;

import java.time.LocalDateTime;

public class NotificacionDTO {
    private String id;
    private String pacienteId;
    private String tipoAviso; // CITA, PREPARACION_PREVIA, INFORME_DISPONIBLE
    private String titulo;
    private String mensaje;
    private String referenciaId; // ID de cita o informe asociado
    private boolean leida;
    private LocalDateTime fechaEnvio;

    public NotificacionDTO() {}

    public NotificacionDTO(String id, String pacienteId, String tipoAviso, String titulo,
                           String mensaje, String referenciaId, boolean leida, LocalDateTime fechaEnvio) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipoAviso = tipoAviso;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.referenciaId = referenciaId;
        this.leida = leida;
        this.fechaEnvio = fechaEnvio;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getTipoAviso() { return tipoAviso; }
    public void setTipoAviso(String tipoAviso) { this.tipoAviso = tipoAviso; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
}