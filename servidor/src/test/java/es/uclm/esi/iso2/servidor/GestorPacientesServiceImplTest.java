package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.pacientes.PacienteDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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

    // --- 1. Constructor ---
    @Test
    void constructorConServicioRolesNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new GestorPacientesServiceImpl(null));
    }

    // --- 2. Alta de pacientes (registrarPaciente) ---
    @Test
    void registrarPacienteValidoConRolAdmisionCreaFichaActiva() {
        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase("12345678a"), "token-ADMISION");

        assertNotNull(creado);
        assertTrue(creado.getPacienteId().startsWith("PAC-"));
        assertEquals("12345678A", creado.getDni());
        assertEquals("Carlos", creado.getNombre());
        assertTrue(creado.isActivo());
        assertNull(creado.getMotivoBaja());
    }

    @Test
    void registrarPacienteDenegadoSinRolAdmision() {
        assertThrows(SecurityException.class, () ->
                servicio.registrarPaciente(crearPacienteBase("12345678A"), "token-MEDICO")
        );
    }

    @Test
    void registrarPacienteConDniDuplicadoActivoLanzaExcepcion() {
        servicio.registrarPaciente(crearPacienteBase("12345678A"), "token-ADMISION");

        assertThrows(IllegalStateException.class, () ->
                servicio.registrarPaciente(crearPacienteBase("12345678a"), "token-ADMINISTRADOR")
        );
    }

    @Test
    void registrarPacienteConDatosObligatoriosInvalidosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPaciente(null, "token-ADMISION"));

        PacienteDTO sinDni = crearPacienteBase("   ");
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPaciente(sinDni, "token-ADMISION"));

        PacienteDTO sinNombre = crearPacienteBase("87654321B");
        sinNombre.setNombre("");
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarPaciente(sinNombre, "token-ADMISION"));
    }

    // --- 3. Modificación de pacientes (modificarPaciente) ---
    @Test
    void modificarPacienteExistenteActualizaCampos() {
        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase("11223344C"), "token-ADMISION");

        PacienteDTO cambios = new PacienteDTO();
        cambios.setNombre("Carlos Alberto");
        cambios.setTelefono("699887766");

        PacienteDTO actualizado = servicio.modificarPaciente(creado.getPacienteId(), cambios, "token-ADMISION");

        assertEquals("Carlos Alberto", actualizado.getNombre());
        assertEquals("699887766", actualizado.getTelefono());
        assertEquals("Pérez Gómez", actualizado.getApellidos()); // Mantiene los no modificados
    }

    @Test
    void modificarPacienteInexistenteODadoDeBajaLanzaExcepcion() {
        assertThrows(IllegalStateException.class, () ->
                servicio.modificarPaciente("PAC-NO-EXISTE", new PacienteDTO(), "token-ADMISION")
        );

        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase("55667788D"), "token-ADMISION");
        servicio.darBajaPaciente(creado.getPacienteId(), "Traslado de comunidad", "token-ADMISION");

        assertThrows(IllegalStateException.class, () ->
                servicio.modificarPaciente(creado.getPacienteId(), new PacienteDTO(), "token-ADMISION")
        );
    }

    // --- 4. Baja de pacientes (darBajaPaciente) ---
    @Test
    void darBajaPacienteMarcaInactivoYConservaHistorial() {
        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase("99887766E"), "token-ADMISION");

        boolean resultado = servicio.darBajaPaciente(creado.getPacienteId(), "Solicitud del paciente", "token-ADMISION");
        assertTrue(resultado);

        // Segunda baja sobre el mismo paciente devuelve false
        assertFalse(servicio.darBajaPaciente(creado.getPacienteId(), "Duplicado", "token-ADMISION"));

        PacienteDTO consultado = servicio.consultarPaciente(creado.getPacienteId(), "token-MEDICO");
        assertFalse(consultado.isActivo());
        assertEquals("Solicitud del paciente", consultado.getMotivoBaja());
    }

    @Test
    void darBajaSinMotivoLanzaExcepcion() {
        PacienteDTO creado = servicio.registrarPaciente(crearPacienteBase("44332211F"), "token-ADMISION");

        assertThrows(IllegalArgumentException.class, () ->
                servicio.darBajaPaciente(creado.getPacienteId(), "   ", "token-ADMISION")
        );
    }

    // --- 5. Consulta de pacientes (consultarPaciente) ---
    @Test
    void consultarPacienteConTokenVacioOInexistenteLanzaExcepcion() {
        assertThrows(SecurityException.class, () -> servicio.consultarPaciente("PAC-01", ""));
        assertThrows(IllegalStateException.class, () -> servicio.consultarPaciente("PAC-FANTASMA", "token-MEDICO"));
    }
}