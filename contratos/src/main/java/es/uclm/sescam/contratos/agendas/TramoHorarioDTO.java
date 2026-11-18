package es.uclm.sescam.contratos.agendas;

import java.time.LocalTime;

public class TramoHorarioDTO {
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean disponible;
    private String motivoBloqueo;

    public TramoHorarioDTO() {}

    public TramoHorarioDTO(LocalTime horaInicio, LocalTime horaFin, boolean disponible, String motivoBloqueo) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.disponible = disponible;
        this.motivoBloqueo = motivoBloqueo;
    }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }
}