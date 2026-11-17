package ecossistema.model.entity;

public enum EntityType {
    RABBIT("coelho"),
    WOLF("lobo"),
    BUSH("arbusto");

    private final String nome;

    EntityType(String nome) { this.nome = nome; }

    public String getNome() { return nome; }
}
