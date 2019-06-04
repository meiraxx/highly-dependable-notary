package pt.ulisboa.tecnico.sec.client.exceptions;

public class ClientEncapsulationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClientEncapsulationException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ClientEncapsulationException(String errorMessage) {
        super(errorMessage);
    }
}
