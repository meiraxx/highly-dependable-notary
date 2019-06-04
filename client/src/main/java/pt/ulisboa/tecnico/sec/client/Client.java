package pt.ulisboa.tecnico.sec.client;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Scanner;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientBroadcastException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;
import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;

public class Client {

    private static boolean isRunning = true;
    private static final String CLIENTS_INFO_PATH = "clients_info.json";

    private static ServerSocket initiateServerSocket(int myServingPortNumber) throws ClientException {
        ServerSocket myServingSocket;
        try {
            myServingSocket = new ServerSocket(myServingPortNumber);
        } catch (IOException ioe) {
            throw new ClientException("initiateServerSocket(): Could not initialize my serving socket.", ioe);
        }
        return myServingSocket;
    }

    private static void closeServerSocket(ServerSocket serverSocket) throws ClientException {
        try {
            serverSocket.close();
        } catch (IOException ioe) {
            throw new ClientException("closeServerSocket(): Could not close server socket. It may be busy.", ioe);
        }
    }



    private static int fetchP2PClientPort(String p2pClient) throws ClientLibraryException {
        JSONObject jsonPortsMapping;
        int p2pPortNumber;
        try {
            jsonPortsMapping = UtilMethods.convertFileToJSON(CLIENTS_INFO_PATH);
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("fetchP2PClient(): Failed to find file '" + CLIENTS_INFO_PATH + "'.", ume);
        }

        try {
            p2pPortNumber = (int) UtilMethods.jsonGetObjectByKey(jsonPortsMapping, p2pClient);
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("fetchP2PClient(): Client you entered doesn't exist, please try again.", ume);
        }
        return p2pPortNumber;
    }

    private static void help() {
        String menuString = "################################################"+ System.lineSeparator()
                + "Command Menu:" + System.lineSeparator()                
                + "1. \"HELP\": print this help menu" + System.lineSeparator()
                + "2. \"GET-STATE-OF-GOOD\": check if a good is on sale or not" + System.lineSeparator()
                + "3. \"INTENTION-TO-SELL\": give your intention to sell a good of yours" + System.lineSeparator()
                + "4. \"BUY-GOOD\": buy a good from another user" + System.lineSeparator()
                + "5. \"PING\": sends a ping message to all alive notary servers" + System.lineSeparator()
                + "6. \"EXIT\": close app" + System.lineSeparator()
                + "################################################"+ System.lineSeparator();
        System.out.println(System.lineSeparator() + menuString);
    }   

    public static void main(String[] args) throws ClientException {
    	final String username;
        final String serverIP = "localhost";
        final String p2pClientIP = "localhost";
        int serverPortNumber = 1121;
        int myServingPortNumber;        
        ServerSocket myServingSocket;
        String input;
        String portsMapping;
        JSONObject jsonPortsMapping;
        String seller;
        String good;
        int numberOfNotaries, numberOfFailures;
        ClientLibrary clientLib;
        Scanner scanner = new Scanner(System.in);
        ClientBroadcast clientBroadcast;

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
        System.out.println("###### SEC P2P-CLIENT ######");
        System.out.print("Your username: ");
        username = UtilMethods.getInput(scanner);
        
        try {
			portsMapping = UtilMethods.readFile(CLIENTS_INFO_PATH);
			jsonPortsMapping = UtilMethods.convertStringToJSON(portsMapping);
			myServingPortNumber = (int) UtilMethods.jsonGetObjectByKey(jsonPortsMapping, username);
            myServingSocket = initiateServerSocket(myServingPortNumber);
		} catch (UtilMethodsException ume) {
            throw new ClientException("main(): Failed to get port from clients info file.", ume);
        }

        System.out.println("Serving port number: " + myServingPortNumber);
        //Thread mainThread = Thread.currentThread();

        new Thread(new WaitForP2PThread(myServingSocket, isRunning, username, option, numberOfNotaries)).start();               
                
        while (isRunning) {
            System.out.print("Press '1' for a HELP menu. Input a command to be run on the server: ");
            input = UtilMethods.getInput(scanner);
            try {
                switch (input) {                	
                    case "1":
                        help();
                        break;
                    case "2":
                        System.out.print("Good you want to fetch the status of: ");
                        good = UtilMethods.getInput(scanner);
                        clientBroadcast = new ClientBroadcast(serverIP, serverPortNumber, username, option, numberOfNotaries, numberOfFailures);
                        clientBroadcast.getStateOfGood(good);
                        break;
                    case "3":
                        System.out.print("Good you want to sell: ");
                        good = UtilMethods.getInput(scanner);
                        clientBroadcast = new ClientBroadcast(serverIP, serverPortNumber, username, option, numberOfNotaries, numberOfFailures);
                        clientBroadcast.intentionToSell(good);
                        break;
                    case "4":
                        int p2pClientPort;
                        // choose seller to talk to
                        System.out.print("Input the user from whom you want to buy a good: ");
                        seller = UtilMethods.getInput(scanner);                        
                        if(username.equals(seller)) {
                        	System.out.println("You cannot buy a good from yourself!");
                        	break;
                        }
                        // choose good to buy
                        System.out.print("Input the good you want to buy: ");
                        good = UtilMethods.getInput(scanner);
                        
                        p2pClientPort = fetchP2PClientPort(seller);
                        clientBroadcast = new ClientBroadcast(serverIP, p2pClientPort, username, option, numberOfNotaries, numberOfFailures);
                        clientBroadcast.buyGood(seller, good);
                        
                        break;
                    case "5":
                    	//posivelmente vamos criar mais uma classe que gere este ciclo while para cada tipo de metodo         
                    	clientBroadcast = new ClientBroadcast(serverIP, serverPortNumber, username, option, numberOfNotaries, numberOfFailures);
                    	clientBroadcast.ping();
                		break;
                    case "6":
                        System.out.println("Closing application.");
                        isRunning = false;
                        break;
                    default:
                        System.out.println("Wrong input command. Try another one.");
                        break;
                }
            } catch (ClientBroadcastException | ClientLibraryException ce) {
            	System.out.println(ce.getMessage());
            	ce.printStackTrace();
                try {
                    //clientLib.closeConnection();
                } catch (NullPointerException npe) {
                    // no problem, no connection exists to close
                }
                //isRunning = false;
                //break;
            }
        }
        scanner.close();
        closeServerSocket(myServingSocket);
        System.out.println("Connection closed");
    }
}