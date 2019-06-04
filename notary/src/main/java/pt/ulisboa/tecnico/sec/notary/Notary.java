package pt.ulisboa.tecnico.sec.notary;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import pt.ulisboa.tecnico.sec.ccUtility;
import pt.ulisboa.tecnico.sec.exceptions.CitizenCardException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryException;

public class Notary {
    //private final static ArrayList<Good>() goodsArrayList;
	public static WaitForEchoMessages waitForEchoMessages;
	public static WaitForEchoMessages waitForReadyMessages;
	
    private static void ccLogin() throws NotaryException {
        // first Citizen Card initialization
        try {
            ccUtility.usePTEID(false);
        } catch(CitizenCardException cce) {
            throw new NotaryException("ccLogin(): Couldn't initialize Citizen Card.");
        }
    }
    
    private static Object[] initiateNotaryServer() throws NotaryException {
    	Object tuple[];
    	ServerSocket myServingSocket = null;
    	int maxPorts = 6, myServingPortNumber = 1121;
        for(int counter = 0; counter < maxPorts; counter++) {
	        try {
	        	if(counter!=0) {
	        		myServingPortNumber += 1;
	        	}
	        	myServingSocket = initiateServerSocket(myServingPortNumber);
	        	
	        	System.out.println("Serving clients on port: " + myServingPortNumber);
	        	
	        	tuple = new Object[]{myServingPortNumber%10, myServingSocket}; 
	        	return tuple;
			} catch (NotaryException ne) {
				if(counter==maxPorts-1) {
					throw new NotaryException("initiateNotaryServer(): cannot open more than " + maxPorts + " sockets...", ne);
				}
			}
        }
        throw new NotaryException("initiateNotaryServer(): something went terribly wrong while trying to initiate the notary!");        
    }    

    private static ServerSocket initiateServerSocket(int myServingPortNumber) throws NotaryException {
        ServerSocket myServingSocket;
        try {
            myServingSocket = new ServerSocket(myServingPortNumber);
        } catch (IOException ioe) {
            throw new NotaryException("initiateServerSocket(): Could not initialize my serving socket.", ioe);
        }
        return myServingSocket;
    }

    private static void closeServerSocket(ServerSocket serverSocket) throws NotaryException {
        try {
            serverSocket.close();
        } catch (IOException ioe) {
            throw new NotaryException("closeServerSocket(): Could not close server socket. It may be busy.", ioe);
        }
    }

    private static void waitForClients(ServerSocket serverSocket, String option, int notary, int numberOfNotaries, int numberOfFailures, WaitForEchoMessages waitForEchoMessages, WaitForEchoMessages waitForReadyMessages) throws NotaryException {
    	Socket clientSocket = null;
    	
        try {
	        if (option.equals("cc")) {
	            ccLogin();
	        }
        } catch(NotaryException ne) {
        	throw new NotaryException("waitForClients(): something went wrong while trying login "
        			+ "with the citizen card: ", ne);
        }
    	while (true) {
			try {
		        clientSocket = serverSocket.accept();
		        InetAddress clientInetAddress = clientSocket.getInetAddress();
		        System.out.printf("Connected to client %s on port %d %n", clientInetAddress.getHostAddress(), clientSocket.getPort());
		    } catch (IOException ioe) {
		            System.err.println("waitForClients(): Accept failed.");
		            System.exit(1);
		    }			
			//os parametros nao estavam a ser passados aqui
    		acceptConnection(clientSocket, option, notary, numberOfNotaries, numberOfFailures, waitForEchoMessages, waitForReadyMessages);
    	}
    }   
    private synchronized static void acceptConnection(Socket clientSocket, String option, int notary, int numberOfNotaries, int numberOfFailures, WaitForEchoMessages waitForEchoMessages, WaitForEchoMessages waitForReadyMessages) {
		//acima de tudo na criacao da thread
        WorkerThread workerThread = new WorkerThread(clientSocket, option, notary, numberOfNotaries, numberOfFailures, waitForEchoMessages, waitForReadyMessages);
        workerThread.run();
    }
    
    private static WaitForEchoMessages startWaitForEchoThread(String option, int notary, int numberOfNotaries, int numberOfFailures, int serverPort) {
    	WaitForEchoMessages waitForEchoMessages = new WaitForEchoMessages(option, notary, numberOfNotaries, numberOfFailures, serverPort);
		new Thread(waitForEchoMessages).start();
		return waitForEchoMessages;
    }
    public static void main(String[] args) throws NotaryException {    	    
        ServerSocket serverSocket = null;
        Object tuple[];
        int notaryNumber, numberOfFailures, numberOfNotaries;        
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Choose 'cc' or 'test': ");
        String option = UtilMethods.getInput(scanner);
        
        while(true) {
        	try {
                System.out.print("Input the number of failures you want to tolerate on the system: ");
                numberOfFailures = Integer.valueOf(UtilMethods.getInput(scanner));
	            break;
        	} catch(NumberFormatException nfe) {
            	System.out.println("You must insert an integer!");
            }
        }
        numberOfNotaries = numberOfFailures*3 + 1;
        
        scanner.close();

        System.out.println("###### SEC SERVER ######");
        
        tuple = initiateNotaryServer();
        
        notaryNumber = (int) tuple[0];
        serverSocket = (ServerSocket) tuple[1];
        
        waitForEchoMessages = startWaitForEchoThread(option, notaryNumber, numberOfNotaries, numberOfFailures, 1130);
        waitForReadyMessages = startWaitForEchoThread(option, notaryNumber, numberOfNotaries, numberOfFailures, 1140);
        waitForClients(serverSocket, option, notaryNumber, numberOfNotaries, numberOfFailures, waitForEchoMessages, waitForReadyMessages);               
        
        closeServerSocket(serverSocket);

        System.out.println("Connection closed");
    }
}
