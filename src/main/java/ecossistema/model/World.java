package ecossistema.model;

import ecossistema.exception.InvalidPlacementException;
import ecossistema.model.entity.Animal;
import ecossistema.model.entity.Entity;
import ecossistema.model.entity.EntityType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** O mundo: o mapa de tiles e todas as entidades vivas. */
public class World {

    public static final int MAX_ENTIDADES = 2000;
    private static final int DIST_MIN_FLORESTA = 4;

    private final int largura, altura;
    private final Tile[][] tiles;
    private final int[][] larguraPraia;

    private final List<Entity> entidades = new ArrayList<>();
    private final List<Entity> paraAdicionar = new ArrayList<>();
    private final List<WorldObserver> observadores = new ArrayList<>();

    private RelationTable relacoes = new RelationTable();
    private double tempo;
    private double timerStats;
    private boolean terrenoMudou = true;

    private static final int TAMANHO_BALDE = 4;
    private final int colunasGrade, linhasGrade;
    private final List<Entity>[][] grade;

    @SuppressWarnings("unchecked")
    public World(int largura, int altura) {
        this.largura = largura;
        this.altura = altura;
        colunasGrade = (largura + TAMANHO_BALDE - 1) / TAMANHO_BALDE;
        linhasGrade = (altura + TAMANHO_BALDE - 1) / TAMANHO_BALDE;
        grade = new List[colunasGrade][linhasGrade];
        for (int i = 0; i < colunasGrade; i++)
            for (int j = 0; j < linhasGrade; j++) grade[i][j] = new ArrayList<>();
        tiles = new Tile[largura][altura];
        larguraPraia = new int[largura][altura];
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                tiles[x][y] = new Tile(Biome.WATER);
                larguraPraia[x][y] = 2;
            }
        }
    }

    // ---------------- Loop ----------------

    public void update(double dt) {
        tempo += dt;
        reconstruirGrade();

        for (Entity e : new ArrayList<>(entidades)) {
            if (e.isAlive()) e.update(this, dt);
        }
        entidades.removeIf(e -> !e.isAlive());

        for (Entity e : paraAdicionar) {
            if (entidades.size() < MAX_ENTIDADES && isLand(e.getPos())) entidades.add(e);
        }
        paraAdicionar.clear();

        timerStats += dt;
        if (timerStats >= 0.5) {
            timerStats = 0;
            notificarObservadores();
        }
    }

    // ---------------- Entidades ----------------

    /** Usado pelo jogador e pelo gerador. Lança exceção se o lugar for inválido. */
    public void spawn(Entity e) throws InvalidPlacementException {
        if (!isLand(e.getPos())) {
            throw new InvalidPlacementException("Não dá pra colocar " + e.type().getNome() + " na água.");
        }
        if (entidades.size() >= MAX_ENTIDADES) {
            throw new InvalidPlacementException("O mundo está cheio (" + MAX_ENTIDADES + " entidades).");
        }
        entidades.add(e);
        colocarNaGrade(e);
    }

    /** Usado durante o update (ex: filhotes), para não mexer na lista enquanto ela é percorrida. */
    public void spawnLater(Entity e) {
        paraAdicionar.add(e);
    }

    public List<Entity> neighbors(Entity eu, double raio) {
        return neighbors(eu.getPos(), raio, eu);
    }

    /**
     * Quem está a até "raio" de um ponto. Em vez de testar todas as entidades,
     * olha só os baldes da grade que o círculo encosta.
     */
    public List<Entity> neighbors(Vec2 centro, double raio, Entity ignorar) {
        List<Entity> resultado = new ArrayList<>();
        double r2 = raio * raio;
        int x0 = balde(centro.x() - raio, colunasGrade), x1 = balde(centro.x() + raio, colunasGrade);
        int y0 = balde(centro.y() - raio, linhasGrade),  y1 = balde(centro.y() + raio, linhasGrade);
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (Entity e : grade[bx][by]) {
                    if (e == ignorar || !e.isAlive()) continue;
                    double dx = e.getPos().x() - centro.x();
                    double dy = e.getPos().y() - centro.y();
                    if (dx * dx + dy * dy <= r2) resultado.add(e);
                }
            }
        }
        return resultado;
    }

    /** Quantas entidades de um tipo existem perto de um ponto. */
    public int countNear(Vec2 centro, double raio, EntityType tipo) {
        int c = 0;
        for (Entity e : neighbors(centro, raio, null)) if (e.type() == tipo) c++;
        return c;
    }

    // ---------------- Grade espacial ----------------
    // O mapa é dividido em baldes de TAMANHO_BALDE x TAMANHO_BALDE tiles.
    // Cada entidade fica no balde onde está; para achar vizinhos basta olhar os baldes ao redor.
    // Os animais andam menos de 1 tile entre uma reconstrução e outra, então a grade
    // pode ficar levemente desatualizada dentro de um quadro sem problema.

    private int balde(double coordenada, int limite) {
        int b = (int) Math.floor(coordenada / TAMANHO_BALDE);
        return Math.max(0, Math.min(limite - 1, b));
    }

    private void colocarNaGrade(Entity e) {
        grade[balde(e.getPos().x(), colunasGrade)][balde(e.getPos().y(), linhasGrade)].add(e);
    }

    private void reconstruirGrade() {
        for (List<Entity>[] coluna : grade) for (List<Entity> b : coluna) b.clear();
        for (Entity e : entidades) colocarNaGrade(e);
    }

    /**
     * Remove entidades num círculo. tipo == null remove tudo; senão só aquele tipo.
     * Tira da lista na hora, para sumir da tela mesmo com o jogo pausado.
     */
    public int removeEntities(Vec2 centro, double raio, EntityType tipo) {
        int removidas = 0;
        for (Entity e : neighbors(centro, raio, null)) {
            if (tipo == null || e.type() == tipo) {
                e.die();
                removidas++;
            }
        }
        if (removidas > 0) {
            entidades.removeIf(e -> !e.isAlive());
            reconstruirGrade();
        }
        return removidas;
    }

    /** O animal vivo mais perto de um ponto, até "raio". Null se não tiver nenhum. */
    public Animal nearestAnimal(Vec2 ponto, double raio) {
        Animal melhor = null;
        double melhorDist = Double.MAX_VALUE;
        for (Entity e : neighbors(ponto, raio, null)) {
            if (e instanceof Animal a) {
                double d = a.getPos().minus(ponto).length();
                if (d < melhorDist) {
                    melhor = a;
                    melhorDist = d;
                }
            }
        }
        return melhor;
    }

    public void killEntitiesInWater() {
        for (Entity e : entidades) {
            if (!isLand(e.getPos())) e.die();
        }
    }

    public int count(EntityType tipo) {
        int c = 0;
        for (Entity e : entidades) if (e.isAlive() && e.type() == tipo) c++;
        return c;
    }

    // ---------------- Terreno ----------------

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < largura && y < altura;
    }

    public Biome biomeAt(Vec2 p) {
        int x = (int) Math.floor(p.x());
        int y = (int) Math.floor(p.y());
        return inBounds(x, y) ? tiles[x][y].getBiome() : Biome.WATER;
    }

    public boolean isLand(Vec2 p) {
        int x = (int) Math.floor(p.x());
        int y = (int) Math.floor(p.y());
        return inBounds(x, y) && tiles[x][y].getBiome().isLand();
    }

    public Tile getTile(int x, int y) { return tiles[x][y]; }

    public void setBiome(int x, int y, Biome b) {
        if (!inBounds(x, y)) return;
        tiles[x][y].setBiome(b);
        terrenoMudou = true;
    }

    public void setLarguraPraia(int x, int y, int valor) {
        larguraPraia[x][y] = valor;
    }

    /**
     * Regras da costa (Information Expert: quem conhece os tiles é o World):
     * terra perto da água vira praia, e floresta perto demais da água vira planície.
     * Chamado na geração e sempre que o jogador mexe no terreno.
     */
    public void applyCoastRules() {
        int[][] dist = distanceToWater();
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                Tile t = tiles[x][y];
                if (!t.getBiome().isLand()) continue;
                if (dist[x][y] <= larguraPraia[x][y]) {
                    t.setBiome(Biome.BEACH);
                } else if (t.getBiome() == Biome.FOREST && dist[x][y] < DIST_MIN_FLORESTA) {
                    t.setBiome(Biome.PLAINS);
                }
            }
        }
        terrenoMudou = true;
    }

    /** Busca em largura (BFS) a partir de todos os tiles de água. */
    public int[][] distanceToWater() {
        int[][] dist = new int[largura][altura];
        ArrayDeque<int[]> fila = new ArrayDeque<>();
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (!tiles[x][y].getBiome().isLand()) {
                    dist[x][y] = 0;
                    fila.add(new int[]{x, y});
                } else {
                    dist[x][y] = Integer.MAX_VALUE;
                }
            }
        }
        int[][] vizinhos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!fila.isEmpty()) {
            int[] atual = fila.poll();
            for (int[] v : vizinhos) {
                int nx = atual[0] + v[0], ny = atual[1] + v[1];
                if (!inBounds(nx, ny)) continue;
                if (dist[nx][ny] > dist[atual[0]][atual[1]] + 1) {
                    dist[nx][ny] = dist[atual[0]][atual[1]] + 1;
                    fila.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    // ---------------- Observer ----------------

    public void addObserver(WorldObserver o)    { observadores.add(o); }
    public void removeObserver(WorldObserver o) { observadores.remove(o); }

    private void notificarObservadores() {
        Stats s = fotografar();
        for (WorldObserver o : observadores) o.onStats(s);
    }

    /** Monta a foto da população agora (contagens e média dos genes). */
    public Stats fotografar() {
        return new Stats(tempo,
                count(EntityType.RABBIT),
                count(EntityType.WOLF),
                count(EntityType.BUSH),
                mediaGeneVelocidade(EntityType.RABBIT),
                mediaGeneVelocidade(EntityType.WOLF));
    }

    /** Média do gene de velocidade de uma espécie (0 se ela não existe mais). */
    public double mediaGeneVelocidade(EntityType tipo) {
        double soma = 0;
        int n = 0;
        for (Entity e : entidades) {
            if (e.isAlive() && e.type() == tipo && e instanceof Animal a) {
                soma += a.getGenVelocidade();
                n++;
            }
        }
        return n == 0 ? 0 : soma / n;
    }

    // ---------------- Getters ----------------

    public int getLargura() { return largura; }
    public int getAltura()  { return altura; }
    public double getTempo() { return tempo; }
    public List<Entity> getEntidades() { return Collections.unmodifiableList(entidades); }

    public RelationTable getRelacoes() { return relacoes; }
    public void setRelacoes(RelationTable r) { this.relacoes = r; }

    public boolean terrenoMudou() { return terrenoMudou; }
    public void marcarTerrenoDesenhado() { terrenoMudou = false; }
}
