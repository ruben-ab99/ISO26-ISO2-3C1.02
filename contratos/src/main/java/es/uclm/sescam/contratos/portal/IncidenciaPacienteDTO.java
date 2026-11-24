package es.uclm.sescam.contratos.portal;

import java.time.LocalDateTime;

public class IncidenciaPacienteDTO {
    private String incidenciaId;
    private String pacienteId;
    private String referenciaCitaOPruebaId;
    private String categoria; // CITA, PRUEBA, ATENCION, OTRA
    private String descripcion;
    private String estado; // ABIERTA, EN_REVISION, RESUELTA
    private LocalDateTime fechaRegistro;

    public IncidenciaPacienteDTO() {}

    public IncidenciaPacienteDTO(String incidenciaId, String pacienteId, String referenciaCitaOPruebaId,
                                 String categoria, String descripcion, String estado,
                                 LocalDateTime fechaRegistro) {
        this.incidenciaId = incidenciaId;
        this.pacienteId = pacienteId;
        this.referenciaCitaOPruebaId = referenciaCitaOPruebaId;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    public String getIncidenciaId() { return incidenciaId; }
    public void setIncidenciaId(String incidenciaId) { this.incidenciaId = incidenciaId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getReferenciaCitaOPruebaId() { return referenciaCitaOPruebaId; }
    public void setReferenciaCitaOPruebaId(String referenciaCitaOPruebaId) { this.referenciaCitaOPruebaId = referenciaCitaOPruebaId; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}