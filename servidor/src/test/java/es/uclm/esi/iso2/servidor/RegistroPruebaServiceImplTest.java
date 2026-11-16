package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistroPruebaServiceImplTest {

    private RegistroPruebaServiceImpl servicio;
    private boolean risNotificado;
    private boolean incidenciaRegistradaHce;

    private class StubNotificadorRisHce implements INotificadorRisHce {
        @Override
        public void notificarRealizacionRis(String registroId, String citaId, String tipoPrueba) {
            risNotificado = true;
        }

        @Override
        public void registrarIncidenciaHce(String registroId, String pacienteId, String incidencias) {
            incidenciaRegistradaHce = true;
        }
    }

    @BeforeEach
    void setUp() {
        risNotificado = false;
        incidenciaRegistradaHce = false;
        servicio = new RegistroPruebaServiceImpl(new StubNotificadorRisHce());
    }

    @Test
    void constructorConConectorNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new RegistroPruebaServiceImpl(null));
    }

    @Test
    void registrarRealizacionSinIncidenciasNotificaRisYNoHce() {
        RegistroPruebaDTO res = servicio.registrarRealizacionPrueba(
                "CITA-10", "PAC-01", "MED-02", "RM Craneal", "Prueba completada normal", null
        );

        assertNotNull(res);
        assertTrue(res.getRegistroId().startsWith("REG-"));
        assertTrue(res.isRealizada());
        assertEquals("CITA-10", res.getCitaId());
        assertEquals("", res.getIncidencias());
        assertNotNull(res.getFechaRealizacion());
        assertTrue(risNotificado);
        assertFalse(incidenciaRegistradaHce);
    }

    @Test
    void registrarRealizacionConIncidenciasNotificaRisYHce() {
        RegistroPruebaDTO res = servicio.registrarRealizacionPrueba(
                "CITA-11", "PAC-02", "MED-02", "TAC Torácico", "Con contraste", "Reacción leve al contraste"
        );

        assertEquals("Reacción leve al contraste", res.getIncidencias());
        assertTrue(risNotificado);
        assertTrue(incidenciaRegistradaHce);
    }

    @Test
    void registrarRealizacionConParametrosObligatoriosInvalidosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba(null, "PAC-1", "MED-1", "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("   ", "PAC-1", "MED-1", "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", null, "MED-1", "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", "", "MED-1", "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", null, "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", " ", "RX", "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", "MED-1", null, "", ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", "MED-1", "  ", "", ""));
    }

    @Test
    void consultarRegistroExistenteEInexistente() {
        RegistroPruebaDTO creado = servicio.registrarRealizacionPrueba("CITA-12", "PAC-03", "MED-01", "Ecografía", "OK", "");
        RegistroPruebaDTO recuperado = servicio.consultarRegistroPrueba(creado.getRegistroId());

        assertEquals(creado.getRegistroId(), recuperado.getRegistroId());
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarRegistroPrueba(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarRegistroPrueba("   "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarRegistroPrueba("REG-INEXISTENTE"));
    }
}