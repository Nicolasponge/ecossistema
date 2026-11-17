package ecossistema.model.entity;

import ecossistema.model.Vec2;
import ecossistema.model.World;

/** Arbusto: não se mexe, dá fruta, e a fruta volta depois de um tempo. */
public class Bush extends Entity {

    private static final double TEMPO_PARA_REBROTAR = 5.0;

    private boolean temFruta = true;
    private double timer;

    public Bush(Vec2 pos) {
        super(pos);
    }

    @Override
    public EntityType type() { return EntityType.BUSH; }

    @Override
    public void update(World world, double dt) {
        if (!temFruta) {
            timer += dt;
            if (timer >= TEMPO_PARA_REBROTAR) {
                temFruta = true;
                timer = 0;
            }
        }
    }

    /** Sem fruta, o arbusto não atrai ninguém. */
    @Override
    public boolean isAttractive() { return temFruta; }

    public boolean temFruta() { return temFruta; }

    public boolean comerFruta() {
        if (!temFruta) return false;
        temFruta = false;
        return true;
    }
}
