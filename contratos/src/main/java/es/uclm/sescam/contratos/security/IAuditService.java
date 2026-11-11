package es.uclm.sescam.contratos.security;

import java.util.List;

public interface IAuditService {
    void registrarEvento(String usuario, String accion, boolean exito, String ip);
    List<AuditLogDTO> obtenerTrazas();
}