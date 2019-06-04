package pt.ulisboa.tecnico.sec.notary;

import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.communications.exceptions.CommunicationsException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryBroadcastException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;

import java.net.Socket;

public class WorkerThread implements Runnable {
    private Socket clientSocket;
    private Thread runningThread = null;
    private String option;
    private int notary;
    private int numberOfNotaries;
    private int numberOfFailures;
    
    private WaitForEchoMessages receiveEchoThread;
    private WaitForEchoMessages receiveReadyThread;
    
    private String echoMessage;     
    
    WorkerThread(Socket clientSocket, String option, int notary, int numberOfNotaries, int numberOfFailures, WaitForEchoMessages receiveEchoThread, WaitForEchoMessages receiveReadyThread) {
        this.clientSocket = clientSocket;
        this.option = option;
        this.notary = notary;
        this.numberOfNotaries = numberOfNotaries;
        this.numberOfFailures = numberOfFailures;
        this.receiveEchoThread = receiveEchoThread;
        this.receiveReadyThread = receiveReadyThread;
    }
    
    public String getEchoMessage() {
    	return this.echoMessage;
    }
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        
        this.receiveEchoThread.resetList();
        this.receiveReadyThread.resetList();
        
        Communications communications = new Communications(this.clientSocket);
        NotaryLibrary notaryLib = new NotaryLibrary(communications, this.option, this.notary);
        NotaryBroadcast notaryBroadcast = new NotaryBroadcast(notaryLib, this.option, this.notary, this.numberOfNotaries, this.numberOfFailures);
        
        String command;

        try {
            System.out.println("Waiting for client command...");
            command = notaryBroadcast.receiveCommand();

            System.out.println("Called command: " + command);
            switch (command) {
            	case "PING":
            		notaryLib.ping();
            		communications.end();
            		break;
                case "GET-STATE-OF-GOOD":
                	notaryBroadcast.getStateOfGood();
                    communications.end();
                    break;
                case "INTENTION-TO-SELL":
                	notaryBroadcast.intentionToSell();
                    communications.end();
                    break;
                case "TRANSFER-GOOD":
                	notaryBroadcast.transferGood();
                    communications.end();
                    break;            
                default:
                    System.out.println("Wrong input command");
                    break;
            }
        } catch (NotaryLibraryException nle) {
            String errorMsg = nle.getMessage();

            if (errorMsg.equals("authenticatedReceive(): Something went wrong with" +
                    " a UtilMethods function.Message: 'receiveMessage(): Communications" +
                    "module broke down...'.")) {
                errorMsg = "Client disconnected.";
            }
            System.out.println(errorMsg);
            try {
                communications.end();
            } catch (CommunicationsException ce) {
                System.out.println("Error closing communications...");
            }
        } catch (CommunicationsException ce) {
            System.out.println("Error closing communications...");
        } catch (NotaryBroadcastException nbe) {
        	String errorMsg = nbe.getMessage();

            if (errorMsg.equals("authenticatedReceive(): Something went wrong with" +
                    " a UtilMethods function.Message: 'receiveMessage(): Communications" +
                    "module broke down...'.")) {
                errorMsg = "Client disconnected.";
            }
            System.out.println(errorMsg);
            try {
                communications.end();
            } catch (CommunicationsException ce) {
                System.out.println("Error closing communications...");
            }
		}
    }
}
