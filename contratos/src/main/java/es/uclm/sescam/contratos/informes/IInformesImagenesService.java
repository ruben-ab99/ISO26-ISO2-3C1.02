package es.uclm.sescam.contratos.informes;

import java.util.List;

public interface IInformesImagenesService {
    InformeDTO crearInforme(String solicitudId, String pacienteId, String medicoId, String contenidoClinico);
    InformeDTO consultarInforme(String informeId, String usuarioId);
    List<EstudioImagenDTO> listarEstudiosPaciente(String pacienteId);
}