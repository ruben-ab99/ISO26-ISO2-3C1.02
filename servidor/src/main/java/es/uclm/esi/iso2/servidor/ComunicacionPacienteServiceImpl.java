package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.portal.EncuestaSatisfaccionDTO;
import es.uclm.sescam.contratos.portal.IComunicacionPacienteService;
import es.uclm.sescam.contratos.portal.IncidenciaPacienteDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ComunicacionPacienteServiceImpl implements IComunicacionPacienteService {

    private static final Set<String> CATEGORIAS_INCIDENCIA_VALIDAS = Set.of(
            "CITA", "PRUEBA", "ATENCION", "OTRA"
    );

    private final Map<String, IncidenciaPacienteDTO> repositorioIncidencias = new LinkedHashMap<>();
    private final Map<String, EncuestaSatisfaccionDTO> repositorioEncuestas = new LinkedHashMap<>();
    private final IAuditService auditService;

    public ComunicacionPacienteServiceImpl() {
        this(new AuditServiceImpl());
    }

    public ComunicacionPacienteServiceImpl(IAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public IncidenciaPacienteDTO registrarIncidencia(String pacienteId, String referenciaCitaOPruebaId,
                                                     String categoria, String descripcion) {
        String pacienteLimpio = validarPaciente(pacienteId);

        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("La categoría de la incidencia es obligatoria");
        }
        String categoriaNormalizada = categoria.trim().toUpperCase();
        if (!CATEGORIAS_INCIDENCIA_VALIDAS.contains(categoriaNormalizada)) {
            throw new IllegalArgumentException("Categoría de incidencia no válida: " + categoria);
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la incidencia es obligatoria");
        }

        String nuevoId = "INC-" + UUID.randomUUID().toString().substring(0, 8);
        String referenciaLimpia = (referenciaCitaOPruebaId != null) ? referenciaCitaOPruebaId.trim() : "";

        IncidenciaPacienteDTO incidencia = new IncidenciaPacienteDTO(
                nuevoId,
                pacienteLimpio,
                referenciaLimpia,
                categoriaNormalizada,
                descripcion.trim(),
                "ABIERTA",
                LocalDateTime.now()
        );

        repositorioIncidencias.put(nuevoId, incidencia);
        registrarAuditoria(pacienteLimpio, "REGISTRAR_INCIDENCIA_PACIENTE:" + nuevoId, true);
        return incidencia;
    }

    @Override
    public List<IncidenciaPacienteDTO> listarMisIncidencias(String pacienteId) {
        String pacienteLimpio = validarPaciente(pacienteId);
        List<IncidenciaPacienteDTO> resultado = new ArrayList<>();
        for (IncidenciaPacienteDTO incidencia : repositorioIncidencias.values()) {
            if (pacienteLimpio.equals(incidencia.getPacienteId())) {
                resultado.add(incidencia);
            }
        }
        return resultado;
    }

    @Override
    public EncuestaSatisfaccionDTO responderEncuesta(String pacienteId, String citaId,
                                                     int puntuacionGlobal, int puntuacionAtencion,
                                                     String comentarios) {
        String pacienteLimpio = validarPaciente(pacienteId);

        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cita es obligatorio");
        }
        if (puntuacionGlobal < 1 || puntuacionGlobal > 5) {
            throw new IllegalArgumentException("La puntuación global debe estar entre 1 y 5");
        }
        if (puntuacionAtencion < 1 || puntuacionAtencion > 5) {
            throw new IllegalArgumentException("La puntuación de atención debe estar entre 1 y 5");
        }

        String citaLimpia = citaId.trim();
        for (EncuestaSatisfaccionDTO existente : repositorioEncuestas.values()) {
            if (pacienteLimpio.equals(existente.getPacienteId())
                    && citaLimpia.equalsIgnoreCase(existente.getCitaId())) {
                throw new IllegalStateException("Ya existe una encuesta registrada para la cita: " + citaLimpia);
            }
        }

        String nuevoId = "ENC-" + UUID.randomUUID().toString().substring(0, 8);
        String comentariosLimpios = (comentarios != null) ? comentarios.trim() : "";

        EncuestaSatisfaccionDTO encuesta = new EncuestaSatisfaccionDTO(
                nuevoId,
                pacienteLimpio,
                citaLimpia,
                puntuacionGlobal,
                puntuacionAtencion,
                comentariosLimpios,
                LocalDateTime.now()
        );

        repositorioEncuestas.put(nuevoId, encuesta);
        registrarAuditoria(pacienteLimpio, "RESPONDER_ENCUESTA_PACIENTE:" + nuevoId, true);
        return encuesta;
    }

    @Override
    public List<EncuestaSatisfaccionDTO> listarMisEncuestas(String pacienteId) {
        String pacienteLimpio = validarPaciente(pacienteId);
        List<EncuestaSatisfaccionDTO> resultado = new ArrayList<>();
        for (EncuestaSatisfaccionDTO encuesta : repositorioEncuestas.values()) {
            if (pacienteLimpio.equals(encuesta.getPacienteId())) {
                resultado.add(encuesta);
            }
        }
        return resultado;
    }

    private String validarPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        return pacienteId.trim();
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            auditService.registrarEvento(usuario, accion, exito, "127.0.0.1");
        }
    }
}