package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;
import es.uclm.sescam.contratos.security.IAutenticacionService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutenticacionServiceImpl implements IAutenticacionService {

    private final IGestorRolesService gestorRoles;
    private final IAuditService auditService;
    private final Set<String> tokensActivos = ConcurrentHashMap.newKeySet();

    public AutenticacionServiceImpl() {
        this(new GestorRolesServiceImpl(), new AuditServiceImpl());
    }

    public AutenticacionServiceImpl(IGestorRolesService gestorRoles) {
        this(gestorRoles, new AuditServiceImpl());
    }

    public AutenticacionServiceImpl(IGestorRolesService gestorRoles, IAuditService auditService) {
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El gestor de roles no puede ser nulo");
        }
        this.gestorRoles = gestorRoles;
        this.auditService = auditService;
    }

    @Override
    public boolean autenticar(String usuario, String password) {
        boolean valido = usuario != null && !usuario.isBlank() && password != null && !password.isBlank();
        registrarAuditoria(usuario, "LOGIN_SSO", valido);
        return valido;
    }

    @Override
    public String generarToken(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return null;
        }
        String usuarioLimpio = usuario.trim();
        Rol rol = gestorRoles.obtenerRolUsuario(usuarioLimpio);
        String rolTexto = (rol != null) ? rol.name() : "SIN_ROL";

        String token = "token-sso-" + usuarioLimpio + "-" + rolTexto + "-" + UUID.randomUUID();
        tokensActivos.add(token);
        registrarAuditoria(usuarioLimpio, "GENERAR_TOKEN_SSO", true);
        return token;
    }

    @Override
    public boolean validarToken(String token) {
        return token != null && tokensActivos.contains(token);
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "ANONIMO";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}