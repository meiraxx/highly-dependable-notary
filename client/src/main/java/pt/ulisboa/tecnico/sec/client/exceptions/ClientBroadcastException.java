package pt.ulisboa.tecnico.sec.client.exceptions;

public class ClientBroadcastException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientBroadcastException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ClientBroadcastException(String errorMessage) {
        super(errorMessage);
    }
}
