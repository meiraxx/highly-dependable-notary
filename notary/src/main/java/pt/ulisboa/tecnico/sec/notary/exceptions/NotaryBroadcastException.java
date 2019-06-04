package pt.ulisboa.tecnico.sec.notary.exceptions;

public class NotaryBroadcastException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotaryBroadcastException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public NotaryBroadcastException(String errorMessage) {
        super(errorMessage);
    }
}
