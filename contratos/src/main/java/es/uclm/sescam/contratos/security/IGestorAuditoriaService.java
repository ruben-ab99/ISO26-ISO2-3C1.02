package es.uclm.sescam.contratos.security;

import java.time.LocalDateTime;
import java.util.List;

public interface IGestorAuditoriaService {

    void registrarActividad(String usuario, String accion, boolean exito, String ip, String tokenSSO);

    List<AuditLogDTO> consultarHistorialCompleto(String tokenSSO);

    List<AuditLogDTO> buscarTrazas(String usuario, String accionContiene, Boolean soloExitosos,
                                   LocalDateTime desde, LocalDateTime hasta, String tokenSSO);

    List<AuditLogDTO> consultarIntentosFallidos(String tokenSSO);
}