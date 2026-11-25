package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;

/**
 * CU-11 · Consultar estadísticas de sus solicitudes (Médico): tiempos de espera,
 * uso de recursos y adherencia al seguimiento. El periodo es opcional (null = sin límite).
 */
public interface IEstadisticasSolicitudesService {

    EstadisticasSolicitudesDTO consultarEstadisticasSolicitudes(String medicoId, LocalDate desde,
                                                                LocalDate hasta, String tokenSSO);
}
