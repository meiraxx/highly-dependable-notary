package pt.ulisboa.tecnico.sec.notary.exceptions;

public class NotaryEncapsulationException extends Exception {
	private static final long serialVersionUID = 1L;

	public NotaryEncapsulationException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
	public NotaryEncapsulationException(String errorMessage) {
		super(errorMessage);
	}
}
