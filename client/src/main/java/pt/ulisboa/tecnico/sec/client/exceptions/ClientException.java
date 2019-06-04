package pt.ulisboa.tecnico.sec.client.exceptions;

public class ClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public ClientException(String errorMessage) {
        super(errorMessage);
    }
}
