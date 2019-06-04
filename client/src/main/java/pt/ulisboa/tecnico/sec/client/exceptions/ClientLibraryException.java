package pt.ulisboa.tecnico.sec.client.exceptions;

public class ClientLibraryException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientLibraryException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ClientLibraryException(String errorMessage) {
        super(errorMessage);
    }
}
