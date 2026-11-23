package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.ParametroSistemaDTO;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguracionSistemaServiceImplTest {

    private ConfiguracionSistemaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new ConfiguracionSistemaServiceImpl();
    }

    @Test
    @DisplayName("Guardar y consultar parámetro válido actualiza su valor y auditoría")
    void guardarYConsultarParametroValido() {
        ParametroSistemaDTO guardado = servicio.guardarParametro(
                "sla_prioridad_urgente_horas", "12", "prioridades", "ADM-01"
        );
        ParametroSistemaDTO consultado = servicio.consultarParametro("SLA_PRIORIDAD_URGENTE_HORAS");

        assertNotNull(guardado);
        assertEquals("12", consultado.getValor());
        assertEquals("PRIORIDADES", consultado.getCategoria());
        assertEquals("ADM-01", consultado.getModificadoPorAdminId());
        assertNotNull(consultado.getFechaUltimaModificacion());
    }

    @Test
    @DisplayName("Guardar parámetro con datos nulos, vacíos o categoría inválida lanza excepción")
    void guardarParametroInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro(null, "10", "HORARIOS", "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro(" ", "10", "HORARIOS", "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", null, "HORARIOS", "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", "", "HORARIOS", "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", "10", null, "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", "10", "CATEGORIA_FALSA", "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", "10", "HORARIOS", null));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarParametro("CLAVE", "10", "HORARIOS", "  "));
    }

    @Test
    @DisplayName("Consultar parámetro nulo, vacío o inexistente lanza excepción")
    void consultarParametroInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarParametro(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarParametro("   "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarParametro("PARAMETRO_NO_EXISTE"));
    }

    @Test
    @DisplayName("Listar parámetros por categoría devuelve los registrados en esa categoría")
    void listarParametrosPorCategoriaValida() {
        List<ParametroSistemaDTO> prioridades = servicio.listarParametrosPorCategoria("prioridades");
        assertEquals(2, prioridades.size());

        assertThrows(IllegalArgumentException.class, () -> servicio.listarParametrosPorCategoria(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarParametrosPorCategoria("INVALIDA"));
    }

    @Test
    @DisplayName("Guardar o actualizar tipo de prueba valida restricciones de administrador y duración")
    void guardarOActualizarTipoPrueba() {
        TipoPruebaDTO nuevaPrueba = new TipoPruebaDTO(
                "pet-tac", "PET-TAC Cuerpo Completo", "PET", "Sala Medicina Nuclear",
                60, "Ayuno 6h", true, true, "Control glucemia", true
        );

        TipoPruebaDTO resultado = servicio.guardarOActualizarTipoPrueba(nuevaPrueba, "ADM-01");
        assertEquals("PET-TAC", resultado.getCodigoPrueba());

        assertThrows(IllegalArgumentException.class, () -> servicio.guardarOActualizarTipoPrueba(null, "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarOActualizarTipoPrueba(nuevaPrueba, ""));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarOActualizarTipoPrueba(
                new TipoPruebaDTO(null, "Nombre", "RX", "S1", 15, "", false, false, "", true), "ADM-01"));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarOActualizarTipoPrueba(
                new TipoPruebaDTO("RX-1", " ", "RX", "S1", 15, "", false, false, "", true), "ADM-01"));

        TipoPruebaDTO sinDuracion = new TipoPruebaDTO("RX-1", "RX", "RX", "S1", 0, "", false, false, "", true);
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarOActualizarTipoPrueba(sinDuracion, "ADM-01"));
    }

    @Test
    @DisplayName("Guardar tipo de prueba en CU-17 lo hace visible inmediatamente en el catálogo CU-24")
    void guardarTipoPruebaComparteCatalogoConCu24() {
        CatalogoPruebasServiceImpl catalogoCompartido = new CatalogoPruebasServiceImpl();
        ConfiguracionSistemaServiceImpl configService = new ConfiguracionSistemaServiceImpl(catalogoCompartido);

        TipoPruebaDTO nuevaPrueba = new TipoPruebaDTO(
                "mamo-3d", "Mamografía Digital con Tomosíntesis", "MAMO", "Sala Mama-01",
                25, "No aplicar desodorante ni talco", false, false, "Sin contraindicaciones", true
        );

        configService.guardarOActualizarTipoPrueba(nuevaPrueba, "ADM-01");

        TipoPruebaDTO recuperada = catalogoCompartido.consultarDetallePrueba("MAMO-3D");
        assertNotNull(recuperada);
        assertEquals("Mamografía Digital con Tomosíntesis", recuperada.getNombre());
    }
}