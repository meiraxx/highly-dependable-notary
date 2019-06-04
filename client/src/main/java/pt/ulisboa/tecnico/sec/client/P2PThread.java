package pt.ulisboa.tecnico.sec.client;

import java.io.IOException;
import java.net.Socket;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientBroadcastException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.communications.Communications;

public class P2PThread implements Runnable {
	
	private boolean isRunning;
	private Communications p2pCommunication;
	private String username;
	private Thread runningThread = null;
	private String option;
	private int numberOfNotaries;

	P2PThread(Socket p2pClientSocket, boolean isRunning, String username, String option, int numberOfNotaries) throws IOException {
		this.isRunning = isRunning;
		this.p2pCommunication = new Communications(p2pClientSocket);
		this.username = username;
		this.option = option;
		this.numberOfNotaries = numberOfNotaries;
	}
     
	@Override
	public void run() {
		synchronized(this){
			this.runningThread = Thread.currentThread();
		}
				
		ClientLibrary p2pLib = new ClientLibrary(this.p2pCommunication, this.username, this.option, 0);		
									
		try {			
			String command = p2pLib.receiveCommand();
			
			final String serverIP = "localhost";
			
			ClientBroadcast clientBroadcast = new ClientBroadcast(serverIP, username, option, numberOfNotaries);
			if (command.equals("BUY-GOOD")) {
				clientBroadcast.setP2PClientLibrary(p2pLib);
				clientBroadcast.transferGood();		
			}
		} catch (ClientLibraryException cle) {
			System.out.println("run(): something went wrong with the ClientLibrary while performing a remote operation: " + cle.getMessage());
		} catch (ClientBroadcastException cbe) {
			System.out.println("run(): something went wrong with the ClientBroadcast while performing a remote operation: " + cbe.getMessage());
		}
		// reput normal input line:
		System.out.print("Press '1' for a HELP menu. Input a command to be run on the server: ");
		p2pLib.closeConnection();
	}

}
