package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.informes.InformeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-09 (Crear y consultar informes e imágenes).
 * Técnicas: clases de equivalencia sobre cada dato obligatorio (válido / nulo / vacío / en blanco),
 * paciente con y sin estudios en el PACS, criterio each use entre parámetros y conjetura de
 * errores (informe inexistente). El sistema externo RIS/PACS/HCE se sustituye por un stub (CU-27).
 */
class InformesImagenesServiceImplTest {

    private InformesImagenesServiceImpl servicio;
    private boolean hceSincronizada;

    /** Stub del sistema externo RIS/PACS/HCE: un estudio por paciente, salvo PAC-SIN-ESTUDIOS. */
    private class StubSistemaExternoRisPacs implements ISistemaExternoRisPacs {
        @Override
        public List<EstudioImagenDTO> obtenerEstudiosDesdePacs(String pacienteId) {
            if ("PAC-SIN-ESTUDIOS".equals(pacienteId)) {
                return List.of();
            }
            return List.of(new EstudioImagenDTO("EST-01", pacienteId, "TAC", LocalDate.of(2026, 11, 10), "pacs://estudio/01"));
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

    // ---------------------------------------------------------------- constructor

    @Test
    @DisplayName("El constructor rechaza un conector RIS/PACS/HCE nulo")
    void constructorConConectorNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new InformesImagenesServiceImpl(null));

        assertEquals("El conector RIS/PACS/HCE no puede ser nulo", e.getMessage());
    }

    // ---------------------------------------------------------------- crearInforme

    @Test
    @DisplayName("Crear un informe válido sincroniza con HCE y asocia los estudios del PACS")
    void crearInformeValidoSincronizaHceYAsociaEstudios() {
        InformeDTO resultado = servicio.crearInforme("SOL-100", "PAC-01", "MED-05", "Sin hallazgos patológicos.");

        assertAll(
                () -> assertNotNull(resultado),
                () -> assertTrue(resultado.getId().startsWith("INF-")),
                () -> assertEquals("SOL-100", resultado.getSolicitudId()),
                () -> assertEquals("PAC-01", resultado.getPacienteId()),
                () -> assertEquals("MED-05", resultado.getMedicoId()),
                () -> assertEquals("Sin hallazgos patológicos.", resultado.getContenidoClinico()),
                () -> assertNotNull(resultado.getFechaCreacion()),
                () -> assertEquals(1, resultado.getEstudiosAsociados().size()),
                () -> assertTrue(hceSincronizada));
    }

    @Test
    @DisplayName("Un paciente sin estudios en el PACS recibe un informe sin estudios asociados")
    void crearInformeDePacienteSinEstudiosNoAsociaNinguno() {
        InformeDTO resultado = servicio.crearInforme("SOL-101", "PAC-SIN-ESTUDIOS", "MED-05", "Pendiente de imagen.");

        assertTrue(resultado.getEstudiosAsociados().isEmpty());
    }

    @ParameterizedTest(name = "solicitud [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con solicitud nula, vacía o en blanco lanza excepción")
    void crearInformeConSolicitudInvalidaLanzaExcepcion(String solicitud) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearInforme(solicitud, "PAC-01", "MED-05", "Texto"));

        assertEquals("El ID de solicitud es obligatorio", e.getMessage());
        assertFalse(hceSincronizada);
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con paciente nulo, vacío o en blanco lanza excepción")
    void crearInformeConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearInforme("SOL-1", paciente, "MED-05", "Texto"));

        assertEquals("El ID de paciente es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "médico [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Crear con médico nulo, vacío o en blanco lanza excepción")
    void crearInformeConMedicoInvalidoLanzaExcepcion(String medico) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearInforme("SOL-1", "PAC-01", medico, "Texto"));

        assertEquals("El ID del médico es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "contenido [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Crear con contenido clínico nulo, vacío o en blanco lanza excepción")
    void crearInformeConContenidoInvalidoLanzaExcepcion(String contenido) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearInforme("SOL-1", "PAC-01", "MED-05", contenido));

        assertEquals("El contenido clínico del informe no puede estar vacío", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarInforme

    @Test
    @DisplayName("Consultar un informe existente devuelve el informe y su contenido")
    void consultarInformeExistenteDevuelveElInformeCorrecto() {
        InformeDTO creado = servicio.crearInforme("SOL-101", "PAC-02", "MED-05", "Fractura limpia.");

        InformeDTO recuperado = servicio.consultarInforme(creado.getId(), "MED-05");

        assertAll(
                () -> assertEquals(creado.getId(), recuperado.getId()),
                () -> assertEquals("Fractura limpia.", recuperado.getContenidoClinico()));
    }

    @Test
    @DisplayName("Consultar un informe inexistente lanza excepción")
    void consultarInformeInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarInforme("INF-NO-EXISTE", "MED-05"));

        assertEquals("No existe ningún informe con ID: INF-NO-EXISTE", e.getMessage());
    }

    @ParameterizedTest(name = "informe [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con identificador de informe nulo, vacío o en blanco lanza excepción")
    void consultarInformeConIdInvalidoLanzaExcepcion(String informeId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarInforme(informeId, "MED-05"));

        assertEquals("El ID de informe es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "usuario [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Consultar con usuario nulo, vacío o en blanco lanza excepción")
    void consultarInformeConUsuarioInvalidoLanzaExcepcion(String usuario) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarInforme("INF-01", usuario));

        assertEquals("El usuario que consulta debe estar identificado", e.getMessage());
    }

    // ---------------------------------------------------------------- listarEstudiosPaciente

    @Test
    @DisplayName("Listar los estudios de un paciente devuelve los del PACS")
    void listarEstudiosPacienteDevuelveListaDelPacs() {
        List<EstudioImagenDTO> estudios = servicio.listarEstudiosPaciente("PAC-01");

        assertAll(
                () -> assertEquals(1, estudios.size()),
                () -> assertEquals("TAC", estudios.get(0).getModalidad()));
    }

    @Test
    @DisplayName("Listar los estudios de un paciente sin estudios devuelve una lista vacía")
    void listarEstudiosPacienteSinEstudiosDevuelveListaVacia() {
        assertTrue(servicio.listarEstudiosPaciente("PAC-SIN-ESTUDIOS").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar con paciente nulo, vacío o en blanco lanza excepción")
    void listarEstudiosPacienteConIdInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarEstudiosPaciente(paciente));

        assertEquals("El ID de paciente es obligatorio", e.getMessage());
    }
}
