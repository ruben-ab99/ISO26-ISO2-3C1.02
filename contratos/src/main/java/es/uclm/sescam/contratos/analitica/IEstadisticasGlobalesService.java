package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;

/**
 * CU-21 · Generar estadísticas globales y reportes de uso (Administrador del Sistema).
 * El uso del sistema se mide sobre las trazas de IAuditService y el catálogo activo de pruebas.
 */
public interface IEstadisticasGlobalesService {

    EstadisticasGlobalesDTO generarEstadisticasGlobales(LocalDate desde, LocalDate hasta,
                                                        String adminId, String tokenSSO);

    String exportarInformeUsoCsv(LocalDate desde, LocalDate hasta, String adminId, String tokenSSO);
}
