package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import es.uclm.sescam.contratos.informes.IInformesImagenesService;
import es.uclm.sescam.contratos.informes.InformeDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InformesImagenesServiceImpl implements IInformesImagenesService {

    private final Map<String, InformeDTO> repositorioInformes = new HashMap<>();
    private final ISistemaExternoRisPacs sistemaExterno;

    public InformesImagenesServiceImpl(ISistemaExternoRisPacs sistemaExterno) {
        if (sistemaExterno == null) {
            throw new IllegalArgumentException("El conector RIS/PACS/HCE no puede ser nulo");
        }
        this.sistemaExterno = sistemaExterno;
    }

    @Override
    public InformeDTO crearInforme(String solicitudId, String pacienteId, String medicoId, String contenidoClinico) {
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("El ID de solicitud es obligatorio");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID de paciente es obligatorio");
        }
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El ID del médico es obligatorio");
        }
        if (contenidoClinico == null || contenidoClinico.isBlank()) {
            throw new IllegalArgumentException("El contenido clínico del informe no puede estar vacío");
        }

        String nuevoId = "INF-" + UUID.randomUUID().toString().substring(0, 8);
        List<EstudioImagenDTO> estudios = sistemaExterno.obtenerEstudiosDesdePacs(pacienteId);

        InformeDTO informe = new InformeDTO(
                nuevoId,
                solicitudId,
                pacienteId,
                medicoId,
                contenidoClinico,
                LocalDateTime.now(),
                estudios
        );

        repositorioInformes.put(nuevoId, informe);
        sistemaExterno.sincronizarInformeConHce(nuevoId, pacienteId, contenidoClinico);
        return informe;
    }

    @Override
    public InformeDTO consultarInforme(String informeId, String usuarioId) {
        if (informeId == null || informeId.isBlank()) {
            throw new IllegalArgumentException("El ID de informe es obligatorio");
        }
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El usuario que consulta debe estar identificado");
        }

        InformeDTO informe = repositorioInformes.get(informeId);
        if (informe == null) {
            throw new IllegalStateException("No existe ningún informe con ID: " + informeId);
        }
        return informe;
    }

    @Override
    public List<EstudioImagenDTO> listarEstudiosPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID de paciente es obligatorio");
        }
        return sistemaExterno.obtenerEstudiosDesdePacs(pacienteId);
    }
}