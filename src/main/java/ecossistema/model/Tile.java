package ecossistema.model;

/** Um quadradinho do mapa. */
public class Tile {
    private Biome biome;

    public Tile(Biome biome) {
        this.biome = biome;
    }

    public Biome getBiome() { return biome; }
    public void setBiome(Biome biome) { this.biome = biome; }
}
