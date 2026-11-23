package es.uclm.sescam.contratos.historial;

import java.time.LocalDate;
import java.util.List;

public interface IHistorialPruebasService {
    List<HistorialPruebaDTO> consultarHistorial(String pacienteId);
    List<HistorialPruebaDTO> consultarHistorialPorTipo(String pacienteId, String tipoPrueba);
    List<HistorialPruebaDTO> consultarHistorialPorFechas(String pacienteId, LocalDate desde, LocalDate hasta);
    HistorialPruebaDTO consultarDetallePrueba(String registroId, String pacienteId);
}