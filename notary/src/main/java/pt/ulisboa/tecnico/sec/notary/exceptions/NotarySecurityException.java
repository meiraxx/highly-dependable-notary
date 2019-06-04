package pt.ulisboa.tecnico.sec.notary.exceptions;

public class NotarySecurityException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotarySecurityException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public NotarySecurityException(String errorMessage) {
        super(errorMessage);
    }
}
