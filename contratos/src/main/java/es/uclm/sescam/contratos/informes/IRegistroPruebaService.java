package es.uclm.sescam.contratos.informes;
 

import java.util.List;

public interface IRegistroPruebaService {
    RegistroPruebaDTO registrarRealizacionPrueba(String citaId, String pacienteId, String medicoId,
                                                 String tipoPrueba, String observaciones, String incidencias);
    RegistroPruebaDTO consultarRegistroPrueba(String registroId);
    List<RegistroPruebaDTO> listarRegistrosPaciente(String pacienteId);
} 