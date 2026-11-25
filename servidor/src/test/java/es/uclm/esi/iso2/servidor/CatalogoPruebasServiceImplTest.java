package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
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
 * Pruebas de CU-24 (Consultar catálogo de pruebas).
 * Técnicas: clases de equivalencia (código válido / nulo o en blanco / inexistente / inactivo),
 * conjetura de errores (mayúsculas y minúsculas, modalidad sin pruebas).
 */
class CatalogoPruebasServiceImplTest {

    private CatalogoPruebasServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CatalogoPruebasServiceImpl();
    }

    @Test
    @DisplayName("Listar el catálogo devuelve solo las pruebas activas")
    void listarCatalogoDevuelveSoloPruebasActivas() {
        List<TipoPruebaDTO> catalogo = servicio.listarCatalogoPruebas();

        assertEquals(4, catalogo.size());
        assertTrue(catalogo.stream().allMatch(TipoPruebaDTO::isActiva));
    }

    @Test
    @DisplayName("Consultar el detalle con un código válido devuelve todos los datos de la prueba")
    void consultarDetallePruebaValidaDevuelveSusDatos() {
        TipoPruebaDTO prueba = servicio.consultarDetallePrueba("TAC-ABDO");

        assertAll(
                () -> assertNotNull(prueba),
                () -> assertEquals("TAC-ABDO", prueba.getCodigoPrueba()),
                () -> assertEquals("TAC", prueba.getModalidad()),
                () -> assertEquals(30, prueba.getDuracionEstimadaMinutos()),
                () -> assertTrue(prueba.isRequiereContraste()),
                () -> assertTrue(prueba.isRequiereConsentimiento()));
    }

    @ParameterizedTest(name = "el código \"{0}\" encuentra {1}")
    @CsvSource({
            "tac-abdo,   TAC-ABDO",
            "Tac-Abdo,   TAC-ABDO",
            "'  RX-TORAX ', RX-TORAX"
    })
    @DisplayName("El código se busca sin distinguir mayúsculas ni espacios sobrantes")
    void consultarDetallePruebaIgnoraMayusculasYEspacios(String codigo, String codigoEsperado) {
        assertEquals(codigoEsperado, servicio.consultarDetallePrueba(codigo).getCodigoPrueba());
    }

    @ParameterizedTest(name = "código [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Consultar con código nulo, vacío o en blanco lanza excepción")
    void consultarDetallePruebaConCodigoInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarDetallePrueba(codigo));

        assertEquals("El código de la prueba es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "código {0} no está disponible")
    @ValueSource(strings = {"NO-EXISTE", "RX-OBSOLETA"})
    @DisplayName("Consultar una prueba inexistente o dada de baja lanza excepción")
    void consultarDetallePruebaNoDisponibleLanzaExcepcion(String codigo) {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarDetallePrueba(codigo));

        assertEquals("No existe ninguna prueba activa con código: " + codigo, e.getMessage());
    }

    @Test
    @DisplayName("Filtrar por modalidad devuelve las pruebas activas que coinciden")
    void filtrarPorModalidadDevuelveCoincidenciasActivas() {
        List<TipoPruebaDTO> pruebasRm = servicio.filtrarPorModalidad("rm");

        assertAll(
                () -> assertEquals(1, pruebasRm.size()),
                () -> assertEquals("RM-CRANEO", pruebasRm.get(0).getCodigoPrueba()));
    }

    @Test
    @DisplayName("Filtrar por una modalidad sin pruebas devuelve una lista vacía")
    void filtrarPorModalidadSinPruebasDevuelveListaVacia() {
        assertTrue(servicio.filtrarPorModalidad("PET").isEmpty());
    }

    @ParameterizedTest(name = "modalidad [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Filtrar con modalidad nula, vacía o en blanco lanza excepción")
    void filtrarPorModalidadInvalidaLanzaExcepcion(String modalidad) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.filtrarPorModalidad(modalidad));

        assertEquals("La modalidad diagnóstica es obligatoria", e.getMessage());
    }
}
