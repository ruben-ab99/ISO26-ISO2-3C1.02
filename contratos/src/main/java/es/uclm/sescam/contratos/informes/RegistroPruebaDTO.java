package es.uclm.sescam.contratos.informes;

import java.time.LocalDateTime;

public class RegistroPruebaDTO {
    private String registroId;
    private String citaId;
    private String pacienteId;
    private String medicoId;
    private String tipoPrueba;
    private String observaciones;
    private String incidencias;
    private boolean realizada;
    private LocalDateTime fechaRealizacion;

    public RegistroPruebaDTO() {}

    public RegistroPruebaDTO(String registroId, String citaId, String pacienteId, String medicoId,
                             String tipoPrueba, String observaciones, String incidencias,
                             boolean realizada, LocalDateTime fechaRealizacion) {
        this.registroId = registroId;
        this.citaId = citaId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.tipoPrueba = tipoPrueba;
        this.observaciones = observaciones;
        this.incidencias = incidencias;
        this.realizada = realizada;
        this.fechaRealizacion = fechaRealizacion;
    }

    public String getRegistroId() { return registroId; }
    public void setRegistroId(String registroId) { this.registroId = registroId; }
    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getTipoPrueba() { return tipoPrueba; }
    public void setTipoPrueba(String tipoPrueba) { this.tipoPrueba = tipoPrueba; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getIncidencias() { return incidencias; }
    public void setIncidencias(String incidencias) { this.incidencias = incidencias; }
    public boolean isRealizada() { return realizada; }
    public void setRealizada(boolean realizada) { this.realizada = realizada; }
    public LocalDateTime getFechaRealizacion() { return fechaRealizacion; }
    public void setFechaRealizacion(LocalDateTime fechaRealizacion) { this.fechaRealizacion = fechaRealizacion; }
}