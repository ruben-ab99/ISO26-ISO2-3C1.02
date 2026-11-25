package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class EstadisticasGlobalesDTO {
    private LocalDate desde;
    private LocalDate hasta;
    private int totalOperaciones;
    private int operacionesFallidas;
    private double porcentajeOperacionesFallidas;
    private int usuariosActivos;
    private LocalDate diaMayorActividad;
    private Map<LocalDate, Integer> operacionesPorDia = new LinkedHashMap<>();
    private Map<String, Integer> operacionesPorTipo = new LinkedHashMap<>();
    private int pruebasCatalogoActivas;
    private Map<String, Integer> pruebasActivasPorModalidad = new LinkedHashMap<>();
    private LocalDateTime fechaGeneracion;

    public EstadisticasGlobalesDTO() {}

    public LocalDate getDesde() { return desde; }
    public void setDesde(LocalDate desde) { this.desde = desde; }
    public LocalDate getHasta() { return hasta; }
    public void setHasta(LocalDate hasta) { this.hasta = hasta; }
    public int getTotalOperaciones() { return totalOperaciones; }
    public void setTotalOperaciones(int totalOperaciones) { this.totalOperaciones = totalOperaciones; }
    public int getOperacionesFallidas() { return operacionesFallidas; }
    public void setOperacionesFallidas(int operacionesFallidas) { this.operacionesFallidas = operacionesFallidas; }
    public double getPorcentajeOperacionesFallidas() { return porcentajeOperacionesFallidas; }
    public void setPorcentajeOperacionesFallidas(double porcentajeOperacionesFallidas) { this.porcentajeOperacionesFallidas = porcentajeOperacionesFallidas; }
    public int getUsuariosActivos() { return usuariosActivos; }
    public void setUsuariosActivos(int usuariosActivos) { this.usuariosActivos = usuariosActivos; }
    public LocalDate getDiaMayorActividad() { return diaMayorActividad; }
    public void setDiaMayorActividad(LocalDate diaMayorActividad) { this.diaMayorActividad = diaMayorActividad; }
    public Map<LocalDate, Integer> getOperacionesPorDia() { return operacionesPorDia; }
    public void setOperacionesPorDia(Map<LocalDate, Integer> operacionesPorDia) { this.operacionesPorDia = operacionesPorDia; }
    public Map<String, Integer> getOperacionesPorTipo() { return operacionesPorTipo; }
    public void setOperacionesPorTipo(Map<String, Integer> operacionesPorTipo) { this.operacionesPorTipo = operacionesPorTipo; }
    public int getPruebasCatalogoActivas() { return pruebasCatalogoActivas; }
    public void setPruebasCatalogoActivas(int pruebasCatalogoActivas) { this.pruebasCatalogoActivas = pruebasCatalogoActivas; }
    public Map<String, Integer> getPruebasActivasPorModalidad() { return pruebasActivasPorModalidad; }
    public void setPruebasActivasPorModalidad(Map<String, Integer> pruebasActivasPorModalidad) { this.pruebasActivasPorModalidad = pruebasActivasPorModalidad; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
