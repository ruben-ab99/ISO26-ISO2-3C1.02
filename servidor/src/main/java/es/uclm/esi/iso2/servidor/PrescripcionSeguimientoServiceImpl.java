package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.catalogo.ICatalogoPruebasService;
import es.uclm.sescam.contratos.catalogo.TipoPruebaDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrescripcionSeguimientoServiceImpl implements IPrescripcionSeguimientoService {

    private static final List<String> FRANJAS_SEGUIMIENTO = List.of(
            "09:00-09:30", "10:00-10:30", "11:00-11:30", "12:00-12:30"
    );

    private final Map<String, PrescripcionSeguimientoDTO> repositorioSeguimientos = new LinkedHashMap<>();
    private final ICatalogoPruebasService catalogoService;
    private final IGestionCitasService gestionCitasService;
    private final IAuditService auditService;

    public PrescripcionSeguimientoServiceImpl() {
        this(new CatalogoPruebasServiceImpl(), new GestionCitasServiceImpl(), new AuditServiceImpl());
    }

    public PrescripcionSeguimientoServiceImpl(ICatalogoPruebasService catalogoService,
                                             IGestionCitasService gestionCitasService) {
        this(catalogoService, gestionCitasService, new AuditServiceImpl());
    }

    public PrescripcionSeguimientoServiceImpl(ICatalogoPruebasService catalogoService,
                                             IGestionCitasService gestionCitasService,
                                             IAuditService auditService) {
        if (catalogoService == null) {
            throw new IllegalArgumentException("El servicio de catálogo de pruebas es obligatorio");
        }
        if (gestionCitasService == null) {
            throw new IllegalArgumentException("El servicio de gestión de citas es obligatorio");
        }
        this.catalogoService = catalogoService;
        this.gestionCitasService = gestionCitasService;
        this.auditService = auditService;
    }

    @Override
    public PrescripcionSeguimientoDTO prescribirSeguimiento(String pacienteId, String medicoId, String codigoPrueba,
                                                            int periodicidadDias, int numeroRevisiones,
                                                            LocalDate fechaInicio, String indicacionesClinicas) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        if (codigoPrueba == null || codigoPrueba.isBlank()) {
            throw new IllegalArgumentException("El código de la prueba diagnóstica es obligatorio");
        }
        if (periodicidadDias <= 0) {
            throw new IllegalArgumentException("La periodicidad en días debe ser mayor que cero");
        }
        if (numeroRevisiones < 1 || numeroRevisiones > 24) {
            throw new IllegalArgumentException("El número de revisiones debe estar comprendido entre 1 y 24");
        }
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio del seguimiento es obligatoria");
        }
        if (indicacionesClinicas == null || indicacionesClinicas.isBlank()) {
            throw new IllegalArgumentException("Las indicaciones clínicas son obligatorias");
        }

        // Verifica que la prueba existe y está activa en el catálogo (CU-24)
        TipoPruebaDTO pruebaCatalogo = catalogoService.consultarDetallePrueba(codigoPrueba);

        String pacienteLimpio = pacienteId.trim();
        String medicoLimpio = medicoId.trim();
        String nuevoSeguimientoId = "SEG-" + UUID.randomUUID().toString().substring(0, 8);

        // Relación <<include>> CU-13: genera automáticamente las citas periódicas necesarias
        List<String> citasGeneradasIds = new ArrayList<>();
        for (int i = 0; i < numeroRevisiones; i++) {
            LocalDate fechaRevision = fechaInicio.plusDays((long) i * periodicidadDias);
            CitaDTO cita = programarCitaEnFranjaDisponible(
                    pacienteLimpio, medicoLimpio, pruebaCatalogo.getCodigoPrueba(), fechaRevision
            );
            citasGeneradasIds.add(cita.getCitaId());
        }

        PrescripcionSeguimientoDTO seguimiento = new PrescripcionSeguimientoDTO(
                nuevoSeguimientoId,
                pacienteLimpio,
                medicoLimpio,
                pruebaCatalogo.getCodigoPrueba(),
                pruebaCatalogo.getNombre(),
                periodicidadDias,
                numeroRevisiones,
                fechaInicio,
                indicacionesClinicas.trim(),
                "ACTIVO",
                "",
                citasGeneradasIds,
                LocalDateTime.now()
        );

        repositorioSeguimientos.put(nuevoSeguimientoId, seguimiento);
        registrarAuditoria(medicoLimpio, "PRESCRIBIR_SEGUIMIENTO:" + nuevoSeguimientoId, true);
        return seguimiento;
    }

    @Override
    public PrescripcionSeguimientoDTO consultarSeguimiento(String seguimientoId) {
        if (seguimientoId == null || seguimientoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del seguimiento es obligatorio");
        }
        PrescripcionSeguimientoDTO seguimiento = repositorioSeguimientos.get(seguimientoId.trim());
        if (seguimiento == null) {
            throw new IllegalStateException("No existe ninguna prescripción de seguimiento con ID: " + seguimientoId);
        }
        return seguimiento;
    }

    @Override
    public List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteBuscado = pacienteId.trim();
        List<PrescripcionSeguimientoDTO> resultado = new ArrayList<>();
        for (PrescripcionSeguimientoDTO seguimiento : repositorioSeguimientos.values()) {
            if (seguimiento.getPacienteId().equals(pacienteBuscado)) {
                resultado.add(seguimiento);
            }
        }
        return resultado;
    }

    @Override
    public PrescripcionSeguimientoDTO cancelarSeguimiento(String seguimientoId, String motivo) {
        if (seguimientoId == null || seguimientoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del seguimiento es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio");
        }

        PrescripcionSeguimientoDTO seguimiento = consultarSeguimiento(seguimientoId);
        if ("CANCELADO".equals(seguimiento.getEstado())) {
            throw new IllegalStateException("La prescripción de seguimiento ya se encuentra cancelada");
        }

        String motivoLimpio = motivo.trim();
        seguimiento.setEstado("CANCELADO");
        seguimiento.setMotivoCancelacion(motivoLimpio);

        // Cancelación en cascada de las citas periódicas asociadas (ADR-016)
        for (String citaId : seguimiento.getCitasGeneradasIds()) {
            try {
                gestionCitasService.cancelarCita(
                        citaId, "Cancelación de seguimiento " + seguimiento.getSeguimientoId() + ": " + motivoLimpio
                );
            } catch (IllegalStateException ignored) {
                // Si la cita ya estaba cancelada individualmente, se continúa con el resto
            }
        }

        registrarAuditoria(seguimiento.getMedicoId(), "CANCELAR_SEGUIMIENTO:" + seguimiento.getSeguimientoId(), true);
        return seguimiento;
    }

    private CitaDTO programarCitaEnFranjaDisponible(String pacienteId, String medicoId,
                                                    String codigoPrueba, LocalDate fecha) {
        IllegalStateException ultimoConflicto = null;
        for (String franja : FRANJAS_SEGUIMIENTO) {
            try {
                return gestionCitasService.programarCita(pacienteId, medicoId, codigoPrueba, fecha, franja);
            } catch (IllegalStateException ex) {
                ultimoConflicto = ex;
            }
        }
        throw ultimoConflicto;
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "MEDICO";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}