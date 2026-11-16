package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.EstudioImagenDTO;
import java.util.List;

public interface ISistemaExternoRisPacs {
    List<EstudioImagenDTO> obtenerEstudiosDesdePacs(String pacienteId);
    void sincronizarInformeConHce(String informeId, String pacienteId, String contenidoClinico);
}