package es.uclm.sescam.contratos.catalogo;

import java.time.LocalDateTime;

public class ParametroSistemaDTO {
    private String clave;
    private String valor;
    private String categoria; // HORARIOS, POLITICAS_CITACION, PRIORIDADES, PROTOCOLOS
    private String modificadoPorAdminId;
    private LocalDateTime fechaUltimaModificacion;

    public ParametroSistemaDTO() {}

    public ParametroSistemaDTO(String clave, String valor, String categoria,
                               String modificadoPorAdminId, LocalDateTime fechaUltimaModificacion) {
        this.clave = clave;
        this.valor = valor;
        this.categoria = categoria;
        this.modificadoPorAdminId = modificadoPorAdminId;
        this.fechaUltimaModificacion = fechaUltimaModificacion;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getModificadoPorAdminId() { return modificadoPorAdminId; }
    public void setModificadoPorAdminId(String modificadoPorAdminId) { this.modificadoPorAdminId = modificadoPorAdminId; }
    public LocalDateTime getFechaUltimaModificacion() { return fechaUltimaModificacion; }
    public void setFechaUltimaModificacion(LocalDateTime fechaUltimaModificacion) { this.fechaUltimaModificacion = fechaUltimaModificacion; }
}