package pt.ulisboa.tecnico.sec.notary.exceptions;

public class WaitForEchoMessagesException extends Exception {
    private static final long serialVersionUID = 1L;

    public WaitForEchoMessagesException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public WaitForEchoMessagesException(String errorMessage) {
        super(errorMessage);
    }
}