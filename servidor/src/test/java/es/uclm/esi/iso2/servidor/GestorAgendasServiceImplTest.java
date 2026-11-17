package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class GestorAgendasServiceImplTest {

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

    @Test
    void constructorNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new GestorAgendasServiceImpl(null));
    }

    @Test
    void configurarAgendaDiariaValidaGeneraTramosCorrectamente() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, "token-ADMISION"
        );

        assertNotNull(agenda);
        assertTrue(agenda.getAgendaId().startsWith("AGD-"));
        assertTrue(agenda.isAbierta());
        assertEquals(3, agenda.getTramos().size()); // 09:00-09:20, 09:20-09:40, 09:40-10:00
        assertTrue(agenda.getTramos().get(0).isDisponible());
    }

    @Test
    void configurarAgendaSinPermisosODatosInvalidosLanzaExcepcion() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);

        assertThrows(SecurityException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 15, "token-INVALIDO"));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("", fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 15, "token-MEDICO"));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(11, 0), LocalTime.of(10, 0), 15, "token-MEDICO"));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 2, "token-MEDICO"));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 10), 20, "token-MEDICO"));
    }

    @Test
    void configurarAgendaDuplicadaParaMismoMedicoYFechaLanzaExcepcion() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(11, 0), 30, "token-ADMISION");

        assertThrows(IllegalStateException.class, () ->
                servicio.configurarAgendaDiaria("MED-01", fecha, LocalTime.of(12, 0), LocalTime.of(14, 0), 30, "token-ADMISION"));
    }

    @Test
    void consultarAgendaExistenteEInexistente() {
        LocalDate fecha = LocalDate.of(2026, 11, 21);
        servicio.configurarAgendaDiaria("MED-02", fecha, LocalTime.of(8, 0), LocalTime.of(9, 0), 30, "token-MEDICO");

        AgendaDiariaDTO encontrada = servicio.consultarAgendaMedico("MED-02", fecha, "token-ADMISION");
        assertEquals("MED-02", encontrada.getMedicoId());

        assertThrows(SecurityException.class, () -> servicio.consultarAgendaMedico("MED-02", fecha, ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarAgendaMedico(null, fecha, "token-ADMISION"));
        assertThrows(IllegalStateException.class, () -> servicio.consultarAgendaMedico("MED-99", fecha, "token-ADMISION"));
    }

    @Test
    void bloquearTramoAgendaYCerrarAgenda() {
        LocalDate fecha = LocalDate.of(2026, 11, 22);
        AgendaDiariaDTO agenda = servicio.configurarAgendaDiaria(
                "MED-03", fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), 20, "token-MEDICO"
        );

        // Bloquea el segundo tramo (09:20 - 09:40)
        boolean bloqueado = servicio.bloquearTramoAgenda(
                agenda.getAgendaId(), LocalTime.of(9, 20), LocalTime.of(9, 40), "Sesión clínica", "token-MEDICO"
        );
        assertTrue(bloqueado);
        assertFalse(agenda.getTramos().get(1).isDisponible());
        assertEquals("Sesión clínica", agenda.getTramos().get(1).getMotivoBloqueo());

        // Volver a bloquear lo ya bloqueado devuelve false
        assertFalse(servicio.bloquearTramoAgenda(
                agenda.getAgendaId(), LocalTime.of(9, 20), LocalTime.of(9, 40), "Reunión", "token-MEDICO"
        ));

        // Cerrar agenda
        assertTrue(servicio.cerrarAgendaDiaria(agenda.getAgendaId(), "token-ADMISION"));
        assertFalse(agenda.isAbierta());
        assertFalse(servicio.cerrarAgendaDiaria(agenda.getAgendaId(), "token-ADMISION")); // Ya cerrada

        // No deja bloquear en agenda cerrada
        assertThrows(IllegalStateException.class, () ->
                servicio.bloquearTramoAgenda(agenda.getAgendaId(), LocalTime.of(9, 0), LocalTime.of(9, 20), "Urgencia", "token-ADMISION"));
    }
}