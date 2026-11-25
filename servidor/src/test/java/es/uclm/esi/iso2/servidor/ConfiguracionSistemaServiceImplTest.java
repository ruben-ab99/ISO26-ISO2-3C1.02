package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.ParametroSistemaDTO;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de CU-17 (Configurar parámetros del sistema).
 * Técnicas: clases de equivalencia (dato válido / nulo / en blanco / categoría no permitida),
 * valores límite de la duración (0 y -1 no válidos; 1 válido), criterio each use entre
 * parámetros y prueba de integración con el catálogo (CU-24).
 */
class ConfiguracionSistemaServiceImplTest {

    private ConfiguracionSistemaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new ConfiguracionSistemaServiceImpl();
    }

    private static TipoPruebaDTO tipoPrueba(String codigo, String nombre, int duracion) {
        return new TipoPruebaDTO(codigo, nombre, "RX", "S1", duracion, "", false, false, "", true);
    }

    // ---------------------------------------------------------------- guardarParametro

    @Test
    @DisplayName("Guardar un parámetro válido lo normaliza y lo deja consultable")
    void guardarParametroValidoLoDejaConsultable() {
        ParametroSistemaDTO guardado = servicio.guardarParametro(
                "sla_prioridad_urgente_horas", "12", "prioridades", "ADM-01");

        ParametroSistemaDTO consultado = servicio.consultarParametro("SLA_PRIORIDAD_URGENTE_HORAS");

        assertAll(
                () -> assertNotNull(guardado),
                () -> assertEquals("12", consultado.getValor()),
                () -> assertEquals("PRIORIDADES", consultado.getCategoria()),
                () -> assertEquals("ADM-01", consultado.getModificadoPorAdminId()),
                () -> assertNotNull(consultado.getFechaUltimaModificacion()));
    }

    @ParameterizedTest(name = "categoría {0} es válida")
    @ValueSource(strings = {"HORARIOS", "politicas_citacion", "Prioridades", "PROTOCOLOS"})
    @DisplayName("Las cuatro categorías permitidas se aceptan sin distinguir mayúsculas")
    void guardarParametroConCategoriaPermitidaLoAcepta(String categoria) {
        assertDoesNotThrow(() -> servicio.guardarParametro("CLAVE", "10", categoria, "ADM-01"));
    }

    @ParameterizedTest(name = "clave [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Guardar con clave nula, vacía o en blanco lanza excepción")
    void guardarParametroConClaveInvalidaLanzaExcepcion(String clave) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarParametro(clave, "10", "HORARIOS", "ADM-01"));

        assertEquals("La clave del parámetro es obligatoria", e.getMessage());
    }

    @ParameterizedTest(name = "valor [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Guardar con valor nulo, vacío o en blanco lanza excepción")
    void guardarParametroConValorInvalidoLanzaExcepcion(String valor) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarParametro("CLAVE", valor, "HORARIOS", "ADM-01"));

        assertEquals("El valor del parámetro es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "categoría [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Guardar con categoría nula, vacía o en blanco lanza excepción")
    void guardarParametroConCategoriaVaciaLanzaExcepcion(String categoria) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarParametro("CLAVE", "10", categoria, "ADM-01"));

        assertEquals("La categoría del parámetro es obligatoria", e.getMessage());
    }

    @Test
    @DisplayName("Guardar con una categoría no permitida lanza excepción")
    void guardarParametroConCategoriaNoPermitidaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarParametro("CLAVE", "10", "CATEGORIA_FALSA", "ADM-01"));

        assertEquals("Categoría no permitida: CATEGORIA_FALSA", e.getMessage());
    }

    @ParameterizedTest(name = "administrador [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Guardar con administrador nulo, vacío o en blanco lanza excepción")
    void guardarParametroConAdministradorInvalidoLanzaExcepcion(String adminId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarParametro("CLAVE", "10", "HORARIOS", adminId));

        assertEquals("El identificador del administrador es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- consultarParametro

    @ParameterizedTest(name = "clave [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Consultar con clave nula, vacía o en blanco lanza excepción")
    void consultarParametroConClaveInvalidaLanzaExcepcion(String clave) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarParametro(clave));

        assertEquals("La clave de consulta es obligatoria", e.getMessage());
    }

    @Test
    @DisplayName("Consultar un parámetro inexistente lanza excepción")
    void consultarParametroInexistenteLanzaExcepcion() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.consultarParametro("PARAMETRO_NO_EXISTE"));

        assertEquals("No existe ningún parámetro con clave: PARAMETRO_NO_EXISTE", e.getMessage());
    }

    // ---------------------------------------------------------------- listarParametrosPorCategoria

    @Test
    @DisplayName("Listar por categoría devuelve los parámetros registrados en ella")
    void listarParametrosPorCategoriaDevuelveLosRegistrados() {
        List<ParametroSistemaDTO> prioridades = servicio.listarParametrosPorCategoria("prioridades");

        assertEquals(2, prioridades.size());
    }

    @ParameterizedTest(name = "categoría [{0}] no es válida")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Listar con categoría nula, vacía o en blanco lanza excepción")
    void listarParametrosPorCategoriaVaciaLanzaExcepcion(String categoria) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarParametrosPorCategoria(categoria));

        assertEquals("La categoría de filtrado es obligatoria", e.getMessage());
    }

    @Test
    @DisplayName("Listar con una categoría no permitida lanza excepción")
    void listarParametrosPorCategoriaNoPermitidaLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.listarParametrosPorCategoria("INVALIDA"));

        assertEquals("Categoría no permitida: INVALIDA", e.getMessage());
    }

    // ---------------------------------------------------------------- guardarOActualizarTipoPrueba

    @Test
    @DisplayName("Guardar un tipo de prueba válido normaliza el código a mayúsculas")
    void guardarTipoPruebaValidoNormalizaElCodigo() {
        TipoPruebaDTO nueva = new TipoPruebaDTO("pet-tac", "PET-TAC Cuerpo Completo", "PET",
                "Sala Medicina Nuclear", 60, "Ayuno 6h", true, true, "Control glucemia", true);

        TipoPruebaDTO resultado = servicio.guardarOActualizarTipoPrueba(nueva, "ADM-01");

        assertEquals("PET-TAC", resultado.getCodigoPrueba());
    }

    @ParameterizedTest(name = "duración {0} min es válida")
    @ValueSource(ints = {1, 15, 240})
    @DisplayName("Una duración estimada de 1 minuto o más (límite 1) se acepta")
    void guardarTipoPruebaConDuracionValidaLoAcepta(int duracion) {
        assertDoesNotThrow(() -> servicio.guardarOActualizarTipoPrueba(tipoPrueba("RX-1", "RX", duracion), "ADM-01"));
    }

    @ParameterizedTest(name = "duración {0} min no es válida")
    @ValueSource(ints = {0, -1, -30})
    @DisplayName("Una duración estimada de 0 o menos (límite 0) lanza excepción")
    void guardarTipoPruebaConDuracionInvalidaLanzaExcepcion(int duracion) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarOActualizarTipoPrueba(tipoPrueba("RX-1", "RX", duracion), "ADM-01"));

        assertEquals("La duración estimada debe ser mayor que 0 minutos", e.getMessage());
    }

    @Test
    @DisplayName("Guardar un tipo de prueba nulo lanza excepción")
    void guardarTipoPruebaNuloLanzaExcepcion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarOActualizarTipoPrueba(null, "ADM-01"));

        assertEquals("El tipo de prueba no puede ser nulo", e.getMessage());
    }

    @ParameterizedTest(name = "administrador [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Guardar un tipo de prueba con administrador nulo, vacío o en blanco lanza excepción")
    void guardarTipoPruebaConAdministradorInvalidoLanzaExcepcion(String adminId) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarOActualizarTipoPrueba(tipoPrueba("RX-1", "RX", 15), adminId));

        assertEquals("El identificador del administrador es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "código [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Guardar un tipo de prueba con código nulo, vacío o en blanco lanza excepción")
    void guardarTipoPruebaConCodigoInvalidoLanzaExcepcion(String codigo) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarOActualizarTipoPrueba(tipoPrueba(codigo, "Nombre", 15), "ADM-01"));

        assertEquals("El código del tipo de prueba es obligatorio", e.getMessage());
    }

    @ParameterizedTest(name = "nombre [{0}] no es válido")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("Guardar un tipo de prueba con nombre nulo, vacío o en blanco lanza excepción")
    void guardarTipoPruebaConNombreInvalidoLanzaExcepcion(String nombre) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.guardarOActualizarTipoPrueba(tipoPrueba("RX-1", nombre, 15), "ADM-01"));

        assertEquals("El nombre del tipo de prueba es obligatorio", e.getMessage());
    }

    // ---------------------------------------------------------------- integración con CU-24

    @Test
    @DisplayName("Un tipo de prueba guardado en CU-17 es visible de inmediato en el catálogo CU-24")
    void guardarTipoPruebaComparteCatalogoConCu24() {
        CatalogoPruebasServiceImpl catalogoCompartido = new CatalogoPruebasServiceImpl();
        ConfiguracionSistemaServiceImpl configService = new ConfiguracionSistemaServiceImpl(catalogoCompartido);
        TipoPruebaDTO nueva = new TipoPruebaDTO("mamo-3d", "Mamografía Digital con Tomosíntesis", "MAMO",
                "Sala Mama-01", 25, "No aplicar desodorante ni talco", false, false, "Sin contraindicaciones", true);

        configService.guardarOActualizarTipoPrueba(nueva, "ADM-01");

        TipoPruebaDTO recuperada = catalogoCompartido.consultarDetallePrueba("MAMO-3D");
        assertEquals("Mamografía Digital con Tomosíntesis", recuperada.getNombre());
    }
}
