package es.uclm.sescam.contratos.agendas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AgendaDiariaDTO {
    private String agendaId;
    private String medicoId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionHuecoMinutos;
    private boolean abierta;
    private List<TramoHorarioDTO> tramos = new ArrayList<>();

    public AgendaDiariaDTO() {}

    public AgendaDiariaDTO(String agendaId, String medicoId, LocalDate fecha, LocalTime horaInicio,
                           LocalTime horaFin, int duracionHuecoMinutos, boolean abierta, List<TramoHorarioDTO> tramos) {
        this.agendaId = agendaId;
        this.medicoId = medicoId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.duracionHuecoMinutos = duracionHuecoMinutos;
        this.abierta = abierta;
        if (tramos != null) {
            this.tramos = tramos;
        }
    }

    public String getAgendaId() { return agendaId; }
    public void setAgendaId(String agendaId) { this.agendaId = agendaId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public int getDuracionHuecoMinutos() { return duracionHuecoMinutos; }
    public void setDuracionHuecoMinutos(int duracionHuecoMinutos) { this.duracionHuecoMinutos = duracionHuecoMinutos; }
    public boolean isAbierta() { return abierta; }
    public void setAbierta(boolean abierta) { this.abierta = abierta; }
    public List<TramoHorarioDTO> getTramos() { return tramos; }
    public void setTramos(List<TramoHorarioDTO> tramos) { this.tramos = tramos; }
}