package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-10 (Registrar realización de prueba e incidencias).
 * Técnicas: clases de equivalencia sobre los cuatro datos obligatorios (válido / nulo / en blanco),
 * criterio each use entre parámetros y conjetura de errores (incidencias en blanco,
 * aviso a RIS y HCE solo cuando hay incidencias).
 */
class RegistroPruebaServiceImplTest {

    private RegistroPruebaServiceImpl servicio;
    private boolean risNotificado;
    private boolean incidenciaRegistradaHce;

    /** Stub del conector externo: solo recuerda qué avisos ha recibido. */
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
    @DisplayName("El constructor rechaza un conector RIS/HCE nulo")
    void constructorConConectorNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new RegistroPruebaServiceImpl(null));

        assertEquals("El conector RIS/HCE no puede ser nulo", e.getMessage());
    }

    @Test
    @DisplayName("Registrar una realización sin incidencias avisa a RIS y no a HCE")
    void registrarRealizacionSinIncidenciasNotificaRisYNoHce() {
        RegistroPruebaDTO res = servicio.registrarRealizacionPrueba(
                "CITA-10", "PAC-01", "MED-02", "RM Craneal", "Prueba completada normal", null);

        assertAll(
                () -> assertNotNull(res),
                () -> assertTrue(res.getRegistroId().startsWith("REG-")),
                () -> assertTrue(res.isRealizada()),
                () -> assertEquals("CITA-10", res.getCitaId()),
                () -> assertEquals("", res.getIncidencias()),
                () -> assertNotNull(res.getFechaRealizacion()),
                () -> assertTrue(risNotificado),
                () -> assertFalse(incidenciaRegistradaHce));
    }

    @Test
    @DisplayName("Registrar una realización con incidencias avisa a RIS y a HCE")
    void registrarRealizacionConIncidenciasNotificaRisYHce() {
        RegistroPruebaDTO res = servicio.registrarRealizacionPrueba(
                "CITA-11", "PAC-02", "MED-02", "TAC Torácico", "Con contraste", "Reacción leve al contraste");

        assertAll(
                () -> assertEquals("Reacción leve al contraste", res.getIncidencias()),
                () -> assertTrue(risNotificado),
                () -> assertTrue(incidenciaRegistradaHce));
    }

    @Test
    @DisplayName("Unas incidencias en blanco no se avisan a HCE")
    void registrarRealizacionConIncidenciasEnBlancoNoAvisaAHce() {
        servicio.registrarRealizacionPrueba("CITA-13", "PAC-02", "MED-02", "RX", "OK", "   ");

        assertAll(
                () -> assertTrue(risNotificado),
                () -> assertFalse(incidenciaRegistradaHce));
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con cita nula, vacía o en blanco lanza excepción")
    void registrarRealizacionConCitaInvalidaLanzaExcepcion(String citaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarRealizacionPrueba(citaId, "PAC-1", "MED-1", "RX", "", ""));

        assertEquals("El ID de la cita es obligatorio", e.getMessage());
        assertFalse(risNotificado);
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con paciente nulo, vacío o en blanco lanza excepción")
    void registrarRealizacionConPacienteInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarRealizacionPrueba("CITA-1", pacienteId, "MED-1", "RX", "", ""));

        assertEquals("El ID del paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con médico nulo, vacío o en blanco lanza excepción")
    void registrarRealizacionConMedicoInvalidoLanzaExcepcion(String medicoId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", medicoId, "RX", "", ""));

        assertEquals("El ID del médico es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "tipo de prueba [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con tipo de prueba nulo, vacío o en blanco lanza excepción")
    void registrarRealizacionConTipoPruebaInvalidoLanzaExcepcion(String tipoPrueba) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarRealizacionPrueba("CITA-1", "PAC-1", "MED-1", tipoPrueba, "", ""));

        assertEquals("El tipo de prueba es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar un registro existente devuelve el mismo registro")
    void consultarRegistroExistenteLoDevuelve() {
        RegistroPruebaDTO creado = servicio.registrarRealizacionPrueba(
                "CITA-12", "PAC-03", "MED-01", "Ecografía", "OK", "");

        RegistroPruebaDTO recuperado = servicio.consultarRegistroPrueba(creado.getRegistroId());

        assertEquals(creado.getRegistroId(), recuperado.getRegistroId());
    }

    @ParameterizedTest(name = "registro [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Consultar con identificador nulo, vacío o en blanco lanza excepción")
    void consultarRegistroConIdInvalidoLanzaExcepcion(String registroId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarRegistroPrueba(registroId));

        assertEquals("El ID del registro es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Consultar un registro inexistente lanza excepción")
    void consultarRegistroInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarRegistroPrueba("REG-INEXISTENTE"));

        assertEquals("No existe ningún registro de prueba con ID: REG-INEXISTENTE", e.getMessage());
    }

    @Test
    @DisplayName("Listar los registros de un paciente devuelve solo los suyos")
    void listarRegistrosPacienteDevuelveSoloLosSuyos() {
        servicio.registrarRealizacionPrueba("CITA-1", "PAC-01", "MED-01", "RX", "", "");
        servicio.registrarRealizacionPrueba("CITA-2", "PAC-01", "MED-01", "RM", "", "");
        servicio.registrarRealizacionPrueba("CITA-3", "PAC-02", "MED-01", "RX", "", "");

        List<RegistroPruebaDTO> registros = servicio.listarRegistrosPaciente("  PAC-01 ");

        assertEquals(2, registros.size());
        registros.forEach(r -> assertEquals("PAC-01", r.getPacienteId()));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarRegistrosPacienteConIdInvalidoLanzaExcepcion(String pacienteId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarRegistrosPaciente(pacienteId));

        assertEquals("El ID del paciente es obligatorio", e.getMessage());
    }
}
