package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAutenticacionService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutenticacionServiceImpl implements IAutenticacionService {

    private final IGestorRolesService gestorRoles;
    private final Set<String> tokensActivos = ConcurrentHashMap.newKeySet();

    public AutenticacionServiceImpl() {
        this(new GestorRolesServiceImpl());
    }

    public AutenticacionServiceImpl(IGestorRolesService gestorRoles) {
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El gestor de roles no puede ser nulo");
        }
        this.gestorRoles = gestorRoles;
    }

    @Override
    public boolean autenticar(String usuario, String password) {
        return usuario != null && !usuario.isBlank() && password != null && !password.isBlank();
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
        return token;
    }

    @Override
    public boolean validarToken(String token) {
        return token != null && tokensActivos.contains(token);
    }
}