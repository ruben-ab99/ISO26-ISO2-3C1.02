package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.IConfiguracionSistemaService;
import es.uclm.sescam.contratos.catalogo.ParametroSistemaDTO;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfiguracionSistemaServiceImpl implements IConfiguracionSistemaService {

    private static final Set<String> CATEGORIAS_VALIDAS = Set.of(
            "HORARIOS", "POLITICAS_CITACION", "PRIORIDADES", "PROTOCOLOS"
    );

    private final Map<String, ParametroSistemaDTO> parametros = new LinkedHashMap<>();
    private final Map<String, TipoPruebaDTO> catalogoPruebas = new LinkedHashMap<>();

    public ConfiguracionSistemaServiceImpl() {
        cargarParametrosPorDefecto();
    }

    private void cargarParametrosPorDefecto() {
        guardarParametro("HORARIO_APERTURA_SALAS", "08:00-21:00", "HORARIOS", "SYSTEM");
        guardarParametro("SLA_PRIORIDAD_URGENTE_HORAS", "24", "PRIORIDADES", "SYSTEM");
        guardarParametro("SLA_PRIORIDAD_PREFERENTE_DIAS", "7", "PRIORIDADES", "SYSTEM");
        guardarParametro("ANTELACION_MINIMA_CANCELACION_HORAS", "24", "POLITICAS_CITACION", "SYSTEM");
        guardarParametro("PROTOCOLO_CONTRASTE_YODADO", "Verificar creatinina < 1.5 mg/dL", "PROTOCOLOS", "SYSTEM");
    }

    @Override
    public ParametroSistemaDTO guardarParametro(String clave, String valor, String categoria, String adminId) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave del parámetro es obligatoria");
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El valor del parámetro es obligatorio");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoría del parámetro es obligatoria");
        }
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("El identificador del administrador es obligatorio");
        }

        String categoriaNormalizada = categoria.trim().toUpperCase();
        if (!CATEGORIAS_VALIDAS.contains(categoriaNormalizada)) {
            throw new IllegalArgumentException("Categoría no permitida: " + categoria);
        }

        String claveNormalizada = clave.trim().toUpperCase();
        ParametroSistemaDTO dto = new ParametroSistemaDTO(
                claveNormalizada,
                valor.trim(),
                categoriaNormalizada,
                adminId.trim(),
                LocalDateTime.now()
        );
        parametros.put(claveNormalizada, dto);
        return dto;
    }

    @Override
    public ParametroSistemaDTO consultarParametro(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de consulta es obligatoria");
        }
        ParametroSistemaDTO dto = parametros.get(clave.trim().toUpperCase());
        if (dto == null) {
            throw new IllegalStateException("No existe ningún parámetro con clave: " + clave);
        }
        return dto;
    }

    @Override
    public List<ParametroSistemaDTO> listarParametrosPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoría de filtrado es obligatoria");
        }
        String categoriaNormalizada = categoria.trim().toUpperCase();
        if (!CATEGORIAS_VALIDAS.contains(categoriaNormalizada)) {
            throw new IllegalArgumentException("Categoría no permitida: " + categoria);
        }

        List<ParametroSistemaDTO> resultado = new ArrayList<>();
        for (ParametroSistemaDTO dto : parametros.values()) {
            if (dto.getCategoria().equals(categoriaNormalizada)) {
                resultado.add(dto);
            }
        }
        return resultado;
    }

    @Override
    public TipoPruebaDTO guardarOActualizarTipoPrueba(TipoPruebaDTO tipoPrueba, String adminId) {
        if (tipoPrueba == null) {
            throw new IllegalArgumentException("El tipo de prueba no puede ser nulo");
        }
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("El identificador del administrador es obligatorio");
        }
        if (tipoPrueba.getCodigoPrueba() == null || tipoPrueba.getCodigoPrueba().isBlank()) {
            throw new IllegalArgumentException("El código del tipo de prueba es obligatorio");
        }
        if (tipoPrueba.getNombre() == null || tipoPrueba.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del tipo de prueba es obligatorio");
        }
        if (tipoPrueba.getDuracionEstimadaMinutos() <= 0) {
            throw new IllegalArgumentException("La duración estimada debe ser mayor que 0 minutos");
        }

        String codigoNormalizado = tipoPrueba.getCodigoPrueba().trim().toUpperCase();
        tipoPrueba.setCodigoPrueba(codigoNormalizado);
        catalogoPruebas.put(codigoNormalizado, tipoPrueba);
        return tipoPrueba;
    }
}