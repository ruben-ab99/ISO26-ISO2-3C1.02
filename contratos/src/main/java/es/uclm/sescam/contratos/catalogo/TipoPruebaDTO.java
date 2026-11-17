package es.uclm.sescam.contratos.catalogo;

public class TipoPruebaDTO {
    private String codigoPrueba;
    private String nombre;
    private String modalidad; // Ej: RX, TAC, RM, ECO
    private String equipoSala;
    private int duracionEstimadaMinutos;
    private String preparacionPrevia;
    private boolean requiereContraste;
    private boolean requiereConsentimiento;
    private String requisitosSeguridad;
    private boolean activa;

    public TipoPruebaDTO() {}

    public TipoPruebaDTO(String codigoPrueba, String nombre, String modalidad, String equipoSala,
                         int duracionEstimadaMinutos, String preparacionPrevia,
                         boolean requiereContraste, boolean requiereConsentimiento,
                         String requisitosSeguridad, boolean activa) {
        this.codigoPrueba = codigoPrueba;
        this.nombre = nombre;
        this.modalidad = modalidad;
        this.equipoSala = equipoSala;
        this.duracionEstimadaMinutos = duracionEstimadaMinutos;
        this.preparacionPrevia = preparacionPrevia;
        this.requiereContraste = requiereContraste;
        this.requiereConsentimiento = requiereConsentimiento;
        this.requisitosSeguridad = requisitosSeguridad;
        this.activa = activa;
    }

    public String getCodigoPrueba() { return codigoPrueba; }
    public void setCodigoPrueba(String codigoPrueba) { this.codigoPrueba = codigoPrueba; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getEquipoSala() { return equipoSala; }
    public void setEquipoSala(String equipoSala) { this.equipoSala = equipoSala; }
    public int getDuracionEstimadaMinutos() { return duracionEstimadaMinutos; }
    public void setDuracionEstimadaMinutos(int duracionEstimadaMinutos) { this.duracionEstimadaMinutos = duracionEstimadaMinutos; }
    public String getPreparacionPrevia() { return preparacionPrevia; }
    public void setPreparacionPrevia(String preparacionPrevia) { this.preparacionPrevia = preparacionPrevia; }
    public boolean isRequiereContraste() { return requiereContraste; }
    public void setRequiereContraste(boolean requiereContraste) { this.requiereContraste = requiereContraste; }
    public boolean isRequiereConsentimiento() { return requiereConsentimiento; }
    public void setRequiereConsentimiento(boolean requiereConsentimiento) { this.requiereConsentimiento = requiereConsentimiento; }
    public String getRequisitosSeguridad() { return requisitosSeguridad; }
    public void setRequisitosSeguridad(String requisitosSeguridad) { this.requisitosSeguridad = requisitosSeguridad; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}