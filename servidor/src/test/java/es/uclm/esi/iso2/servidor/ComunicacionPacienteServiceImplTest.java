package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.portal.EncuestaSatisfaccionDTO;
import es.uclm.sescam.contratos.portal.IncidenciaPacienteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-04 (Comunicar incidencias y encuestas de satisfacción).
 * Técnicas: clases de equivalencia (categoría permitida / no permitida, datos nulos o en blanco),
 * valores límite de las puntuaciones (0 y 6 no válidos; 1 y 5 válidos), criterio each use
 * entre parámetros y conjetura de errores (encuesta duplicada con otra capitalización).
 */
class ComunicacionPacienteServiceImplTest {

    private static final String MSG_PACIENTE = "El identificador del paciente es obligatorio";

    private AuditServiceImpl auditService;
    private ComunicacionPacienteServiceImpl servicio;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl();
        servicio = new ComunicacionPacienteServiceImpl(auditService);
    }

    // ---------------------------------------------------------------- registrarIncidencia

    @Test
    @DisplayName("Registrar una incidencia válida la crea ABIERTA y la audita")
    void registrarIncidenciaValidaLaCreaAbiertaYLaAudita() {
        IncidenciaPacienteDTO incidencia = servicio.registrarIncidencia(
                "PAC-001", "CIT-101", "cita", "Retraso de 40 minutos en la sala de espera");

        assertAll(
                () -> assertTrue(incidencia.getIncidenciaId().startsWith("INC-")),
                () -> assertEquals("CITA", incidencia.getCategoria()),
                () -> assertEquals("CIT-101", incidencia.getReferenciaCitaOPruebaId()),
                () -> assertEquals("ABIERTA", incidencia.getEstado()),
                () -> assertNotNull(incidencia.getFechaRegistro()),
                () -> assertEquals(1, auditService.obtenerTrazas().size()));
    }

    @Test
    @DisplayName("Una incidencia sin referencia guarda una referencia vacía")
    void registrarIncidenciaSinReferenciaGuardaCadenaVacia() {
        IncidenciaPacienteDTO incidencia = servicio.registrarIncidencia(
                "PAC-001", null, "ATENCION", "Duda sobre preparación previa");

        assertEquals("", incidencia.getReferenciaCitaOPruebaId());
    }

    @ParameterizedTest(name = "categoría {0} se guarda como {1}")
    @CsvSource({
            "cita,     CITA",
            "PRUEBA,   PRUEBA",
            "Atencion, ATENCION",
            "otra,     OTRA"
    })
    @DisplayName("Las cuatro categorías permitidas se aceptan y se normalizan a mayúsculas")
    void registrarIncidenciaNormalizaLaCategoria(String categoria, String categoriaEsperada) {
        IncidenciaPacienteDTO incidencia = servicio.registrarIncidencia("PAC-1", "CIT-1", categoria, "Desc");

        assertEquals(categoriaEsperada, incidencia.getCategoria());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Registrar con paciente nulo, vacío o en blanco lanza excepción")
    void registrarIncidenciaConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarIncidencia(paciente, "CIT-1", "CITA", "Desc"));

        assertEquals(MSG_PACIENTE, e.getMessage());
        assertTrue(auditService.obtenerTrazas().isEmpty());
    }

    @ParameterizedTest(name = "categoría [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Registrar con categoría nula, vacía o en blanco lanza excepción")
    void registrarIncidenciaConCategoriaVaciaLanzaExcepcion(String categoria) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarIncidencia("PAC-1", "CIT-1", categoria, "Desc"));

        assertEquals("La categoría de la incidencia es obligatoria", e.getMessage());
    }

    @Test
    @DisplayName("Registrar con una categoría no permitida lanza excepción")
    void registrarIncidenciaConCategoriaNoPermitidaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarIncidencia("PAC-1", "CIT-1", "INVENTADA", "Desc"));

        assertEquals("Categoría de incidencia no válida: INVENTADA", e.getMessage());
    }

    @ParameterizedTest(name = "descripción [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Registrar con descripción nula, vacía o en blanco lanza excepción")
    void registrarIncidenciaConDescripcionInvalidaLanzaExcepcion(String descripcion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarIncidencia("PAC-1", "CIT-1", "OTRA", descripcion));

        assertEquals("La descripción de la incidencia es obligatoria", e.getMessage());
    }

    // ---------------------------------------------------------------- listarMisIncidencias

    @Test
    @DisplayName("Listar mis incidencias devuelve solo las del paciente solicitado")
    void listarMisIncidenciasDevuelveSoloLasDelPaciente() {
        servicio.registrarIncidencia("PAC-01", "CIT-1", "CITA", "Problema con horario");
        servicio.registrarIncidencia("PAC-01", "REG-2", "PRUEBA", "Molestia leve tras contraste");
        servicio.registrarIncidencia("PAC-02", "CIT-9", "OTRA", "Consulta general");

        List<IncidenciaPacienteDTO> lista = servicio.listarMisIncidencias("PAC-01");

        assertEquals(2, lista.size());
        lista.forEach(i -> assertEquals("PAC-01", i.getPacienteId()));
    }

    @Test
    @DisplayName("Listar las incidencias de un paciente sin incidencias devuelve una lista vacía")
    void listarMisIncidenciasSinIncidenciasDevuelveListaVacia() {
        assertTrue(servicio.listarMisIncidencias("PAC-99").isEmpty());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar incidencias con paciente nulo, vacío o en blanco lanza excepción")
    void listarMisIncidenciasConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarMisIncidencias(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    // ---------------------------------------------------------------- responderEncuesta

    @ParameterizedTest(name = "global {0} y atención {1} son válidas")
    @CsvSource({
            "1, 1",
            "1, 5",
            "5, 1",
            "5, 5",
            "3, 3"
    })
    @DisplayName("Las puntuaciones entre 1 y 5, incluidos los límites, se aceptan")
    void responderEncuestaConPuntuacionesValidasLaGuarda(int global, int atencion) {
        EncuestaSatisfaccionDTO encuesta = servicio.responderEncuesta("PAC-01", "CIT-200", global, atencion, "Ok");

        assertAll(
                () -> assertEquals(global, encuesta.getPuntuacionGlobal()),
                () -> assertEquals(atencion, encuesta.getPuntuacionAtencion()));
    }

    @Test
    @DisplayName("Una encuesta válida queda registrada, sin comentarios si no se indican, y se audita")
    void responderEncuestaValidaQuedaRegistradaYAuditada() {
        EncuestaSatisfaccionDTO encuesta = servicio.responderEncuesta("PAC-01", "CIT-201", 5, 1, null);

        assertAll(
                () -> assertTrue(encuesta.getEncuestaId().startsWith("ENC-")),
                () -> assertEquals("", encuesta.getComentarios()),
                () -> assertNotNull(encuesta.getFechaRespuesta()),
                () -> assertEquals(1, auditService.obtenerTrazas().size()));
    }

    @ParameterizedTest(name = "puntuación global {0} fuera de rango")
    @ValueSource(ints = {0, 6, -1})
    @DisplayName("Una puntuación global fuera de 1 a 5 (límites 0 y 6) lanza excepción")
    void responderEncuestaConPuntuacionGlobalFueraDeRangoLanzaExcepcion(int global) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.responderEncuesta("PAC-1", "CIT-1", global, 4, "Ok"));

        assertEquals("La puntuación global debe estar entre 1 y 5", e.getMessage());
    }

    @ParameterizedTest(name = "puntuación de atención {0} fuera de rango")
    @ValueSource(ints = {0, 6, -1})
    @DisplayName("Una puntuación de atención fuera de 1 a 5 (límites 0 y 6) lanza excepción")
    void responderEncuestaConPuntuacionAtencionFueraDeRangoLanzaExcepcion(int atencion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.responderEncuesta("PAC-1", "CIT-1", 4, atencion, "Ok"));

        assertEquals("La puntuación de atención debe estar entre 1 y 5", e.getMessage());
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Responder con paciente nulo, vacío o en blanco lanza excepción")
    void responderEncuestaConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.responderEncuesta(paciente, "CIT-1", 4, 4, "Ok"));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }

    @ParameterizedTest(name = "cita [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Responder con cita nula, vacía o en blanco lanza excepción")
    void responderEncuestaConCitaInvalidaLanzaExcepcion(String cita) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.responderEncuesta("PAC-1", cita, 4, 4, "Ok"));

        assertEquals("El identificador de la cita es obligatorio", e.getMessage());
    }

    @Test
    @DisplayName("Responder dos veces la encuesta de la misma cita lanza excepción aunque cambie la capitalización")
    void responderEncuestaDuplicadaLanzaExcepcion() {
        servicio.responderEncuesta("PAC-01", "CIT-200", 4, 4, "Primera");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.responderEncuesta("PAC-01", "cit-200", 4, 4, "Intento duplicado"));

        assertEquals("Ya existe una encuesta registrada para la cita: cit-200", e.getMessage());
    }

    @Test
    @DisplayName("Otro paciente sí puede responder la encuesta de una cita con el mismo código")
    void responderEncuestaDeOtroPacienteNoEsDuplicada() {
        servicio.responderEncuesta("PAC-01", "CIT-200", 4, 4, "Primera");

        assertDoesNotThrow(() -> servicio.responderEncuesta("PAC-02", "CIT-200", 3, 3, "Otra persona"));
    }

    // ---------------------------------------------------------------- listarMisEncuestas

    @Test
    @DisplayName("Listar mis encuestas devuelve solo las del paciente y funciona sin auditoría")
    void listarMisEncuestasDevuelveSoloLasDelPacienteSinAuditoria() {
        ComunicacionPacienteServiceImpl sinAudit = new ComunicacionPacienteServiceImpl(null);
        sinAudit.responderEncuesta("PAC-10", "CIT-10", 5, 5, "Todo perfecto");
        sinAudit.responderEncuesta("PAC-20", "CIT-20", 3, 4, "Correcto");

        List<EncuestaSatisfaccionDTO> encuestas = sinAudit.listarMisEncuestas("PAC-10");

        assertAll(
                () -> assertEquals(1, encuestas.size()),
                () -> assertEquals("CIT-10", encuestas.get(0).getCitaId()),
                () -> assertTrue(sinAudit.listarMisEncuestas("PAC-SIN-ENCUESTAS").isEmpty()));
    }

    @ParameterizedTest(name = "paciente [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Listar encuestas con paciente nulo, vacío o en blanco lanza excepción")
    void listarMisEncuestasConPacienteInvalidoLanzaExcepcion(String paciente) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarMisEncuestas(paciente));

        assertEquals(MSG_PACIENTE, e.getMessage());
    }
}
