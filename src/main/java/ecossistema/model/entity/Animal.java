package ecossistema.model.entity;

import ecossistema.model.Biome;
import ecossistema.model.Vec2;
import ecossistema.model.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Base de todo animal.
 *
 * update() é um Template Method (GoF): a receita é igual para todos
 * (envelhecer, gastar energia, mover pelas "vontades", agir, reproduzir, morrer),
 * e cada espécie só preenche as partes que mudam: behave(), createOffspring()
 * e os seus números (velocidade, visão, etc).
 */
public abstract class Animal extends Entity {

    protected static final Random RNG = new Random();
    private static final double MAX_ENERGIA = 100;
    private static int contadorNascimentos = 0;
    private static final double ATRACAO_PARCEIRO = 2.0;
    private static final double ESPACO_PESSOAL = 1.6;   // ~ largura de uma sprite
    private static final double REPULSAO_PESSOAL = 4.0;
    private static final double PULOS_POR_TILE = 0.8;
    private static final double LIMITE_SACIEDADE = 85;
    private static final double RAIO_LOTACAO = 4.0;
    private static final double VELOCIDADE_NA_VELHICE = 0.6;

    // genes: multiplicadores herdados (1.0 = igual à média da espécie)
    private static int contadorId = 0;
    private static final double VARIACAO_INICIAL = 0.06; // diferença entre indivíduos do mapa inicial
    private static final double MUTACAO = 0.04;           // quanto o filhote pode sair diferente da média dos pais
    private static final double GENE_MIN = 0.6, GENE_MAX = 1.6;

    private final int id;
    private final Sex sexo;
    private double genVelocidade;
    private double genVisao;
    private int geracao = 1;
    private int idMae, idPai; // 0 = veio do mapa ou foi invocado pelo jogador
    private int filhos;
    private double energia;
    private double idade;
    private double cooldownReproducao;
    private Vec2 velocidade = Vec2.ZERO;
    private double anguloVagar;
    private double faseAndar = RNG.nextDouble() * Math.PI;

    protected Animal(Vec2 pos) {
        super(pos);
        this.id = ++contadorId;
        this.genVelocidade = limitarGene(1 + RNG.nextGaussian() * VARIACAO_INICIAL);
        this.genVisao = limitarGene(1 + RNG.nextGaussian() * VARIACAO_INICIAL);
        this.sexo = (contadorNascimentos++ % 2 == 0) ? Sex.MALE : Sex.FEMALE; // metade de cada
        this.energia = energiaInicial();
        this.cooldownReproducao = cooldownReproducao() * 0.5;
        this.anguloVagar = RNG.nextDouble() * Math.PI * 2;
    }

    // ---------------- Template Method ----------------

    @Override
    public final void update(World world, double dt) {
        idade += dt;
        energia -= gastoPorSegundo() * custoDosGenes() * dt;
        cooldownReproducao -= dt;

        mover(world, dt);
        behave(world, dt);
        tentarReproduzir(world);

        if (energia <= 0 || idade >= idadeMaxima()) die();
    }

    /** O que a espécie faz quando encosta em algo (ex: comer). */
    protected abstract void behave(World world, double dt);

    /** Creator (GRASP): cada espécie sabe criar o próprio filhote. */
    protected abstract Animal createOffspring(Vec2 pos);

    /** O que essa espécie come. */
    protected abstract boolean ehComida(Entity outro);

    // números de cada espécie
    protected abstract double velocidadeMax();
    protected abstract double raioVisao();
    protected abstract double gastoPorSegundo();
    protected abstract double idadeMaxima();
    protected abstract double energiaInicial();
    protected abstract double energiaParaReproduzir();
    protected abstract double custoReproducao();
    protected abstract double cooldownReproducao();

    // ---------------- Movimento: soma das vontades ----------------

    private void mover(World world, double dt) {
        Vec2 vontade = Vec2.ZERO;

        Vec2 bando = Vec2.ZERO;
        int tamanhoBando = 0;

        for (Entity outro : world.neighbors(this, visao())) {
            Vec2 paraOutro = outro.getPos().minus(pos);
            double d = paraOutro.length();
            if (!consegueVer(world, outro, d)) continue;

            // espaço pessoal: empurra forte quem está colado, some a partir de ESPACO_PESSOAL
            if (outro.type() == type() && d < ESPACO_PESSOAL) {
                double aperto = (ESPACO_PESSOAL - d) / ESPACO_PESSOAL;
                Vec2 afastar = d < 1e-6 ? new Vec2(RNG.nextDouble() - 0.5, RNG.nextDouble() - 0.5) : paraOutro.times(-1);
                vontade = vontade.plus(afastar.normalized().times(aperto * REPULSAO_PESSOAL));
            }

            if (!outro.isAttractive()) continue;
            double forca = world.getRelacoes().get(type(), outro.type());
            if (ehComida(outro)) forca *= fome(); // quanto mais fome, mais vontade de comer
            if (forca == 0) continue;

            // perto pesa mais, mas sem explodir quando está colado
            Vec2 puxao = paraOutro.normalized().times(forca / Math.max(d, 1.0));
            if (outro.type() == type()) {
                bando = bando.plus(puxao);   // mesma espécie: entra na média (boids)
                tamanhoBando++;
            } else {
                vontade = vontade.plus(puxao);
            }
        }

        // média, não soma: um bando grande não se puxa mais forte que um pequeno
        if (tamanhoBando > 0) vontade = vontade.plus(bando.times(1.0 / tamanhoBando));

        Animal parceiro = procurarParceiro(world);
        if (parceiro != null) {
            vontade = vontade.plus(parceiro.getPos().minus(pos).normalized().times(ATRACAO_PARCEIRO));
        }

        // um pouco de aleatório para não ficarem parados
        anguloVagar += (RNG.nextDouble() - 0.5) * 6 * dt;
        vontade = vontade.plus(new Vec2(Math.cos(anguloVagar), Math.sin(anguloVagar)).times(0.3));

        double vel = velocidadeMax() * genVelocidade * modificadorTerreno(world.biomeAt(pos)) * modificadorIdade();
        Vec2 alvo = vontade.normalized().times(vel);
        velocidade = velocidade.plus(alvo.minus(velocidade).times(Math.min(1, dt * 4))); // curva suave

        Vec2 proxima = pos.plus(velocidade.times(dt));
        if (world.isLand(proxima)) {
            pos = proxima;
            faseAndar += velocidade.length() * dt * PULOS_POR_TILE * Math.PI; // anima os pulinhos
        } else {
            velocidade = velocidade.times(-0.5);
            anguloVagar += Math.PI;
        }
    }

    // ---------------- Reprodução ----------------

    /**
     * Só o parceiro pronto mais perto puxa (antes, todos os prontos puxavam e formava bolo).
     * O raio de busca pode ser maior que a visão: lobos se acham pelo uivo e pelo cheiro.
     */
    private Animal procurarParceiro(World world) {
        if (!prontoParaReproduzir()) return null;
        Animal melhor = null;
        double melhorDist = Double.MAX_VALUE;
        for (Entity outro : world.neighbors(this, raioBuscaParceiro())) {
            if (outro instanceof Animal a && a.type() == type() && a.sexo != sexo && a.prontoParaReproduzir()) {
                double d = a.getPos().minus(pos).length();
                if (d < melhorDist) {
                    melhor = a;
                    melhorDist = d;
                }
            }
        }
        return melhor;
    }

    /** Até onde o animal percebe um parceiro pronto. Padrão: o raio de visão. */
    protected double raioBuscaParceiro() {
        return visao();
    }

    private boolean prontoParaReproduzir() {
        return cooldownReproducao <= 0 && energia >= energiaParaReproduzir();
    }

    private void tentarReproduzir(World world) {
        if (sexo != Sex.FEMALE || !prontoParaReproduzir()) return;

        for (Entity outro : world.neighbors(this, 1.5)) {
            if (outro instanceof Animal par
                    && par.type() == type()
                    && par.sexo == Sex.MALE
                    && par.prontoParaReproduzir()) {

                cooldownReproducao = cooldownReproducao();
                par.cooldownReproducao = cooldownReproducao();

                if (RNG.nextDouble() >= chanceCriaVingar(world)) return; // não vingou: perdem a vez, mas não a energia

                Animal filhote = createOffspring(pos);
                filhote.herdar(this, par);
                world.spawnLater(filhote);
                energia -= custoReproducao();
                par.energia -= custoReproducao();
                return;
            }
        }
    }

    /**
     * Chance de um acasalamento gerar filhote. Padrão: crescimento logístico,
     * quanto mais lotado o lugar, menor a chance: 1 - lotacao / capacidade.
     */
    protected double chanceCriaVingar(World world) {
        int lotacao = world.countNear(pos, raioLotacao(), type());
        return 1.0 - lotacao / capacidadeLocal();
    }

    /**
     * Capacidade de suporte local: quantos da mesma espécie cabem em raioLotacao()
     * antes de parar de nascer filhote. Padrão: sem limite. Cada espécie pode sobrescrever.
     */
    protected double capacidadeLocal() {
        return Double.POSITIVE_INFINITY;
    }

    /** Tamanho da "vizinhança" usada para medir a lotação. */
    protected double raioLotacao() {
        return RAIO_LOTACAO;
    }

    // ---------------- Genes (herança) ----------------

    /**
     * Herança: o filhote recebe a média dos pais mais uma mutação pequena.
     * Quem é mais rápido escapa (ou caça) melhor e deixa mais filhos,
     * então a média da população pode mudar com o tempo: seleção natural.
     */
    private void herdar(Animal mae, Animal pai) {
        genVelocidade = limitarGene((mae.genVelocidade + pai.genVelocidade) / 2 + RNG.nextGaussian() * MUTACAO);
        genVisao = limitarGene((mae.genVisao + pai.genVisao) / 2 + RNG.nextGaussian() * MUTACAO);
        geracao = Math.max(mae.geracao, pai.geracao) + 1;
        idMae = mae.id;
        idPai = pai.id;
        mae.filhos++;
        pai.filhos++;
    }

    /**
     * Ser rápido e enxergar longe tem um preço: gasta mais energia.
     * Sem esse custo, a evolução só empurraria tudo para cima sem parar.
     * Com os genes em 1.0 o custo é exatamente 1.0 (igual a antes).
     */
    private double custoDosGenes() {
        return (0.6 + 0.4 * genVelocidade * genVelocidade) * (0.85 + 0.15 * genVisao);
    }

    private static double limitarGene(double g) {
        return Math.max(GENE_MIN, Math.min(GENE_MAX, g));
    }

    /** Raio de visão deste indivíduo (o da espécie vezes o gene). */
    protected double visao() {
        return raioVisao() * genVisao;
    }

    /** Se este animal percebe o outro (para as vontades). Padrão: vê tudo no raio de visão. */
    protected boolean consegueVer(World world, Entity outro, double distancia) {
        return true;
    }

    /** Quanto o terreno atrapalha. Padrão: nada. Cada espécie pode sobrescrever. */
    protected double modificadorTerreno(Biome bioma) {
        return 1.0;
    }

    /**
     * Velho anda mais devagar: 100% até a metade da vida,
     * depois cai em linha reta até VELOCIDADE_NA_VELHICE no fim.
     */
    protected double modificadorIdade() {
        double meiaVida = idadeMaxima() / 2;
        if (idade <= meiaVida) return 1.0;
        double t = Math.min(1.0, (idade - meiaVida) / meiaVida);
        return 1.0 - t * (1.0 - VELOCIDADE_NA_VELHICE);
    }

    /** 0 = barriga cheia, 1 = morrendo de fome (nunca abaixo de 0.15). */
    protected double fome() {
        return Math.max(0.15, 1 - energia / MAX_ENERGIA);
    }

    protected boolean estaComFome() {
        return energia < LIMITE_SACIEDADE;
    }

    protected void comer(double quantidade) {
        energia = Math.min(energia + quantidade, MAX_ENERGIA);
    }

    /**
     * Para a população inicial do mapa: idades misturadas (entre filhote e 30% da vida),
     * senão todos morrem de velhice no mesmo instante.
     */
    public void sortearIdadeInicial(Random rng) {
        idade = rng.nextDouble() * idadeMaxima() * 0.3;
    }

    public int getId() { return id; }
    public Sex getSexo() { return sexo; }
    public double getGenVelocidade() { return genVelocidade; }
    public double getGenVisao() { return genVisao; }
    public int getGeracao() { return geracao; }
    public int getIdMae() { return idMae; }
    public int getIdPai() { return idPai; }
    public int getFilhos() { return filhos; }
    public double getIdadeMaxima() { return idadeMaxima(); }
    /** Velocidade máxima deste indivíduo agora (espécie x gene x idade), em tiles/s. */
    public double getVelocidadeAtual() { return velocidadeMax() * genVelocidade * modificadorIdade(); }
    public double getVisao() { return visao(); }

    /** Os números da espécie, para exportar (I/O). LinkedHashMap mantém a ordem. */
    public Map<String, Double> parametrosDaEspecie() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("velocidade_max", velocidadeMax());
        m.put("raio_visao", raioVisao());
        m.put("gasto_por_segundo", gastoPorSegundo());
        m.put("idade_maxima", idadeMaxima());
        m.put("energia_inicial", energiaInicial());
        m.put("energia_para_reproduzir", energiaParaReproduzir());
        m.put("custo_reproducao", custoReproducao());
        m.put("cooldown_reproducao", cooldownReproducao());
        m.put("mutacao_genes", MUTACAO);
        return m;
    }
    public double getEnergia() { return energia; }
    public double getIdade() { return idade; }
    public Vec2 getVelocidade() { return velocidade; }

    /** Altura do pulinho entre 0 (no chão) e 1 (no alto). A View usa isso para animar. */
    public double getAlturaPulo() {
        return Math.abs(Math.sin(faseAndar));
    }
}
