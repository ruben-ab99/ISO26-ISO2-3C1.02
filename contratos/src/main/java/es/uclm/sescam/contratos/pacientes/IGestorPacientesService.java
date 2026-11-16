package es.uclm.sescam.contratos.pacientes;

public interface IGestorPacientesService {
    PacienteDTO registrarPaciente(PacienteDTO nuevoPaciente, String tokenSSO);
    PacienteDTO modificarPaciente(String pacienteId, PacienteDTO datosActualizados, String tokenSSO);
    boolean darBajaPaciente(String pacienteId, String motivoBaja, String tokenSSO);
    PacienteDTO consultarPaciente(String pacienteId, String tokenSSO);
}