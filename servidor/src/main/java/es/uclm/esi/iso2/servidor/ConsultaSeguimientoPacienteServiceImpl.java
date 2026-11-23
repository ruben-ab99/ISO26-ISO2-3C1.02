package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.IConsultaSeguimientoPacienteService;
import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.util.ArrayList;
import java.util.List;

public class ConsultaSeguimientoPacienteServiceImpl implements IConsultaSeguimientoPacienteService {

    private final IPrescripcionSeguimientoService prescripcionSeguimientoService;
    private final IAuditService auditService;

    public ConsultaSeguimientoPacienteServiceImpl(IPrescripcionSeguimientoService prescripcionSeguimientoService) {
        this(prescripcionSeguimientoService, new AuditServiceImpl());
    }

    public ConsultaSeguimientoPacienteServiceImpl(IPrescripcionSeguimientoService prescripcionSeguimientoService,
                                                  IAuditService auditService) {
        if (prescripcionSeguimientoService == null) {
            throw new IllegalArgumentException("El servicio de prescripciones de seguimiento es obligatorio");
        }
        this.prescripcionSeguimientoService = prescripcionSeguimientoService;
        this.auditService = auditService;
    }

    @Override
    public List<PrescripcionSeguimientoDTO> listarMisSeguimientos(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteLimpio = pacienteId.trim();
        List<PrescripcionSeguimientoDTO> seguimientos =
                prescripcionSeguimientoService.listarSeguimientosPaciente(pacienteLimpio);

        registrarAuditoria(pacienteLimpio, "CONSULTAR_SEGUIMIENTOS_PACIENTE", true);
        return (seguimientos != null) ? seguimientos : new ArrayList<>();
    }

    @Override
    public List<PrescripcionSeguimientoDTO> listarMisSeguimientosActivos(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteLimpio = pacienteId.trim();
        List<PrescripcionSeguimientoDTO> todos =
                prescripcionSeguimientoService.listarSeguimientosPaciente(pacienteLimpio);

        List<PrescripcionSeguimientoDTO> activos = new ArrayList<>();
        if (todos != null) {
            for (PrescripcionSeguimientoDTO seguimiento : todos) {
                if ("ACTIVO".equalsIgnoreCase(seguimiento.getEstado())) {
                    activos.add(seguimiento);
                }
            }
        }

        registrarAuditoria(pacienteLimpio, "CONSULTAR_SEGUIMIENTOS_ACTIVOS_PACIENTE", true);
        return activos;
    }

    @Override
    public PrescripcionSeguimientoDTO consultarDetalleMiSeguimiento(String seguimientoId, String pacienteId) {
        if (seguimientoId == null || seguimientoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del seguimiento es obligatorio");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }

        String seguimientoLimpio = seguimientoId.trim();
        String pacienteLimpio = pacienteId.trim();

        PrescripcionSeguimientoDTO seguimiento =
                prescripcionSeguimientoService.consultarSeguimiento(seguimientoLimpio);

        if (seguimiento.getPacienteId() == null || !seguimiento.getPacienteId().equals(pacienteLimpio)) {
            registrarAuditoria(pacienteLimpio, "ACCESO_DENEGADO_SEGUIMIENTO:" + seguimientoLimpio, false);
            throw new SecurityException("Acceso denegado: la prescripción de seguimiento no pertenece al paciente");
        }

        registrarAuditoria(pacienteLimpio, "CONSULTAR_DETALLE_SEGUIMIENTO:" + seguimientoLimpio, true);
        return seguimiento;
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "PACIENTE";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}