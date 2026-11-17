package ecossistema.controller.tools;

import ecossistema.model.Biome;
import ecossistema.model.World;

/** Apagar = pintar água, e quem estava em cima morre. */
public class EraseTool extends PaintBiomeTool {

    public EraseTool() {
        super(Biome.WATER);
    }

    @Override
    public String getNome() {
        return "Apagar";
    }

    @Override
    public void apply(World world, double x, double y, int raio) {
        pintar(world, (int) x, (int) y, raio);
        world.killEntitiesInWater();
        world.applyCoastRules();
    }
}
