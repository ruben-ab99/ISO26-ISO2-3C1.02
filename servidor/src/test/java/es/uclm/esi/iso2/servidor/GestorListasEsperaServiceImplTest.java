package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.listasespera.PrioridadEspera;
import es.uclm.sescam.contratos.listasespera.SolicitudEsperaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-15 (Gestionar listas de espera y huecos libres).
 * Técnicas: clases de equivalencia (token con / sin permiso, datos válidos / nulos / en blanco,
 * solicitud pendiente / atendida / inexistente, agenda abierta / cerrada), criterio each use
 * entre parámetros y conjetura de errores (duplicados con otra capitalización, orden por
 * prioridad y antigüedad). Los servicios de roles y agendas se sustituyen por stubs (ADR-012).
 */
class GestorListasEsperaServiceImplTest {

    private static final String TOKEN_ADMISION = "token-ADMISION";
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);

    private GestorListasEsperaServiceImpl servicio;
    private boolean agendaAbiertaStub;
    private boolean bloquearExitosoStub;
    private List<TramoHorarioDTO> tramosStub;

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

    private final IGestorAgendasService stubAgendas = new IGestorAgendasService() {
        @Override
        public AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                                      LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO) {
            return null;
        }

        @Override
        public AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO) {
            return new AgendaDiariaDTO("AGD-TEST", medicoId, fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, agendaAbiertaStub, tramosStub);
        }

        @Override
        public boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicio, LocalTime horaFin, String motivo, String tokenSSO) {
            return bloquearExitosoStub;
        }

        @Override
        public boolean cerrarAgendaDiaria(String agendaId, String tokenSSO) {
            return false;
        }
    };

    @BeforeEach
    void setUp() {
        agendaAbiertaStub = true;
        bloquearExitosoStub = true;
        tramosStub = null;
        servicio = new GestorListasEsperaServiceImpl(stubRoles, stubAgendas);
    }

    private SolicitudEsperaDTO registrar(String paciente, String prueba, PrioridadEspera prioridad) {
        return servicio.registrarEnListaEspera(paciente, prueba, prioridad, TOKEN_ADMISION);
    }

    // ---------------------------------------------------------------- constructor y permisos

    @ParameterizedTest(name = "sin roles={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("El constructor rechaza un servicio de roles o de agendas nulo")
    void constructorConColaboradorNuloLanzaExcepcion(boolean sinRoles) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new GestorListasEsperaServiceImpl(sinRoles ? null : stubRoles, sinRoles ? stubAgendas : null));

        assertEquals("Los servicios de roles y agendas son obligatorios", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} puede gestionar la lista de espera")
    @ValueSource(strings = {"token-ADMISION", "token-ADMINISTRADOR"})
    @DisplayName("Los roles ADMISION y ADMINISTRADOR pueden gestionar la lista de espera")
    void registrarConRolPermitidoLoRegistra(String token) {
        assertNotNull(servicio.registrarEnListaEspera("PAC-01", "RM", PrioridadEspera.ORDINARIA, token));
    }

    @ParameterizedTest(name = "token [{0}] no está informado")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Sin token SSO se deniega cualquier operación de la lista de espera")
    void gestionarListaSinTokenLanzaSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.registrarEnListaEspera("PAC-01", "RM", PrioridadEspera.URGENTE, token));

        assertEquals("Token SSO obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} no tiene permiso")
    @ValueSource(strings = {"token-MEDICO", "token-PACIENTE", "token-INVALIDO"})
    @DisplayName("Sin rol ADMISION ni ADMINISTRADOR se deniegan todas las operaciones")
    void gestionarListaSinPermisoLanzaSecurityException(String token) {
        SolicitudEsperaDTO existente = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        assertAll(
                () -> assertEquals("Acceso denegado: se requiere rol ADMISION o ADMINISTRADOR",
                        assertThrows(SecurityException.class, () ->
                                servicio.registrarEnListaEspera("PAC-02", "RM", PrioridadEspera.URGENTE, token)).getMessage()),
                () -> assertThrows(SecurityException.class, () -> servicio.consultarListaEspera("RM", token)),
                () -> assertThrows(SecurityException.class, () ->
                        servicio.actualizarPrioridad(existente.getSolicitudId(), PrioridadEspera.ORDINARIA, token)),
                () -> assertThrows(SecurityException.class, () -> servicio.contarHuecosLibres("MED-01", FECHA, token)),
                () -> assertThrows(SecurityException.class, () -> servicio.asignarHuecoLibre(
                        existente.getSolicitudId(), "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), token)));
    }

    // ---------------------------------------------------------------- registrarEnListaEspera

    @Test
    @DisplayName("Registrar una solicitud válida la deja pendiente y sin espacios sobrantes")
    void registrarSolicitudValidaLaDejaPendiente() {
        SolicitudEsperaDTO solicitud = registrar("  PAC-01 ", " RM ", PrioridadEspera.PREFERENTE);

        assertAll(
                () -> assertTrue(solicitud.getSolicitudId().startsWith("ESP-")),
                () -> assertEquals("PAC-01", solicitud.getPacienteId()),
                () -> assertEquals("RM", solicitud.getTipoPrueba()),
                () -> assertEquals(PrioridadEspera.PREFERENTE, solicitud.getPrioridad()),
                () -> assertFalse(solicitud.isAtendida()),
                () -> assertNotNull(solicitud.getFechaRegistro()));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Registrar con paciente nulo, vacío o en blanco lanza excepción")
    void registrarConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> registrar(paciente, "RM", PrioridadEspera.URGENTE));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "tipo de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Registrar con tipo de prueba nulo, vacío o en blanco lanza excepción")
    void registrarConTipoPruebaInvalidoLanzaExcepcion(String tipoPrueba) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> registrar("PAC-01", tipoPrueba, PrioridadEspera.URGENTE));

        assertEquals("El tipo de prueba es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Registrar sin prioridad lanza excepción")
    void registrarSinPrioridadLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> registrar("PAC-01", "RM", null));

        assertEquals("La prioridad es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "duplicado como {0} / {1}")
    @CsvSource({
            "PAC-01, RM",
            "pac-01, rm",
            "' PAC-01 ', ' RM '"
    })
    @DisplayName("Un paciente no puede tener dos solicitudes pendientes de la misma prueba (sin distinguir mayúsculas)")
    void registrarDuplicadoLanzaExcepcion(String paciente, String prueba) {
        registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registrar(paciente, prueba, PrioridadEspera.ORDINARIA));

        assertEquals("El paciente ya tiene una solicitud pendiente para esa prueba", e.getMessage());
    }

    @Test
    @DisplayName("El mismo paciente sí puede esperar una prueba distinta")
    void registrarOtraPruebaDelMismoPacienteEsValido() {
        registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        assertDoesNotThrow(() -> registrar("PAC-01", "TAC", PrioridadEspera.URGENTE));
    }

    // ---------------------------------------------------------------- consultarListaEspera

    @Test
    @DisplayName("La lista se ordena por prioridad y, a igual prioridad, por antigüedad")
    void consultarListaEsperaOrdenaPorPrioridadYAntiguedad() {
        SolicitudEsperaDTO ordinaria = registrar("PAC-01", "RM", PrioridadEspera.ORDINARIA);
        SolicitudEsperaDTO urgente = registrar("PAC-02", "RM", PrioridadEspera.URGENTE);
        SolicitudEsperaDTO preferente = registrar("PAC-03", "RM", PrioridadEspera.PREFERENTE);

        List<SolicitudEsperaDTO> lista = servicio.consultarListaEspera("RM", TOKEN_ADMISION);

        assertAll(
                () -> assertEquals(3, lista.size()),
                () -> assertEquals(urgente.getSolicitudId(), lista.get(0).getSolicitudId()),
                () -> assertEquals(preferente.getSolicitudId(), lista.get(1).getSolicitudId()),
                () -> assertEquals(ordinaria.getSolicitudId(), lista.get(2).getSolicitudId()));
    }

    @Test
    @DisplayName("Filtrar por tipo de prueba devuelve solo las solicitudes de ese tipo")
    void consultarListaEsperaFiltraPorTipoDePrueba() {
        registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        registrar("PAC-02", "TAC", PrioridadEspera.URGENTE);

        assertEquals(1, servicio.consultarListaEspera("rm", TOKEN_ADMISION).size());
    }

    @ParameterizedTest(name = "filtro [{0}] devuelve todas")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin filtro de tipo se devuelven todas las solicitudes pendientes")
    void consultarListaEsperaSinFiltroDevuelveTodas(String tipoPrueba) {
        registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        registrar("PAC-02", "TAC", PrioridadEspera.URGENTE);

        assertEquals(2, servicio.consultarListaEspera(tipoPrueba, TOKEN_ADMISION).size());
    }

    // ---------------------------------------------------------------- actualizarPrioridad

    @ParameterizedTest(name = "de ORDINARIA a {0} se actualiza")
    @EnumSource(value = PrioridadEspera.class, names = {"URGENTE", "PREFERENTE"})
    @DisplayName("Cambiar a una prioridad distinta actualiza la solicitud")
    void actualizarPrioridadDistintaLaActualiza(PrioridadEspera nueva) {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.ORDINARIA);

        boolean actualizada = servicio.actualizarPrioridad(solicitud.getSolicitudId(), nueva, TOKEN_ADMISION);

        assertAll(
                () -> assertTrue(actualizada),
                () -> assertEquals(nueva, solicitud.getPrioridad()));
    }

    @Test
    @DisplayName("Cambiar a la misma prioridad devuelve false")
    void actualizarPrioridadIgualDevuelveFalse() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        assertFalse(servicio.actualizarPrioridad(solicitud.getSolicitudId(), PrioridadEspera.URGENTE, TOKEN_ADMISION));
    }

    @Test
    @DisplayName("Cambiar la prioridad de una solicitud ya atendida devuelve false")
    void actualizarPrioridadDeSolicitudAtendidaDevuelveFalse() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        servicio.asignarHuecoLibre(solicitud.getSolicitudId(), "MED-01", FECHA,
                LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION);

        assertFalse(servicio.actualizarPrioridad(solicitud.getSolicitudId(), PrioridadEspera.ORDINARIA, TOKEN_ADMISION));
    }

    @ParameterizedTest(name = "solicitud [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Actualizar con identificador nulo, vacío o en blanco lanza excepción")
    void actualizarPrioridadConIdInvalidoLanzaExcepcion(String solicitudId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarPrioridad(solicitudId, PrioridadEspera.URGENTE, TOKEN_ADMISION));

        assertEquals("Identificador de solicitud y nueva prioridad obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Actualizar sin nueva prioridad lanza excepción")
    void actualizarPrioridadSinPrioridadLanzaExcepcion() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarPrioridad(solicitud.getSolicitudId(), null, TOKEN_ADMISION));

        assertEquals("Identificador de solicitud y nueva prioridad obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Actualizar una solicitud inexistente lanza excepción")
    void actualizarPrioridadInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.actualizarPrioridad("ESP-NO-EXISTE", PrioridadEspera.URGENTE, TOKEN_ADMISION));

        assertEquals("No existe la solicitud indicada", e.getMessage());
    }

    // ---------------------------------------------------------------- contarHuecosLibres

    @Test
    @DisplayName("Contar huecos libres devuelve solo los tramos disponibles de una agenda abierta")
    void contarHuecosLibresCuentaSoloLosDisponibles() {
        tramosStub = List.of(
                new TramoHorarioDTO(LocalTime.of(9, 0), LocalTime.of(9, 20), true, ""),
                new TramoHorarioDTO(LocalTime.of(9, 20), LocalTime.of(9, 40), false, "Bloqueado"),
                new TramoHorarioDTO(LocalTime.of(9, 40), LocalTime.of(10, 0), true, ""));

        assertEquals(2, servicio.contarHuecosLibres("MED-01", FECHA, TOKEN_ADMISION));
    }

    @Test
    @DisplayName("Una agenda cerrada no tiene huecos libres")
    void contarHuecosLibresConAgendaCerradaDevuelveCero() {
        agendaAbiertaStub = false;
        tramosStub = List.of(new TramoHorarioDTO(LocalTime.of(9, 0), LocalTime.of(9, 20), true, ""));

        assertEquals(0, servicio.contarHuecosLibres("MED-01", FECHA, TOKEN_ADMISION));
    }

    @Test
    @DisplayName("Una agenda sin tramos no tiene huecos libres")
    void contarHuecosLibresSinTramosDevuelveCero() {
        tramosStub = null;

        assertEquals(0, servicio.contarHuecosLibres("MED-01", FECHA, TOKEN_ADMISION));
    }

    // ---------------------------------------------------------------- asignarHuecoLibre

    @Test
    @DisplayName("Asignar un hueco libre atiende la solicitud y guarda agenda, fecha y hora")
    void asignarHuecoLibreAtiendeLaSolicitud() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        SolicitudEsperaDTO asignada = servicio.asignarHuecoLibre(solicitud.getSolicitudId(), "MED-01", FECHA,
                LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION);

        assertAll(
                () -> assertTrue(asignada.isAtendida()),
                () -> assertEquals("AGD-TEST", asignada.getAgendaAsignadaId()),
                () -> assertEquals(FECHA, asignada.getFechaAsignada()),
                () -> assertEquals(LocalTime.of(9, 0), asignada.getHoraAsignada()));
    }

    @Test
    @DisplayName("Una solicitud atendida desaparece de la lista de espera")
    void asignarHuecoLibreLaQuitaDeLaLista() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        servicio.asignarHuecoLibre(solicitud.getSolicitudId(), "MED-01", FECHA,
                LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION);

        assertTrue(servicio.consultarListaEspera("RM", TOKEN_ADMISION).isEmpty());
    }

    @ParameterizedTest(name = "solicitud [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Asignar con solicitud nula, vacía o en blanco lanza excepción")
    void asignarHuecoConSolicitudInvalidaLanzaExcepcion(String solicitudId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.asignarHuecoLibre(
                solicitudId, "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION));

        assertEquals("Datos de solicitud, médico, fecha y franja horaria obligatorios", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Asignar con médico nulo, vacío o en blanco lanza excepción")
    void asignarHuecoConMedicoInvalidoLanzaExcepcion(String medico) {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.asignarHuecoLibre(
                solicitud.getSolicitudId(), medico, FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION));

        assertEquals("Datos de solicitud, médico, fecha y franja horaria obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Asignar sin fecha o sin horas lanza excepción")
    void asignarHuecoConFechaOHorasNulasLanzaExcepcion() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        String id = solicitud.getSolicitudId();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.asignarHuecoLibre(
                        id, "MED-01", null, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.asignarHuecoLibre(
                        id, "MED-01", FECHA, null, LocalTime.of(9, 20), TOKEN_ADMISION)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.asignarHuecoLibre(
                        id, "MED-01", FECHA, LocalTime.of(9, 0), null, TOKEN_ADMISION)));
    }

    @Test
    @DisplayName("Asignar una solicitud inexistente lanza excepción")
    void asignarHuecoSolicitudInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.asignarHuecoLibre(
                "ESP-NO-EXISTE", "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION));

        assertEquals("No existe la solicitud en lista de espera", e.getMessage());
    }

    @Test
    @DisplayName("Asignar una solicitud ya atendida lanza excepción")
    void asignarHuecoSolicitudYaAtendidaLanzaExcepcion() {
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);
        servicio.asignarHuecoLibre(solicitud.getSolicitudId(), "MED-01", FECHA,
                LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.asignarHuecoLibre(
                solicitud.getSolicitudId(), "MED-01", FECHA, LocalTime.of(9, 20), LocalTime.of(9, 40), TOKEN_ADMISION));

        assertEquals("La solicitud ya ha sido atendida", e.getMessage());
    }

    @Test
    @DisplayName("Asignar en una agenda cerrada lanza excepción y no atiende la solicitud")
    void asignarHuecoEnAgendaCerradaLanzaExcepcion() {
        agendaAbiertaStub = false;
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.asignarHuecoLibre(
                solicitud.getSolicitudId(), "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION));

        assertAll(
                () -> assertEquals("La agenda del médico está cerrada para esa fecha", e.getMessage()),
                () -> assertFalse(solicitud.isAtendida()));
    }

    @Test
    @DisplayName("Asignar un hueco que no está libre lanza excepción y no atiende la solicitud")
    void asignarHuecoNoLibreLanzaExcepcion() {
        bloquearExitosoStub = false;
        SolicitudEsperaDTO solicitud = registrar("PAC-01", "RM", PrioridadEspera.URGENTE);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.asignarHuecoLibre(
                solicitud.getSolicitudId(), "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), TOKEN_ADMISION));

        assertAll(
                () -> assertEquals("El hueco solicitado no está libre en la agenda", e.getMessage()),
                () -> assertFalse(solicitud.isAtendida()));
    }
}
