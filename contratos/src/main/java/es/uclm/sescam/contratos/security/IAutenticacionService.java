package es.uclm.sescam.contratos.security;

public interface IAutenticacionService {
    boolean autenticar(String usuario, String password);
    String generarToken(String usuario);
    boolean validarToken(String token);
}