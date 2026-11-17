package es.uclm.sescam.contratos.citacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CitaDTO {
    private String citaId;
    private String pacienteId;
    private String medicoId;
    private String codigoPrueba;
    private LocalDate fecha;
    private String franjaHoraria;
    private String estado; // PROGRAMADA, REPROGRAMADA, CANCELADA
    private String motivoCancelacion;
    private LocalDateTime fechaUltimaActualizacion;

    public CitaDTO() {}

    public CitaDTO(String citaId, String pacienteId, String medicoId, String codigoPrueba,
                   LocalDate fecha, String franjaHoraria, String estado,
                   String motivoCancelacion, LocalDateTime fechaUltimaActualizacion) {
        this.citaId = citaId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.codigoPrueba = codigoPrueba;
        this.fecha = fecha;
        this.franjaHoraria = franjaHoraria;
        this.estado = estado;
        this.motivoCancelacion = motivoCancelacion;
        this.fechaUltimaActualizacion = fechaUltimaActualizacion;
    }

    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getFranjaHoraria() { return franjaHoraria; }
    public void setFranjaHoraria(String franjaHoraria) { this.franjaHoraria = franjaHoraria; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public LocalDateTime getFechaUltimaActualizacion() { return fechaUltimaActualizacion; }
    public void setFechaUltimaActualizacion(LocalDateTime fechaUltimaActualizacion) { this.fechaUltimaActualizacion = fechaUltimaActualizacion; }
}