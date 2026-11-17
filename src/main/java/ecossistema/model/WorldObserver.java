package ecossistema.model;

/**
 * Padrão Observer (GoF): quem quiser saber da população implementa isso
 * e se inscreve no World. O World não sabe quem está ouvindo.
 */
public interface WorldObserver {
    void onStats(Stats stats);
}
