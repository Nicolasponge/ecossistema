package ecossistema.exception;

/** Lançada quando o jogador tenta colocar algo onde não pode (ex: coelho na água). */
public class InvalidPlacementException extends Exception {
    public InvalidPlacementException(String mensagem) {
        super(mensagem);
    }
}
