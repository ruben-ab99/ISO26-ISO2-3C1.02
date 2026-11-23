package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CatalogoPruebasServiceImpl implements ICatalogoPruebasService {

    private final Map<String, TipoPruebaDTO> catalogo = new ConcurrentHashMap<>();

    public CatalogoPruebasServiceImpl() {
        cargarCatalogoInicialSescam();
    }

    private void cargarCatalogoInicialSescam() {
        registrarPruebaEnCatalogo(new TipoPruebaDTO(
                "RX-TORAX", "Radiografía de Tórax PA y Lateral", "RX", "Sala RX-01",
                15, "Retirar objetos metálicos del torso", false, false,
                "Protección plomada gonadal; verificar embarazo", true
        ));
        registrarPruebaEnCatalogo(new TipoPruebaDTO(
                "TAC-ABDO", "TAC Abdominal con Contraste IV", "TAC", "Sala TAC-02",
                30, "Ayuno de 6 horas y analítica de función renal reciente", true, true,
                "Cuestionario de alergia al yodo y consentimiento firmado", true
        ));
        registrarPruebaEnCatalogo(new TipoPruebaDTO(
                "RM-CRANEO", "Resonancia Magnética Craneal", "RM", "Sala RM-01",
                45, "Acudir sin joyas, relojes ni maquillaje metálico", false, true,
                "Descartar marcapasos, implantes cocleares o clips ferromagnéticos", true
        ));
        registrarPruebaEnCatalogo(new TipoPruebaDTO(
                "ECO-ABDO", "Ecografía Abdominal Completa", "ECO", "Sala ECO-03",
                20, "Ayuno de 6 horas y vejiga llena", false, false,
                "Sin radiación ionizante", true
        ));
        registrarPruebaEnCatalogo(new TipoPruebaDTO(
                "RX-OBSOLETA", "Radiografía Convencional Analógica", "RX", "Sala Archivo",
                20, "N/A", false, false, "Prueba dada de baja", false
        ));
    }

    @Override
    public void registrarPruebaEnCatalogo(TipoPruebaDTO prueba) {
        if (prueba != null && prueba.getCodigoPrueba() != null && !prueba.getCodigoPrueba().isBlank()) {
            String codigoNormalizado = prueba.getCodigoPrueba().trim().toUpperCase();
            prueba.setCodigoPrueba(codigoNormalizado);
            catalogo.put(codigoNormalizado, prueba);
        }
    }

    @Override
    public List<TipoPruebaDTO> listarCatalogoPruebas() {
        List<TipoPruebaDTO> activas = new ArrayList<>();
        for (TipoPruebaDTO prueba : catalogo.values()) {
            if (prueba.isActiva()) {
                activas.add(prueba);
            }
        }
        return activas;
    }

    @Override
    public TipoPruebaDTO consultarDetallePrueba(String codigoPrueba) {
        if (codigoPrueba == null || codigoPrueba.isBlank()) {
            throw new IllegalArgumentException("El código de la prueba es obligatorio");
        }
        TipoPruebaDTO prueba = catalogo.get(codigoPrueba.trim().toUpperCase());
        if (prueba == null || !prueba.isActiva()) {
            throw new IllegalStateException("No existe ninguna prueba activa con código: " + codigoPrueba);
        }
        return prueba;
    }

    @Override
    public List<TipoPruebaDTO> filtrarPorModalidad(String modalidad) {
        if (modalidad == null || modalidad.isBlank()) {
            throw new IllegalArgumentException("La modalidad diagnóstica es obligatoria");
        }
        String modalidadBuscada = modalidad.trim().toUpperCase();
        List<TipoPruebaDTO> filtradas = new ArrayList<>();
        for (TipoPruebaDTO prueba : catalogo.values()) {
            if (prueba.isActiva() && prueba.getModalidad().equalsIgnoreCase(modalidadBuscada)) {
                filtradas.add(prueba);
            }
        }
        return filtradas;
    }
}