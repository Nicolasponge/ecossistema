package ecossistema.model.entity;

import ecossistema.model.Biome;
import ecossistema.model.Vec2;
import ecossistema.model.World;

public class Wolf extends Animal {

    /** Refúgio: dentro da floresta o lobo só percebe coelho a até esta distância. */
    private static final double VISAO_NA_FLORESTA = 2.0;
    /** Holling III: com esta quantidade de coelhos por perto, o bote acerta metade das vezes. */
    private static final double MEIA_SATURACAO = 1.5;
    /** Depois de errar o bote, o lobo leva este tempo (s) para tentar de novo. */
    private static final double TEMPO_APOS_ERRAR = 1.5;
    /** Leslie-Gower: quantos coelhos no território cada lobo "precisa" para criar filhote. */
    private static final double COELHOS_POR_LOBO = 4.0;
    private static final double RAIO_TERRITORIO = 15.0;
    /** Uivo: até onde um lobo pronto para reproduzir acha parceiro. */
    private static final double RAIO_UIVO = 30.0;

    private double esperaBote;

    public Wolf(Vec2 pos) {
        super(pos);
    }

    @Override
    public EntityType type() { return EntityType.WOLF; }

    @Override
    protected void behave(World world, double dt) {
        esperaBote -= dt;
        if (!estaComFome() || esperaBote > 0) return; // barriga cheia não caça
        for (Entity e : world.neighbors(this, 0.7)) {
            if (e instanceof Rabbit coelho && coelho.isAlive()) {
                // Resposta funcional de Holling tipo III: com poucos coelhos por perto
                // o lobo "não pega o jeito" e erra mais. Chance = n² / (n² + h²).
                // Isso deixa sempre uma reserva de presas quando elas ficam raras.
                int n = world.countNear(pos, raioVisao(), EntityType.RABBIT);
                double chance = (double) n * n / (n * n + MEIA_SATURACAO * MEIA_SATURACAO);
                if (RNG.nextDouble() < chance) {
                    coelho.die();
                    comer(60);
                } else {
                    esperaBote = TEMPO_APOS_ERRAR;
                }
                return;
            }
        }
    }

    /** Refúgio: coelho dentro da floresta só é percebido de pertinho. */
    @Override
    protected boolean consegueVer(World world, Entity outro, double distancia) {
        if (outro instanceof Rabbit && world.biomeAt(outro.getPos()) == Biome.FOREST) {
            return distancia <= VISAO_NA_FLORESTA;
        }
        return true;
    }

    /**
     * Leslie-Gower: a capacidade do lobo é proporcional aos coelhos do território.
     * Chance = 1 - COELHOS_POR_LOBO * lobos / coelhos. Com lobo demais para pouco
     * coelho, eles param de reproduzir ANTES de acabar com a presa.
     */
    @Override
    protected double chanceCriaVingar(World world) {
        int lobos = world.countNear(pos, RAIO_TERRITORIO, EntityType.WOLF);
        int coelhos = world.countNear(pos, RAIO_TERRITORIO, EntityType.RABBIT);
        if (coelhos == 0) return 0;
        return 1.0 - COELHOS_POR_LOBO * lobos / coelhos;
    }

    /** Uivo: lobo pronto para reproduzir acha parceiro bem mais longe do que enxerga presa. */
    @Override
    protected double raioBuscaParceiro() { return RAIO_UIVO; }

    @Override
    protected boolean ehComida(Entity outro) { return outro instanceof Rabbit; }

    /** Floresta fechada atrapalha o lobo: vira o refúgio dos coelhos (ideia do experimento de Gause). */
    @Override
    protected double modificadorTerreno(Biome bioma) {
        return bioma == Biome.FOREST ? 0.5 : 1.0;
    }

    @Override
    protected Animal createOffspring(Vec2 pos) { return new Wolf(pos); }

    // predador: mais rápido, faro longo, reproduz devagar, vive ~3x mais que o coelho
    @Override protected double velocidadeMax()         { return 2.6; }
    @Override protected double raioVisao()             { return 6.0; }
    @Override protected double gastoPorSegundo()       { return 1.2; }
    @Override protected double idadeMaxima()           { return 300; }
    @Override protected double energiaInicial()        { return 90; }
    @Override protected double energiaParaReproduzir() { return 70; }
    @Override protected double custoReproducao()       { return 25; }
    @Override protected double cooldownReproducao()    { return 14; }
}
