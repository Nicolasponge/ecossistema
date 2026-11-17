package ecossistema.controller;

import ecossistema.controller.tools.ClearEntitiesTool;
import ecossistema.controller.tools.EraseTool;
import ecossistema.controller.tools.PaintBiomeTool;
import ecossistema.controller.tools.SpawnTool;
import ecossistema.controller.tools.Tool;
import ecossistema.exception.InvalidPlacementException;
import ecossistema.io.DadosIO;
import ecossistema.model.Biome;
import ecossistema.model.RelationTable;
import ecossistema.model.Vec2;
import ecossistema.model.World;
import ecossistema.model.entity.Animal;
import ecossistema.model.entity.Bush;
import ecossistema.model.entity.EntityType;
import ecossistema.model.entity.Rabbit;
import ecossistema.model.entity.Wolf;
import ecossistema.model.generation.WorldGenerator;
import ecossistema.view.Icones;
import ecossistema.view.MainView;
import ecossistema.view.Sprites;
import ecossistema.view.WorldRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Controller (MVC e GRASP): recebe as ações do jogador e coordena Model e View.
 * Tem o start() e o loop(deltaTime).
 */
public class GameController {

    public static final int LARGURA = 160;
    public static final int ALTURA = 100;
    private static final double RAIO_CLIQUE = 3.0; // em tiles: o quanto perto do bicho precisa clicar
    private static final double PASSO_MAXIMO = 0.05; // a simulação nunca anda mais que isso de uma vez

    private final MainView view;
    private final RelationTable relacoes;
    private final WorldRenderer renderer;
    private final AnimationTimer timer;

    private World world;
    private long seed;
    private Tool ferramentaAtual;
    private boolean pausado;
    private double velocidadeTempo = 1;
    private int raioPincel = 2;
    private Animal selecionado;
    private double ultimoX, ultimoY; // última posição do mouse, para arrastar o mapa

    public GameController(MainView view, RelationTable relacoes) {
        this.view = view;
        this.relacoes = relacoes;
        this.renderer = new WorldRenderer(view.getCanvas());

        this.timer = new AnimationTimer() {
            private long ultimo = 0;

            @Override
            public void handle(long agora) {
                if (ultimo == 0) { ultimo = agora; return; }
                double dt = (agora - ultimo) / 1e9;
                ultimo = agora;
                loop(dt);
            }
        };
    }

    public void start() {
        configurarFerramentas();
        configurarTempo();
        configurarPincel();
        configurarBotoes();
        configurarMouse();
        novoMundo(System.currentTimeMillis());
        timer.start();
    }

    private void loop(double dt) {
        if (!pausado) {
            // tempo acelerado = vários passinhos por quadro (não um passo gigante, que quebraria a física)
            double restante = Math.min(dt, PASSO_MAXIMO) * velocidadeTempo;
            while (restante > 1e-9) {
                double passo = Math.min(restante, PASSO_MAXIMO);
                world.update(passo);
                restante -= passo;
            }
        }
        renderer.render();
        view.getInspetor().mostrar(selecionado);
    }

    private void novoMundo(long seed) {
        if (world != null) world.removeObserver(view.getStats());
        this.seed = seed;
        world = new WorldGenerator(seed).generate(LARGURA, ALTURA);
        world.setRelacoes(relacoes);
        world.addObserver(view.getStats());
        selecionar(null);
        view.getStats().limpar();
        renderer.setWorld(world);
        view.setStatus("Seed: " + seed);
    }

    // ---------------- configuração ----------------

    private void configurarFerramentas() {
        for (Biome b : new Biome[]{Biome.PLAINS, Biome.FOREST, Biome.BEACH}) {
            PaintBiomeTool t = new PaintBiomeTool(b);
            ligar(view.addTerrenoButton(t.getNome(), Icones.bioma(b)), t);
        }
        ligar(view.addTerrenoButton("Apagar", Icones.bioma(Biome.WATER)), new EraseTool());

        ligar(view.addVidaButton("Coelho", Icones.sprite(Sprites.COELHO)), new SpawnTool("Coelho", Rabbit::new));
        ligar(view.addVidaButton("Lobo", Icones.sprite(Sprites.LOBO)), new SpawnTool("Lobo", Wolf::new));
        ligar(view.addVidaButton("Arbusto", Icones.sprite(Sprites.ARBUSTO)), new SpawnTool("Arbusto", Bush::new));

        // Limpar: uma ferramenta por opção da caixinha; o botão usa a que estiver escolhida
        List<Tool> limpar = List.of(
                new ClearEntitiesTool(null),
                new ClearEntitiesTool(EntityType.RABBIT),
                new ClearEntitiesTool(EntityType.WOLF),
                new ClearEntitiesTool(EntityType.BUSH));
        ToggleButton botaoLimpar = view.addLimparButton(Icones.limpar(), limpar.stream().map(Tool::getNome).toList());
        var escolha = view.getEscolhaLimpar().getSelectionModel();
        botaoLimpar.setOnAction(e ->
                ferramentaAtual = botaoLimpar.isSelected() ? limpar.get(escolha.getSelectedIndex()) : null);
        escolha.selectedIndexProperty().addListener((obs, antigo, novo) -> {
            botaoLimpar.setSelected(true); // escolher na caixinha já liga a ferramenta
            ferramentaAtual = limpar.get(novo.intValue());
        });
    }

    private void ligar(ToggleButton botao, Tool ferramenta) {
        botao.setOnAction(e -> ferramentaAtual = botao.isSelected() ? ferramenta : null);
    }

    private void configurarTempo() {
        view.getBotaoPausar().setOnAction(e -> {
            pausado = !pausado;
            view.getBotaoPausar().setText(pausado ? "Seguir" : "Pausar");
        });

        for (Map.Entry<ToggleButton, Double> par : view.getBotoesVelocidade().entrySet()) {
            ToggleButton b = par.getKey();
            double valor = par.getValue();
            b.setOnAction(e -> {
                velocidadeTempo = valor;
                b.setSelected(true); // não deixa ficar sem nenhuma velocidade marcada
            });
        }
    }

    private void configurarPincel() {
        Slider s = view.getSliderPincel();
        s.valueProperty().addListener((obs, antigo, novo) -> {
            raioPincel = (int) Math.round(novo.doubleValue());
            renderer.setRaioPincel(raioPincel);
            view.setTextoPincel(raioPincel);
        });
        renderer.setRaioPincel(raioPincel);
    }

    private void configurarBotoes() {
        view.getBotaoNovoMundo().setOnAction(e -> novoMundo(System.currentTimeMillis()));

        view.getBotaoExportar().setOnAction(e -> exportarDados());
        view.getBotaoImportar().setOnAction(e -> importarRelacoes());
    }

    private void exportarDados() {
        try {
            Path pasta = DadosIO.exportar(Path.of("dados"), seed, world, relacoes, view.getStats().getHistorico());
            view.setStatus("Dados salvos em " + pasta.toAbsolutePath());
        } catch (IOException ex) {
            view.setStatus("Não consegui salvar os dados (" + ex.getMessage()
                    + "). Veja se a pasta não está aberta em outro programa.");
        }
    }

    private void importarRelacoes() {
        FileChooser escolher = new FileChooser();
        escolher.setTitle("Escolha um relacoes.csv");
        escolher.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File pastaDados = new File("dados");
        if (pastaDados.isDirectory()) escolher.setInitialDirectory(pastaDados);

        File arquivo = escolher.showOpenDialog(view.getScene().getWindow());
        if (arquivo == null) return; // cancelou

        try {
            int n = DadosIO.importarRelacoes(arquivo.toPath(), relacoes);
            view.getTabelaRelacoes().atualizar();
            view.setStatus(n + " relações carregadas de " + arquivo.getName());
        } catch (IOException ex) {
            view.setStatus("Não consegui ler " + arquivo.getName() + ": " + ex.getMessage()
                    + ". A tabela continua como estava.");
        }
    }

    /**
     * Botão esquerdo = usa a ferramenta (ou inspeciona, se nenhuma estiver marcada).
     * Botão direito ou do meio arrastando = move o mapa.
     * Roda = zoom no cursor. Shift + roda ou [ e ] = tamanho do pincel.
     */
    private void configurarMouse() {
        var canvas = view.getCanvas();
        canvas.setOnMousePressed(e -> {
            ultimoX = e.getX();
            ultimoY = e.getY();
            if (e.getButton() == MouseButton.PRIMARY) usarFerramenta(e, true);
        });
        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.MIDDLE) {
                renderer.arrastar(e.getX() - ultimoX, e.getY() - ultimoY);
            } else {
                usarFerramenta(e, false);
            }
            ultimoX = e.getX();
            ultimoY = e.getY();
            atualizarPincel(e.getX(), e.getY());
        });
        canvas.setOnMouseMoved(e -> atualizarPincel(e.getX(), e.getY()));
        canvas.setOnMouseExited(e -> renderer.setPincel(0, 0, false));

        canvas.setOnScroll(e -> {
            // no Windows, com Shift a roda vem como rolagem horizontal (deltaX)
            double delta = e.getDeltaY() != 0 ? e.getDeltaY() : e.getDeltaX();
            if (delta == 0) return;
            if (e.isShiftDown()) {
                mudarPincel(delta > 0 ? 1 : -1);
            } else {
                renderer.zoomNo(e.getX(), e.getY(), delta > 0 ? 1.25 : 0.8);
            }
        });

        // teclas: [ e ] mudam o pincel, Esc tira a seleção, 0 volta a câmera ao normal
        view.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getTarget() instanceof TextInputControl) return; // digitando num campo
            switch (e.getCode()) {
                case OPEN_BRACKET -> mudarPincel(-1);
                case CLOSE_BRACKET -> mudarPincel(1);
                case ESCAPE -> selecionar(null);
                case DIGIT0, NUMPAD0 -> renderer.resetarCamera();
                default -> { }
            }
        });
    }

    private void mudarPincel(int passo) {
        Slider s = view.getSliderPincel();
        s.setValue(s.getValue() + passo);
    }

    private void atualizarPincel(double x, double y) {
        boolean mostrar = ferramentaAtual != null && ferramentaAtual.usaPincel();
        renderer.setPincel(x, y, mostrar);
    }

    private void usarFerramenta(MouseEvent e, boolean clique) {
        Vec2 p = renderer.telaParaMundo(e.getX(), e.getY());

        if (ferramentaAtual == null) {
            // sem ferramenta, o clique serve para inspecionar o bicho mais perto
            if (clique) selecionar(world.nearestAnimal(p, RAIO_CLIQUE));
            return;
        }
        if (!clique && !ferramentaAtual.isContinua()) return;

        try {
            ferramentaAtual.apply(world, p.x(), p.y(), raioPincel);
        } catch (InvalidPlacementException ex) {
            view.setStatus(ex.getMessage());
        }
    }

    private void selecionar(Animal a) {
        selecionado = a;
        renderer.setSelecionado(a);
    }
}
