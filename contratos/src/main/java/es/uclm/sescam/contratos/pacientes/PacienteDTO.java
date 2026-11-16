package es.uclm.sescam.contratos.pacientes;

import java.time.LocalDate;

public class PacienteDTO {
    private String pacienteId;
    private String dni;
    private String cip; // Código de Identificación Personal del SESCAM
    private String nombre;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String telefono;
    private boolean activo;
    private String motivoBaja;

    public PacienteDTO() {}

    public PacienteDTO(String pacienteId, String dni, String cip, String nombre, String apellidos,
                       LocalDate fechaNacimiento, String telefono, boolean activo, String motivoBaja) {
        this.pacienteId = pacienteId;
        this.dni = dni;
        this.cip = cip;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.activo = activo;
        this.motivoBaja = motivoBaja;
    }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getCip() { return cip; }
    public void setCip(String cip) { this.cip = cip; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public String getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(String motivoBaja) { this.motivoBaja = motivoBaja; }
}