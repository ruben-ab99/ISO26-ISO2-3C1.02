package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.pacientes.PacienteDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-12 (Gestionar pacientes: alta, modificación y baja lógica).
 * Técnicas: clases de equivalencia (datos válidos / nulos / en blanco, paciente existente /
 * inexistente / dado de baja), control de acceso por rol (permitido / denegado), criterio each use
 * entre nombre y apellidos, y conjetura de errores (DNI en minúsculas o duplicado, reingreso tras
 * la baja). El gestor de roles se sustituye por un stub (ADR-012) que, como indica el propio
 * servicio, deja pasar a ADMISION y a ADMINISTRADOR.
 */
class GestorPacientesServiceImplTest {

    private GestorPacientesServiceImpl servicio;

    // Stub desacoplado de roles para aislar la prueba unitaria (ADR-002 / CU-29)
    private final IGestorRolesService stubRoles = new IGestorRolesService() {
        @Override
        public Rol extraerRolDeToken(String tokenSSO) {
            if (tokenSSO == null) return Rol.SIN_ROL;
            if (tokenSSO.contains("ADMISION")) return Rol.ADMISION;
            if (tokenSSO.contains("ADMINISTRADOR")) return Rol.ADMINISTRADOR;
            if (tokenSSO.contains("MEDICO")) return Rol.MEDICO;
            return Rol.SIN_ROL;
        }

        @Override
        public boolean tieneAcceso(String tokenSSO, Rol rolRequerido) {
            Rol actual = extraerRolDeToken(tokenSSO);
            return actual == Rol.ADMINISTRADOR || actual == rolRequerido;
        }

        @Override
        public boolean asignarRol(String idUsuario, Rol nuevoRol, String tokenAdmin) {
            return false;
        }
    };

    private PacienteDTO crearPacienteBase(String dni) {
        return new PacienteDTO(
                null,
                dni,
                "CIP-123456",
                "Carlos",
                "Pérez Gómez",
                LocalDate.of(1985, 5, 20),
                "600112233",
                true,
                null
        );
    }

    @BeforeEach
    void setUp() {
        servicio = new GestorPacientesServiceImpl(stubRoles);
    }

    private static final String ADMISION = "token-ADMISION";
    private static final String DNI = "12345678A";

    private PacienteDTO registrar(String dni) {
        return servicio.registrarPaciente(crearPacienteBase(dni), ADMISION);
    }

    // ---------------------------------------------------------------- constructor y permisos

    @Test
    @DisplayName("El constructor rechaza un servicio de roles nulo")
    void constructorConServicioRolesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GestorPacientesServiceImpl(null));

        assertEquals("El servicio de gestión de roles no puede ser nulo", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} puede gestionar pacientes")
    @ValueSource(strings = {"token-ADMISION", "token-ADMINISTRADOR"})
    @DisplayName("Los roles ADMISION y ADMINISTRADOR pueden dar de alta pacientes")
    void registrarPacienteConRolPermitidoCreaFichaActiva(String token) {
        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase(DNI), token);

        assertTrue(creado.isActivo());
    }

    @ParameterizedTest(name = "el token [{0}] no puede gestionar pacientes")
    @NullAndEmptySource
    @ValueSource(strings = {"token-MEDICO", "token-PACIENTE", "sin-rol-aqui"})
    @DisplayName("Sin rol permitido se deniegan el alta, la modificación y la baja")
    void gestionarPacientesSinPermisoLanzaSecurityException(String token) {
        PacienteDTO existente = registrar("87654321B");

        assertAll(
                () -> assertEquals("Acceso denegado: Se requiere rol ADMISION o ADMINISTRADOR para gestionar pacientes.",
                        assertThrows(SecurityException.class,
                                () -> servicio.registrarPaciente(crearPacienteBase(DNI), token)).getMessage()),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.modificarPaciente(existente.getPacienteId(), new PacienteDTO(), token)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.darBajaPaciente(existente.getPacienteId(), "Motivo", token)));
    }

    // ---------------------------------------------------------------- registrarPaciente

    @Test
    @DisplayName("Registrar un paciente válido crea una ficha activa con el DNI en mayúsculas")
    void registrarPacienteValidoCreaFichaActiva() {
        PacienteDTO creado = registrar("12345678a");

        assertAll(
                () -> assertNotNull(creado),
                () -> assertTrue(creado.getPacienteId().startsWith("PAC-")),
                () -> assertEquals("12345678A", creado.getDni()),
                () -> assertEquals("Carlos", creado.getNombre()),
                () -> assertTrue(creado.isActivo()),
                () -> assertNull(creado.getMotivoBaja()));
    }

    @Test
    @DisplayName("Registrar un paciente con el DNI de otro paciente activo lanza excepción (sin distinguir mayúsculas)")
    void registrarPacienteConDniDuplicadoActivoLanzaExcepcion() {
        registrar(DNI);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.registrarPaciente(crearPacienteBase("12345678a"), "token-ADMINISTRADOR"));

        assertEquals("Ya existe un paciente activo registrado con el DNI: 12345678a", e.getMessage());
    }

    @Test
    @DisplayName("Tras dar de baja a un paciente se puede registrar otra ficha con el mismo DNI")
    void registrarPacienteConDniDeUnPacienteDadoDeBajaEsValido() {
        PacienteDTO primero = registrar(DNI);
        servicio.darBajaPaciente(primero.getPacienteId(), "Traslado", ADMISION);

        assertDoesNotThrow(() -> registrar(DNI));
    }

    @Test
    @DisplayName("Registrar un paciente nulo lanza excepción")
    void registrarPacienteNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPaciente(null, ADMISION));

        assertEquals("Los datos del paciente son obligatorios", e.getMessage());
    }

    @ParameterizedTest(name = "DNI [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con DNI nulo, vacío o en blanco lanza excepción")
    void registrarPacienteConDniInvalidoLanzaExcepcion(String dni) {
        PacienteDTO sinDni = crearPacienteBase("87654321B");
        sinDni.setDni(dni);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPaciente(sinDni, ADMISION));

        assertEquals("El DNI del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "nombre [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con nombre nulo, vacío o en blanco lanza excepción")
    void registrarPacienteConNombreInvalidoLanzaExcepcion(String nombre) {
        PacienteDTO sinNombre = crearPacienteBase("87654321B");
        sinNombre.setNombre(nombre);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPaciente(sinNombre, ADMISION));

        assertEquals("Nombre y apellidos son obligatorios", e.getMessage());
    }

    @ParameterizedTest(name = "apellidos [{0}] no son válidos")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con apellidos nulos, vacíos o en blanco lanza excepción")
    void registrarPacienteConApellidosInvalidosLanzaExcepcion(String apellidos) {
        PacienteDTO sinApellidos = crearPacienteBase("87654321B");
        sinApellidos.setApellidos(apellidos);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarPaciente(sinApellidos, ADMISION));

        assertEquals("Nombre y apellidos son obligatorios", e.getMessage());
    }

    // ---------------------------------------------------------------- modificarPaciente

    @Test
    @DisplayName("Modificar un paciente actualiza solo los campos informados")
    void modificarPacienteExistenteActualizaSoloLosCamposInformados() {
        PacienteDTO creado = registrar("11223344C");
        PacienteDTO cambios = new PacienteDTO();
        cambios.setNombre("Carlos Alberto");
        cambios.setTelefono("699887766");

        PacienteDTO actualizado = servicio.modificarPaciente(creado.getPacienteId(), cambios, ADMISION);

        assertAll(
                () -> assertEquals("Carlos Alberto", actualizado.getNombre()),
                () -> assertEquals("699887766", actualizado.getTelefono()),
                () -> assertEquals("Pérez Gómez", actualizado.getApellidos()),
                () -> assertEquals("11223344C", actualizado.getDni()));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Modificar con identificador nulo, vacío o en blanco lanza excepción")
    void modificarPacienteConIdInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.modificarPaciente(pacienteId, new PacienteDTO(), ADMISION));

        assertEquals("El ID del paciente es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Modificar con datos nulos lanza excepción")
    void modificarPacienteConDatosNulosLanzaExcepcion() {
        PacienteDTO creado = registrar("11223344C");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.modificarPaciente(creado.getPacienteId(), null, ADMISION));

        assertEquals("Los datos actualizados no pueden ser nulos", e.getMessage());
    }

    @Test
    @DisplayName("Modificar un paciente inexistente lanza excepción")
    void modificarPacienteInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.modificarPaciente("PAC-NO-EXISTE", new PacienteDTO(), ADMISION));

        assertEquals("No existe ningún paciente con ID: PAC-NO-EXISTE", e.getMessage());
    }

    @Test
    @DisplayName("Modificar un paciente dado de baja lanza excepción")
    void modificarPacienteDadoDeBajaLanzaExcepcion() {
        PacienteDTO creado = registrar("55667788D");
        servicio.darBajaPaciente(creado.getPacienteId(), "Traslado de comunidad", ADMISION);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.modificarPaciente(creado.getPacienteId(), new PacienteDTO(), ADMISION));

        assertEquals("No se puede modificar un paciente dado de baja: " + creado.getPacienteId(), e.getMessage());
    }

    // ---------------------------------------------------------------- darBajaPaciente

    @Test
    @DisplayName("La baja es lógica: marca inactivo, guarda el motivo y conserva la ficha")
    void darBajaPacienteMarcaInactivoYConservaLaFicha() {
        PacienteDTO creado = registrar("99887766E");

        boolean resultado = servicio.darBajaPaciente(creado.getPacienteId(), "Solicitud del paciente", ADMISION);

        PacienteDTO consultado = servicio.consultarPaciente(creado.getPacienteId(), "token-MEDICO");
        assertAll(
                () -> assertTrue(resultado),
                () -> assertFalse(consultado.isActivo()),
                () -> assertEquals("Solicitud del paciente", consultado.getMotivoBaja()),
                () -> assertEquals("99887766E", consultado.getDni()));
    }

    @Test
    @DisplayName("Dar de baja dos veces al mismo paciente devuelve false la segunda vez")
    void darBajaPacienteYaDadoDeBajaDevuelveFalse() {
        PacienteDTO creado = registrar("99887766E");
        servicio.darBajaPaciente(creado.getPacienteId(), "Solicitud del paciente", ADMISION);

        assertFalse(servicio.darBajaPaciente(creado.getPacienteId(), "Duplicado", ADMISION));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Dar de baja con identificador nulo, vacío o en blanco lanza excepción")
    void darBajaConIdInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.darBajaPaciente(pacienteId, "Motivo", ADMISION));

        assertEquals("El ID del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Dar de baja con motivo nulo, vacío o en blanco lanza excepción")
    void darBajaConMotivoInvalidoLanzaExcepcion(String motivo) {
        PacienteDTO creado = registrar("44332211F");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.darBajaPaciente(creado.getPacienteId(), motivo, ADMISION));

        assertEquals("Debe indicarse un motivo para la baja del paciente", e.getMessage());
    }

    @Test
    @DisplayName("Dar de baja a un paciente inexistente lanza excepción")
    void darBajaPacienteInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.darBajaPaciente("PAC-NO-EXISTE", "Motivo", ADMISION));

        assertEquals("No existe ningún paciente con ID: PAC-NO-EXISTE", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarPaciente

    @Test
    @DisplayName("Consultar un paciente existente con un token informado lo devuelve")
    void consultarPacienteExistenteLoDevuelve() {
        PacienteDTO creado = registrar("11223344C");

        assertEquals(creado.getPacienteId(), servicio.consultarPaciente(creado.getPacienteId(), "token-MEDICO").getPacienteId());
    }

    @ParameterizedTest(name = "token [{0}] no está informado")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Consultar sin token de sesión lanza SecurityException")
    void consultarPacienteSinTokenLanzaSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.consultarPaciente("PAC-01", token));

        assertEquals("Token de sesión obligatorio para consultar datos de pacientes", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Consultar con identificador nulo, vacío o en blanco lanza excepción")
    void consultarPacienteConIdInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarPaciente(pacienteId, "token-MEDICO"));

        assertEquals("El ID del paciente es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar un paciente inexistente lanza excepción")
    void consultarPacienteInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarPaciente("PAC-FANTASMA", "token-MEDICO"));

        assertEquals("No existe ningún paciente con ID: PAC-FANTASMA", e.getMessage());
    }
}
