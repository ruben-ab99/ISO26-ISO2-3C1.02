package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoPruebasServiceImplTest {

    private CatalogoPruebasServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CatalogoPruebasServiceImpl();
    }

    @Test
    @DisplayName("Listar catálogo devuelve solo las pruebas activas")
    void listarCatalogoDevuelveSoloPruebasActivas() {
        // Preparar & Ejecutar
        List<TipoPruebaDTO> catalogo = servicio.listarCatalogoPruebas();

        // Comprobar
        assertEquals(4, catalogo.size());
        assertTrue(catalogo.stream().allMatch(TipoPruebaDTO::isActiva));
    }

    @Test
    @DisplayName("Consultar detalle con código válido (insensible a mayúsculas) devuelve la prueba")
    void consultarDetallePruebaValida() {
        // Preparar
        String codigo = "tac-abdo";

        // Ejecutar
        TipoPruebaDTO prueba = servicio.consultarDetallePrueba(codigo);

        // Comprobar
        assertNotNull(prueba);
        assertEquals("TAC-ABDO", prueba.getCodigoPrueba());
        assertEquals("TAC", prueba.getModalidad());
        assertEquals(30, prueba.getDuracionEstimadaMinutos());
        assertTrue(prueba.isRequiereContraste());
        assertTrue(prueba.isRequiereConsentimiento());
    }

    @Test
    @DisplayName("Consultar detalle con código nulo, vacío, inexistente o inactivo lanza excepción")
    void consultarDetallePruebaInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetallePrueba(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarDetallePrueba("   "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarDetallePrueba("NO-EXISTE"));
        assertThrows(IllegalStateException.class, () -> servicio.consultarDetallePrueba("RX-OBSOLETA"));
    }

    @Test
    @DisplayName("Filtrar por modalidad devuelve coincidencias activas o lista vacía si no hay")
    void filtrarPorModalidadValida() {
        // Ejecutar
        List<TipoPruebaDTO> pruebasRm = servicio.filtrarPorModalidad("rm");
        List<TipoPruebaDTO> pruebasPet = servicio.filtrarPorModalidad("PET");

        // Comprobar
        assertEquals(1, pruebasRm.size());
        assertEquals("RM-CRANEO", pruebasRm.get(0).getCodigoPrueba());
        assertTrue(pruebasPet.isEmpty());
    }

    @Test
    @DisplayName("Filtrar por modalidad nula o en blanco lanza excepción")
    void filtrarPorModalidadInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.filtrarPorModalidad(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.filtrarPorModalidad(" "));
    }
}