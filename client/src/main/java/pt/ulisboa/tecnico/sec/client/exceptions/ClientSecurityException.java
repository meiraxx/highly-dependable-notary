package pt.ulisboa.tecnico.sec.client.exceptions;

public class ClientSecurityException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientSecurityException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ClientSecurityException(String errorMessage) {
        super(errorMessage);
    }
}
