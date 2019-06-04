package pt.ulisboa.tecnico.sec.notary;

import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.communications.exceptions.CommunicationsException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryBroadcastException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;

import java.net.Socket;

public class ReceiveEchoThread implements Runnable {
    private Socket clientSocket;
    private Thread runningThread = null;
    private String option;
    
    private int notary;
    
    private String echoMessage; 
    
    ReceiveEchoThread(Socket clientSocket, String option, int notary) {
        this.clientSocket = clientSocket;
        this.option = option;
        this.notary = notary;
    }
    
    public String getEchoMessage() {
    	return this.echoMessage;
    }
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        Communications communications = new Communications(this.clientSocket);
        NotaryLibrary notaryLib = new NotaryLibrary(communications, this.option, this.notary);
        
        String command;

        try {
            System.out.println("Waiting for client command...");
            command = notaryLib.receiveCommand();

            System.out.println("Called command: " + command);
            switch (command) {            	
                case "BROADCAST":
                	this.echoMessage = notaryLib.broadcastConsensusEcho();
                	break;
                case "READY":
                	this.echoMessage = notaryLib.broadcastConsensusReady();
                	break;
                case "BROADCAST-TG":
                	this.echoMessage = notaryLib.broadcastConsensusEchoTG();
                	break;
                case "READY-TG":
                	this.echoMessage = notaryLib.broadcastConsensusReadyTG();
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
        }
    }
}
