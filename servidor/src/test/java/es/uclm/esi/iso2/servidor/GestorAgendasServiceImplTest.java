package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-14 (Gestionar agendas de los médicos).
 * Técnicas: clases de equivalencia (token permitido / denegado, datos válidos / nulos / en blanco),
 * valores límite (duración 4 y 5 minutos, inicio igual a fin, rango justo suficiente) y
 * conjetura de errores (agenda duplicada, solape parcial del bloqueo, agenda cerrada).
 * El gestor de roles se sustituye por un stub (ADR-012).
 */
class GestorAgendasServiceImplTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final String TOKEN_ADMISION = "token-ADMISION";
    private static final String TOKEN_MEDICO = "token-MEDICO";

    private GestorAgendasServiceImpl servicio;

    private final IGestorRolesService stubRoles = new IGestorRolesService() {
        @Override
        public Rol extraerRolDeToken(String tokenSSO) {
            if (tokenSSO == null) return Rol.SIN_ROL;
            if (tokenSSO.contains("ADMISION")) return Rol.ADMISION;
            if (tokenSSO.contains("MEDICO")) return Rol.MEDICO;
            if (tokenSSO.contains("ADMINISTRADOR")) return Rol.ADMINISTRADOR;
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

    @BeforeEach
    void setUp() {
        servicio = new GestorAgendasServiceImpl(stubRoles);
    }

    private AgendaDiariaDTO agendaDe9a10CadaVeinte(String medico) {
        return servicio.configurarAgendaDiaria(medico, FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, TOKEN_MEDICO);
    }

    // ---------------------------------------------------------------- constructor y permisos

    @Test
    @DisplayName("El constructor rechaza un gestor de roles nulo")
    void constructorConGestorDeRolesNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GestorAgendasServiceImpl(null));

        assertEquals("El servicio de gestión de roles no puede ser nulo", e.getMessage());
    }

    @ParameterizedTest(name = "el token {0} puede configurar agendas")
    @ValueSource(strings = {"token-ADMISION", "token-MEDICO", "token-ADMINISTRADOR"})
    @DisplayName("Los roles ADMISION, MEDICO y ADMINISTRADOR pueden configurar agendas")
    void configurarAgendaConRolPermitidoLaCrea(String token) {
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, token);

        assertNotNull(agenda);
    }

    @ParameterizedTest(name = "el token [{0}] no puede gestionar agendas")
    @NullAndEmptySource
    @ValueSource(strings = {"token-INVALIDO", "token-PACIENTE"})
    @DisplayName("Sin rol permitido se deniega configurar, bloquear y cerrar agendas")
    void gestionarAgendasSinPermisoLanzaSecurityException(String token) {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-01");

        assertAll(
                () -> assertEquals("Acceso denegado: Se requiere rol ADMISION, MEDICO o ADMINISTRADOR.",
                        assertThrows(SecurityException.class, () -> servicio.configurarAgendaDiaria(
                                "MED-02", FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 15, token)).getMessage()),
                () -> assertThrows(SecurityException.class, () -> servicio.bloquearTramoAgenda(
                        agenda.getAgendaId(), LocalTime.of(9, 0), LocalTime.of(9, 20), "Motivo", token)),
                () -> assertThrows(SecurityException.class,
                        () -> servicio.cerrarAgendaDiaria(agenda.getAgendaId(), token)));
    }

    // ---------------------------------------------------------------- configurarAgendaDiaria

    @Test
    @DisplayName("Configurar una agenda válida genera los tramos esperados, todos libres")
    void configurarAgendaValidaGeneraTramosLibres() {
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, TOKEN_ADMISION);

        assertAll(
                () -> assertNotNull(agenda),
                () -> assertTrue(agenda.getAgendaId().startsWith("AGD-")),
                () -> assertTrue(agenda.isAbierta()),
                () -> assertEquals(3, agenda.getTramos().size()), // 09:00-09:20, 09:20-09:40, 09:40-10:00
                () -> assertTrue(agenda.getTramos().stream().allMatch(t -> t.isDisponible())));
    }

    @ParameterizedTest(name = "huecos de {0} min en 09:00-09:30 generan {1} tramos")
    @CsvSource({
            "5,  6",
            "10, 3",
            "15, 2",
            "30, 1"
    })
    @DisplayName("El número de tramos depende de la duración del hueco (incluye el mínimo de 5 min)")
    void configurarAgendaGeneraTantosTramosComoCabenEnElRango(int duracion, int tramosEsperados) {
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 30), duracion, TOKEN_MEDICO);

        assertEquals(tramosEsperados, agenda.getTramos().size());
    }

    @ParameterizedTest(name = "duración {0} min no es válida")
    @ValueSource(ints = {4, 1, 0, -5})
    @DisplayName("Una duración de hueco menor de 5 minutos (límite 4) lanza excepción")
    void configurarAgendaConDuracionInferiorAlMinimoLanzaExcepcion(int duracion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), duracion, TOKEN_MEDICO));

        assertEquals("La duración mínima de cada tramo debe ser de 5 minutos", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Configurar con médico nulo, vacío o en blanco lanza excepción")
    void configurarAgendaConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria(medico, FECHA, LocalTime.of(9, 0), LocalTime.of(10, 0), 15, TOKEN_MEDICO));

        assertEquals("El identificador del médico es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Configurar sin fecha o sin horas lanza excepción")
    void configurarAgendaConFechaOHorasNulasLanzaExcepcion() {
        String mensaje = "La fecha y las horas de inicio y fin son obligatorias";

        assertAll(
                () -> assertEquals(mensaje, assertThrows(IllegalArgumentException.class, () ->
                        servicio.configurarAgendaDiaria("MED-01", null, LocalTime.of(9, 0), LocalTime.of(10, 0), 15, TOKEN_MEDICO)).getMessage()),
                () -> assertEquals(mensaje, assertThrows(IllegalArgumentException.class, () ->
                        servicio.configurarAgendaDiaria("MED-01", FECHA, null, LocalTime.of(10, 0), 15, TOKEN_MEDICO)).getMessage()),
                () -> assertEquals(mensaje, assertThrows(IllegalArgumentException.class, () ->
                        servicio.configurarAgendaDiaria("MED-01", FECHA, LocalTime.of(9, 0), null, 15, TOKEN_MEDICO)).getMessage()));
    }

    @ParameterizedTest(name = "inicio {0} y fin {1} no son válidos")
    @CsvSource({
            "11:00, 10:00",
            "10:00, 10:00",
            "10:00, 09:59"
    })
    @DisplayName("Un inicio posterior o igual al fin (límite: iguales) lanza excepción")
    void configurarAgendaConRangoHorarioInvertidoLanzaExcepcion(String inicio, String fin) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", FECHA, LocalTime.parse(inicio), LocalTime.parse(fin), 15, TOKEN_MEDICO));

        assertEquals("La hora de inicio debe ser anterior a la hora de fin", e.getMessage());
    }

    @Test
    @DisplayName("Un rango menor que la duración del hueco lanza excepción")
    void configurarAgendaConRangoInsuficienteLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 10), 20, TOKEN_MEDICO));

        assertEquals("El rango horario es insuficiente para generar al menos un hueco de 20 minutos", e.getMessage());
    }

    @Test
    @DisplayName("Un rango exactamente igual a la duración del hueco genera un solo tramo")
    void configurarAgendaConRangoIgualALaDuracionGeneraUnTramo() {
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(9, 20), 20, TOKEN_MEDICO);

        assertEquals(1, agenda.getTramos().size());
    }

    @ParameterizedTest(name = "el médico {0} ya tiene agenda ese día")
    @ValueSource(strings = {"MED-01", "med-01"})
    @DisplayName("No se admite una segunda agenda abierta del mismo médico y fecha (sin distinguir mayúsculas)")
    void configurarAgendaDuplicadaLanzaExcepcion(String medicoDuplicado) {
        servicio.configurarAgendaDiaria("MED-01", FECHA, LocalTime.of(9, 0), LocalTime.of(11, 0), 30, TOKEN_ADMISION);

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.configurarAgendaDiaria(medicoDuplicado, FECHA, LocalTime.of(12, 0), LocalTime.of(14, 0), 30, TOKEN_ADMISION));

        assertEquals("El médico " + medicoDuplicado + " ya tiene una agenda abierta para la fecha " + FECHA, e.getMessage());
    }

    @Test
    @DisplayName("Tras cerrar una agenda se puede configurar otra del mismo médico y fecha")
    void configurarAgendaTrasCerrarLaAnteriorEsValido() {
        AgendaDiariaDTO primera = agendaDe9a10CadaVeinte("MED-01");
        servicio.cerrarAgendaDiaria(primera.getAgendaId(), TOKEN_ADMISION);

        assertDoesNotThrow(() -> agendaDe9a10CadaVeinte("MED-01"));
    }

    // ---------------------------------------------------------------- consultarAgendaMedico

    @Test
    @DisplayName("Consultar una agenda existente la devuelve, sin distinguir mayúsculas del médico")
    void consultarAgendaExistenteLaDevuelve() {
        agendaDe9a10CadaVeinte("MED-02");

        AgendaDiariaDTO encontrada = servicio.consultarAgendaMedico("med-02", FECHA, TOKEN_ADMISION);

        assertEquals("MED-02", encontrada.getMedicoId());
    }

    @ParameterizedTest(name = "token [{0}] no está informado")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar sin token de sesión lanza SecurityException")
    void consultarAgendaSinTokenLanzaSecurityException(String token) {
        SecurityException e = assertThrows(SecurityException.class,
                () -> servicio.consultarAgendaMedico("MED-02", FECHA, token));

        assertEquals("Token de sesión obligatorio para consultar agendas", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con médico nulo, vacío o en blanco lanza excepción")
    void consultarAgendaConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarAgendaMedico(medico, FECHA, TOKEN_ADMISION));

        assertEquals("El médico y la fecha son obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Consultar sin fecha lanza excepción")
    void consultarAgendaSinFechaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarAgendaMedico("MED-02", null, TOKEN_ADMISION));

        assertEquals("El médico y la fecha son obligatorios", e.getMessage());
    }

    @Test
    @DisplayName("Consultar la agenda de un médico sin agenda lanza excepción")
    void consultarAgendaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarAgendaMedico("MED-99", FECHA, TOKEN_ADMISION));

        assertEquals("No existe agenda configurada para el médico MED-99 en fecha " + FECHA, e.getMessage());
    }

    // ---------------------------------------------------------------- bloquearTramoAgenda

    @Test
    @DisplayName("Bloquear un tramo lo marca no disponible con su motivo")
    void bloquearTramoLoMarcaNoDisponibleConSuMotivo() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        boolean bloqueado = servicio.bloquearTramoAgenda(
                agenda.getAgendaId(), LocalTime.of(9, 20), LocalTime.of(9, 40), "  Sesión clínica ", TOKEN_MEDICO);

        assertAll(
                () -> assertTrue(bloqueado),
                () -> assertTrue(agenda.getTramos().get(0).isDisponible()),
                () -> assertFalse(agenda.getTramos().get(1).isDisponible()),
                () -> assertEquals("Sesión clínica", agenda.getTramos().get(1).getMotivoBloqueo()),
                () -> assertTrue(agenda.getTramos().get(2).isDisponible()));
    }

    @Test
    @DisplayName("Un bloqueo que solapa parcialmente bloquea todos los tramos afectados")
    void bloquearTramoConSolapeParcialBloqueaLosTramosAfectados() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.of(9, 10), LocalTime.of(9, 30), "Reunión", TOKEN_MEDICO);

        assertAll(
                () -> assertFalse(agenda.getTramos().get(0).isDisponible()),
                () -> assertFalse(agenda.getTramos().get(1).isDisponible()),
                () -> assertTrue(agenda.getTramos().get(2).isDisponible()));
    }

    @Test
    @DisplayName("Bloquear un tramo ya bloqueado devuelve false")
    void bloquearTramoYaBloqueadoDevuelveFalse() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");
        servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.of(9, 20), LocalTime.of(9, 40), "Sesión clínica", TOKEN_MEDICO);

        boolean segundo = servicio.bloquearTramoAgenda(
                agenda.getAgendaId(), LocalTime.of(9, 20), LocalTime.of(9, 40), "Reunión", TOKEN_MEDICO);

        assertFalse(segundo);
    }

    @ParameterizedTest(name = "agenda [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Bloquear con identificador de agenda nulo, vacío o en blanco lanza excepción")
    void bloquearTramoConAgendaInvalidaLanzaExcepcion(String agendaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.bloquearTramoAgenda(agendaId, LocalTime.of(9, 0), LocalTime.of(9, 20), "Motivo", TOKEN_MEDICO));

        assertEquals("El ID de la agenda es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "rango {0} - {1} no es válido")
    @CsvSource({
            "09:20, 09:20",
            "09:40, 09:20"
    })
    @DisplayName("Bloquear con un rango de inicio igual o posterior al fin lanza excepción")
    void bloquearTramoConRangoInvalidoLanzaExcepcion(String inicio, String fin) {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.parse(inicio), LocalTime.parse(fin), "Motivo", TOKEN_MEDICO));

        assertEquals("El rango horario a bloquear no es válido", e.getMessage());
    }

    @Test
    @DisplayName("Bloquear sin hora de inicio o de fin lanza excepción")
    void bloquearTramoConHorasNulasLanzaExcepcion() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.bloquearTramoAgenda(
                        agenda.getAgendaId(), null, LocalTime.of(9, 20), "Motivo", TOKEN_MEDICO)),
                () -> assertThrows(IllegalArgumentException.class, () -> servicio.bloquearTramoAgenda(
                        agenda.getAgendaId(), LocalTime.of(9, 0), null, "Motivo", TOKEN_MEDICO)));
    }

    @ParameterizedTest(name = "motivo [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Bloquear con motivo nulo, vacío o en blanco lanza excepción")
    void bloquearTramoConMotivoInvalidoLanzaExcepcion(String motivo) {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.of(9, 0), LocalTime.of(9, 20), motivo, TOKEN_MEDICO));

        assertEquals("Debe indicarse un motivo para bloquear el tramo", e.getMessage());
    }

    @Test
    @DisplayName("Bloquear en una agenda inexistente lanza excepción")
    void bloquearTramoEnAgendaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.bloquearTramoAgenda("AGD-NO-EXISTE", LocalTime.of(9, 0), LocalTime.of(9, 20), "Motivo", TOKEN_MEDICO));

        assertEquals("No existe ninguna agenda con ID: AGD-NO-EXISTE", e.getMessage());
    }

    @Test
    @DisplayName("Bloquear en una agenda cerrada lanza excepción")
    void bloquearTramoEnAgendaCerradaLanzaExcepcion() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");
        servicio.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMISION);

        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.of(9, 0), LocalTime.of(9, 20), "Urgencia", TOKEN_ADMISION));

        assertEquals("No se pueden bloquear tramos en una agenda cerrada", e.getMessage());
    }

    // ---------------------------------------------------------------- cerrarAgendaDiaria

    @Test
    @DisplayName("Cerrar una agenda abierta la cierra y devuelve true")
    void cerrarAgendaAbiertaLaCierra() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");

        boolean cerrada = servicio.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMISION);

        assertAll(
                () -> assertTrue(cerrada),
                () -> assertFalse(agenda.isAbierta()));
    }

    @Test
    @DisplayName("Cerrar una agenda ya cerrada devuelve false")
    void cerrarAgendaYaCerradaDevuelveFalse() {
        AgendaDiariaDTO agenda = agendaDe9a10CadaVeinte("MED-03");
        servicio.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMISION);

        assertFalse(servicio.cerrarAgendaDiaria(agenda.getAgendaId(), TOKEN_ADMISION));
    }

    @ParameterizedTest(name = "agenda [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Cerrar con identificador de agenda nulo, vacío o en blanco lanza excepción")
    void cerrarAgendaConIdInvalidoLanzaExcepcion(String agendaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.cerrarAgendaDiaria(agendaId, TOKEN_ADMISION));

        assertEquals("El ID de la agenda es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Cerrar una agenda inexistente lanza excepción")
    void cerrarAgendaInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.cerrarAgendaDiaria("AGD-NO-EXISTE", TOKEN_ADMISION));

        assertEquals("No existe ninguna agenda con ID: AGD-NO-EXISTE", e.getMessage());
    }
}
