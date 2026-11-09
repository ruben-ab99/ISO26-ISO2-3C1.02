package es.uclm.esi.iso2.servidor;
public class AutenticacionServiceImpl implements IAutenticacionService {

    @Override
    public boolean autenticar(String usuario, String password) {
        return usuario != null && !usuario.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String generarToken(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return null;
        }
        return "token-sso-" + usuario;
    }

    @Override
    public boolean validarToken(String token) {
        return token != null && token.startsWith("token-sso-");
    }
}