package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioDicomDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-23 (Visualizar imágenes DICOM).
 * Técnicas: clases de equivalencia sobre cada identificador (válido / nulo / en blanco),
 * cumplimiento del estándar (prefijo "DICOM-") y criterio each use entre parámetros.
 */
class VisorDicomServiceImplTest {

    private VisorDicomServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new VisorDicomServiceImpl();
    }

    // ---------------------------------------------------------------- obtenerEstudioDicom

    @Test
    @DisplayName("Obtener un estudio DICOM conforme devuelve el estudio solicitado")
    void obtenerEstudioDicomConformeDevuelveElEstudio() {
        EstudioDicomDTO estudio = servicio.obtenerEstudioDicom("DICOM-001", "MED-01");

        assertNotNull(estudio);
    }

    @ParameterizedTest(name = "estudio [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Obtener un estudio con identificador nulo, vacío o en blanco lanza excepción")
    void obtenerEstudioDicomConEstudioInvalidoLanzaExcepcion(String estudioId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerEstudioDicom(estudioId, "MED-01"));

        assertEquals("El ID del estudio DICOM es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "usuario [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Obtener un estudio con usuario nulo, vacío o en blanco lanza excepción")
    void obtenerEstudioDicomConUsuarioInvalidoLanzaExcepcion(String usuarioId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerEstudioDicom("DICOM-001", usuarioId));

        assertEquals("El ID del usuario es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "estudio {0} no cumple DICOM")
    @ValueSource(strings = {"EST-001", "dicom-001", "DICOM001", "XDICOM-1"})
    @DisplayName("Obtener un estudio que no cumple el estándar DICOM lanza excepción")
    void obtenerEstudioDicomNoConformeLanzaExcepcion(String estudioId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerEstudioDicom(estudioId, "MED-01"));

        assertEquals("El estudio no cumple con el estándar DICOM: " + estudioId, e.getMessage());
    }

    // ---------------------------------------------------------------- obtenerImagenDicom

    @Test
    @DisplayName("Obtener una imagen válida devuelve los bytes con la cabecera DICM esperada")
    void obtenerImagenDicomValidaDevuelveCabeceraEsperada() {
        byte[] imagenBytes = servicio.obtenerImagenDicom("DICOM-001", "SERIE-001", "INST-001");

        assertNotNull(imagenBytes);
        assertEquals("DICM|DICOM-001|SERIE-001|INST-001", new String(imagenBytes, StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "estudio [{0}] no es conforme")
    @NullAndEmptySource
    @ValueSource(strings = {"EST-999"})
    @DisplayName("Obtener una imagen de un estudio nulo, vacío o no conforme lanza excepción")
    void obtenerImagenDicomConEstudioNoConformeLanzaExcepcion(String estudioId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerImagenDicom(estudioId, "SERIE-001", "INST-001"));

        assertEquals("Estudio no conforme con DICOM", e.getMessage());
    }

    @ParameterizedTest(name = "serie [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Obtener una imagen con serie nula, vacía o en blanco lanza excepción")
    void obtenerImagenDicomConSerieInvalidaLanzaExcepcion(String serieId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerImagenDicom("DICOM-001", serieId, "INST-001"));

        assertEquals("La serie y la instancia son obligatorias", e.getMessage());
    }

    @ParameterizedTest(name = "instancia [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Obtener una imagen con instancia nula, vacía o en blanco lanza excepción")
    void obtenerImagenDicomConInstanciaInvalidaLanzaExcepcion(String instanciaId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerImagenDicom("DICOM-001", "SERIE-001", instanciaId));

        assertEquals("La serie y la instancia son obligatorias", e.getMessage());
    }

    // ---------------------------------------------------------------- verificarConformidadDicom

    @ParameterizedTest(name = "{0} es conforme")
    @ValueSource(strings = {"DICOM-12345", "DICOM-1"})
    @DisplayName("Un identificador con el prefijo DICOM- es conforme")
    void verificarConformidadDicomConPrefijoCorrectoDevuelveTrue(String estudioId) {
        assertTrue(servicio.verificarConformidadDicom(estudioId));
    }

    @ParameterizedTest(name = "[{0}] no es conforme")
    @NullAndEmptySource
    @ValueSource(strings = {"EST-12345", "dicom-12345"})
    @DisplayName("Un identificador nulo, vacío o sin el prefijo DICOM- no es conforme")
    void verificarConformidadDicomSinPrefijoCorrectoDevuelveFalse(String estudioId) {
        assertFalse(servicio.verificarConformidadDicom(estudioId));
    }
}
