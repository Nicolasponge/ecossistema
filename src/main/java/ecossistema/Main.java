package ecossistema;

import ecossistema.controller.GameController;
import ecossistema.model.RelationTable;
import ecossistema.view.MainView;
import ecossistema.view.WorldRenderer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/** Ponto de entrada: monta a View, entrega pro Controller e liga o loop. */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        RelationTable relacoes = new RelationTable();
        MainView view = new MainView(
                GameController.LARGURA * WorldRenderer.TILE,
                GameController.ALTURA * WorldRenderer.TILE,
                relacoes);
        GameController controller = new GameController(view, relacoes);

        Scene cena = new Scene(view);
        URL estilo = Main.class.getResource("/style.css");
        if (estilo != null) cena.getStylesheets().add(estilo.toExternalForm());

        stage.setTitle("Ecossistema");
        stage.setScene(cena);
        stage.show();

        controller.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
