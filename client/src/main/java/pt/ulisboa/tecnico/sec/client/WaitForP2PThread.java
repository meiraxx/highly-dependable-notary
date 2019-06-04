package pt.ulisboa.tecnico.sec.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class WaitForP2PThread implements Runnable {
	private ServerSocket serverSocket;
	private boolean isRunning;
	private String username;
	private String option;
	private int numberOfNotaries;

	WaitForP2PThread(ServerSocket serverSocket, boolean isRunning, String username, String option, int numberOfNotaries) {
		this.serverSocket = serverSocket;
		this.isRunning = isRunning;
		this.username = username;
		this.option = option;
		this.numberOfNotaries = numberOfNotaries;
	}

	@Override
	public void run() {
		Socket p2pClientSocket = null;
		
		while(this.isRunning) {
	        try {
	        	p2pClientSocket = this.serverSocket.accept();
	            InetAddress p2pClientInetAddress = p2pClientSocket.getInetAddress();

	            System.out.printf(System.lineSeparator() + "P2P Client (%s) connected on port %d %n",
						p2pClientInetAddress.getHostAddress(), p2pClientSocket.getPort());
				new Thread(new P2PThread(p2pClientSocket, this.isRunning, this.username, this.option, this.numberOfNotaries)).start();
	        } catch (IOException ioe) {
	        	//System.err.println("Error on WaitForP2PThread class. Two things might have happened:");
	        	//System.err.println("1. Server socket may be already taken on that port;");
	            //System.err.println("2. Server socket was closed and is no longer accepting connections.");
	            System.out.println(ioe.getMessage());
	            System.exit(1);
	        }
		}		
	}
}
