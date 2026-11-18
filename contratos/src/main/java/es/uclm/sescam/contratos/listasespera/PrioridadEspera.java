package es.uclm.sescam.contratos.listasespera;

public enum PrioridadEspera {
    URGENTE(1),
    PREFERENTE(2),
    ORDINARIA(3);

    private final int nivel;

    PrioridadEspera(int nivel) {
        this.nivel = nivel;
    }

    public int getNivel() {
        return nivel;
    }
}