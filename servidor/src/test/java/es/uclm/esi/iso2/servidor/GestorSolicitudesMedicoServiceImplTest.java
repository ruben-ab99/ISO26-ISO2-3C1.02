package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("CP-01: Los constructores validan dependencias obligatorias y rechazan nulos")
    void constructoresValidanDependencias() {
        // Preparar y Ejecutar / Comprobar
        assertAll(
                () -> assertNotNull(new GestorSolicitudesMedicoServiceImpl()),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new GestorSolicitudesMedicoServiceImpl(null, new SolicitudPruebaServiceImpl(), stubAgendas)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new GestorSolicitudesMedicoServiceImpl(stubRoles, null, stubAgendas)),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        new GestorSolicitudesMedicoServiceImpl(stubRoles, new SolicitudPruebaServiceImpl(), null))
        );
    }

    @ParameterizedTest(name = "CP-02: Prioridad {0} con prueba {1} genera nivel {2}")
    @CsvSource({
            "1, TAC-ABDO, URGENTE, token-MEDICO",
            "3, RM-CRANEO, PREFERENTE, token-ADMINISTRADOR",
            "5, RX-TORAX, ORDINARIA, token-MEDICO"
    })
    @DisplayName("CP-02: Registrar solicitud válida crea instancias distintas en estado PENDIENTE y asigna prioridad")
    void registrarSolicitudValidaPorClasesYLimites(int prioridad, String codigoPrueba, String nivelEsperado, String token) {
        // Preparar
        String pacienteId = "PAC-100";
        String medicoId = "MED-01";

        // Ejecutar
        SolicitudPruebaDTO creada = servicio.registrarSolicitudMedico(
                pacienteId, medicoId, codigoPrueba, prioridad, "Justificación clínica", token
        );
        SolicitudPruebaDTO segunda = servicio.registrarSolicitudMedico(
                pacienteId, medicoId, codigoPrueba, prioridad, "Justificación clínica", token
        );

        // Comprobar
        assertAll(
                () -> assertNotNull(creada),
                () -> assertNotSame(creada, segunda, "Dos solicitudes deben ser objetos distintos"),
                () -> assertTrue(creada.getSolicitudId().startsWith("SOL-")),
                () -> assertEquals("PENDIENTE", creada.getEstado()),
                () -> assertEquals(nivelEsperado, creada.getNivelPrioridad())
        );
    }

    @ParameterizedTest(name = "CP-03: Prioridad fuera de rango ({0}) lanza IllegalArgumentException")
    @ValueSource(ints = {0, -1, 6})
    @DisplayName("CP-03: Valores límite adyacentes de prioridad lanzan IllegalArgumentException")
    void registrarSolicitudPrioridadFueraDeRango(int prioridadInvalida) {
        // Preparar
        String token = "token-MEDICO";

        // Ejecutar y Comprobar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarSolicitudMedico("PAC-100", "MED-01", "RX-TORAX", prioridadInvalida, "Justif", token)
        );
        assertTrue(ex.getMessage().contains("prioridad"));
    }

    @ParameterizedTest(name = "CP-04: Token no autorizado [{0}] es rechazado")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "token-ADMISION", "token-PACIENTE", "token-INVALIDO"})
    @DisplayName("CP-04: Usuarios sin rol MEDICO o ADMINISTRADOR reciben SecurityException")
    void operacionesRechazanRolesNoAutorizados(String tokenNoAutorizado) {
        // Preparar
        LocalDate fecha = LocalDate.of(2026, 11, 24);

        // Ejecutar y Comprobar
        assertAll(
                () -> assertThrows(SecurityException.class, () ->
                        servicio.registrarSolicitudMedico("PAC-01", "MED-01", "RX-TORAX", 3, "Obs", tokenNoAutorizado)),
                () -> assertThrows(SecurityException.class, () ->
                        servicio.consultarSolicitudesMedico("MED-01", null, tokenNoAutorizado)),
                () -> assertThrows(SecurityException.class, () ->
                        servicio.actualizarEstadoSolicitud("SOL-1", "CITADA", null, tokenNoAutorizado)),
                () -> assertThrows(SecurityException.class, () ->
                        servicio.consultarAgendaRealizacion("MED-RAD-01", fecha, tokenNoAutorizado))
        );
    }

    @ParameterizedTest(name = "CP-05: Combinación inválida ({0}, {1}, {2}, {3})")
    @CsvSource(nullValues = "NULL", value = {
            "NULL, MED-01, RX-TORAX, Justificación",
            "'   ', MED-01, RX-TORAX, Justificación",
            "PAC-01, NULL, RX-TORAX, Justificación",
            "PAC-01, '', RX-TORAX, Justificación",
            "PAC-01, MED-01, NULL, Justificación",
            "PAC-01, MED-01, ' ', Justificación",
            "PAC-01, MED-01, RX-TORAX, NULL",
            "PAC-01, MED-01, RX-TORAX, ''"
    })
    @DisplayName("CP-05: Parámetros obligatorios nulos o en blanco lanzan IllegalArgumentException")
    void registrarSolicitudConParametrosObligatoriosInvalidos(String pacienteId, String medicoId,
                                                              String codigoPrueba, String justificacion) {
        // Ejecutar y Comprobar
        assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarSolicitudMedico(pacienteId, medicoId, codigoPrueba, 3, justificacion, "token-MEDICO"));
    }

    @ParameterizedTest(name = "CP-06: Código de prueba no disponible [{0}]")
    @ValueSource(strings = {"PRUEBA-INEXISTENTE", "RX-OBSOLETA"})
    @DisplayName("CP-06: Solicitar pruebas inexistentes o dadas de baja en catálogo lanza IllegalStateException")
    void registrarSolicitudConPruebaInactivaOInexistente(String codigoPrueba) {
        // Ejecutar y Comprobar
        assertThrows(IllegalStateException.class, () ->
                servicio.registrarSolicitudMedico("PAC-01", "MED-01", codigoPrueba, 2, "Sospecha clínica", "token-MEDICO"));
    }

    @Test
    @DisplayName("CP-07 y CP-08: Consultar solicitudes filtra por médico y estado y rechaza entradas inválidas")
    void consultarSolicitudesMedicoConFiltrosYValidaciones() {
        // Preparar
        SolicitudPruebaDTO s1 = servicio.registrarSolicitudMedico("PAC-1", "MED-01", "RX-TORAX", 4, "Control", "token-MEDICO");
        SolicitudPruebaDTO s2 = servicio.registrarSolicitudMedico("PAC-2", "MED-01", "TAC-ABDO", 1, "Dolor", "token-MEDICO");
        servicio.registrarSolicitudMedico("PAC-3", "MED-02", "ECO-ABDO", 3, "Revisión", "token-MEDICO");
        servicio.actualizarEstadoSolicitud(s2.getSolicitudId(), "CITADA", null, "token-MEDICO");

        // Ejecutar
        List<SolicitudPruebaDTO> todasMed01 = servicio.consultarSolicitudesMedico("MED-01", null, "token-MEDICO");
        List<SolicitudPruebaDTO> pendientesMed01 = servicio.consultarSolicitudesMedico("MED-01", "pendiente", "token-MEDICO");
        List<SolicitudPruebaDTO> citadasMed01 = servicio.consultarSolicitudesMedico("MED-01", "CITADA", "token-MEDICO");

        // Comprobar
        assertAll(
                () -> assertEquals(2, todasMed01.size()),
                () -> assertEquals(1, pendientesMed01.size()),
                () -> assertEquals(s1.getSolicitudId(), pendientesMed01.get(0).getSolicitudId()),
                () -> assertEquals(1, citadasMed01.size()),
                () -> assertEquals(s2.getSolicitudId(), citadasMed01.get(0).getSolicitudId()),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.consultarSolicitudesMedico("", null, "token-MEDICO")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.consultarSolicitudesMedico("MED-01", "ESTADO_INVALIDO", "token-MEDICO"))
        );
    }

    @Test
    @DisplayName("CP-09 y CP-10: Ciclo de vida de estados respeta transiciones válidas y bloquea estados finales")
    void actualizarEstadoRespetaCicloDeVida() {
        // Preparar
        SolicitudPruebaDTO solRealizada = servicio.registrarSolicitudMedico("PAC-10", "MED-01", "TAC-ABDO", 1, "Urgente", "token-MEDICO");
        SolicitudPruebaDTO solCancelada = servicio.registrarSolicitudMedico("PAC-11", "MED-01", "RX-TORAX", 5, "Control", "token-MEDICO");

        // Ejecutar
        servicio.actualizarEstadoSolicitud(solRealizada.getSolicitudId(), "EN_REALIZACION", null, "token-MEDICO");
        servicio.actualizarEstadoSolicitud(solRealizada.getSolicitudId(), "REALIZADA", null, "token-MEDICO");
        SolicitudPruebaDTO resultadoCancelacion = servicio.actualizarEstadoSolicitud(
                solCancelada.getSolicitudId(), "CANCELADA", "Prueba duplicada", "token-MEDICO"
        );

        // Comprobar
        assertAll(
                () -> assertEquals("REALIZADA", solRealizada.getEstado()),
                () -> assertEquals("CANCELADA", resultadoCancelacion.getEstado()),
                () -> assertEquals("Prueba duplicada", resultadoCancelacion.getMotivoCancelacion()),
                () -> assertThrows(IllegalStateException.class, () ->
                        servicio.actualizarEstadoSolicitud(solRealizada.getSolicitudId(), "CANCELADA", "Tarde", "token-MEDICO")),
                () -> assertThrows(IllegalStateException.class, () ->
                        servicio.actualizarEstadoSolicitud(solCancelada.getSolicitudId(), "CITADA", null, "token-MEDICO")),
                () -> assertThrows(IllegalStateException.class, () ->
                        servicio.actualizarEstadoSolicitud("SOL-INEXISTENTE", "CITADA", null, "token-MEDICO")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.actualizarEstadoSolicitud("", "CITADA", null, "token-MEDICO")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.actualizarEstadoSolicitud(solRealizada.getSolicitudId(), "NO_EXISTE", null, "token-MEDICO"))
        );
    }

    @Test
    @DisplayName("CP-11: Consultar agenda y huecos libres filtra disponibilidad y gestiona agendas cerradas")
    void consultarAgendaYHuecosDisponiblesDeRealizacion() {
        // Preparar
        LocalDate fecha = LocalDate.of(2026, 11, 24);

        // Ejecutar
        AgendaDiariaDTO agenda = servicio.consultarAgendaRealizacion("MED-RAD-01", fecha, "token-MEDICO");
        List<TramoHorarioDTO> huecosLibres = servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", fecha, "token-MEDICO");

        // Comprobar estado abierto con tramos mixtos
        assertAll(
                () -> assertEquals("AGD-RAD-01", agenda.getAgendaId()),
                () -> assertEquals(2, huecosLibres.size()),
                () -> assertEquals(LocalTime.of(8, 30), huecosLibres.get(0).getHoraInicio()),
                () -> assertEquals(LocalTime.of(9, 30), huecosLibres.get(1).getHoraInicio()),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.consultarAgendaRealizacion("", fecha, "token-MEDICO")),
                () -> assertThrows(IllegalArgumentException.class, () ->
                        servicio.consultarAgendaRealizacion("MED-RAD-01", null, "token-MEDICO"))
        );

        // Conjetura de errores: agenda cerrada o sin tramos devuelve lista vacía
        agendaAbiertaStub = false;
        assertTrue(servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", fecha, "token-MEDICO").isEmpty());

        agendaAbiertaStub = true;
        tramosStub = null;
        assertTrue(servicio.consultarHuecosDisponiblesRealizacion("MED-RAD-01", fecha, "token-MEDICO").isEmpty());
    }
}