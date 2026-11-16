package es.uclm.sescam.contratos.informes;

import java.time.LocalDate;

public class EstudioImagenDTO {
    private String estudioId;
    private String pacienteId;
    private String modalidad; // Ej: RX, TAC, RM
    private LocalDate fechaRealizacion;
    private String urlAccesoPacs;

    public EstudioImagenDTO() {}

    public EstudioImagenDTO(String estudioId, String pacienteId, String modalidad,
                            LocalDate fechaRealizacion, String urlAccesoPacs) {
        this.estudioId = estudioId;
        this.pacienteId = pacienteId;
        this.modalidad = modalidad;
        this.fechaRealizacion = fechaRealizacion;
        this.urlAccesoPacs = urlAccesoPacs;
    }

    public String getEstudioId() { return estudioId; }
    public void setEstudioId(String estudioId) { this.estudioId = estudioId; }
    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public LocalDate getFechaRealizacion() { return fechaRealizacion; }
    public void setFechaRealizacion(LocalDate fechaRealizacion) { this.fechaRealizacion = fechaRealizacion; }
    public String getUrlAccesoPacs() { return urlAccesoPacs; }
    public void setUrlAccesoPacs(String urlAccesoPacs) { this.urlAccesoPacs = urlAccesoPacs; }
}