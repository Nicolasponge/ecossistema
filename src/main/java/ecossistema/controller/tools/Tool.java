package ecossistema.controller.tools;

import ecossistema.exception.InvalidPlacementException;
import ecossistema.model.World;

/**
 * Padrão Strategy (GoF): cada ferramenta faz o seu "apply" do seu jeito.
 * O controller só guarda a ferramenta atual e chama apply, sem saber qual é.
 */
public interface Tool {

    String getNome();

    /** x e y em tiles; raio é o tamanho do pincel (0 = um tile só). */
    void apply(World world, double x, double y, int raio) throws InvalidPlacementException;

    /** true = funciona arrastando o mouse; false = só no clique. */
    default boolean isContinua() {
        return true;
    }

    /** true = mostra o círculo do pincel no mapa. */
    default boolean usaPincel() {
        return true;
    }
}
