package es.uclm.sescam.contratos.roles;

public interface IGestorRolesService {
    
    Rol extraerRolDeToken(String tokenSSO);

    boolean tieneAcceso(String tokenSSO, Rol rolRequerido);
    
    boolean asignarRol(String idUsuario, Rol nuevoRol, String tokenAdmin);
}