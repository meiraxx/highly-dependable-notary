package pt.ulisboa.tecnico.sec.exceptions;

public class CryptoAuxException extends Exception {
    private static final long serialVersionUID = 1L;

    public CryptoAuxException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public CryptoAuxException(String errorMessage) {
        super(errorMessage);
    }

}