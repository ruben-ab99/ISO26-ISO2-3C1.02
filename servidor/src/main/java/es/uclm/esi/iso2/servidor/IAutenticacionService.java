package es.uclm.esi.iso2.servidor;

public interface IAutenticacionService {
    boolean autenticar(String usuario, String password);
    String generarToken(String usuario);
    boolean validarToken(String token);
}