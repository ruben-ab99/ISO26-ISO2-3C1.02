package es.uclm.sescam.contratos.informes;

import java.time.LocalDateTime;
import java.util.List;

public class InformeDTO {
    private String id;
    private String solicitudId;
    private String pacienteId;
    private String medicoId;
    private String contenidoClinico;
    private LocalDateTime fechaCreacion;
    private List<EstudioImagenDTO> estudiosAsociados;

    public InformeDTO() {}

    public InformeDTO(String id, String solicitudId, String pacienteId, String medicoId,
                      String contenidoClinico, LocalDateTime fechaCreacion,
                      List<EstudioImagenDTO> estudiosAsociados) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.contenidoClinico = contenidoClinico;
        this.fechaCreacion = fechaCreacion;
        this.estudiosAsociados = estudiosAsociados;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSolicitudId() { return solicitudId; }
    public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getMedicoId() { return medicoId; }
    public void setMedicoId(String medicoId) { this.medicoId = medicoId; }
    public String getContenidoClinico() { return contenidoClinico; }
    public void setContenidoClinico(String contenidoClinico) { this.contenidoClinico = contenidoClinico; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public List<EstudioImagenDTO> getEstudiosAsociados() { return estudiosAsociados; }
    public void setEstudiosAsociados(List<EstudioImagenDTO> estudiosAsociados) { this.estudiosAsociados = estudiosAsociados; }
}