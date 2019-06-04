package pt.ulisboa.tecnico.sec.client;

import java.io.IOException;
import java.net.Socket;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientBroadcastException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.communications.Communications;

public class RemoteCallThread implements Runnable  {
	
	private static final String OK_MESSAGE = "OK";
	private static final String NOT_OK_MESSAGE = "NOT-OK";
	private static final String EMPTY = "";
	
	private Thread runningThread= null;
	
	private String command;
	
	private String username;		
	private String seller;
	private String good;
	
	private ClientLibrary clientLibrary;
		
	private int counter;
	
	private String ack = EMPTY;	
	private String message;
		
	private Object[] remoteResult;
	
	RemoteCallThread(String command, ClientLibrary clientLibrary, int counter, String username) {
		this.command = command;
		this.clientLibrary = clientLibrary;
		this.counter = counter;
		this.username = username;		
	}
	
	public String getAck() {
		return this.ack;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setSeller(String seller) {
		this.seller = seller;
	}
	
	public void setGood(String good) {
		this.good = good;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	@Override
	public void run() {
		synchronized(this){
			this.runningThread = Thread.currentThread();
		}
		
		try {
			switch(command) {
				case "PING":
					remoteResult = this.clientLibrary.ping(this.username);
					break;
				case "GET-STATE-OF-GOOD":
					remoteResult = this.clientLibrary.getStateOfGood(this.username, good);
					break;
				case "INTENTION-TO-SELL-PHASE-ONE":
					remoteResult = this.clientLibrary.intentionToSellPhaseOne(this.username, good); 
					break;
				case "INTENTION-TO-SELL-PHASE-TWO":
					remoteResult = this.clientLibrary.intentionToSellPhaseTwo(this.message, this.good); 
					break;
				case "BUY-GOOD-PHASE-ONE":
					remoteResult = this.clientLibrary.buyGoodP2PClientPhaseOne(this.username, seller, good, 1120+counter+1);
					break;
				case "BUY-GOOD-PHASE-TWO":
					remoteResult = this.clientLibrary.buyGoodP2PClientPhaseTwo(this.message, this.username, good);
					break;
				case "WRITE-BACK":
					remoteResult = this.clientLibrary.writeBack(this.message);
					break;
				default:
					System.out.println("run(): That method does not exist in the system!");
			}
			
			if((boolean)remoteResult[0]==true) {
				ack = OK_MESSAGE;				
			} else if((boolean)remoteResult[0]==false) {
				ack = NOT_OK_MESSAGE;
			} 
			message = (String) remoteResult[1];			
		} catch(ClientLibraryException cle) {
			System.out.println("run(): something went wrong while invoking a remote method with the ClientLibrary...");
		}
	}
	
}
