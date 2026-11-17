package ecossistema.controller.tools;

import ecossistema.model.Vec2;
import ecossistema.model.World;
import ecossistema.model.entity.EntityType;

/**
 * Remove seres dentro do pincel sem mexer no terreno.
 * tipo == null remove tudo; senão só aquele tipo (ex: só arbustos).
 * Mais uma Strategy: o controller nem sabe que ela existe, só chama apply().
 */
public class ClearEntitiesTool implements Tool {

    private final EntityType tipo;

    public ClearEntitiesTool(EntityType tipo) {
        this.tipo = tipo;
    }

    @Override
    public String getNome() {
        if (tipo == null) return "Tudo";
        return switch (tipo) {
            case RABBIT -> "Coelhos";
            case WOLF -> "Lobos";
            case BUSH -> "Arbustos";
        };
    }

    @Override
    public void apply(World world, double x, double y, int raio) {
        // mesmo círculo que o pincel de terreno pinta (centro do tile, raio + meio tile)
        Vec2 centro = new Vec2((int) x + 0.5, (int) y + 0.5);
        world.removeEntities(centro, raio + 0.5, tipo);
    }
}
