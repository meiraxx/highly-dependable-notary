package pt.ulisboa.tecnico.sec.notary.exceptions;

public class NotaryException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotaryException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public NotaryException(String errorMessage) {
        super(errorMessage);
    }
}
