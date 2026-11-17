package ecossistema.model.entity;

import ecossistema.model.Vec2;
import ecossistema.model.World;

/** Tudo que existe em cima do mapa. */
public abstract class Entity {

    protected Vec2 pos;
    private boolean viva = true;

    protected Entity(Vec2 pos) {
        this.pos = pos;
    }

    public abstract EntityType type();

    public abstract void update(World world, double dt);

    /** Se outros animais devem sentir atração/repulsão por esta entidade agora. */
    public boolean isAttractive() {
        return true;
    }

    public Vec2 getPos() { return pos; }
    public boolean isAlive() { return viva; }
    public void die() { viva = false; }
}
