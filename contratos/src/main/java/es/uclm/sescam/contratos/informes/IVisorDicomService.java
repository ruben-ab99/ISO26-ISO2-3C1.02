package es.uclm.sescam.contratos.informes;

public interface IVisorDicomService {
    EstudioDicomDTO obtenerEstudioDicom(String estudioId, String usuarioId);
    byte[] obtenerImagenDicom(String estudioId, String serieId, String instanciaId);
    boolean verificarConformidadDicom(String estudioId);
}