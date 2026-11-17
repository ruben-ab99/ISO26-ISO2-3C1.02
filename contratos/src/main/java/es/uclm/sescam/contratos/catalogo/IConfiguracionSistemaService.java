package es.uclm.sescam.contratos.catalogo;

import java.util.List;

public interface IConfiguracionSistemaService {
    ParametroSistemaDTO guardarParametro(String clave, String valor, String categoria, String adminId);
    ParametroSistemaDTO consultarParametro(String clave);
    List<ParametroSistemaDTO> listarParametrosPorCategoria(String categoria);
    TipoPruebaDTO guardarOActualizarTipoPrueba(TipoPruebaDTO tipoPrueba, String adminId);
}