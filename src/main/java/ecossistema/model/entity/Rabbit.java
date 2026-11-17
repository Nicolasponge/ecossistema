package ecossistema.model.entity;

import ecossistema.model.Vec2;
import ecossistema.model.World;

public class Rabbit extends Animal {

    public Rabbit(Vec2 pos) {
        super(pos);
    }

    @Override
    public EntityType type() { return EntityType.RABBIT; }

    @Override
    protected void behave(World world, double dt) {
        if (!estaComFome()) return;
        for (Entity e : world.neighbors(this, 0.7)) {
            if (e instanceof Bush arbusto && arbusto.comerFruta()) {
                comer(30);
                return;
            }
        }
    }

    /** Crescimento logístico: com 10 coelhos num raio de 4 tiles, não nasce mais filhote ali. */
    private static final double CAPACIDADE = 10;

    @Override
    protected double capacidadeLocal() { return CAPACIDADE; }

    @Override
    protected boolean ehComida(Entity outro) { return outro instanceof Bush; }

    @Override
    protected Animal createOffspring(Vec2 pos) { return new Rabbit(pos); }

    // presa: reproduz rápido e vive de fruta
    @Override protected double velocidadeMax()         { return 2.0; }
    @Override protected double raioVisao()             { return 5.0; }
    @Override protected double gastoPorSegundo()       { return 2.5; }
    @Override protected double idadeMaxima()           { return 100; }
    @Override protected double energiaInicial()        { return 70; }
    @Override protected double energiaParaReproduzir() { return 50; }
    @Override protected double custoReproducao()       { return 20; }
    @Override protected double cooldownReproducao()    { return 6; }
}
