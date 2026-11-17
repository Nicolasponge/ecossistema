package ecossistema.model.generation;

import java.util.Random;

/**
 * Perlin noise 2D (Ken Perlin, 1983).
 *
 * Ideia: um grid com uma "setinha" aleatória em cada canto. A altura de um ponto
 * é a mistura suave do quanto ele aponta na direção das 4 setinhas em volta.
 */
public class PerlinNoise {

    private final int[] p = new int[512];

    public PerlinNoise(long seed) {
        Random rng = new Random(seed);
        int[] base = new int[256];
        for (int i = 0; i < 256; i++) base[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = base[i]; base[i] = base[j]; base[j] = tmp;
        }
        for (int i = 0; i < 512; i++) p[i] = base[i & 255];
    }

    /** Valor entre mais ou menos -1 e 1. */
    public double noise(double x, double y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        double u = fade(x), v = fade(y);

        int aa = p[p[X] + Y],     ab = p[p[X] + Y + 1];
        int ba = p[p[X + 1] + Y], bb = p[p[X + 1] + Y + 1];

        return lerp(
                lerp(grad(aa, x, y),     grad(ba, x - 1, y),     u),
                lerp(grad(ab, x, y - 1), grad(bb, x - 1, y - 1), u),
                v);
    }

    /** Soma de camadas: morros grandes + morrinhos + detalhes (detalhe dentro de detalhe). */
    public double fbm(double x, double y, int camadas) {
        double soma = 0, forca = 1, freq = 1, total = 0;
        for (int i = 0; i < camadas; i++) {
            soma += forca * noise(x * freq, y * freq);
            total += forca;
            forca *= 0.5;
            freq *= 2;
        }
        return soma / total;
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double a, double b, double t) { return a + t * (b - a); }

    private static double grad(int hash, double x, double y) {
        switch (hash & 7) {
            case 0: return  x + y;
            case 1: return -x + y;
            case 2: return  x - y;
            case 3: return -x - y;
            case 4: return  x;
            case 5: return -x;
            case 6: return  y;
            default: return -y;
        }
    }
}
