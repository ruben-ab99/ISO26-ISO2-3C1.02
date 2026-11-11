package es.uclm.esi.iso2.cliente.roles;

public interface IGestorRolesService {
    
    Rol extraerRolDeToken(String tokenSSO);

    boolean tieneAcceso(String tokenSSO, Rol rolRequerido);
    
    boolean asignarRol(String idUsuario, Rol nuevoRol, String tokenAdmin);
}