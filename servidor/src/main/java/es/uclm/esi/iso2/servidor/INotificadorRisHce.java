package es.uclm.esi.iso2.servidor;

public interface INotificadorRisHce {
    void notificarRealizacionRis(String registroId, String citaId, String tipoPrueba);
    void registrarIncidenciaHce(String registroId, String pacienteId, String incidencias);
}