package ecossistema.controller.tools;

import ecossistema.model.Biome;
import ecossistema.model.World;

/** Pinta um bioma num círculo em volta do mouse. */
public class PaintBiomeTool implements Tool {

    private final Biome bioma;

    public PaintBiomeTool(Biome bioma) {
        this.bioma = bioma;
    }

    public Biome getBioma() {
        return bioma;
    }

    @Override
    public String getNome() {
        String n = bioma.getNome();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    public void apply(World world, double x, double y, int raio) {
        pintar(world, (int) x, (int) y, raio);
        world.applyCoastRules();
    }

    protected void pintar(World world, int cx, int cy, int raio) {
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dy = -raio; dy <= raio; dy++) {
                if (dx * dx + dy * dy <= raio * raio + raio) world.setBiome(cx + dx, cy + dy, bioma);
            }
        }
    }
}
