package es.uclm.sescam.contratos.prescripciones;

import java.time.LocalDateTime;

public class SolicitudPruebaDTO {
    private String solicitudId;
    private String pacienteId;
    private String medicoId;
    private String codigoPrueba;
    private String nombrePrueba;
    private int prioridad; // 1 (urgente) a 5 (ordinaria)
    private String nivelPrioridad; // URGENTE, PREFERENTE, ORDINARIA
    private String justificacionClinica;
    private String estado; // PENDIENTE, CANCELADA
    private String motivoCancelacion;
    private LocalDateTime fechaSolicitud;

    public SolicitudPruebaDTO() {}

    public SolicitudPruebaDTO(String solicitudId, String pacienteId, String medicoId,
                              String codigoPrueba, String nombrePrueba, int prioridad,
                              String nivelPrioridad, String justificacionClinica,
                              String estado, String motivoCancelacion, LocalDateTime fechaSolicitud) {
        this.solicitudId = solicitudId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.codigoPrueba = codigoPrueba;
        this.nombrePrueba = nombrePrueba;
        this.prioridad = prioridad;
        this.nivelPrioridad = nivelPrioridad;
        this.justificacionClinica = justificacionClinica;
        this.estado = estado;
        this.motivoCancelacion = motivoCancelacion;
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }
    public String getNombrePrueba() { return nombrePrueba; }
    public void setNombrePrueba(String nombrePrueba) { this.nombrePrueba = nombrePrueba; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public String getNivelPrioridad() { return nivelPrioridad; }
    public void setNivelPrioridad(String nivelPrioridad) { this.nivelPrioridad = nivelPrioridad; }
    public String getJustificacionClinica() { return justificacionClinica; }
    public void setJustificacionClinica(String justificacionClinica) { this.justificacionClinica = justificacionClinica; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
}