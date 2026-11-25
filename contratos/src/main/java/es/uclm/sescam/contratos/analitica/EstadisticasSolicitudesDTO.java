package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class EstadisticasSolicitudesDTO {
    private String medicoId;
    private LocalDate desde;
    private LocalDate hasta;

    // Estado de las solicitudes
    private int totalSolicitudes;
    private int solicitudesPendientes;
    private int solicitudesEnCurso;
    private int solicitudesRealizadas;
    private int solicitudesCanceladas;
    private Map<String, Integer> solicitudesPorPrioridad = new LinkedHashMap<>();

    // Tiempos de espera (desde la solicitud hasta la cita)
    private int solicitudesConCita;
    private double tiempoMedioHastaCitaDias;
    private long tiempoMaximoHastaCitaDias;
    private Map<String, Double> tiempoMedioHastaCitaPorPrioridad = new LinkedHashMap<>();
    private int solicitudesSinCita;
    private double tiempoMedioSinCitaDias;

    // Uso de recursos
    private Map<String, Integer> solicitudesPorModalidad = new LinkedHashMap<>();
    private Map<String, Integer> minutosEstimadosPorModalidad = new LinkedHashMap<>();
    private Map<String, Integer> solicitudesPorEquipoSala = new LinkedHashMap<>();

    // Adherencia al seguimiento
    private int seguimientosActivos;
    private int seguimientosCancelados;
    private int revisionesProgramadas;
    private int revisionesVencidas;
    private int revisionesRealizadas;
    private double porcentajeAdherencia;

    private LocalDateTime fechaGeneracion;

    public EstadisticasSolicitudesDTO() {}

    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public int getTotalSolicitudes() { return totalSolicitudes; }
    public void setTotalSolicitudes(int totalSolicitudes) { this.totalSolicitudes = totalSolicitudes; }
    public int getSolicitudesPendientes() { return solicitudesPendientes; }
    public void setSolicitudesPendientes(int solicitudesPendientes) { this.solicitudesPendientes = solicitudesPendientes; }
    public int getSolicitudesEnCurso() { return solicitudesEnCurso; }
    public void setSolicitudesEnCurso(int solicitudesEnCurso) { this.solicitudesEnCurso = solicitudesEnCurso; }
    public int getSolicitudesRealizadas() { return solicitudesRealizadas; }
    public void setSolicitudesRealizadas(int solicitudesRealizadas) { this.solicitudesRealizadas = solicitudesRealizadas; }
    public int getSolicitudesCanceladas() { return solicitudesCanceladas; }
    public void setSolicitudesCanceladas(int solicitudesCanceladas) { this.solicitudesCanceladas = solicitudesCanceladas; }
    public Map<String, Integer> getSolicitudesPorPrioridad() { return solicitudesPorPrioridad; }
    public void setSolicitudesPorPrioridad(Map<String, Integer> solicitudesPorPrioridad) { this.solicitudesPorPrioridad = solicitudesPorPrioridad; }
    public int getSolicitudesConCita() { return solicitudesConCita; }
    public void setSolicitudesConCita(int solicitudesConCita) { this.solicitudesConCita = solicitudesConCita; }
    public double getTiempoMedioHastaCitaDias() { return tiempoMedioHastaCitaDias; }
    public void setTiempoMedioHastaCitaDias(double tiempoMedioHastaCitaDias) { this.tiempoMedioHastaCitaDias = tiempoMedioHastaCitaDias; }
    public long getTiempoMaximoHastaCitaDias() { return tiempoMaximoHastaCitaDias; }
    public void setTiempoMaximoHastaCitaDias(long tiempoMaximoHastaCitaDias) { this.tiempoMaximoHastaCitaDias = tiempoMaximoHastaCitaDias; }
    public Map<String, Double> getTiempoMedioHastaCitaPorPrioridad() { return tiempoMedioHastaCitaPorPrioridad; }
    public void setTiempoMedioHastaCitaPorPrioridad(Map<String, Double> tiempoMedioHastaCitaPorPrioridad) { this.tiempoMedioHastaCitaPorPrioridad = tiempoMedioHastaCitaPorPrioridad; }
    public int getSolicitudesSinCita() { return solicitudesSinCita; }
    public void setSolicitudesSinCita(int solicitudesSinCita) { this.solicitudesSinCita = solicitudesSinCita; }
    public double getTiempoMedioSinCitaDias() { return tiempoMedioSinCitaDias; }
    public void setTiempoMedioSinCitaDias(double tiempoMedioSinCitaDias) { this.tiempoMedioSinCitaDias = tiempoMedioSinCitaDias; }
    public Map<String, Integer> getSolicitudesPorModalidad() { return solicitudesPorModalidad; }
    public void setSolicitudesPorModalidad(Map<String, Integer> solicitudesPorModalidad) { this.solicitudesPorModalidad = solicitudesPorModalidad; }
    public Map<String, Integer> getMinutosEstimadosPorModalidad() { return minutosEstimadosPorModalidad; }
    public void setMinutosEstimadosPorModalidad(Map<String, Integer> minutosEstimadosPorModalidad) { this.minutosEstimadosPorModalidad = minutosEstimadosPorModalidad; }
    public Map<String, Integer> getSolicitudesPorEquipoSala() { return solicitudesPorEquipoSala; }
    public void setSolicitudesPorEquipoSala(Map<String, Integer> solicitudesPorEquipoSala) { this.solicitudesPorEquipoSala = solicitudesPorEquipoSala; }
    public int getSeguimientosActivos() { return seguimientosActivos; }
    public void setSeguimientosActivos(int seguimientosActivos) { this.seguimientosActivos = seguimientosActivos; }
    public int getSeguimientosCancelados() { return seguimientosCancelados; }
    public void setSeguimientosCancelados(int seguimientosCancelados) { this.seguimientosCancelados = seguimientosCancelados; }
    public int getRevisionesProgramadas() { return revisionesProgramadas; }
    public void setRevisionesProgramadas(int revisionesProgramadas) { this.revisionesProgramadas = revisionesProgramadas; }
    public int getRevisionesVencidas() { return revisionesVencidas; }
    public void setRevisionesVencidas(int revisionesVencidas) { this.revisionesVencidas = revisionesVencidas; }
    public int getRevisionesRealizadas() { return revisionesRealizadas; }
    public void setRevisionesRealizadas(int revisionesRealizadas) { this.revisionesRealizadas = revisionesRealizadas; }
    public double getPorcentajeAdherencia() { return porcentajeAdherencia; }
    public void setPorcentajeAdherencia(double porcentajeAdherencia) { this.porcentajeAdherencia = porcentajeAdherencia; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
