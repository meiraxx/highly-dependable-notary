package pt.ulisboa.tecnico.sec.notary.exceptions;

public class NotaryLibraryException extends Exception {
    private static final long serialVersionUID = 1L;

    public NotaryLibraryException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public NotaryLibraryException(String errorMessage) {
        super(errorMessage);
    }
}
