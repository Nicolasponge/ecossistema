package ecossistema.view;

import ecossistema.model.RelationTable;
import ecossistema.model.entity.EntityType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

/** A tabelinha de relações na tela. Mudou um valor, o ecossistema muda na hora. */
public class RelationTableView extends GridPane {

    private record Campo(EntityType de, EntityType para, Spinner<Double> spinner) { }

    private final RelationTable tabela;
    private final List<Campo> campos = new ArrayList<>();

    public RelationTableView(RelationTable tabela) {
        this.tabela = tabela;
        setHgap(6);
        setVgap(6);

        EntityType[] linhas = {EntityType.RABBIT, EntityType.WOLF}; // arbusto não anda
        EntityType[] colunas = EntityType.values();

        add(new Label("quem \\ por quem"), 0, 0);
        for (int c = 0; c < colunas.length; c++) add(new Label(nome(colunas[c])), c + 1, 0);

        for (int l = 0; l < linhas.length; l++) {
            EntityType de = linhas[l];
            add(new Label(nome(de)), 0, l + 1);

            for (int c = 0; c < colunas.length; c++) {
                EntityType para = colunas[c];
                Spinner<Double> campo = new Spinner<>(-5.0, 5.0, tabela.get(de, para), 0.5);
                campo.setEditable(true);
                campo.setPrefWidth(80);
                campo.valueProperty().addListener((obs, antigo, novo) -> {
                    if (novo != null) tabela.set(de, para, novo);
                });
                campos.add(new Campo(de, para, campo));
                add(campo, c + 1, l + 1);
            }
        }
    }

    /** Lê de novo os valores da tabela (ex: depois de importar um arquivo). */
    public void atualizar() {
        for (Campo c : campos) c.spinner().getValueFactory().setValue(tabela.get(c.de(), c.para()));
    }

    private String nome(EntityType t) {
        String n = t.getNome();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
