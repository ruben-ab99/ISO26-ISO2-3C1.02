package es.uclm.sescam.contratos.auditoria;

import es.uclm.sescam.contratos.security.AuditLogDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CU-19 · Auditar actividad y accesos a datos clínicos (Administrador del Sistema).
 * Solo lectura sobre las trazas registradas por IAuditService (CU-20); cada consulta
 * queda a su vez registrada para cumplir CU-25 (100 % de accesos registrados).
 */
public interface IAuditoriaService {

    List<AuditLogDTO> consultarTrazas(String filtroUsuario, String filtroAccion, LocalDateTime desde,
                                      LocalDateTime hasta, String adminId, String tokenSSO);

    List<AuditLogDTO> consultarAccesosDenegados(LocalDateTime desde, LocalDateTime hasta,
                                                String adminId, String tokenSSO);

    ResumenAuditoriaDTO generarResumenAuditoria(LocalDateTime desde, LocalDateTime hasta,
                                                String adminId, String tokenSSO);
}
