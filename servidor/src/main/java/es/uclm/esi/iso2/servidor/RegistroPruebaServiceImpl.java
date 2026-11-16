package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.informes.IRegistroPruebaService;
import es.uclm.sescam.contratos.informes.RegistroPruebaDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegistroPruebaServiceImpl implements IRegistroPruebaService {

    private final Map<String, RegistroPruebaDTO> repositorioRegistros = new HashMap<>();
    private final INotificadorRisHce notificadorExterno;

    public RegistroPruebaServiceImpl(INotificadorRisHce notificadorExterno) {
        if (notificadorExterno == null) {
            throw new IllegalArgumentException("El conector RIS/HCE no puede ser nulo");
        }
        this.notificadorExterno = notificadorExterno;
    }

    @Override
    public RegistroPruebaDTO registrarRealizacionPrueba(String citaId, String pacienteId, String medicoId,
                                                        String tipoPrueba, String observaciones, String incidencias) {
        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El ID de la cita es obligatorio");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio");
        }
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El ID del médico es obligatorio");
        }
        if (tipoPrueba == null || tipoPrueba.isBlank()) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio");
        }

        String nuevoId = "REG-" + UUID.randomUUID().toString().substring(0, 8);
        String obsLimpia = (observaciones != null) ? observaciones : "";
        String incLimpia = (incidencias != null) ? incidencias : "";

        RegistroPruebaDTO registro = new RegistroPruebaDTO(
                nuevoId,
                citaId,
                pacienteId,
                medicoId,
                tipoPrueba,
                obsLimpia,
                incLimpia,
                true,
                LocalDateTime.now()
        );

        repositorioRegistros.put(nuevoId, registro);
        notificadorExterno.notificarRealizacionRis(nuevoId, citaId, tipoPrueba);

        if (!incLimpia.isBlank()) {
            notificadorExterno.registrarIncidenciaHce(nuevoId, pacienteId, incLimpia);
        }

        return registro;
    }

    @Override
    public RegistroPruebaDTO consultarRegistroPrueba(String registroId) {
        if (registroId == null || registroId.isBlank()) {
            throw new IllegalArgumentException("El ID del registro es obligatorio");
        }
        RegistroPruebaDTO registro = repositorioRegistros.get(registroId);
        if (registro == null) {
            throw new IllegalStateException("No existe ningún registro de prueba con ID: " + registroId);
        }
        return registro;
    }
}