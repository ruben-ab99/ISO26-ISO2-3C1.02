package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.informes.InformeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InformesImagenesServiceImplTest {

    private InformesImagenesServiceImpl servicio;
    private boolean hceSincronizada;

    // Stub del sistema externo RIS/PACS/HCE (CU-27)
    private class StubSistemaExternoRisPacs implements ISistemaExternoRisPacs {
        @Override
        public List<EstudioImagenDTO> obtenerEstudiosDesdePacs(String pacienteId) {
            if ("PAC-SIN-ESTUDIOS".equals(pacienteId)) {
                return List.of();
            }
            return List.of(
                    new EstudioImagenDTO("EST-01", pacienteId, "TAC", LocalDate.of(2026, 11, 10), "pacs://estudio/01")
            );
        }

        @Override
        public void sincronizarInformeConHce(String informeId, String pacienteId, String contenidoClinico) {
            hceSincronizada = true;
        }
    }

    @BeforeEach
    void setUp() {
        hceSincronizada = false;
        servicio = new InformesImagenesServiceImpl(new StubSistemaExternoRisPacs());
    }

    // --- 1. Constructor (Conjetura de errores) ---
    @Test
    void constructorConConectorNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new InformesImagenesServiceImpl(null));
    }

    // --- 2. crearInforme (Clases de equivalencia y valores límite) ---
    @Test
    void crearInformeValidoSincronizaHceYAsociaEstudios() {
        InformeDTO resultado = servicio.crearInforme("SOL-100", "PAC-01", "MED-05", "Sin hallazgos patológicos.");

        assertNotNull(resultado);
        assertTrue(resultado.getId().startsWith("INF-"));
        assertEquals("SOL-100", resultado.getSolicitudId());
        assertEquals("PAC-01", resultado.getPacienteId());
        assertEquals("MED-05", resultado.getMedicoId());
        assertEquals("Sin hallazgos patológicos.", resultado.getContenidoClinico());
        assertNotNull(resultado.getFechaCreacion());
        assertEquals(1, resultado.getEstudiosAsociados().size());
        assertTrue(hceSincronizada);
    }

    @Test
    void crearInformeConDatosInvalidosONulosLanzaExcepcion() {
        // Solicitud nula o vacía
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme(null, "PAC-01", "MED-05", "Texto"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("   ", "PAC-01", "MED-05", "Texto"));
        // Paciente nulo o vacío
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", null, "MED-05", "Texto"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", "", "MED-05", "Texto"));
        // Médico nulo o vacío
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", "PAC-01", null, "Texto"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", "PAC-01", " ", "Texto"));
        // Contenido clínico nulo o vacío
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", "PAC-01", "MED-05", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearInforme("SOL-1", "PAC-01", "MED-05", "   "));
    }

    // --- 3. consultarInforme (Equivalencia válida, no existente e inválidos) ---
    @Test
    void consultarInformeExistenteDevuelveElInformeCorrecto() {
        InformeDTO creado = servicio.crearInforme("SOL-101", "PAC-02", "MED-05", "Fractura limpia.");
        InformeDTO recuperado = servicio.consultarInforme(creado.getId(), "MED-05");

        assertEquals(creado.getId(), recuperado.getId());
        assertEquals("Fractura limpia.", recuperado.getContenidoClinico());
    }

    @Test
    void consultarInformeInexistenteLanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> servicio.consultarInforme("INF-NO-EXISTE", "MED-05"));
    }

    @Test
    void consultarInformeConParametrosNulosOVaciosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarInforme(null, "MED-05"));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarInforme(" ", "MED-05"));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarInforme("INF-01", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarInforme("INF-01", ""));
    }

    // --- 4. listarEstudiosPaciente (Con estudios, sin estudios e inválido) ---
    @Test
    void listarEstudiosPacienteDevuelveListaDelPacs() {
        List<EstudioImagenDTO> estudios = servicio.listarEstudiosPaciente("PAC-01");
        assertEquals(1, estudios.size());
        assertEquals("TAC", estudios.get(0).getModalidad());

        List<EstudioImagenDTO> vacio = servicio.listarEstudiosPaciente("PAC-SIN-ESTUDIOS");
        assertTrue(vacio.isEmpty());
    }

    @Test
    void listarEstudiosPacienteInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.listarEstudiosPaciente(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarEstudiosPaciente("   "));
    }
}