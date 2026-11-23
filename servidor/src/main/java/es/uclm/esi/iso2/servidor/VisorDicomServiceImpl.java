package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioDicomDTO;
import es.uclm.sescam.contratos.informes.IVisorDicomService;
import es.uclm.sescam.contratos.security.IAuditService;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class VisorDicomServiceImpl implements IVisorDicomService {

    private final IAuditService auditService;

    public VisorDicomServiceImpl() {
        this(new AuditServiceImpl());
    }

    public VisorDicomServiceImpl(IAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public EstudioDicomDTO obtenerEstudioDicom(String estudioId, String usuarioId) {
        
        if (estudioId == null || estudioId.isBlank()) {
            throw new IllegalArgumentException("El ID del estudio DICOM es obligatorio");
        }
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        if (!verificarConformidadDicom(estudioId)) {
            throw new IllegalArgumentException("El estudio no cumple con el estándar DICOM: " + estudioId);
        }

        if (auditService != null) {
            auditService.registrarEvento(usuarioId.trim(), "VISUALIZAR_ESTUDIO_DICOM:" + estudioId.trim(), true, "127.0.0.1");
        }

        // Simulación de recuperación desde el servidor PACS (CU-27)
        return new EstudioDicomDTO(
                estudioId,
                "PAC-01",
                "TAC",
                "Estudio radiológico de alta resolución",
                List.of("SERIE-001", "SERIE-002")
        );
    }

    @Override
    public byte[] obtenerImagenDicom(String estudioId, String serieId, String instanciaId) {
        if (!verificarConformidadDicom(estudioId)) {
            throw new IllegalArgumentException("Estudio no conforme con DICOM");
        }
        if (serieId == null || serieId.isBlank() || instanciaId == null || instanciaId.isBlank()) {
            throw new IllegalArgumentException("La serie y la instancia son obligatorias");
        }

        // Simulamos la cabecera estándar DICOM + bytes de la imagen
        String payload = "DICM|" + estudioId + "|" + serieId + "|" + instanciaId;
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean verificarConformidadDicom(String estudioId) {
        // Estándar definido en el proyecto: los identificadores DICOM empiezan por "DICOM-"
        return estudioId != null && estudioId.startsWith("DICOM-");
    }
}