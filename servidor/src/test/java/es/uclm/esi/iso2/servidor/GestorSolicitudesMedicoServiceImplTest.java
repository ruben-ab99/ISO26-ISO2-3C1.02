package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-08 (Gestionar solicitudes de prueba y agendas de realización del médico).
 * Técnicas: clases de equivalencia (token con / sin permiso, datos válidos / nulos / en blanco,
 * estado válido / no válido), valores límite de la prioridad (0 y 6 no válidos; 1, 3 y 5 válidos),
 * criterio each use entre parámetros y conjetura de errores (estados finales, agenda cerrada o sin
 * tramos). Los servicios de roles y agendas se sustituyen por stubs (ADR-012).
 */
class GestorSolicitudesMedicoServiceImplTest {

    private GestorSolicitudesMedicoServiceImpl servicio;
    private boolean agendaAbiertaStub;
    private List<TramoHorarioDTO> tramosStub;

    private final IGestorRolesService stubRoles = new IGestorRolesService() {
        @Override
        public Rol extraerRolDeToken(String tokenSSO) {
            if (tokenSSO == null) return Rol.SIN_ROL;
            if (tokenSSO.contains("MEDICO")) return Rol.MEDICO;
            if (tokenSSO.contains("ADMINISTRADOR")) return Rol.ADMINISTRADOR;
            if (tokenSSO.contains("ADMISION")) return Rol.ADMISION;
            if (tokenSSO.contains("PACIENTE")) return Rol.PACIENTE;
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
            return new AgendaDiariaDTO("AGD-RAD-01", medicoId, fecha, LocalTime.of(8, 30), LocalTime.of(10, 0), 30, agendaAbiertaStub, tramosStub);
        }

        @Override
        public boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicio, LocalTime horaFin, String motivo, String tokenSSO) {
            return false;
        }

        @Override
        public boolean cerrarAgendaDiaria(String agendaId, String tokenSSO) {
            return false;
        }
    };

    @BeforeEach
    void setUp() {
        agendaAbiertaStub = true;
        tramosStub = List.of(
                new TramoHorarioDTO(LocalTime.of(8, 30), LocalTime.of(9, 0), true, null),
                new TramoHorarioDTO(LocalTime.of(9, 0), LocalTime.of(9, 30), false, "Paciente citado"),
                new TramoHorarioDTO(LocalTime.of(9, 30), LocalTime.of(10, 0), true, null)
        );
        servicio = new GestorSolicitudesMedicoServiceImpl(stubRoles, new SolicitudPruebaServiceImpl(), stubAgendas);
    }

    @AfterEach
    void tearDown() {
        servicio = null;
        tramosStub = null;
    }

    private static final String MEDICO = "token-MEDICO";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 24);

    private SolicitudPruebaDTO nueva(String paciente, String prueba, int prioridad) {
        return servicio.registrarSolicitudMedico(paciente, "MED-01", prueba, prioridad, "Justificación clínica", MEDICO);
    }

    // ---------------------------------------------------------------- constructores

    @Test
    @DisplayName("El constructor por defecto crea un servicio válido")
    void constructorPorDefectoNoLanzaExcepcion() {
        assertNotNull(new GestorSolicitudesMedicoServiceImpl());
    }

    @ParameterizedTest(name = "sin roles={0}, sin prescripciones={1}, sin agendas={2}")
    @CsvSource({
            "true,  false, false",
            "false, true,  false",
            "false, false, true"
    })
    @DisplayName("El constructor rechaza cada servicio colaborador nulo (criterio each use)")
    void constructorConColaboradorNuloLanzaExcepcion(boolean sinRoles, boolean sinSolicitudes, boolean sinAgendas) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new GestorSolicitudesMedicoServiceImpl(
                        sinRoles ? null : stubRoles,
                        sinSolicitudes ? null : new SolicitudPruebaServiceImpl(),
                        sinAgendas ? null : stubAgendas));

        assertEquals("Los servicios de roles, prescripciones y agendas son obligatorios", e.getMessage());
    }

    // ---------------------------------------------------------------- seguridad

    @ParameterizedTest(name = "token [{0}] no está informado")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("CP-04a: Sin token SSO se deniega cualquier operación del médico")
    void operacionesSinTokenLanzanSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.registrarSolicitudMedico("PAC-01", "MED-01", "RX-TORAX", 3, "Obs", token));

        assertEquals("Token SSO obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} no tiene permiso")
    @ValueSource(strings = {"token-ADMISION", "token-PACIENTE", "token-INVALIDO"})
    @DisplayName("CP-04b: Sin rol MEDICO ni ADMINISTRADOR se deniegan todas las operaciones")
    void operacionesSinPermisoLanzanSecurityException(String token) {
        assertAll(
                () -> assertEquals("Acceso denegado: se requiere rol MEDICO o ADMINISTRADOR",
                        assertThrows(SecurityException.class, () ->
                                servicio.registrarSolicitudMedico("PAC-01", "MED-01", "RX-TORAX", 3, "Obs", token)).getMessage()),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.consultarSolicitudesMedico("MED-01", null, token)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.actualizarEstadoSolicitud("SOL-1", "CITADA", null, token)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.consultarAgendaRealizacion("MED-RAD-01", FECHA, token)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", FECHA, token)));
    }

    // ---------------------------------------------------------------- registrarSolicitudMedico

    @ParameterizedTest(name = "CP-02: prioridad {0} con prueba {1} genera nivel {2}")
    @CsvSource({
            "1, TAC-ABDO,  URGENTE,    token-MEDICO",
            "2, ECO-ABDO,  PREFERENTE, token-MEDICO",
            "3, RM-CRANEO, PREFERENTE, token-ADMINISTRADOR",
            "4, ECO-ABDO,  ORDINARIA,  token-MEDICO",
            "5, RX-TORAX,  ORDINARIA,  token-MEDICO"
    })
    @DisplayName("CP-02: Registrar una solicitud válida crea instancias distintas PENDIENTES con su nivel de prioridad")
    void registrarSolicitudValidaPorClasesYLimites(int prioridad, String codigoPrueba, String nivelEsperado, String token) {
        SolicitudPruebaDTO creada = servicio.registrarSolicitudMedico("PAC-100", "MED-01", codigoPrueba, prioridad, "Justificación clínica", token);
        SolicitudPruebaDTO segunda = servicio.registrarSolicitudMedico("PAC-100", "MED-01", codigoPrueba, prioridad, "Justificación clínica", token);

        assertAll(
                () -> assertNotNull(creada),
                () -> assertNotSame(creada, segunda, "Dos solicitudes deben ser objetos distintos"),
                () -> assertTrue(creada.getSolicitudId().startsWith("SOL-")),
                () -> assertEquals("PENDIENTE", creada.getEstado()),
                () -> assertEquals(nivelEsperado, creada.getNivelPrioridad()));
    }

    @ParameterizedTest(name = "CP-03: prioridad {0} fuera de rango")
    @ValueSource(ints = {0, -1, 6})
    @DisplayName("CP-03: Los valores límite adyacentes de prioridad (0 y 6) lanzan excepción")
    void registrarSolicitudPrioridadFueraDeRangoLanzaExcepcion(int prioridadInvalida) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico("PAC-100", "MED-01", "RX-TORAX", prioridadInvalida, "Justif", MEDICO));

        assertEquals("La prioridad debe estar comprendida entre 1 (urgente) y 5 (ordinaria)", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("CP-05a: Registrar con paciente nulo, vacío o en blanco lanza excepción")
    void registrarSolicitudConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico(paciente, "MED-01", "RX-TORAX", 3, "Justificación", MEDICO));

        assertEquals("El identificador del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("CP-05b: Registrar con médico nulo, vacío o en blanco lanza excepción")
    void registrarSolicitudConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico("PAC-01", medico, "RX-TORAX", 3, "Justificación", MEDICO));

        assertEquals("El identificador del médico es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "código de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-05c: Registrar con código de prueba nulo, vacío o en blanco lanza excepción")
    void registrarSolicitudConCodigoPruebaInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico("PAC-01", "MED-01", codigo, 3, "Justificación", MEDICO));

        assertEquals("El código de la prueba diagnóstica es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "justificación [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-05d: Registrar con justificación nula, vacía o en blanco lanza excepción")
    void registrarSolicitudConJustificacionInvalidaLanzaExcepcion(String justificacion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico("PAC-01", "MED-01", "RX-TORAX", 3, justificacion, MEDICO));

        assertEquals("La justificación clínica es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "CP-06: código {0} no disponible")
    @ValueSource(strings = {"PRUEBA-INEXISTENTE", "RX-OBSOLETA"})
    @DisplayName("CP-06: Solicitar una prueba inexistente o dada de baja en el catálogo lanza excepción")
    void registrarSolicitudConPruebaNoDisponibleLanzaExcepcion(String codigoPrueba) {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.registrarSolicitudMedico("PAC-01", "MED-01", codigoPrueba, 2, "Sospecha clínica", MEDICO));

        assertEquals("No existe ninguna prueba activa con código: " + codigoPrueba, e.getMessage());
    }

    // ---------------------------------------------------------------- consultarSolicitudesMedico

    @Test
    @DisplayName("CP-07a: Consultar sin filtro devuelve todas las solicitudes del médico y no las de otro")
    void consultarSolicitudesMedicoSinFiltroDevuelveLasSuyas() {
        nueva("PAC-1", "RX-TORAX", 4);
        nueva("PAC-2", "TAC-ABDO", 1);
        servicio.registrarSolicitudMedico("PAC-3", "MED-02", "ECO-ABDO", 3, "Revisión", MEDICO);

        List<SolicitudPruebaDTO> todas = servicio.consultarSolicitudesMedico("MED-01", null, MEDICO);

        assertEquals(2, todas.size());
    }

    @ParameterizedTest(name = "CP-07b: filtro \"{0}\" devuelve {1} solicitud(es)")
    @CsvSource({
            "pendiente, 1",
            "PENDIENTE, 1",
            "CITADA,    1",
            "REALIZADA, 0"
    })
    @DisplayName("CP-07b: El filtro de estado se aplica sin distinguir mayúsculas")
    void consultarSolicitudesMedicoFiltraPorEstado(String filtro, int esperadas) {
        nueva("PAC-1", "RX-TORAX", 4);
        SolicitudPruebaDTO s2 = nueva("PAC-2", "TAC-ABDO", 1);
        servicio.actualizarEstadoSolicitud(s2.getSolicitudId(), "CITADA", null, MEDICO);

        assertEquals(esperadas, servicio.consultarSolicitudesMedico("MED-01", filtro, MEDICO).size());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-08a: Consultar con médico nulo, vacío o en blanco lanza excepción")
    void consultarSolicitudesMedicoConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarSolicitudesMedico(medico, null, MEDICO));

        assertEquals("El identificador del médico es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("CP-08b: Consultar con un estado de filtro no válido lanza excepción")
    void consultarSolicitudesMedicoConFiltroNoValidoLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarSolicitudesMedico("MED-01", "ESTADO_INVALIDO", MEDICO));

        assertEquals("Estado de solicitud no válido: ESTADO_INVALIDO", e.getMessage());
    }

    // ---------------------------------------------------------------- actualizarEstadoSolicitud

    @ParameterizedTest(name = "CP-09: de PENDIENTE a {0}")
    @ValueSource(strings = {"CITADA", "EN_REALIZACION", "REALIZADA", "citada"})
    @DisplayName("CP-09: Una solicitud pendiente puede pasar a cualquier estado válido (sin distinguir mayúsculas)")
    void actualizarEstadoDesdePendientePasaAlNuevoEstado(String nuevoEstado) {
        SolicitudPruebaDTO solicitud = nueva("PAC-10", "TAC-ABDO", 1);

        SolicitudPruebaDTO resultado = servicio.actualizarEstadoSolicitud(solicitud.getSolicitudId(), nuevoEstado, null, MEDICO);

        assertEquals(nuevoEstado.toUpperCase(), resultado.getEstado());
    }

    @Test
    @DisplayName("CP-09b: Cancelar una solicitud guarda el motivo y la deja CANCELADA")
    void actualizarEstadoACanceladaGuardaElMotivo() {
        SolicitudPruebaDTO solicitud = nueva("PAC-11", "RX-TORAX", 5);

        SolicitudPruebaDTO cancelada = servicio.actualizarEstadoSolicitud(
                solicitud.getSolicitudId(), "CANCELADA", "Prueba duplicada", MEDICO);

        assertAll(
                () -> assertEquals("CANCELADA", cancelada.getEstado()),
                () -> assertEquals("Prueba duplicada", cancelada.getMotivoCancelacion()));
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("CP-09c: Cancelar sin motivo lanza excepción")
    void actualizarEstadoACanceladaSinMotivoLanzaExcepcion(String motivo) {
        SolicitudPruebaDTO solicitud = nueva("PAC-11", "RX-TORAX", 5);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.actualizarEstadoSolicitud(solicitud.getSolicitudId(), "CANCELADA", motivo, MEDICO));

        assertEquals("El motivo de cancelación es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "CP-10: una solicitud {0} no admite cambios")
    @ValueSource(strings = {"REALIZADA", "CANCELADA"})
    @DisplayName("CP-10: Una solicitud finalizada o cancelada no se puede modificar")
    void actualizarEstadoDeSolicitudFinalLanzaExcepcion(String estadoFinal) {
        SolicitudPruebaDTO solicitud = nueva("PAC-12", "TAC-ABDO", 1);
        servicio.actualizarEstadoSolicitud(solicitud.getSolicitudId(), estadoFinal, "Motivo", MEDICO);

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.actualizarEstadoSolicitud(solicitud.getSolicitudId(), "CITADA", null, MEDICO));

        assertEquals("No se puede modificar una solicitud ya finalizada o cancelada", e.getMessage());
    }

    @ParameterizedTest(name = "solicitud [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-10b: Actualizar con identificador nulo, vacío o en blanco lanza excepción")
    void actualizarEstadoConSolicitudInvalidaLanzaExcepcion(String solicitudId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoSolicitud(solicitudId, "CITADA", null, MEDICO));

        assertEquals("El identificador de la solicitud es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "estado [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-10c: Actualizar con estado nulo, vacío o en blanco lanza excepción")
    void actualizarEstadoConEstadoVacioLanzaExcepcion(String estado) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoSolicitud("SOL-1", estado, null, MEDICO));

        assertEquals("El nuevo estado es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("CP-10d: Actualizar con un estado inexistente lanza excepción")
    void actualizarEstadoConEstadoNoValidoLanzaExcepcion() {
        SolicitudPruebaDTO solicitud = nueva("PAC-13", "RX-TORAX", 4);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoSolicitud(solicitud.getSolicitudId(), "NO_EXISTE", null, MEDICO));

        assertEquals("Estado de solicitud no válido: NO_EXISTE", e.getMessage());
    }

    @Test
    @DisplayName("CP-10e: Actualizar una solicitud inexistente lanza excepción")
    void actualizarEstadoDeSolicitudInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.actualizarEstadoSolicitud("SOL-INEXISTENTE", "CITADA", null, MEDICO));

        assertEquals("No existe ninguna solicitud con ID: SOL-INEXISTENTE", e.getMessage());
    }

    // ---------------------------------------------------------------- agenda de realización

    @Test
    @DisplayName("CP-11a: Consultar la agenda de realización devuelve la agenda del médico realizador")
    void consultarAgendaRealizacionDevuelveLaAgenda() {
        AgendaDiariaDTO agenda = servicio.consultarAgendaRealizacion("MED-RAD-01", FECHA, MEDICO);

        assertEquals("AGD-RAD-01", agenda.getAgendaId());
    }

    @ParameterizedTest(name = "médico realizador [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("CP-11b: Consultar la agenda con médico nulo, vacío o en blanco lanza excepción")
    void consultarAgendaRealizacionConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarAgendaRealizacion(medico, FECHA, MEDICO));

        assertEquals("El médico realizador y la fecha son obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("CP-11c: Consultar la agenda sin fecha lanza excepción")
    void consultarAgendaRealizacionSinFechaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarAgendaRealizacion("MED-RAD-01", null, MEDICO));

        assertEquals("El médico realizador y la fecha son obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("CP-11d: Los huecos disponibles incluyen solo los tramos libres de una agenda abierta")
    void consultarHuecosDisponiblesDevuelveSoloLosTramosLibres() {
        List<TramoHorarioDTO> libres = servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", FECHA, MEDICO);

        assertAll(
                () -> assertEquals(2, libres.size()),
                () -> assertEquals(LocalTime.of(8, 30), libres.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(9, 30), libres.get(1).getHoraInicio()));
    }

    @Test
    @DisplayName("CP-11e: Una agenda cerrada no tiene huecos disponibles")
    void consultarHuecosDisponiblesConAgendaCerradaDevuelveListaVacia() {
        agendaAbiertaStub = false;

        assertTrue(servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", FECHA, MEDICO).isEmpty());
    }

    @Test
    @DisplayName("CP-11f: Una agenda sin tramos no tiene huecos disponibles")
    void consultarHuecosDisponiblesSinTramosDevuelveListaVacia() {
        tramosStub = null;

        assertTrue(servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", FECHA, MEDICO).isEmpty());
    }
}
