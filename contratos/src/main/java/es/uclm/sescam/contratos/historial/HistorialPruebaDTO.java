package es.uclm.sescam.contratos.historial;

import java.time.LocalDateTime;

public class HistorialPruebaDTO {
    private String registroId;
    private String pacienteId;
    private String medicoId;
    private String tipoPrueba;
    private LocalDateTime fechaRealizacion;
    private String incidencias;
    private boolean tieneEstudioImagen;

    public HistorialPruebaDTO() {}

    public HistorialPruebaDTO(String registroId, String pacienteId, String medicoId, String tipoPrueba,
                              LocalDateTime fechaRealizacion, String incidencias, boolean tieneEstudioImagen) {
        this.registroId = registroId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.tipoPrueba = tipoPrueba;
        this.fechaRealizacion = fechaRealizacion;
        this.incidencias = incidencias;
        this.tieneEstudioImagen = tieneEstudioImagen;
    }

    public String getRegistroId() { return registroId; }
    public void setRegistroId(String registroId) { this.registroId = registroId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getTipoPrueba() { return tipoPrueba; }
    public void setTipoPrueba(String tipoPrueba) { this.tipoPrueba = tipoPrueba; }
    public LocalDateTime getFechaRealizacion() { return fechaRealizacion; }
    public void setFechaRealizacion(LocalDateTime fechaRealizacion) { this.fechaRealizacion = fechaRealizacion; }
    public String getIncidencias() { return incidencias; }
    public void setIncidencias(String incidencias) { this.incidencias = incidencias; }
    public boolean isTieneEstudioImagen() { return tieneEstudioImagen; }
    public void setTieneEstudioImagen(boolean tieneEstudioImagen) { this.tieneEstudioImagen = tieneEstudioImagen; }
}
