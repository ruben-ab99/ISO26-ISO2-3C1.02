package es.uclm.esi.iso2.cliente.roles;

public class GestorRolesServiceImpl implements IGestorRolesService {

    @Override
    public Rol extraerRolDeToken(String tokenSSO) {
        if (tokenSSO == null || !tokenSSO.startsWith("token-sso-")) {
            return Rol.SIN_ROL;
        }
        
    
        String[] partes = tokenSSO.split("-");
        if (partes.length >= 4) {
            try {
                return Rol.valueOf(partes[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                return Rol.SIN_ROL;
            }
        }
        return Rol.SIN_ROL;
    }

    @Override
    public boolean tieneAcceso(String tokenSSO, Rol rolRequerido) {
        Rol rolActual = extraerRolDeToken(tokenSSO);
        
        // El administrador tiene acceso total por defecto
        if (rolActual == Rol.ADMINISTRADOR) {
            return true;
        }
        return rolActual == rolRequerido;
    }

    @Override
    public boolean asignarRol(String idUsuario, Rol nuevoRol, String tokenAdmin) {
        // Solo un ADMINISTRADOR puede asignar roles
        if (!tieneAcceso(tokenAdmin, Rol.ADMINISTRADOR)) {
            return false;
        }
        // Aqu  ir a la llamada HTTP real al backend. Por ahora lo damos por v lido.
        System.out.println("Rol " + nuevoRol + " asignado al usuario " + idUsuario);
        return true;
    }
}