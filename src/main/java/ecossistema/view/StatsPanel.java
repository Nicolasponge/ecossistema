package ecossistema.view;

import ecossistema.model.Stats;
import ecossistema.model.WorldObserver;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Painel de estatísticas. É um Observer do World: a cada foto (Stats) ele atualiza.
 * Aba "População": o ciclo predador-presa. Aba "Evolução": média do gene de velocidade.
 */
public class StatsPanel extends VBox implements WorldObserver {

    private static final int MAX_PONTOS = 240;

    private final Label resumo = new Label();
    private final XYChart.Series<Number, Number> coelhos = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> lobos = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> genCoelhos = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> genLobos = new XYChart.Series<>();
    private final List<Stats> historico = new ArrayList<>();

    public StatsPanel() {
        setSpacing(6);

        coelhos.setName("Coelhos");
        lobos.setName("Lobos");
        LineChart<Number, Number> populacao = grafico("população", coelhos, lobos);

        genCoelhos.setName("Coelhos");
        genLobos.setName("Lobos");
        LineChart<Number, Number> evolucao = grafico("gene de velocidade", genCoelhos, genLobos);
        // escala fixa: 1,0 = velocidade da espécie no início. Com escala automática,
        // uma flutuação de 0,5% pareceria uma evolução enorme.
        NumberAxis eixoGene = (NumberAxis) evolucao.getYAxis();
        eixoGene.setAutoRanging(false);
        eixoGene.setLowerBound(0.8);
        eixoGene.setUpperBound(1.2);
        eixoGene.setTickUnit(0.05);

        Tab abaPop = new Tab("População", populacao);
        Tab abaEvo = new Tab("Evolução", evolucao);
        TabPane abas = new TabPane(abaPop, abaEvo);
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        getChildren().addAll(resumo, abas);
    }

    private static LineChart<Number, Number> grafico(String nomeY, XYChart.Series<Number, Number> a,
                                                    XYChart.Series<Number, Number> b) {
        NumberAxis eixoX = new NumberAxis();
        eixoX.setLabel("tempo (s)");
        eixoX.setForceZeroInRange(false);
        NumberAxis eixoY = new NumberAxis();
        eixoY.setLabel(nomeY);
        eixoY.setForceZeroInRange(false);

        LineChart<Number, Number> g = new LineChart<>(eixoX, eixoY);
        g.setCreateSymbols(false);
        g.setAnimated(false);
        g.setLegendSide(Side.RIGHT); // embaixo ela ficava cortada
        g.setPrefSize(340, 195);
        g.getData().add(a);
        g.getData().add(b);
        return g;
    }

    @Override
    public void onStats(Stats s) {
        historico.add(s);
        resumo.setText(String.format("Coelhos: %d   Lobos: %d   Arbustos: %d",
                s.coelhos(), s.lobos(), s.arbustos()));

        adicionar(coelhos, s.tempo(), s.coelhos());
        adicionar(lobos, s.tempo(), s.lobos());
        // espécie extinta não tem média de gene (vem 0); não desenha pra não esmagar o gráfico
        if (s.coelhos() > 0) adicionar(genCoelhos, s.tempo(), s.geneVelCoelhos());
        if (s.lobos() > 0) adicionar(genLobos, s.tempo(), s.geneVelLobos());
    }

    private static void adicionar(XYChart.Series<Number, Number> serie, double x, double y) {
        serie.getData().add(new XYChart.Data<>(x, y));
        if (serie.getData().size() > MAX_PONTOS) serie.getData().remove(0);
    }

    public void limpar() {
        historico.clear();
        coelhos.getData().clear();
        lobos.getData().clear();
        genCoelhos.getData().clear();
        genLobos.getData().clear();
    }

    /** Todo o histórico desde o começo do mundo (o gráfico só mostra o final). */
    public List<Stats> getHistorico() {
        return List.copyOf(historico);
    }
}
