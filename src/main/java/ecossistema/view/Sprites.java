package ecossistema.view;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.InputStream;

/** Carrega as sprites uma vez só. */
public final class Sprites {

    public static final Image COELHO = carregar("/sprites/coelho.png");
    public static final Image LOBO = carregar("/sprites/lobo.png");
    public static final Image ARBUSTO = carregar("/sprites/arbusto.png");
    public static final Image ARBUSTO_SEM_FRUTA = tirarFrutas(ARBUSTO);

    private Sprites() {
    }

    private static Image carregar(String caminho) {
        InputStream in = Sprites.class.getResourceAsStream(caminho);
        if (in == null) throw new IllegalStateException("Sprite não encontrada: " + caminho);
        return new Image(in);
    }

    /** Gera a versão sem fruta: pinta de folha os pixels avermelhados. */
    private static Image tirarFrutas(Image original) {
        int w = (int) original.getWidth(), h = (int) original.getHeight();
        WritableImage copia = new WritableImage(w, h);
        PixelReader leitor = original.getPixelReader();
        PixelWriter escritor = copia.getPixelWriter();
        Color folha = Color.rgb(121, 166, 76);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Color c = leitor.getColor(x, y);
                boolean ehFruta = c.getOpacity() > 0 && c.getRed() > c.getGreen() + 0.15;
                escritor.setColor(x, y, ehFruta ? folha : c);
            }
        }
        return copia;
    }
}
