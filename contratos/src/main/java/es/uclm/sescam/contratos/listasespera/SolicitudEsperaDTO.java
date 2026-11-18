package es.uclm.sescam.contratos.listasespera;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SolicitudEsperaDTO {
    private String solicitudId;
    private String pacienteId;
    private String tipoPrueba;
    private PrioridadEspera prioridad;
    private LocalDateTime fechaRegistro;
    private boolean atendida;
    private String agendaAsignadaId;
    private LocalDate fechaAsignada;
    private LocalTime horaAsignada;

    public SolicitudEsperaDTO() {}

    public SolicitudEsperaDTO(String solicitudId, String pacienteId, String tipoPrueba,
                              PrioridadEspera prioridad, LocalDateTime fechaRegistro, boolean atendida) {
        this.solicitudId = solicitudId;
        this.pacienteId = pacienteId;
        this.tipoPrueba = tipoPrueba;
        this.prioridad = prioridad;
        this.fechaRegistro = fechaRegistro;
        this.atendida = atendida;
    }

    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getTipoPrueba() { return tipoPrueba; }
    public void setTipoPrueba(String tipoPrueba) { this.tipoPrueba = tipoPrueba; }
    public PrioridadEspera getPrioridad() { return prioridad; }
    public void setPrioridad(PrioridadEspera prioridad) { this.prioridad = prioridad; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public boolean isAtendida() { return atendida; }
    public void setAtendida(boolean atendida) { this.atendida = atendida; }
    public String getAgendaAsignadaId() { return agendaAsignadaId; }
    public void setAgendaAsignadaId(String agendaAsignadaId) { this.agendaAsignadaId = agendaAsignadaId; }
    public LocalDate getFechaAsignada() { return fechaAsignada; }
    public void setFechaAsignada(LocalDate fechaAsignada) { this.fechaAsignada = fechaAsignada; }
    public LocalTime getHoraAsignada() { return horaAsignada; }
    public void setHoraAsignada(LocalTime horaAsignada) { this.horaAsignada = horaAsignada; }
}