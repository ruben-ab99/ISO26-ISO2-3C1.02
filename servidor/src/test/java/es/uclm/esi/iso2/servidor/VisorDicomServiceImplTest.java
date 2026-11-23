package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioDicomDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VisorDicomServiceImplTest {

    private VisorDicomServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new VisorDicomServiceImpl();
    }

    @Test
    @DisplayName("Obtener estudio DICOM conforme devuelve el DTO del estudio")
    void obtenerEstudioDicomValido() {
        // Preparar
        String estudioId = "DICOM-001";
        String usuarioId = "MED-01";

        // Ejecutar
        EstudioDicomDTO estudio = servicio.obtenerEstudioDicom(estudioId, usuarioId);

        // Comprobar
        assertNotNull(estudio);
    }

    @Test
    @DisplayName("Obtener estudio DICOM con parámetros nulos, en blanco o no conformes lanza excepción")
    void obtenerEstudioDicomParametrosInvalidosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEstudioDicom(null, "MED-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEstudioDicom("   ", "MED-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEstudioDicom("DICOM-001", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEstudioDicom("DICOM-001", "   "));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerEstudioDicom("EST-001", "MED-01"));
    }

    @Test
    @DisplayName("Obtener imagen DICOM válida devuelve los bytes con la cabecera DICM esperada")
    void obtenerImagenDicomValida() {
        // Preparar
        String estudioId = "DICOM-001";
        String serieId = "SERIE-001";
        String instanciaId = "INST-001";

        // Ejecutar
        byte[] imagenBytes = servicio.obtenerImagenDicom(estudioId, serieId, instanciaId);

        // Comprobar
        assertNotNull(imagenBytes);
        String contenido = new String(imagenBytes, StandardCharsets.UTF_8);
        assertEquals("DICM|DICOM-001|SERIE-001|INST-001", contenido);
    }

    @Test
    @DisplayName("Obtener imagen DICOM con datos inválidos o no conformes lanza excepción")
    void obtenerImagenDicomParametrosInvalidosLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom(null, "SERIE-001", "INST-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom("EST-999", "SERIE-001", "INST-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom("DICOM-001", null, "INST-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom("DICOM-001", "  ", "INST-001"));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom("DICOM-001", "SERIE-001", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerImagenDicom("DICOM-001", "SERIE-001", "  "));
    }

    @Test
    @DisplayName("Verificar conformidad DICOM comprueba el prefijo DICOM- correctamente")
    void verificarConformidadDicom() {
        assertTrue(servicio.verificarConformidadDicom("DICOM-12345"));
        assertFalse(servicio.verificarConformidadDicom("EST-12345"));
        assertFalse(servicio.verificarConformidadDicom(""));
        assertFalse(servicio.verificarConformidadDicom(null));
    }
}