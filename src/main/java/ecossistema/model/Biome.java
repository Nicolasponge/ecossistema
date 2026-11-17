package ecossistema.model;

public enum Biome {
    WATER("água", false),
    BEACH("praia", true),
    PLAINS("planície", true),
    FOREST("floresta", true);

    private final String nome;
    private final boolean terra;

    Biome(String nome, boolean terra) {
        this.nome = nome;
        this.terra = terra;
    }

    public String getNome() { return nome; }
    public boolean isLand() { return terra; }
}
