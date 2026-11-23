package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorRolesServiceImpl implements IGestorRolesService {

    private final Map<String, Rol> rolesPorUsuario = new ConcurrentHashMap<>();

    @Override
    public Rol extraerRolDeToken(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            return null;
        }
        String[] partes = tokenSSO.trim().split("-");
        for (String parte : partes) {
            String candidata = parte.trim().toUpperCase();
            for (Rol r : Rol.values()) {
                if (r.name().equals(candidata)) {
                    return r;
                }
            }
        }
        return null;
    }

    @Override
    public boolean tieneAcceso(String tokenSSO, Rol rolRequerido) {
        if (rolRequerido == null) {
            return false;
        }
        Rol rolUsuario = extraerRolDeToken(tokenSSO);
        return rolUsuario != null && rolUsuario == rolRequerido;
    }

    @Override
    public boolean asignarRol(String idUsuario, Rol nuevoRol, String tokenAdmin) {
        if (idUsuario == null || idUsuario.isBlank() || nuevoRol == null) {
            return false;
        }
        Rol rolSolicitante = extraerRolDeToken(tokenAdmin);
        if (!esAdmin(rolSolicitante)) {
            return false;
        }
        rolesPorUsuario.put(idUsuario.trim(), nuevoRol);
        return true;
    }

    @Override
    public Rol obtenerRolUsuario(String idUsuario) {
        if (idUsuario == null || idUsuario.isBlank()) {
            return null;
        }
        return rolesPorUsuario.get(idUsuario.trim());
    }

    private boolean esAdmin(Rol rol) {
        if (rol == null) {
            return false;
        }
        String nombre = rol.name();
        return "ADMINISTRADOR".equals(nombre) || "ADMIN".equals(nombre);
    }
}