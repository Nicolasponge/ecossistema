package ecossistema.view;

import ecossistema.model.Biome;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Ícones dos botões da barra. */
public final class Icones {

    private Icones() {
    }

    public static Node sprite(Image img) {
        ImageView iv = new ImageView(img);
        iv.setSmooth(false);
        iv.setPreserveRatio(true);
        iv.setFitWidth(img.getWidth() * 2);
        return iv;
    }

    /** Um "X" vermelho, para a ferramenta de limpar. */
    public static Node limpar() {
        Line a = new Line(0, 0, 18, 18), b = new Line(0, 18, 18, 0);
        for (Line l : new Line[]{a, b}) {
            l.setStroke(Color.web("#d9534f"));
            l.setStrokeWidth(4);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        return new Group(a, b);
    }

    public static Node bioma(Biome b) {
        Rectangle r = new Rectangle(28, 22, WorldRenderer.corBase(b));
        r.setStroke(Color.web("#1d1814"));
        r.setStrokeWidth(2);
        r.setArcWidth(6);
        r.setArcHeight(6);
        return r;
    }
}
