package pt.ulisboa.tecnico.sec.exceptions;

public class CitizenCardException extends Exception {
    private static final long serialVersionUID = 1L;

    public CitizenCardException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public CitizenCardException(String errorMessage) {
        super(errorMessage);
    }

}
