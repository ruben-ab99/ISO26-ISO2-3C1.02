package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import es.uclm.sescam.contratos.prescripciones.ISolicitudPruebaService;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SolicitudPruebaServiceImpl implements ISolicitudPruebaService {

    private final Map<String, SolicitudPruebaDTO> repositorioSolicitudes = new LinkedHashMap<>();
    private final ICatalogoPruebasService catalogoService;

    public SolicitudPruebaServiceImpl() {
        this(new CatalogoPruebasServiceImpl());
    }

    public SolicitudPruebaServiceImpl(ICatalogoPruebasService catalogoService) {
        if (catalogoService == null) {
            throw new IllegalArgumentException("El servicio de catálogo de pruebas es obligatorio");
        }
        this.catalogoService = catalogoService;
    }

    @Override
    public SolicitudPruebaDTO crearSolicitudPrueba(String pacienteId, String medicoId, String codigoPrueba,
                                                   int prioridad, String justificacionClinica) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        if (codigoPrueba == null || codigoPrueba.isBlank()) {
            throw new IllegalArgumentException("El código de la prueba diagnóstica es obligatorio");
        }
        if (prioridad < 1 || prioridad > 5) {
            throw new IllegalArgumentException("La prioridad debe estar comprendida entre 1 (urgente) y 5 (ordinaria)");
        }
        if (justificacionClinica == null || justificacionClinica.isBlank()) {
            throw new IllegalArgumentException("La justificación clínica es obligatoria");
        }

        // Relación <<include>> CU-24: verifica que la prueba existe y está activa en el catálogo
        TipoPruebaDTO pruebaCatalogo = catalogoService.consultarDetallePrueba(codigoPrueba);

        String nuevaSolicitudId = "SOL-" + UUID.randomUUID().toString().substring(0, 8);
        String etiquetaPrioridad = mapearNivelPrioridad(prioridad);

        SolicitudPruebaDTO solicitud = new SolicitudPruebaDTO(
                nuevaSolicitudId,
                pacienteId.trim(),
                medicoId.trim(),
                pruebaCatalogo.getCodigoPrueba(),
                pruebaCatalogo.getNombre(),
                prioridad,
                etiquetaPrioridad,
                justificacionClinica.trim(),
                "PENDIENTE",
                "",
                LocalDateTime.now()
        );

        repositorioSolicitudes.put(nuevaSolicitudId, solicitud);
        return solicitud;
    }

    @Override
    public SolicitudPruebaDTO consultarSolicitud(String solicitudId) {
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la solicitud es obligatorio");
        }
        SolicitudPruebaDTO solicitud = repositorioSolicitudes.get(solicitudId.trim());
        if (solicitud == null) {
            throw new IllegalStateException("No existe ninguna solicitud de prueba con ID: " + solicitudId);
        }
        return solicitud;
    }

    @Override
    public List<SolicitudPruebaDTO> listarSolicitudesPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteBuscado = pacienteId.trim();
        List<SolicitudPruebaDTO> resultado = new ArrayList<>();
        for (SolicitudPruebaDTO solicitud : repositorioSolicitudes.values()) {
            if (solicitud.getPacienteId().equals(pacienteBuscado)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    @Override
    public SolicitudPruebaDTO cancelarSolicitud(String solicitudId, String motivo) {
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la solicitud es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio");
        }

        SolicitudPruebaDTO solicitud = consultarSolicitud(solicitudId);
        if ("CANCELADA".equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya se encuentra cancelada");
        }

        solicitud.setEstado("CANCELADA");
        solicitud.setMotivoCancelacion(motivo.trim());
        return solicitud;
    }

    private String mapearNivelPrioridad(int prioridad) {
        if (prioridad == 1) {
            return "URGENTE";
        }
        if (prioridad <= 3) {
            return "PREFERENTE";
        }
        return "ORDINARIA";
    }

    @Override
    public List<SolicitudPruebaDTO> listarSolicitudesMedico(String medicoId) {
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        String medicoBuscado = medicoId.trim();
        List<SolicitudPruebaDTO> resultado = new ArrayList<>();
        for (SolicitudPruebaDTO solicitud : repositorioSolicitudes.values()) {
            if (solicitud.getMedicoId() != null && solicitud.getMedicoId().equals(medicoBuscado)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }
}