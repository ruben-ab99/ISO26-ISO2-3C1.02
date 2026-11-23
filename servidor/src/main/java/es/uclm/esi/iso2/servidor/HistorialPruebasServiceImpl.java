package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.historial.HistorialPruebaDTO;
import es.uclm.sescam.contratos.historial.IHistorialPruebasService;
import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.informes.IInformesImagenesService;
import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HistorialPruebasServiceImpl implements IHistorialPruebasService {

    private final IRegistroPruebaService registroPruebaService;
    private final IInformesImagenesService informesImagenesService;
    private final IAuditService auditService;

    // Inyección de dependencias por constructor (ADR-012)
    public HistorialPruebasServiceImpl(IRegistroPruebaService registroPruebaService,
                                       IInformesImagenesService informesImagenesService,
                                       IAuditService auditService) {
        if (registroPruebaService == null) {
            throw new IllegalArgumentException("El servicio de registro de pruebas es obligatorio");
        }
        if (informesImagenesService == null) {
            throw new IllegalArgumentException("El servicio de informes e imágenes es obligatorio");
        }
        this.registroPruebaService = registroPruebaService;
        this.informesImagenesService = informesImagenesService;
        this.auditService = auditService;
    }

    @Override
    public List<HistorialPruebaDTO> consultarHistorial(String pacienteId) {
        String pacienteLimpio = validarPaciente(pacienteId);
        List<HistorialPruebaDTO> historial = construirHistorial(pacienteLimpio);
        registrarAuditoria(pacienteLimpio, "CONSULTAR_HISTORIAL_PRUEBAS", true);
        return historial;
    }

    @Override
    public List<HistorialPruebaDTO> consultarHistorialPorTipo(String pacienteId, String tipoPrueba) {
        String pacienteLimpio = validarPaciente(pacienteId);
        if (tipoPrueba == null || tipoPrueba.isBlank()) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio");
        }
        String tipoLimpio = tipoPrueba.trim();

        List<HistorialPruebaDTO> filtrado = new ArrayList<>();
        for (HistorialPruebaDTO prueba : construirHistorial(pacienteLimpio)) {
            if (tipoLimpio.equalsIgnoreCase(prueba.getTipoPrueba())) {
                filtrado.add(prueba);
            }
        }
        registrarAuditoria(pacienteLimpio, "CONSULTAR_HISTORIAL_POR_TIPO:" + tipoLimpio, true);
        return filtrado;
    }

    @Override
    public List<HistorialPruebaDTO> consultarHistorialPorFechas(String pacienteId, LocalDate desde, LocalDate hasta) {
        String pacienteLimpio = validarPaciente(pacienteId);
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        List<HistorialPruebaDTO> filtrado = new ArrayList<>();
        for (HistorialPruebaDTO prueba : construirHistorial(pacienteLimpio)) {
            LocalDate fecha = prueba.getFechaRealizacion().toLocalDate();
            if (!fecha.isBefore(desde) && !fecha.isAfter(hasta)) {
                filtrado.add(prueba);
            }
        }
        registrarAuditoria(pacienteLimpio, "CONSULTAR_HISTORIAL_POR_FECHAS", true);
        return filtrado;
    }

    @Override
    public HistorialPruebaDTO consultarDetallePrueba(String registroId, String pacienteId) {
        if (registroId == null || registroId.isBlank()) {
            throw new IllegalArgumentException("El identificador del registro es obligatorio");
        }
        String pacienteLimpio = validarPaciente(pacienteId);
        String registroLimpio = registroId.trim();

        RegistroPruebaDTO registro = registroPruebaService.consultarRegistroPrueba(registroLimpio);
        if (registro.getPacienteId() == null || !registro.getPacienteId().equals(pacienteLimpio)) {
            registrarAuditoria(pacienteLimpio, "ACCESO_DENEGADO_HISTORIAL:" + registroLimpio, false);
            throw new SecurityException("Acceso denegado: la prueba no pertenece al paciente");
        }

        HistorialPruebaDTO detalle = convertir(registro, obtenerEstudios(pacienteLimpio));
        registrarAuditoria(pacienteLimpio, "CONSULTAR_DETALLE_HISTORIAL:" + registroLimpio, true);
        return detalle;
    }

    private List<HistorialPruebaDTO> construirHistorial(String pacienteId) {
        List<RegistroPruebaDTO> registros = registroPruebaService.listarRegistrosPaciente(pacienteId);
        List<EstudioImagenDTO> estudios = obtenerEstudios(pacienteId);

        List<HistorialPruebaDTO> historial = new ArrayList<>();
        if (registros != null) {
            for (RegistroPruebaDTO registro : registros) {
                if (registro.isRealizada() && registro.getFechaRealizacion() != null) {
                    historial.add(convertir(registro, estudios));
                }
            }
        }
        historial.sort(Comparator.comparing(HistorialPruebaDTO::getFechaRealizacion).reversed());
        return historial;
    }

    private List<EstudioImagenDTO> obtenerEstudios(String pacienteId) {
        List<EstudioImagenDTO> estudios = informesImagenesService.listarEstudiosPaciente(pacienteId);
        return (estudios != null) ? estudios : new ArrayList<>();
    }

    private HistorialPruebaDTO convertir(RegistroPruebaDTO registro, List<EstudioImagenDTO> estudios) {
        boolean tieneEstudio = false;
        if (registro.getFechaRealizacion() != null) {
            LocalDate fechaRegistro = registro.getFechaRealizacion().toLocalDate();
            for (EstudioImagenDTO estudio : estudios) {
                if (fechaRegistro.equals(estudio.getFechaRealizacion())) {
                    tieneEstudio = true;
                    break;
                }
            }
        }
        return new HistorialPruebaDTO(
                registro.getRegistroId(),
                registro.getPacienteId(),
                registro.getMedicoId(),
                registro.getTipoPrueba(),
                registro.getFechaRealizacion(),
                registro.getIncidencias(),
                tieneEstudio
        );
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