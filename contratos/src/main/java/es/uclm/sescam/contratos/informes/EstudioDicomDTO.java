package es.uclm.sescam.contratos.informes;

import java.util.List;

public class EstudioDicomDTO {
    private String estudioId;
    private String pacienteId;
    private String modalidad; // Ej: RX, TAC, RM
    private String descripcion;
    private List<String> seriesIds;

    public EstudioDicomDTO() {}

    public EstudioDicomDTO(String estudioId, String pacienteId, String modalidad, String descripcion, List<String> seriesIds) {
        this.estudioId = estudioId;
        this.pacienteId = pacienteId;
        this.modalidad = modalidad;
        this.descripcion = descripcion;
        this.seriesIds = seriesIds;
    }

    public String getEstudioId() { return estudioId; }
    public void setEstudioId(String estudioId) { this.estudioId = estudioId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public List<String> getSeriesIds() { return seriesIds; }
    public void setSeriesIds(List<String> seriesIds) { this.seriesIds = seriesIds; }
}