package pt.ulisboa.tecnico.sec.communications.exceptions;

public class CommunicationsException extends Exception {
	private static final long serialVersionUID = 1L;

	public CommunicationsException(String errorMessage) {
        super(errorMessage);
    }
}
