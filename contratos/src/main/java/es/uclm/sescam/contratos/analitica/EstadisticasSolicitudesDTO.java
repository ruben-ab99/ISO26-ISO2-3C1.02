package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class EstadisticasSolicitudesDTO {
    private String medicoId;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    private int totalSolicitudes;
    private int solicitudesActivas;
    private int solicitudesCanceladas;
    private int solicitudesConCita;
    private int solicitudesSinCita;

    private double tiempoMedioEsperaCitaDias;
    private double tiempoMedioEsperaSinCitaDias;

    private Map<String, Integer> usoPorTipoPrueba;
    private Map<String, Integer> distribucionPorPrioridad;

    private int totalSeguimientos;
    private int revisionesVencidas;
    private int revisionesRealizadas;
    private double porcentajeAdherenciaSeguimiento;

    public EstadisticasSolicitudesDTO() {
        this.usoPorTipoPrueba = new LinkedHashMap<>();
        this.distribucionPorPrioridad = new LinkedHashMap<>();
    }

    public EstadisticasSolicitudesDTO(String medicoId, LocalDate fechaDesde, LocalDate fechaHasta,
                                      int totalSolicitudes, int solicitudesActivas, int solicitudesCanceladas,
                                      int solicitudesConCita, int solicitudesSinCita,
                                      double tiempoMedioEsperaCitaDias, double tiempoMedioEsperaSinCitaDias,
                                      Map<String, Integer> usoPorTipoPrueba,
                                      Map<String, Integer> distribucionPorPrioridad,
                                      int totalSeguimientos, int revisionesVencidas,
                                      int revisionesRealizadas, double porcentajeAdherenciaSeguimiento) {
        this.medicoId = medicoId;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.totalSolicitudes = totalSolicitudes;
        this.solicitudesActivas = solicitudesActivas;
        this.solicitudesCanceladas = solicitudesCanceladas;
        this.solicitudesConCita = solicitudesConCita;
        this.solicitudesSinCita = solicitudesSinCita;
        this.tiempoMedioEsperaCitaDias = tiempoMedioEsperaCitaDias;
        this.tiempoMedioEsperaSinCitaDias = tiempoMedioEsperaSinCitaDias;
        this.usoPorTipoPrueba = (usoPorTipoPrueba != null) ? new LinkedHashMap<>(usoPorTipoPrueba) : new LinkedHashMap<>();
        this.distribucionPorPrioridad = (distribucionPorPrioridad != null) ? new LinkedHashMap<>(distribucionPorPrioridad) : new LinkedHashMap<>();
        this.totalSeguimientos = totalSeguimientos;
        this.revisionesVencidas = revisionesVencidas;
        this.revisionesRealizadas = revisionesRealizadas;
        this.porcentajeAdherenciaSeguimiento = porcentajeAdherenciaSeguimiento;
    }

    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(LocalDate fechaDesde) { this.fechaDesde = fechaDesde; }
    public LocalDate getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(LocalDate fechaHasta) { this.fechaHasta = fechaHasta; }
    public int getTotalSolicitudes() { return totalSolicitudes; }
    public void setTotalSolicitudes(int totalSolicitudes) { this.totalSolicitudes = totalSolicitudes; }
    public int getSolicitudesActivas() { return solicitudesActivas; }
    public void setSolicitudesActivas(int solicitudesActivas) { this.solicitudesActivas = solicitudesActivas; }
    public int getSolicitudesCanceladas() { return solicitudesCanceladas; }
    public void setSolicitudesCanceladas(int solicitudesCanceladas) { this.solicitudesCanceladas = solicitudesCanceladas; }
    public int getSolicitudesConCita() { return solicitudesConCita; }
    public void setSolicitudesConCita(int solicitudesConCita) { this.solicitudesConCita = solicitudesConCita; }
    public int getSolicitudesSinCita() { return solicitudesSinCita; }
    public void setSolicitudesSinCita(int solicitudesSinCita) { this.solicitudesSinCita = solicitudesSinCita; }
    public double getTiempoMedioEsperaCitaDias() { return tiempoMedioEsperaCitaDias; }
    public void setTiempoMedioEsperaCitaDias(double tiempoMedioEsperaCitaDias) { this.tiempoMedioEsperaCitaDias = tiempoMedioEsperaCitaDias; }
    public double getTiempoMedioEsperaSinCitaDias() { return tiempoMedioEsperaSinCitaDias; }
    public void setTiempoMedioEsperaSinCitaDias(double tiempoMedioEsperaSinCitaDias) { this.tiempoMedioEsperaSinCitaDias = tiempoMedioEsperaSinCitaDias; }
    public Map<String, Integer> getUsoPorTipoPrueba() { return usoPorTipoPrueba; }
    public void setUsoPorTipoPrueba(Map<String, Integer> usoPorTipoPrueba) {
        this.usoPorTipoPrueba = (usoPorTipoPrueba != null) ? new LinkedHashMap<>(usoPorTipoPrueba) : new LinkedHashMap<>();
    }
    public Map<String, Integer> getDistribucionPorPrioridad() { return distribucionPorPrioridad; }
    public void setDistribucionPorPrioridad(Map<String, Integer> distribucionPorPrioridad) {
        this.distribucionPorPrioridad = (distribucionPorPrioridad != null) ? new LinkedHashMap<>(distribucionPorPrioridad) : new LinkedHashMap<>();
    }
    public int getTotalSeguimientos() { return totalSeguimientos; }
    public void setTotalSeguimientos(int totalSeguimientos) { this.totalSeguimientos = totalSeguimientos; }
    public int getRevisionesVencidas() { return revisionesVencidas; }
    public void setRevisionesVencidas(int revisionesVencidas) { this.revisionesVencidas = revisionesVencidas; }
    public int getRevisionesRealizadas() { return revisionesRealizadas; }
    public void setRevisionesRealizadas(int revisionesRealizadas) { this.revisionesRealizadas = revisionesRealizadas; }
    public double getPorcentajeAdherenciaSeguimiento() { return porcentajeAdherenciaSeguimiento; }
    public void setPorcentajeAdherenciaSeguimiento(double porcentajeAdherenciaSeguimiento) { this.porcentajeAdherenciaSeguimiento = porcentajeAdherenciaSeguimiento; }
}