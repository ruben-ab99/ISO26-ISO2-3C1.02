package es.uclm.sescam.contratos.portal;

import java.time.LocalDateTime;

public class EncuestaSatisfaccionDTO {
    private String encuestaId;
    private String pacienteId;
    private String citaId;
    private int puntuacionGlobal;   // 1 a 5
    private int puntuacionAtencion; // 1 a 5
    private String comentarios;
    private LocalDateTime fechaRespuesta;

    public EncuestaSatisfaccionDTO() {}

    public EncuestaSatisfaccionDTO(String encuestaId, String pacienteId, String citaId,
                                   int puntuacionGlobal, int puntuacionAtencion,
                                   String comentarios, LocalDateTime fechaRespuesta) {
        this.encuestaId = encuestaId;
        this.pacienteId = pacienteId;
        this.citaId = citaId;
        this.puntuacionGlobal = puntuacionGlobal;
        this.puntuacionAtencion = puntuacionAtencion;
        this.comentarios = comentarios;
        this.fechaRespuesta = fechaRespuesta;
    }

    public String getEncuestaId() { return encuestaId; }
    public void setEncuestaId(String encuestaId) { this.encuestaId = encuestaId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }
    public int getPuntuacionGlobal() { return puntuacionGlobal; }
    public void setPuntuacionGlobal(int puntuacionGlobal) { this.puntuacionGlobal = puntuacionGlobal; }
    public int getPuntuacionAtencion() { return puntuacionAtencion; }
    public void setPuntuacionAtencion(int puntuacionAtencion) { this.puntuacionAtencion = puntuacionAtencion; }
    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}