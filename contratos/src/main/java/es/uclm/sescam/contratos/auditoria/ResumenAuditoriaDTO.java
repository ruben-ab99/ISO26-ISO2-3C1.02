package es.uclm.sescam.contratos.auditoria;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResumenAuditoriaDTO {
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private int totalEventos;
    private int eventosExitosos;
    private int eventosFallidos;
    private double porcentajeEventosFallidos;
    private int usuariosDistintos;
    private Map<String, Integer> eventosPorUsuario = new LinkedHashMap<>();
    private Map<String, Integer> eventosPorTipoAccion = new LinkedHashMap<>();
    private Map<String, Integer> accesosDenegadosPorUsuario = new LinkedHashMap<>();
    private LocalDateTime fechaGeneracion;

    public ResumenAuditoriaDTO() {}

    public LocalDateTime getDesde() { return desde; }
    public void setDesde(LocalDateTime desde) { this.desde = desde; }
    public LocalDateTime getHasta() { return hasta; }
    public void setHasta(LocalDateTime hasta) { this.hasta = hasta; }
    public int getTotalEventos() { return totalEventos; }
    public void setTotalEventos(int totalEventos) { this.totalEventos = totalEventos; }
    public int getEventosExitosos() { return eventosExitosos; }
    public void setEventosExitosos(int eventosExitosos) { this.eventosExitosos = eventosExitosos; }
    public int getEventosFallidos() { return eventosFallidos; }
    public void setEventosFallidos(int eventosFallidos) { this.eventosFallidos = eventosFallidos; }
    public double getPorcentajeEventosFallidos() { return porcentajeEventosFallidos; }
    public void setPorcentajeEventosFallidos(double porcentajeEventosFallidos) { this.porcentajeEventosFallidos = porcentajeEventosFallidos; }
    public int getUsuariosDistintos() { return usuariosDistintos; }
    public void setUsuariosDistintos(int usuariosDistintos) { this.usuariosDistintos = usuariosDistintos; }
    public Map<String, Integer> getEventosPorUsuario() { return eventosPorUsuario; }
    public void setEventosPorUsuario(Map<String, Integer> eventosPorUsuario) { this.eventosPorUsuario = eventosPorUsuario; }
    public Map<String, Integer> getEventosPorTipoAccion() { return eventosPorTipoAccion; }
    public void setEventosPorTipoAccion(Map<String, Integer> eventosPorTipoAccion) { this.eventosPorTipoAccion = eventosPorTipoAccion; }
    public Map<String, Integer> getAccesosDenegadosPorUsuario() { return accesosDenegadosPorUsuario; }
    public void setAccesosDenegadosPorUsuario(Map<String, Integer> accesosDenegadosPorUsuario) { this.accesosDenegadosPorUsuario = accesosDenegadosPorUsuario; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
