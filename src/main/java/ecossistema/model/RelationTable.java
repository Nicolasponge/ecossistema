package ecossistema.model;

import ecossistema.model.entity.EntityType;

/**
 * A "tabelinha de relações": o quanto cada tipo é puxado (positivo)
 * ou empurrado (negativo) por outro tipo. O jogador edita ao vivo.
 */
public class RelationTable {

    private final double[][] valores;

    public RelationTable() {
        int n = EntityType.values().length;
        valores = new double[n][n];

        set(EntityType.RABBIT, EntityType.RABBIT, 0.3);   // bando
        set(EntityType.RABBIT, EntityType.WOLF, -2.0);    // medo
        set(EntityType.RABBIT, EntityType.BUSH, 1.5);     // comida

        set(EntityType.WOLF, EntityType.RABBIT, 3.0);     // caça
        set(EntityType.WOLF, EntityType.WOLF, 0.1);       // matilha
        set(EntityType.WOLF, EntityType.BUSH, 0.0);
    }

    public double get(EntityType de, EntityType para) {
        return valores[de.ordinal()][para.ordinal()];
    }

    public void set(EntityType de, EntityType para, double valor) {
        valores[de.ordinal()][para.ordinal()] = valor;
    }
}
