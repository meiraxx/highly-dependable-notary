package pt.ulisboa.tecnico.sec.notary;

import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;

public class BroadcastThread implements Runnable {
	
	private Thread runningThread = null;
	private String command;
	private String notaryName;
	private String message;
	private NotaryLibrary notaryLibrary;
	
	public BroadcastThread(String command, NotaryLibrary notaryLibrary, String notaryName) {
		this.command = command;
		this.notaryLibrary = notaryLibrary;
		this.notaryName = notaryName;
	}
	public String getMessage() {
		return this.message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public void run() {
		synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
		
		try {
			switch(this.command) {
				case "GET-STATE-OF-GOOD-ECHO":
					notaryLibrary.sendBroadcast(this.message);
					break;
				case "GET-STATE-OF-GOOD-READY":
					notaryLibrary.sendReady(this.message);
					break;
				case "INTENTION-TO-SELL-ECHO":
					notaryLibrary.sendBroadcast(this.message);
					break;
				case "INTENTION-TO-SELL-READY":
					notaryLibrary.sendReady(this.message);
					break;
				case "TRANSFER-GOOD-ECHO":
					notaryLibrary.sendBroadcastTG(this.message);
					break;
				case "TRANSFER-GOOD-READY":
					notaryLibrary.sendReadyTG(this.message);
					break;
				default:
					System.out.println("run(): there is no option with the name: " + this.command);			
			}
		} catch(NotaryLibraryException nle ) {
			System.out.println("run(): something went wrong while invoking a remote method with the NotaryLibrary...");
		}
	}	
}
