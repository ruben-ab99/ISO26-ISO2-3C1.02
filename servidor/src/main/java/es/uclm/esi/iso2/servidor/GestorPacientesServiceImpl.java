package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.pacientes.IGestorPacientesService;
import es.uclm.sescam.contratos.pacientes.PacienteDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GestorPacientesServiceImpl implements IGestorPacientesService {

    private final Map<String, PacienteDTO> repositorioPacientes = new HashMap<>();
    private final IGestorRolesService gestorRoles;

    public GestorPacientesServiceImpl(IGestorRolesService gestorRoles) {
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El servicio de gestión de roles no puede ser nulo");
        }
        this.gestorRoles = gestorRoles;
    }

    private void verificarPermisoAdmision(String tokenSSO) {
        if (!gestorRoles.tieneAcceso(tokenSSO, Rol.ADMISION)) {
            throw new SecurityException("Acceso denegado: Se requiere rol ADMISION o ADMINISTRADOR para gestionar pacientes.");
        }
    }

    @Override
    public PacienteDTO registrarPaciente(PacienteDTO nuevoPaciente, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (nuevoPaciente == null) {
            throw new IllegalArgumentException("Los datos del paciente son obligatorios");
        }
        if (nuevoPaciente.getDni() == null || nuevoPaciente.getDni().isBlank()) {
            throw new IllegalArgumentException("El DNI del paciente es obligatorio");
        }
        if (nuevoPaciente.getNombre() == null || nuevoPaciente.getNombre().isBlank() ||
            nuevoPaciente.getApellidos() == null || nuevoPaciente.getApellidos().isBlank()) {
            throw new IllegalArgumentException("Nombre y apellidos son obligatorios");
        }

        boolean existeDni = repositorioPacientes.values().stream()
                .anyMatch(p -> p.isActivo() && p.getDni().equalsIgnoreCase(nuevoPaciente.getDni().trim()));
        if (existeDni) {
            throw new IllegalStateException("Ya existe un paciente activo registrado con el DNI: " + nuevoPaciente.getDni());
        }

        String nuevoId = "PAC-" + UUID.randomUUID().toString().substring(0, 8);
        PacienteDTO pacienteGuardado = new PacienteDTO(
                nuevoId,
                nuevoPaciente.getDni().trim().toUpperCase(),
                nuevoPaciente.getCip(),
                nuevoPaciente.getNombre().trim(),
                nuevoPaciente.getApellidos().trim(),
                nuevoPaciente.getFechaNacimiento(),
                nuevoPaciente.getTelefono(),
                true,
                null
        );

        repositorioPacientes.put(nuevoId, pacienteGuardado);
        return pacienteGuardado;
    }

    @Override
    public PacienteDTO modificarPaciente(String pacienteId, PacienteDTO datosActualizados, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio");
        }
        if (datosActualizados == null) {
            throw new IllegalArgumentException("Los datos actualizados no pueden ser nulos");
        }

        PacienteDTO existente = repositorioPacientes.get(pacienteId);
        if (existente == null) {
            throw new IllegalStateException("No existe ningún paciente con ID: " + pacienteId);
        }
        if (!existente.isActivo()) {
            throw new IllegalStateException("No se puede modificar un paciente dado de baja: " + pacienteId);
        }

        if (datosActualizados.getNombre() != null && !datosActualizados.getNombre().isBlank()) {
            existente.setNombre(datosActualizados.getNombre().trim());
        }
        if (datosActualizados.getApellidos() != null && !datosActualizados.getApellidos().isBlank()) {
            existente.setApellidos(datosActualizados.getApellidos().trim());
        }
        if (datosActualizados.getTelefono() != null) {
            existente.setTelefono(datosActualizados.getTelefono().trim());
        }
        if (datosActualizados.getFechaNacimiento() != null) {
            existente.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        }
        if (datosActualizados.getCip() != null && !datosActualizados.getCip().isBlank()) {
            existente.setCip(datosActualizados.getCip().trim());
        }

        return existente;
    }

    @Override
    public boolean darBajaPaciente(String pacienteId, String motivoBaja, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio");
        }
        if (motivoBaja == null || motivoBaja.isBlank()) {
            throw new IllegalArgumentException("Debe indicarse un motivo para la baja del paciente");
        }

        PacienteDTO existente = repositorioPacientes.get(pacienteId);
        if (existente == null) {
            throw new IllegalStateException("No existe ningún paciente con ID: " + pacienteId);
        }
        if (!existente.isActivo()) {
            return false; // Ya estaba dado de baja
        }

        existente.setActivo(false);
        existente.setMotivoBaja(motivoBaja.trim());
        return true;
    }

    @Override
    public PacienteDTO consultarPaciente(String pacienteId, String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para consultar datos de pacientes");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio");
        }

        PacienteDTO existente = repositorioPacientes.get(pacienteId);
        if (existente == null) {
            throw new IllegalStateException("No existe ningún paciente con ID: " + pacienteId);
        }
        return existente;
    }
}