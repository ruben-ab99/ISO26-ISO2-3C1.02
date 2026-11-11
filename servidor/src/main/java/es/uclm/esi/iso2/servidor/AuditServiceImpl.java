package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.security.AuditLogDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AuditServiceImpl implements IAuditService {
    private final List<AuditLogDTO> historial = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void registrarEvento(String usuario, String accion, boolean exito, String ip) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }
        historial.add(new AuditLogDTO(usuario, accion, exito, ip));
    }

    @Override
    public List<AuditLogDTO> obtenerTrazas() {
        return new ArrayList<>(historial);
    }
}