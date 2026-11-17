package ecossistema.controller.tools;

import ecossistema.exception.InvalidPlacementException;
import ecossistema.model.Vec2;
import ecossistema.model.World;
import ecossistema.model.entity.Entity;

import java.util.function.Function;

/**
 * Invoca uma entidade. Não existe fábrica central: a ferramenta recebe
 * o próprio construtor da criatura (ex: Rabbit::new).
 */
public class SpawnTool implements Tool {

    private final String nome;
    private final Function<Vec2, Entity> criar;

    public SpawnTool(String nome, Function<Vec2, Entity> criar) {
        this.nome = nome;
        this.criar = criar;
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public void apply(World world, double x, double y, int raio) throws InvalidPlacementException {
        world.spawn(criar.apply(new Vec2(x, y)));
    }

    @Override
    public boolean isContinua() {
        return false;
    }

    @Override
    public boolean usaPincel() {
        return false;
    }
}
