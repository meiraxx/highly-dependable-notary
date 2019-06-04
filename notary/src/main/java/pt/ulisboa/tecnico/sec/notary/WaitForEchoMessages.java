package pt.ulisboa.tecnico.sec.notary;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import pt.ulisboa.tecnico.sec.notary.exceptions.WaitForEchoMessagesException;

public class WaitForEchoMessages implements Runnable {
	
	public ArrayList<ReceiveEchoThread> workerThreadArrayList;
	
	private int notary;
	private int numberOfNotaries;
	private int numberOfFailures;
	private int serverPort; 
	
	private String option;
	
	private Thread runningThread = null;	
	
	WaitForEchoMessages(String option, int notary, int numberOfNotaries, int numberOfFailures, int serverPort) {
		this.option = option;
		this.notary = notary;
		this.numberOfNotaries = numberOfNotaries;
		this.numberOfFailures = numberOfFailures;
		this.serverPort = serverPort;
	}	
	public synchronized void resetList() {
		this.workerThreadArrayList = new ArrayList<ReceiveEchoThread>();
	}
    private ServerSocket initiateServerSocket(int myServingPortNumber) throws WaitForEchoMessagesException {
        ServerSocket myServingSocket;
        try {
            myServingSocket = new ServerSocket(myServingPortNumber);
        } catch (IOException ioe) {
            throw new WaitForEchoMessagesException("initiateServerSocket(): Could not initialize my serving socket...");
        }       
        return myServingSocket;
    }
    
	@Override
	public void run() {
		synchronized(this){
			this.runningThread = Thread.currentThread();
		}
		
		ServerSocket serverSocket = null;
				
		while(true) {
			try {				
				int serverPort = this.serverPort + this.notary;
				serverSocket = initiateServerSocket(serverPort);
				break;
			} catch(WaitForEchoMessagesException wfeme) {
				System.out.println("run(): There is already another WaitForEchoMessages thread running! Waiting...");
			}
		}
		
		Socket waitNotarySocket = null;
		this.workerThreadArrayList = new ArrayList<ReceiveEchoThread>();
		
		while(true) {
	        try {
	        	waitNotarySocket = serverSocket.accept();
	            InetAddress p2pClientInetAddress = waitNotarySocket.getInetAddress();

	            System.out.printf(System.lineSeparator() + "P2P Client (%s) connected on port %d %n",
						p2pClientInetAddress.getHostAddress(), waitNotarySocket.getPort());
	            
	            ReceiveEchoThread receiveEchoThread = new ReceiveEchoThread(waitNotarySocket, option, notary);	            	            
	            new Thread(receiveEchoThread).start();
	            this.workerThreadArrayList.add(receiveEchoThread);
	        } catch (IOException ioe) {
	        	//System.err.println("Error on WaitForEchoMessages class. Two things might have happened:");
	        	//System.err.println("1. Server socket may be already taken on that port;");
	            //System.err.println("2. Server socket was closed and is no longer accepting connections.");
	            System.out.println(ioe.getMessage());
	            System.exit(1);
	        }
		}		
	}
}
