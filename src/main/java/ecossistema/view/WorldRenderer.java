package ecossistema.view;

import ecossistema.model.Biome;
import ecossistema.model.Vec2;
import ecossistema.model.World;
import ecossistema.model.entity.Animal;
import ecossistema.model.entity.Bush;
import ecossistema.model.entity.Entity;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Desenha o mundo no Canvas. O terreno só é redesenhado quando muda. */
public class WorldRenderer {

    public static final int TILE = 6;
    private static final double ALTURA_PULO = 3.0; // em pixels

    private static final Color AGUA_RASA = Color.web("#5fa3df");
    private static final Color AGUA_MEDIA = Color.web("#3b7cc4");
    private static final Color AGUA_FUNDA = Color.web("#2a5f9f");
    private static final Color PRAIA = Color.web("#e9d08c");
    private static final Color PLANICIE = Color.web("#7db649");
    private static final Color FLORESTA = Color.web("#3f7a34");
    private static final Color SOMBRA = Color.rgb(0, 0, 0, 0.28);

    private final Canvas canvas;
    private World world;
    private WritableImage terreno;

    private double mouseX, mouseY;
    private boolean mostrarPincel;
    private int raioPincel = 2;
    private Animal selecionado;

    private static final double ZOOM_MIN = 1, ZOOM_MAX = 5;
    private double zoom = 1;
    private double camX, camY;

    public WorldRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void setWorld(World world) {
        this.world = world;
        this.terreno = null;
    }

    /** x e y em pixels da tela (o renderer converte para o mundo). */
    public void setPincel(double x, double y, boolean visivel) {
        mouseX = x;
        mouseY = y;
        mostrarPincel = visivel;
    }

    // ---------------- Câmera ----------------
    // zoom = quantas vezes maior; (camX, camY) = qual pixel do mapa aparece no canto de cima da tela.

    /** Converte um ponto da tela (pixels do canvas) para o mundo (tiles). */
    public Vec2 telaParaMundo(double x, double y) {
        return new Vec2((x / zoom + camX) / TILE, (y / zoom + camY) / TILE);
    }

    /** Zoom mantendo parado o ponto que está embaixo do mouse (igual Google Maps). */
    public void zoomNo(double x, double y, double fator) {
        double mundoX = x / zoom + camX, mundoY = y / zoom + camY;
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * fator));
        camX = mundoX - x / zoom;
        camY = mundoY - y / zoom;
        limitarCamera();
    }

    /** Arrastar o mapa: dx e dy em pixels da tela. */
    public void arrastar(double dx, double dy) {
        camX -= dx / zoom;
        camY -= dy / zoom;
        limitarCamera();
    }

    public void resetarCamera() {
        zoom = 1;
        camX = camY = 0;
    }

    /** Não deixa a câmera sair do mapa (nunca aparece "fora do mundo"). */
    private void limitarCamera() {
        double maxX = canvas.getWidth() - canvas.getWidth() / zoom;
        double maxY = canvas.getHeight() - canvas.getHeight() / zoom;
        camX = Math.max(0, Math.min(maxX, camX));
        camY = Math.max(0, Math.min(maxY, camY));
    }

    public void setRaioPincel(int raio) { raioPincel = raio; }

    /** Animal com o anel de seleção (null = nenhum). */
    public void setSelecionado(Animal a) { selecionado = a; }

    /** Cor "oficial" de cada bioma (usada também nos ícones dos botões). */
    public static Color corBase(Biome b) {
        return switch (b) {
            case WATER -> AGUA_MEDIA;
            case BEACH -> PRAIA;
            case PLAINS -> PLANICIE;
            case FOREST -> FLORESTA;
        };
    }

    // ---------------- Quadro ----------------

    public void render() {
        if (world == null) return;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false); // pixel art nítida

        if (terreno == null || world.terrenoMudou()) {
            desenharTerreno();
            world.marcarTerrenoDesenhado();
        }
        // câmera: tudo daqui até o restore() é desenhado ampliado e deslocado
        g.save();
        g.scale(zoom, zoom);
        g.translate(-camX, -camY);

        g.drawImage(terreno, 0, 0);

        // quem está mais embaixo na tela é desenhado por cima (dá profundidade)
        List<Entity> ordem = new ArrayList<>(world.getEntidades());
        ordem.sort(Comparator.comparingDouble(e -> e.getPos().y()));
        // o anel fica no chão, então vai antes dos bichos
        if (selecionado != null && selecionado.isAlive()) desenharAnel(g, selecionado);
        for (Entity e : ordem) {
            if (e.isAlive()) desenhar(g, e);
        }

        if (mostrarPincel) {
            Vec2 m = telaParaMundo(mouseX, mouseY);
            // centro do tile embaixo do mouse, que é onde a ferramenta pinta
            double cx = ((int) m.x() + 0.5) * TILE, cy = ((int) m.y() + 0.5) * TILE;
            double r = raioPincel * TILE + TILE / 2.0;
            g.setStroke(Color.rgb(255, 255, 255, 0.85));
            g.setLineWidth(1.5 / zoom);
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.restore();
    }

    // ---------------- Entidades ----------------

    private void desenharAnel(GraphicsContext g, Animal a) {
        double px = a.getPos().x() * TILE, py = a.getPos().y() * TILE;
        g.setLineWidth(1.5);
        g.setStroke(Color.rgb(0, 0, 0, 0.5));
        g.strokeOval(px - 8, py - 2.5, 16, 7);
        g.setStroke(Color.web("#f2c14e"));
        g.strokeOval(px - 8, py - 3.5, 16, 7);
    }

    private void desenhar(GraphicsContext g, Entity e) {
        double px = e.getPos().x() * TILE;
        double py = e.getPos().y() * TILE; // "pé" da entidade

        if (e instanceof Bush b) {
            g.setFill(SOMBRA);
            g.fillOval(px - 6, py - 2, 12, 4);
            Image img = b.temFruta() ? Sprites.ARBUSTO : Sprites.ARBUSTO_SEM_FRUTA;
            g.drawImage(img, px - img.getWidth() / 2, py - img.getHeight() + 1);
            return;
        }

        if (e instanceof Animal a) {
            Image img = switch (a.type()) {
                case WOLF -> Sprites.LOBO;
                default -> Sprites.COELHO;
            };
            double w = img.getWidth(), h = img.getHeight();
            double pulo = a.getAlturaPulo() * ALTURA_PULO;

            // sombra fica no chão e encolhe quando o bicho sobe
            double s = 1 - a.getAlturaPulo() * 0.3;
            g.setFill(SOMBRA);
            g.fillOval(px - 5 * s, py - 1.5, 10 * s, 3);

            double topo = py - h - pulo;
            if (a.getVelocidade().x() < 0) {
                // sprite olha pra direita; espelha quando anda pra esquerda
                g.save();
                g.translate(px, 0);
                g.scale(-1, 1);
                g.drawImage(img, -w / 2, topo);
                g.restore();
            } else {
                g.drawImage(img, px - w / 2, topo);
            }
        }
    }

    // ---------------- Terreno ----------------

    private void desenharTerreno() {
        int w = world.getLargura(), h = world.getAltura();
        terreno = new WritableImage(w * TILE, h * TILE);
        PixelWriter pw = terreno.getPixelWriter();
        int[][] distTerra = distanciaAteTerra();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Biome b = world.getTile(x, y).getBiome();
                Biome cima = y > 0 ? world.getTile(x, y - 1).getBiome() : b;
                Biome baixo = y < h - 1 ? world.getTile(x, y + 1).getBiome() : b;

                Color base = corDoTile(b, distTerra[x][y]);
                // cada tile um pouquinho diferente; na água quase nada, senão vira xadrez
                double amplitude = switch (b) {
                    case WATER -> 0.015;
                    case BEACH -> 0.02;
                    default -> 0.04;
                };
                double variacao = 1 + amplitude * ((hash(x, y) % 101) / 50.0 - 1);
                Color tile = base.deriveColor(0, 1, variacao, 1);

                for (int px = 0; px < TILE; px++) {
                    for (int py = 0; py < TILE; py++) {
                        int gx = x * TILE + px, gy = y * TILE + py;
                        Color c = textura(b, tile, gx, gy);
                        pw.setColor(gx, gy, relevo(b, cima, baixo, c, py));
                    }
                }
            }
        }
    }

    /**
     * Efeito "3D" do WorldBox: a luz vem de cima, e a terra é um degrau acima do mar.
     * py é a linha do pixel dentro do tile (0 = topo, TILE-1 = base).
     */
    private Color relevo(Biome b, Biome cima, Biome baixo, Color c, int py) {
        if (b.isLand()) {
            // barranco: a borda de baixo da terra que encosta na água fica mais escura
            if (baixo == Biome.WATER && py >= TILE - 2) {
                return c.deriveColor(0, 1.05, py == TILE - 1 ? 0.62 : 0.76, 1);
            }
            if (b == Biome.FOREST) {
                // copa da floresta é "mais alta": base escura e topo levemente claro
                if (baixo != Biome.FOREST && py >= TILE - 2) return c.deriveColor(0, 1, 0.72, 1);
                if (cima != Biome.FOREST && py == 0) return c.deriveColor(0, 1, 1.18, 1);
            }
            return c;
        }
        // sombra da terra caindo na água logo abaixo dela
        if (cima.isLand() && py <= 2) {
            return c.deriveColor(0, 1, py == 0 ? 0.68 : py == 1 ? 0.78 : 0.88, 1);
        }
        return c;
    }

    private Color corDoTile(Biome b, int distTerra) {
        if (b != Biome.WATER) return corBase(b);
        if (distTerra <= 1) return AGUA_RASA;
        if (distTerra <= 4) return AGUA_MEDIA;
        return AGUA_FUNDA;
    }

    /** Tufinhos de grama só na planície e uns brilhos claros na floresta. Água e areia ficam lisas. */
    private Color textura(Biome b, Color c, int gx, int gy) {
        int n = hash(gx, gy) % 29;
        return switch (b) {
            case PLAINS -> n == 0 ? c.deriveColor(0, 1, 1.08, 1) : c;
            case FOREST -> n == 0 ? c.deriveColor(0, 1, 1.10, 1) : c;
            default -> c;
        };
    }

    /** BFS a partir da terra: quanto mais longe, mais funda a água. */
    private int[][] distanciaAteTerra() {
        int w = world.getLargura(), h = world.getAltura();
        int[][] dist = new int[w][h];
        ArrayDeque<int[]> fila = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (world.getTile(x, y).getBiome().isLand()) {
                    fila.add(new int[]{x, y});
                } else {
                    dist[x][y] = Integer.MAX_VALUE;
                }
            }
        }
        int[][] viz = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!fila.isEmpty()) {
            int[] a = fila.poll();
            for (int[] v : viz) {
                int nx = a[0] + v[0], ny = a[1] + v[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (dist[nx][ny] > dist[a[0]][a[1]] + 1) {
                    dist[nx][ny] = dist[a[0]][a[1]] + 1;
                    fila.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    private static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }
}
