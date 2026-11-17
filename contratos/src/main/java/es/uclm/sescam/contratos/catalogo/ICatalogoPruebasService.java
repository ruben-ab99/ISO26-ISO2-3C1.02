package es.uclm.sescam.contratos.catalogo;

import java.util.List;

public interface ICatalogoPruebasService {
    List<TipoPruebaDTO> listarCatalogoPruebas();
    TipoPruebaDTO consultarDetallePrueba(String codigoPrueba);
    List<TipoPruebaDTO> filtrarPorModalidad(String modalidad);
}