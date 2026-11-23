package es.uclm.sescam.contratos.prescripciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PrescripcionSeguimientoDTO {
    private String seguimientoId;
    private String pacienteId;
    private String medicoId;
    private String codigoPrueba;
    private String nombrePrueba;
    private int periodicidadDias;
    private int numeroRevisiones;
    private LocalDate fechaInicio;
    private String indicacionesClinicas;
    private String estado; // ACTIVO, CANCELADO
    private String motivoCancelacion;
    private List<String> citasGeneradasIds;
    private LocalDateTime fechaCreacion;

    public PrescripcionSeguimientoDTO() {
        this.citasGeneradasIds = new ArrayList<>();
    }

    public PrescripcionSeguimientoDTO(String seguimientoId, String pacienteId, String medicoId,
                                      String codigoPrueba, String nombrePrueba, int periodicidadDias,
                                      int numeroRevisiones, LocalDate fechaInicio,
                                      String indicacionesClinicas, String estado,
                                      String motivoCancelacion, List<String> citasGeneradasIds,
                                      LocalDateTime fechaCreacion) {
        this.seguimientoId = seguimientoId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.codigoPrueba = codigoPrueba;
        this.nombrePrueba = nombrePrueba;
        this.periodicidadDias = periodicidadDias;
        this.numeroRevisiones = numeroRevisiones;
        this.fechaInicio = fechaInicio;
        this.indicacionesClinicas = indicacionesClinicas;
        this.estado = estado;
        this.motivoCancelacion = motivoCancelacion;
        this.citasGeneradasIds = (citasGeneradasIds != null) ? new ArrayList<>(citasGeneradasIds) : new ArrayList<>();
        this.fechaCreacion = fechaCreacion;
    }

    public String getSeguimientoId() { return seguimientoId; }
    public void setSeguimientoId(String seguimientoId) { this.seguimientoId = seguimientoId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }
    public String getNombrePrueba() { return nombrePrueba; }
    public void setNombrePrueba(String nombrePrueba) { this.nombrePrueba = nombrePrueba; }
    public int getPeriodicidadDias() { return periodicidadDias; }
    public void setPeriodicidadDias(int periodicidadDias) { this.periodicidadDias = periodicidadDias; }
    public int getNumeroRevisiones() { return numeroRevisiones; }
    public void setNumeroRevisiones(int numeroRevisiones) { this.numeroRevisiones = numeroRevisiones; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getIndicacionesClinicas() { return indicacionesClinicas; }
    public void setIndicacionesClinicas(String indicacionesClinicas) { this.indicacionesClinicas = indicacionesClinicas; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public List<String> getCitasGeneradasIds() { return citasGeneradasIds; }
    public void setCitasGeneradasIds(List<String> citasGeneradasIds) {
        this.citasGeneradasIds = (citasGeneradasIds != null) ? new ArrayList<>(citasGeneradasIds) : new ArrayList<>();
    }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}