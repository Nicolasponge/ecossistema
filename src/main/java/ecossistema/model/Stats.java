package ecossistema.model;

/**
 * Foto da população num instante.
 * geneVelCoelhos/geneVelLobos = média do gene de velocidade (1.0 = igual ao início).
 * Se essa média sobe com o tempo, a evolução está acontecendo.
 */
public record Stats(double tempo, int coelhos, int lobos, int arbustos,
                    double geneVelCoelhos, double geneVelLobos) {
}
