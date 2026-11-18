package es.uclm.esi.iso2.servidor;

public interface INotificadorCitas {
    void notificarCambioCita(String pacienteId, String citaId, String tipoEvento, String detalle);
}