package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.AuditLogDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-22 (Autenticarse mediante SSO) / CU-25 (Seguridad).
 * Técnicas: clases de equivalencia sobre usuario y contraseña (válido / nulo / vacío / en blanco),
 * criterio each use para combinar los dos parámetros, y conjetura de errores sobre los tokens.
 */
class AutenticacionServiceImplTest {

    private static final String TOKEN_ADMIN = "token-sso-root-ADMINISTRADOR-0001";

    private GestorRolesServiceImpl gestorRoles;
    private AuditServiceImpl auditoria;
    private AutenticacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        gestorRoles = new GestorRolesServiceImpl();
        auditoria = new AuditServiceImpl();
        servicio = new AutenticacionServiceImpl(gestorRoles, auditoria);
    }

    // ---------------------------------------------------------------- constructor

    @Test
    @DisplayName("El constructor rechaza un gestor de roles nulo")
    void constructorConGestorDeRolesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new AutenticacionServiceImpl(null));

        assertEquals("El gestor de roles no puede ser nulo", e.getMessage());
    }

    @Test
    @DisplayName("El constructor sin argumentos crea un servicio utilizable")
    void constructorPorDefectoCreaServicioFuncional() {
        AutenticacionServiceImpl porDefecto = new AutenticacionServiceImpl();

        assertTrue(porDefecto.autenticar("admin", "1234"));
    }

    // ---------------------------------------------------------------- autenticar

    @ParameterizedTest(name = "usuario {0} con contraseña {1} se autentica")
    @CsvSource({
            "admin,     1234",
            "dr.house,  secreto",
            "a,         b"
    })
    @DisplayName("Usuario y contraseña informados autentican correctamente")
    void autenticarConCredencialesInformadasDevuelveTrue(String usuario, String password) {
        assertTrue(servicio.autenticar(usuario, password));
    }

    @ParameterizedTest(name = "usuario [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un usuario nulo, vacío o en blanco no autentica")
    void autenticarConUsuarioInvalidoDevuelveFalse(String usuario) {
        assertFalse(servicio.autenticar(usuario, "1234"));
    }

    @ParameterizedTest(name = "contraseña [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Una contraseña nula, vacía o en blanco no autentica")
    void autenticarConPasswordInvalidaDevuelveFalse(String password) {
        assertFalse(servicio.autenticar("admin", password));
    }

    @Test
    @DisplayName("Usuario y contraseña vacíos a la vez no autentican")
    void autenticarConAmbosCamposVaciosDevuelveFalse() {
        assertFalse(servicio.autenticar("", ""));
    }

    @Test
    @DisplayName("Un inicio de sesión correcto queda registrado en la auditoría")
    void autenticarCorrectoRegistraEventoExitoso() {
        servicio.autenticar("admin", "1234");

        List<AuditLogDTO> trazas = auditoria.obtenerTrazas();
        assertEquals(1, trazas.size());
        AuditLogDTO traza = trazas.get(0);
        assertAll(
                () -> assertEquals("admin", traza.getUsuario()),
                () -> assertEquals("LOGIN_SSO", traza.getAccion()),
                () -> assertTrue(traza.isExito()));
    }

    @Test
    @DisplayName("Un inicio de sesión fallido sin usuario se audita como ANONIMO")
    void autenticarFallidoSinUsuarioRegistraEventoAnonimo() {
        servicio.autenticar(null, "1234");

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("ANONIMO", traza.getUsuario()),
                () -> assertEquals("LOGIN_SSO", traza.getAccion()),
                () -> assertFalse(traza.isExito()));
    }

    // ---------------------------------------------------------------- generarToken

    @ParameterizedTest(name = "usuario [{0}] no recibe token")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("No se genera token para un usuario nulo, vacío o en blanco")
    void generarTokenConUsuarioInvalidoDevuelveNull(String usuario) {
        assertNull(servicio.generarToken(usuario));
    }

    @Test
    @DisplayName("El token generado incluye el usuario y SIN_ROL si no tiene rol asignado")
    void generarTokenSinRolAsignadoUsaSinRol() {
        String token = servicio.generarToken("ana");

        assertNotNull(token);
        assertTrue(token.startsWith("token-sso-ana-SIN_ROL-"));
    }

    @Test
    @DisplayName("El token generado se registra en la auditoría")
    void generarTokenRegistraEventoDeAuditoria() {
        servicio.generarToken("ana");

        AuditLogDTO traza = auditoria.obtenerTrazas().get(0);
        assertAll(
                () -> assertEquals("ana", traza.getUsuario()),
                () -> assertEquals("GENERAR_TOKEN_SSO", traza.getAccion()),
                () -> assertTrue(traza.isExito()));
    }

    @Test
    @DisplayName("El usuario se recorta antes de incluirlo en el token")
    void generarTokenRecortaEspaciosDelUsuario() {
        String token = servicio.generarToken("  ana  ");

        assertTrue(token.startsWith("token-sso-ana-"));
    }

    @Test
    @DisplayName("Dos tokens del mismo usuario son distintos")
    void generarTokenDosVecesDevuelveTokensDistintos() {
        String primero = servicio.generarToken("ana");
        String segundo = servicio.generarToken("ana");

        assertNotEquals(primero, segundo);
    }

    @Test
    @DisplayName("El token generado con un rol asignado transporta ese rol")
    void generarTokenConRolAsignadoTransportaElRol() {
        assertTrue(gestorRoles.asignarRol("dr.house", Rol.MEDICO, TOKEN_ADMIN));

        String token = servicio.generarToken("dr.house");

        assertAll(
                () -> assertNotNull(token),
                () -> assertTrue(servicio.validarToken(token)),
                () -> assertEquals(Rol.MEDICO, gestorRoles.extraerRolDeToken(token)),
                () -> assertTrue(gestorRoles.tieneAcceso(token, Rol.MEDICO)));
    }

    // ---------------------------------------------------------------- validarToken

    @Test
    @DisplayName("Un token generado por el servicio es válido")
    void validarTokenGeneradoPorElServicioDevuelveTrue() {
        String token = servicio.generarToken("admin");

        assertTrue(servicio.validarToken(token));
    }

    @ParameterizedTest(name = "token [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "token-sso-hacker-ADMINISTRADOR-9999", "cualquier-cosa"})
    @DisplayName("Un token nulo, vacío o falsificado se rechaza")
    void validarTokenNuloVacioOFalsificadoDevuelveFalse(String token) {
        assertFalse(servicio.validarToken(token));
    }

    @Test
    @DisplayName("Un token emitido por otra instancia del servicio se rechaza")
    void validarTokenDeOtraInstanciaDevuelveFalse() {
        String tokenAjeno = new AutenticacionServiceImpl().generarToken("admin");

        assertFalse(servicio.validarToken(tokenAjeno));
    }

    // ---------------------------------------------------------------- vulnerabilidad detectada

    /**
     * VULNERABILIDAD DETECTADA (CU-25): el rol se extrae del texto del token y el nombre de
     * usuario forma parte de ese texto. Un usuario llamado "medico" o "mal-ADMINISTRADOR"
     * obtiene un token que GestorRolesServiceImpl interpreta con ese rol aunque no se le haya
     * asignado. Hasta corregirlo (issue + rama bugfix/), el test queda desactivado para no
     * poner el CI en rojo. Al activarlo debe pasar.
     */
    @ParameterizedTest(name = "el usuario {0} no debe obtener un rol por su nombre")
    @ValueSource(strings = {"medico", "mal-ADMINISTRADOR"})
    @Disabled("Pendiente de issue: el nombre de usuario puede inyectar un rol en el token")
    @DisplayName("El nombre de usuario no puede otorgar un rol sin asignación")
    void generarTokenConNombreQueImitaUnRolNoOtorgaEseRol(String usuario) {
        String token = servicio.generarToken(usuario);

        assertEquals(Rol.SIN_ROL, gestorRoles.extraerRolDeToken(token));
    }
}
