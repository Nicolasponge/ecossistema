package ecossistema.view;

import ecossistema.model.RelationTable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Monta a janela, no estilo do WorldBox: mapa no centro, barra de ferramentas embaixo. Sem regra de jogo aqui. */
public class MainView extends BorderPane {

    private final Canvas canvas;
    private final StatsPanel stats = new StatsPanel();
    private final AnimalInspector inspetor = new AnimalInspector();
    private final Label status = new Label();

    private final ToggleGroup grupoFerramentas = new ToggleGroup();
    private final HBox linhaTerreno = new HBox(4);
    private final HBox linhaVida = new HBox(4);
    private final HBox linhaLimpar = new HBox(4);
    private final ChoiceBox<String> escolhaLimpar = new ChoiceBox<>();

    private final Button botaoPausar = botao("Pausar", true);
    private final Map<ToggleButton, Double> botoesVelocidade = new LinkedHashMap<>();
    private final Slider sliderPincel = new Slider(0, 8, 2);
    private final Label textoPincel = new Label();
    private final Button botaoNovoMundo = botao("Novo mundo", false);
    private final Button botaoExportar = botao("Exportar dados", true);
    private final Button botaoImportar = botao("Importar relações", true);
    private RelationTableView tabelaRelacoes;

    public MainView(int larguraCanvas, int alturaCanvas, RelationTable relacoes) {
        getStyleClass().add("raiz");
        canvas = new Canvas(larguraCanvas, alturaCanvas);

        // mapa com moldura
        StackPane moldura = new StackPane(canvas, inspetor);
        moldura.getStyleClass().add("moldura-mapa");
        StackPane.setAlignment(inspetor, Pos.TOP_LEFT);
        StackPane.setMargin(inspetor, new Insets(10));

        // painel da direita
        Label tituloPop = titulo("População");
        Label tituloRel = titulo("Relações");
        Label dica = new Label("positivo puxa, negativo empurra");
        dica.getStyleClass().add("dica");
        tabelaRelacoes = new RelationTableView(relacoes);
        Label tituloDados = titulo("Dados");
        Label dicaDados = new Label("CSVs na pasta dados/ (Python, Google Planilhas)");
        dicaDados.getStyleClass().add("dica");
        VBox lateral = new VBox(8, tituloPop, stats, tituloRel, dica, tabelaRelacoes,
                tituloDados, dicaDados, new HBox(6, botaoExportar, botaoImportar));
        lateral.getStyleClass().add("painel");

        // barra de baixo
        ToggleGroup grupoVelocidade = new ToggleGroup();
        HBox linhaTempo = new HBox(4, botaoPausar);
        double[] velocidades = {0.5, 1, 2, 5};
        for (double v : velocidades) {
            ToggleButton b = new ToggleButton(formatarVelocidade(v));
            b.getStyleClass().addAll("botao", "botao-pequeno");
            b.setToggleGroup(grupoVelocidade);
            if (v == 1) b.setSelected(true);
            botoesVelocidade.put(b, v);
            linhaTempo.getChildren().add(b);
        }

        sliderPincel.setMajorTickUnit(1);
        sliderPincel.setMinorTickCount(0);
        sliderPincel.setSnapToTicks(true);
        sliderPincel.setPrefWidth(100);
        setTextoPincel(2);
        VBox caixaPincel = new VBox(6, sliderPincel, textoPincel);

        HBox barra = new HBox(6,
                grupo("Tempo", linhaTempo),
                grupo("Terreno", linhaTerreno),
                grupo("Vida", linhaVida),
                grupo("Limpar", linhaLimpar),
                grupo("Pincel (Shift + roda)", caixaPincel),
                grupo("Mundo", botaoNovoMundo));
        barra.getStyleClass().add("barra-inferior");

        status.getStyleClass().add("status");
        VBox baixo = new VBox(0, barra, status);

        setCenter(moldura);
        setRight(lateral);
        setBottom(baixo);
    }

    // ---------------- usado pelo Controller ----------------

    public ToggleButton addTerrenoButton(String nome, Node icone) {
        return addFerramenta(linhaTerreno, nome, icone);
    }

    public ToggleButton addVidaButton(String nome, Node icone) {
        return addFerramenta(linhaVida, nome, icone);
    }

    /** Botão "Limpar" + caixinha para escolher o que limpar. */
    public ToggleButton addLimparButton(Node icone, List<String> opcoes) {
        escolhaLimpar.getItems().setAll(opcoes);
        escolhaLimpar.getSelectionModel().selectFirst();
        ToggleButton b = addFerramenta(linhaLimpar, "Limpar", icone);
        linhaLimpar.getChildren().add(escolhaLimpar);
        linhaLimpar.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    public ChoiceBox<String> getEscolhaLimpar() { return escolhaLimpar; }

    public void setStatus(String texto) { status.setText(texto); }

    public void setTextoPincel(int raio) {
        textoPincel.setText(raio == 0 ? "Tamanho: 1 tile" : "Raio: " + raio);
    }

    public Canvas getCanvas() { return canvas; }
    public StatsPanel getStats() { return stats; }
    public AnimalInspector getInspetor() { return inspetor; }
    public Button getBotaoPausar() { return botaoPausar; }
    public Map<ToggleButton, Double> getBotoesVelocidade() { return botoesVelocidade; }
    public Slider getSliderPincel() { return sliderPincel; }
    public Button getBotaoNovoMundo() { return botaoNovoMundo; }
    public Button getBotaoExportar() { return botaoExportar; }
    public Button getBotaoImportar() { return botaoImportar; }
    public RelationTableView getTabelaRelacoes() { return tabelaRelacoes; }

    // ---------------- montagem ----------------

    private ToggleButton addFerramenta(HBox linha, String nome, Node icone) {
        ToggleButton b = new ToggleButton(nome);
        b.setGraphic(icone);
        b.getStyleClass().add("botao");
        b.setToggleGroup(grupoFerramentas);
        linha.getChildren().add(b);
        return b;
    }

    private static Button botao(String texto, boolean pequeno) {
        Button b = new Button(texto);
        b.getStyleClass().add("botao");
        if (pequeno) b.getStyleClass().add("botao-pequeno");
        return b;
    }

    private static VBox grupo(String titulo, Node conteudo) {
        Label t = new Label(titulo);
        t.getStyleClass().add("titulo-grupo");
        VBox g = new VBox(4, t, conteudo);
        g.getStyleClass().add("grupo");
        return g;
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo");
        return l;
    }

    private static String formatarVelocidade(double v) {
        return (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)) + "x";
    }
}
