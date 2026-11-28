package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class InformeOperativoDTO {
    private LocalDate desde;
    private LocalDate hasta;

    // Ocupación de agendas
    private int agendasAnalizadas;
    private int totalTramos;
    private int tramosNoDisponibles;
    private double porcentajeOcupacion;
    private Map<String, Double> ocupacionPorMedico = new LinkedHashMap<>();

    // Tiempos de espera (lista de espera a fecha de generación)
    private int solicitudesEnEspera;
    private double tiempoMedioEsperaDias;
    private long tiempoMaximoEsperaDias;
    private Map<String, Integer> solicitudesEnEsperaPorPrioridad = new LinkedHashMap<>();
    private Map<String, Double> tiempoMedioEsperaPorPrioridad = new LinkedHashMap<>();

    // Cancelaciones
    private int totalCitas;
    private int citasCanceladas;
    private int citasReprogramadas;
    private double porcentajeCancelacion;
    private Map<String, Integer> cancelacionesPorMedico = new LinkedHashMap<>();

    private LocalDateTime fechaGeneracion;

    public InformeOperativoDTO() {}

    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public int getAgendasAnalizadas() { return agendasAnalizadas; }
    public void setAgendasAnalizadas(int agendasAnalizadas) { this.agendasAnalizadas = agendasAnalizadas; }
    public int getTotalTramos() { return totalTramos; }
    public void setTotalTramos(int totalTramos) { this.totalTramos = totalTramos; }
    public int getTramosNoDisponibles() { return tramosNoDisponibles; }
    public void setTramosNoDisponibles(int tramosNoDisponibles) { this.tramosNoDisponibles = tramosNoDisponibles; }
    public double getPorcentajeOcupacion() { return porcentajeOcupacion; }
    public void setPorcentajeOcupacion(double porcentajeOcupacion) { this.porcentajeOcupacion = porcentajeOcupacion; }
    public Map<String, Double> getOcupacionPorMedico() { return ocupacionPorMedico; }
    public void setOcupacionPorMedico(Map<String, Double> ocupacionPorMedico) { this.ocupacionPorMedico = ocupacionPorMedico; }
    public int getSolicitudesEnEspera() { return solicitudesEnEspera; }
    public void setSolicitudesEnEspera(int solicitudesEnEspera) { this.solicitudesEnEspera = solicitudesEnEspera; }
    public double getTiempoMedioEsperaDias() { return tiempoMedioEsperaDias; }
    public void setTiempoMedioEsperaDias(double tiempoMedioEsperaDias) { this.tiempoMedioEsperaDias = tiempoMedioEsperaDias; }
    public long getTiempoMaximoEsperaDias() { return tiempoMaximoEsperaDias; }
    public void setTiempoMaximoEsperaDias(long tiempoMaximoEsperaDias) { this.tiempoMaximoEsperaDias = tiempoMaximoEsperaDias; }
    public Map<String, Integer> getSolicitudesEnEsperaPorPrioridad() { return solicitudesEnEsperaPorPrioridad; }
    public void setSolicitudesEnEsperaPorPrioridad(Map<String, Integer> solicitudesEnEsperaPorPrioridad) { this.solicitudesEnEsperaPorPrioridad = solicitudesEnEsperaPorPrioridad; }
    public Map<String, Double> getTiempoMedioEsperaPorPrioridad() { return tiempoMedioEsperaPorPrioridad; }
    public void setTiempoMedioEsperaPorPrioridad(Map<String, Double> tiempoMedioEsperaPorPrioridad) { this.tiempoMedioEsperaPorPrioridad = tiempoMedioEsperaPorPrioridad; }
    public int getTotalCitas() { return totalCitas; }
    public void setTotalCitas(int totalCitas) { this.totalCitas = totalCitas; }
    public int getCitasCanceladas() { return citasCanceladas; }
    public void setCitasCanceladas(int citasCanceladas) { this.citasCanceladas = citasCanceladas; }
    public int getCitasReprogramadas() { return citasReprogramadas; }
    public void setCitasReprogramadas(int citasReprogramadas) { this.citasReprogramadas = citasReprogramadas; }
    public double getPorcentajeCancelacion() { return porcentajeCancelacion; }
    public void setPorcentajeCancelacion(double porcentajeCancelacion) { this.porcentajeCancelacion = porcentajeCancelacion; }
    public Map<String, Integer> getCancelacionesPorMedico() { return cancelacionesPorMedico; }
    public void setCancelacionesPorMedico(Map<String, Integer> cancelacionesPorMedico) { this.cancelacionesPorMedico = cancelacionesPorMedico; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
