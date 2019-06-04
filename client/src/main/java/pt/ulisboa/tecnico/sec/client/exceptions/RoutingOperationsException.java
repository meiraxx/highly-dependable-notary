package pt.ulisboa.tecnico.sec.client.exceptions;

public class RoutingOperationsException extends Exception {
    private static final long serialVersionUID = 1L;

    public RoutingOperationsException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public RoutingOperationsException(String errorMessage) {
        super(errorMessage);
    }
}
