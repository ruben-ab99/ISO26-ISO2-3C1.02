package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.listasespera.PrioridadEspera;
import es.uclm.sescam.contratos.listasespera.SolicitudEsperaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

class GestorListasEsperaServiceImplTest {

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

    @Test
    void constructorNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new GestorListasEsperaServiceImpl(null, stubAgendas));
        assertThrows(IllegalArgumentException.class, () -> new GestorListasEsperaServiceImpl(stubRoles, null));
    }

    @Test
    void registrarYOrdenarPorPrioridadCorrectamente() {
        SolicitudEsperaDTO s1 = servicio.registrarEnListaEspera("PAC-01", "TAC", PrioridadEspera.ORDINARIA, "token-ADMISION");
        SolicitudEsperaDTO s2 = servicio.registrarEnListaEspera("PAC-02", "TAC", PrioridadEspera.URGENTE, "token-ADMISION");
        SolicitudEsperaDTO s3 = servicio.registrarEnListaEspera("PAC-03", "TAC", PrioridadEspera.PREFERENTE, "token-ADMINISTRADOR");
        servicio.registrarEnListaEspera("PAC-04", "RMN", PrioridadEspera.URGENTE, "token-ADMISION");

        assertTrue(s1.getSolicitudId().startsWith("ESP-"));
        assertFalse(s1.isAtendida());

        List<SolicitudEsperaDTO> colaTac = servicio.consultarListaEspera("TAC", "token-ADMISION");
        assertEquals(3, colaTac.size());
        assertEquals("PAC-02", colaTac.get(0).getPacienteId()); // URGENTE primero
        assertEquals("PAC-03", colaTac.get(1).getPacienteId()); // PREFERENTE segundo
        assertEquals("PAC-01", colaTac.get(2).getPacienteId()); // ORDINARIA tercero

        // Consulta global sin filtro de prueba
        assertEquals(4, servicio.consultarListaEspera(null, "token-ADMISION").size());
    }

    @Test
    void validacionesPermisosYDuplicados() {
        assertThrows(SecurityException.class, () ->
                servicio.registrarEnListaEspera("PAC-01", "ECO", PrioridadEspera.ORDINARIA, "token-MEDICO"));
        assertThrows(SecurityException.class, () ->
                servicio.consultarListaEspera("ECO", ""));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarEnListaEspera("", "ECO", PrioridadEspera.ORDINARIA, "token-ADMISION"));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarEnListaEspera("PAC-01", null, PrioridadEspera.ORDINARIA, "token-ADMISION"));
        assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarEnListaEspera("PAC-01", "ECO", null, "token-ADMISION"));

        servicio.registrarEnListaEspera("PAC-01", "ECO", PrioridadEspera.ORDINARIA, "token-ADMISION");
        assertThrows(IllegalStateException.class, () ->
                servicio.registrarEnListaEspera("PAC-01", "ECO", PrioridadEspera.URGENTE, "token-ADMISION"));
    }

    @Test
    void actualizarPrioridadFuncionaYValidaEstados() {
        SolicitudEsperaDTO sol = servicio.registrarEnListaEspera("PAC-10", "RX", PrioridadEspera.ORDINARIA, "token-ADMISION");

        assertTrue(servicio.actualizarPrioridad(sol.getSolicitudId(), PrioridadEspera.URGENTE, "token-ADMISION"));
        assertEquals(PrioridadEspera.URGENTE, sol.getPrioridad());
        assertFalse(servicio.actualizarPrioridad(sol.getSolicitudId(), PrioridadEspera.URGENTE, "token-ADMISION"));

        assertThrows(IllegalArgumentException.class, () ->
                servicio.actualizarPrioridad("", PrioridadEspera.PREFERENTE, "token-ADMISION"));
        assertThrows(IllegalStateException.class, () ->
                servicio.actualizarPrioridad("ESP-NOEXISTE", PrioridadEspera.PREFERENTE, "token-ADMISION"));
    }

    @Test
    void asignarHuecoLibreYContarHuecos() {
        LocalDate fecha = LocalDate.of(2026, 11, 25);
        SolicitudEsperaDTO sol = servicio.registrarEnListaEspera("PAC-20", "RMN", PrioridadEspera.PREFERENTE, "token-ADMISION");

        // Conteo con tramos nulos y con tramos mixtos (2 libres, 1 ocupado)
        assertEquals(0, servicio.contarHuecosLibres("MED-01", fecha, "token-ADMISION"));
        tramosStub = List.of(
                new TramoHorarioDTO(LocalTime.of(9, 0), LocalTime.of(9, 20), true, null),
                new TramoHorarioDTO(LocalTime.of(9, 20), LocalTime.of(9, 40), false, "Ocupado"),
                new TramoHorarioDTO(LocalTime.of(9, 40), LocalTime.of(10, 0), true, null)
        );
        assertEquals(2, servicio.contarHuecosLibres("MED-01", fecha, "token-ADMISION"));

        // Validaciones de parámetros y estado de agenda
        assertThrows(IllegalArgumentException.class, () ->
                servicio.asignarHuecoLibre("", "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 20), "token-ADMISION"));
        assertThrows(IllegalStateException.class, () ->
                servicio.asignarHuecoLibre("ESP-FALSO", "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 20), "token-ADMISION"));

        bloquearExitosoStub = false;
        assertThrows(IllegalStateException.class, () ->
                servicio.asignarHuecoLibre(sol.getSolicitudId(), "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 20), "token-ADMISION"));

        agendaAbiertaStub = false;
        assertEquals(0, servicio.contarHuecosLibres("MED-01", fecha, "token-ADMISION"));
        assertThrows(IllegalStateException.class, () ->
                servicio.asignarHuecoLibre(sol.getSolicitudId(), "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 20), "token-ADMISION"));

        // Asignación exitosa
        agendaAbiertaStub = true;
        bloquearExitosoStub = true;
        SolicitudEsperaDTO asignada = servicio.asignarHuecoLibre(
                sol.getSolicitudId(), "MED-01", fecha, LocalTime.of(9, 0), LocalTime.of(9, 20), "token-ADMISION"
        );

        assertTrue(asignada.isAtendida());
        assertEquals("AGD-TEST", asignada.getAgendaAsignadaId());
        assertEquals(fecha, asignada.getFechaAsignada());
        assertEquals(LocalTime.of(9, 0), asignada.getHoraAsignada());
        assertTrue(servicio.consultarListaEspera("RMN", "token-ADMISION").isEmpty());

        // Ya atendida no se puede reasignar ni cambiar prioridad
        assertThrows(IllegalStateException.class, () ->
                servicio.asignarHuecoLibre(sol.getSolicitudId(), "MED-01", fecha, LocalTime.of(9, 20), LocalTime.of(9, 40), "token-ADMISION"));
        assertFalse(servicio.actualizarPrioridad(sol.getSolicitudId(), PrioridadEspera.ORDINARIA, "token-ADMISION"));
    }
}