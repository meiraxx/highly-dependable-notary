package pt.ulisboa.tecnico.sec.client.exceptions;

public class UtilMethodsException extends Exception {
    private static final long serialVersionUID = 1L;

    public UtilMethodsException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public UtilMethodsException(String errorMessage) {
        super(errorMessage);
    }
}
