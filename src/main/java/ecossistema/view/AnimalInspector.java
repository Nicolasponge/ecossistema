package ecossistema.view;

import ecossistema.model.entity.Animal;
import ecossistema.model.entity.EntityType;
import ecossistema.model.entity.Sex;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.util.Locale;

/**
 * Ficha do animal selecionado, flutuando por cima do mapa (como no WorldBox).
 * Só lê o Animal; quem decide quem está selecionado é o Controller.
 */
public class AnimalInspector extends VBox {

    private final Label titulo = new Label();
    private final Label idade = new Label();
    private final ProgressBar barraEnergia = new ProgressBar();
    private final Label energia = new Label();
    private final Label velocidade = new Label();
    private final Label visao = new Label();
    private final Label familia = new Label();
    private final Label dica = new Label("clique em outro bicho ou no vazio para fechar");

    public AnimalInspector() {
        getStyleClass().add("inspetor");
        titulo.getStyleClass().add("titulo");
        dica.getStyleClass().add("dica");
        barraEnergia.setPrefWidth(200);
        getChildren().addAll(titulo, idade, energia, barraEnergia, velocidade, visao, familia, dica);
        setSpacing(3);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMouseTransparent(true); // não atrapalha os cliques no mapa
        setVisible(false);
    }

    /** Atualiza a ficha. null esconde. */
    public void mostrar(Animal a) {
        if (a == null) {
            setVisible(false);
            return;
        }
        setVisible(true);

        String especie = a.type() == EntityType.WOLF ? "Lobo" : "Coelho";
        String sexo = a.getSexo() == Sex.MALE ? "macho" : "fêmea";
        String morto = a.isAlive() ? "" : "  (morreu)";
        titulo.setText(especie + " #" + a.getId() + ", " + sexo + morto);

        double vida = a.getIdade() / a.getIdadeMaxima();
        idade.setText(fmt("Idade: %.0f s de %.0f (%s)", a.getIdade(), a.getIdadeMaxima(),
                vida < 0.15 ? "filhote" : vida < 0.5 ? "adulto" : "velho, anda mais devagar"));

        energia.setText(fmt("Energia: %.0f de 100", a.getEnergia()));
        barraEnergia.setProgress(Math.max(0, a.getEnergia() / 100.0));

        velocidade.setText(fmt("Velocidade: %.2f tiles/s  (gene %s)", a.getVelocidadeAtual(),
                porcentagem(a.getGenVelocidade())));
        visao.setText(fmt("Visão: %.1f tiles  (gene %s)", a.getVisao(), porcentagem(a.getGenVisao())));

        String pais = a.getIdMae() == 0
                ? "veio do mapa inicial"
                : "mãe #" + a.getIdMae() + ", pai #" + a.getIdPai();
        familia.setText("Geração " + a.getGeracao() + ", " + pais + ", " + a.getFilhos() + " filho(s)");
    }

    /** Gene 1.07 vira "+7%": fica fácil comparar pai e filho na apresentação. */
    private static String porcentagem(double gene) {
        long p = Math.round((gene - 1) * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    private static String fmt(String f, Object... args) {
        return String.format(Locale.forLanguageTag("pt-BR"), f, args);
    }
}
