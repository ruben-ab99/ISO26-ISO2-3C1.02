package es.uclm.sescam.contratos.informes;

public interface IRegistroPruebaService {
    RegistroPruebaDTO registrarRealizacionPrueba(String citaId, String pacienteId, String medicoId,
                                                 String tipoPrueba, String observaciones, String incidencias);
    RegistroPruebaDTO consultarRegistroPrueba(String registroId);
}