package ecossistema.model.generation;

import ecossistema.exception.InvalidPlacementException;
import ecossistema.model.Biome;
import ecossistema.model.Vec2;
import ecossistema.model.World;
import ecossistema.model.entity.Animal;
import ecossistema.model.entity.Bush;
import ecossistema.model.entity.Entity;
import ecossistema.model.entity.Rabbit;
import ecossistema.model.entity.Wolf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Gera o mundo em etapas, na ordem da natureza:
 * relevo -> terra e água -> praia -> floresta -> (resto é planície) -> vida.
 */
public class WorldGenerator {

    public static final double NIVEL_DO_MAR = 0.48;
    private static final int CAMADAS = 4;
    private static final double ESCALA = 28.0;
    private static final int DIST_NUCLEO_FLORESTA = 7;

    private final PerlinNoise noise;
    private final Random rng;

    public WorldGenerator(long seed) {
        this.noise = new PerlinNoise(seed);
        this.rng = new Random(seed);
    }

    public World generate(int largura, int altura) {
        World world = new World(largura, altura);

        double[][] relevo = gerarRelevo(largura, altura);
        marcarTerraEAgua(world, relevo);
        definirLarguraDasPraias(world);
        world.applyCoastRules();
        plantarFlorestas(world);
        povoar(world);

        return world;
    }

    // Etapa 1: Perlin + camadas + "máscara de ilha" (mais longe do centro = mais baixo)
    private double[][] gerarRelevo(int w, int h) {
        double[][] relevo = new double[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double nx = (double) x / w - 0.5;
                double ny = (double) y / h - 0.5;
                double distCentro = Math.sqrt(nx * nx * 1.2 + ny * ny * 2) * 1.6;
                relevo[x][y] = noise.fbm(x / ESCALA, y / ESCALA, CAMADAS) * 0.9 + 0.75 - distCentro * 0.55;
            }
        }
        return relevo;
    }

    // Etapa 2: acima do nível do mar é terra
    private void marcarTerraEAgua(World world, double[][] relevo) {
        for (int x = 0; x < world.getLargura(); x++) {
            for (int y = 0; y < world.getAltura(); y++) {
                world.setBiome(x, y, relevo[x][y] > NIVEL_DO_MAR ? Biome.PLAINS : Biome.WATER);
            }
        }
    }

    // Etapa 3: largura da praia varia ao longo da costa (2, mais ou menos 1), usando outro ruído
    private void definirLarguraDasPraias(World world) {
        for (int x = 0; x < world.getLargura(); x++) {
            for (int y = 0; y < world.getAltura(); y++) {
                int largura = 2 + (int) Math.round(noise.noise(x / 6.0 + 9, y / 6.0 + 9) * 2);
                world.setLarguraPraia(x, y, Math.max(1, Math.min(3, largura)));
            }
        }
    }

    // Etapa 4: núcleos de floresta longe da costa, crescendo com borda irregular + "subs"
    private void plantarFlorestas(World world) {
        int[][] dist = world.distanceToWater();
        List<int[]> candidatos = new ArrayList<>();
        for (int x = 0; x < world.getLargura(); x++) {
            for (int y = 0; y < world.getAltura(); y++) {
                if (dist[x][y] >= DIST_NUCLEO_FLORESTA && dist[x][y] != Integer.MAX_VALUE) {
                    candidatos.add(new int[]{x, y});
                }
            }
        }
        if (candidatos.isEmpty()) return;

        int nucleos = Math.max(3, candidatos.size() / 350);
        for (int i = 0; i < nucleos; i++) {
            int[] c = candidatos.get(rng.nextInt(candidatos.size()));
            crescerFloresta(world, dist, c[0], c[1]);
        }
    }

    private void crescerFloresta(World world, int[][] dist, int cx, int cy) {
        int raio = 4 + rng.nextInt(3);

        for (int dx = -raio - 3; dx <= raio + 3; dx++) {
            for (int dy = -raio - 3; dy <= raio + 3; dy++) {
                int x = cx + dx, y = cy + dy;
                double borda = noise.noise(x / 5.0 + 50, y / 5.0 + 50) * 4;
                if (Math.hypot(dx, dy) < raio + borda) plantar(world, dist, x, y);
            }
        }

        // subs: blocos de até 6x6 grudados no núcleo
        int subs = 1 + rng.nextInt(3);
        for (int s = 0; s < subs; s++) {
            int sw = 2 + rng.nextInt(5), sh = 2 + rng.nextInt(5);
            int sx = cx + rng.nextInt(2 * raio + 1) - raio - sw / 2;
            int sy = cy + rng.nextInt(2 * raio + 1) - raio - sh / 2;
            for (int x = sx; x < sx + sw; x++) {
                for (int y = sy; y < sy + sh; y++) plantar(world, dist, x, y);
            }
        }
    }

    private void plantar(World world, int[][] dist, int x, int y) {
        if (world.inBounds(x, y) && world.getTile(x, y).getBiome().isLand() && dist[x][y] >= 4) {
            world.setBiome(x, y, Biome.FOREST);
        }
    }

    // Etapa 5: vida
    private void povoar(World world) {
        for (int x = 0; x < world.getLargura(); x++) {
            for (int y = 0; y < world.getAltura(); y++) {
                Biome b = world.getTile(x, y).getBiome();
                double chance = b == Biome.FOREST ? 0.15 : b == Biome.PLAINS ? 0.06 : 0;
                if (rng.nextDouble() < chance) {
                    colocar(world, new Bush(new Vec2(x + 0.5, y + 0.5)));
                }
            }
        }
        espalharEmGrupos(world, Rabbit::new, 8, 5);  // tocas de coelho
        espalharEmGrupos(world, Wolf::new, 2, 4);    // matilhas
    }

    private void espalharEmGrupos(World world, Function<Vec2, Entity> criar, int grupos, int porGrupo) {
        for (int g = 0; g < grupos; g++) {
            Vec2 centro = sortearTerraFirme(world);
            if (centro == null) return;
            int colocados = 0, tentativas = 0;
            while (colocados < porGrupo && tentativas++ < 200) {
                Vec2 p = centro.plus(new Vec2(rng.nextDouble() * 4 - 2, rng.nextDouble() * 4 - 2));
                if (world.isLand(p)) {
                    Entity e = criar.apply(p);
                    if (e instanceof Animal a) a.sortearIdadeInicial(rng); // idades misturadas
                    colocar(world, e);
                    colocados++;
                }
            }
        }
    }

    private Vec2 sortearTerraFirme(World world) {
        for (int i = 0; i < 10000; i++) {
            double x = rng.nextDouble() * world.getLargura();
            double y = rng.nextDouble() * world.getAltura();
            Biome b = world.getTile((int) x, (int) y).getBiome();
            if (b == Biome.PLAINS || b == Biome.FOREST) return new Vec2(x, y);
        }
        return null;
    }

    private void colocar(World world, Entity e) {
        try {
            world.spawn(e);
        } catch (InvalidPlacementException ignorada) {
            // na geração, se não couber, só pula
        }
    }
}
