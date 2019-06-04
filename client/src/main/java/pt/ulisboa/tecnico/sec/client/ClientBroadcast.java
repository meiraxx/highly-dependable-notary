package pt.ulisboa.tecnico.sec.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientBroadcastException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientEncapsulationException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;
import pt.ulisboa.tecnico.sec.communications.Communications;

public class ClientBroadcast {
	
	private static final String OK_MESSAGE = "OK";
	private static final String NOT_OK_MESSAGE = "NOT-OK";
	
	private static final String ERROR = "ERROR";
	
	private String serverIP;
	private String username;
	private String option;
	
	private int numberOfNotaries;
	private int numberOfFailures;
	private int serverPortNumber;
	private ClientLibrary p2pLibrary;	
	
	//usado pela P2PThread
	public ClientBroadcast(String serverIP, String username, String option, int numberOfNotaries) {
		this.serverIP = serverIP;
		this.username = username;
		this.option = option;
		this.numberOfNotaries = numberOfNotaries;
	}
	
	public ClientBroadcast(String serverIP, int serverPortNumber, String username, String option, int numberOfNotaries, int numberOfFailures) {
		this.serverIP = serverIP;
		this.serverPortNumber = serverPortNumber;
		this.username = username;
		this.option = option;
		this.numberOfNotaries = numberOfNotaries;
		this.numberOfFailures = numberOfFailures;
	}
	
	public void setP2PClientLibrary(ClientLibrary p2pLibrary) {
		this.p2pLibrary = p2pLibrary;
	}	
	private ClientLibrary initiateNewClientLib(String serverIP, int serverPortNumber, int notary) throws ClientBroadcastException {
        Socket clientSocket = initiateClientSocket(serverIP, serverPortNumber);
        Communications clientCommunication = new Communications(clientSocket);
        return new ClientLibrary(clientCommunication, this.username, this.option, notary);
    }
    
    private Socket initiateClientSocket(String serverIP, int serverPortNumber) throws ClientBroadcastException {
        Socket clientSocket;
        try {
            clientSocket = new Socket(serverIP, serverPortNumber);
        } catch (UnknownHostException uhe) {
            throw new ClientBroadcastException("Could not find a listening port in '" + serverIP + "' host machine, port '"
                    + serverPortNumber + "'. Client might be down.", uhe);
        } catch (IOException ioe) {
            throw new ClientBroadcastException("initiateClientSocket(): Could not initialize my client socket. Aborting...", ioe);
        }
        return clientSocket;
    }
    
    private ArrayList<ClientLibrary> initiateClientLibraryBroadcast() throws ClientBroadcastException {    	
    	ClientLibrary clientLib;    
    	ArrayList<ClientLibrary> clientLibraryBroadcast = new ArrayList<ClientLibrary>();    	
    	int port = this.serverPortNumber;
    	for(int counter = 0; counter < this.numberOfNotaries; counter++) {
    		try {
				clientLib = initiateNewClientLib(this.serverIP, port, counter+1);
				clientLibraryBroadcast.add(clientLib);
			} catch (ClientBroadcastException cbe) {
				//If you can't open a connection with the current one, open one with the next node
				System.out.println("initiateClientLibraryBroadcast(): something went wrong while initiating a ClientLibrary connection with Notary on port: " + port + "...");
			}
    		port++;
    	}    	    	    	
    	return clientLibraryBroadcast;
    }
    
    private ArrayList<ClientLibrary> initiateClientP2PLibrary() throws ClientBroadcastException {    	
    	ClientLibrary clientLib;    
    	ArrayList<ClientLibrary> clientP2PLibraryBroadcast = new ArrayList<ClientLibrary>();    	
    	int port = this.serverPortNumber;
    	for(int counter = 0; counter < this.numberOfNotaries; counter++) {
    		try {
				clientLib = initiateNewClientLib(this.serverIP, port, counter+1);
				clientP2PLibraryBroadcast.add(counter, clientLib);
			} catch (ClientBroadcastException cbe) {
				//If you can't open a connection with the client, abort program
				throw new ClientBroadcastException("initiateClientP2PLibrary(): something went wrong with the initiating a ClientLibrary...");				
			}
    	}    	    	    	
    	return clientP2PLibraryBroadcast;    	    
    }
    private void terminateClientLibraryBroadcast(ArrayList<ClientLibrary> clientLibraryBroadcast) {
    	ClientLibrary clientLib;
    	for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {
    		clientLib = clientLibraryBroadcast.get(counter);
    		clientLib.closeConnection();
    	}
    }
    private String sendWriteBack(String message, String good, String command) throws ClientBroadcastException {
    	String result;
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	ClientLibrary clientLib;
    	ArrayList<ClientLibrary> clientLibraryBroadcast;
    	
    	try {
			clientLibraryBroadcast = initiateClientLibraryBroadcast();
			
			for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
	    		clientLib = clientLibraryBroadcast.get(counter);  
	    		RemoteCallThread remoteCallThread = new RemoteCallThread(command, clientLib, 1120+counter+1, this.username);
	    		remoteCallThread.setMessage(message);
	    		remoteCallThread.setGood(good);
				new Thread(remoteCallThread).start();
				threadArrayList.add(remoteCallThread);
	    	}  
		} catch (ClientBroadcastException cle) {
			throw new ClientBroadcastException("sendWriteBack(): something went wrong while initiating the writeback ClientBroadcastLibrary...");
		}
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 30);
    	
    	return result;    	
    }
    private String sendWriteBack(String message, String good, String command, ArrayList<ClientLibrary> clientLibraryBroadcast) throws ClientBroadcastException {
    	String result;
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	ClientLibrary clientLib;
    						
		for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread(command, clientLib, 1120+counter+1, this.username);
    		remoteCallThread.setMessage(message);
    		remoteCallThread.setGood(good);
			new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}
		
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 30);
    	
    	return result;    	
    }
    private String sendWriteBack(String message, String good, String command, ArrayList<ClientLibrary> clientLibraryBroadcast, String seller) throws ClientBroadcastException {
    	String result;
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	ClientLibrary clientLib;
    						
		for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread(command, clientLib, 1120+counter+1, seller);
    		remoteCallThread.setMessage(message);
    		remoteCallThread.setGood(good);
    		new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}
		
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 40);
    	
    	return result;    	
    }
    private String setWriteBackMessage(String good, String lastMessage, String message, String operation, int freshnessVersion) {
    	JSONObject messageJSONObject, lastMessageJSONObject, valueJSONObject, sentJSONObject = new JSONObject();
    	String ownerId, state, sentMessageString;
    	int version, writeVersion;
    	
    	lastMessageJSONObject = new JSONObject(lastMessage);
    	
    	valueJSONObject = (JSONObject) lastMessageJSONObject.get("value");
    	
    	ownerId = (String) valueJSONObject.get("owner-id");
    	state = (String) valueJSONObject.get("state");    	
    	writeVersion = (int) lastMessageJSONObject.get("write-version");    	
    	
    	sentJSONObject.put("good", good);
    	sentJSONObject.put("owner-id", ownerId);
    	sentJSONObject.put("state", state);
    	sentJSONObject.put("version", freshnessVersion+1);
    	sentJSONObject.put("write-version", writeVersion+1);
    	sentJSONObject.put("operation", operation);
    	sentJSONObject.put("signer", this.username);
    	sentJSONObject.put("value", valueJSONObject);
    	
    	return sentJSONObject.toString();
    }
    public void ping() throws ClientBroadcastException {
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ClientLibrary clientLib;
    	ArrayList<ClientLibrary> clientLibraryBroadcast;
    	
    	clientLibraryBroadcast = initiateClientLibraryBroadcast();
    	for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread("PING", clientLib, 1120+counter+1, this.username);
			remoteCallThread.setCounter(counter);
			new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}    	
    	terminateClientLibraryBroadcast(clientLibraryBroadcast);
    }      
    public void getStateOfGood(String good) throws ClientBroadcastException {
    	String result;
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	ClientLibrary clientLib;
    	ArrayList<ClientLibrary> clientLibraryBroadcast;
    	
    	clientLibraryBroadcast = initiateClientLibraryBroadcast();
    	for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread("GET-STATE-OF-GOOD", clientLib, 1120+counter+1, this.username);
			remoteCallThread.setGood(good);
			remoteCallThread.setCounter(counter);
			new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}    	    	
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 10);
    	if(result.equals(OK_MESSAGE)) {    		    		
    		Object tuple[] = getHighestConsensusValue(threadArrayList);
    		String lastState = (String) tuple[3];
    		String message = (String) tuple[4];
    		int maxFreshVersion = (int) tuple[5];
    			    		    	    	
    		switch(sendWriteBack(setWriteBackMessage(good, lastState, message, "GSOF", maxFreshVersion), good, "WRITE-BACK", clientLibraryBroadcast)) {
				case OK_MESSAGE:
					System.out.println("Good '" + good + "' (version '" + tuple[1] + "'): < " + tuple[0] + ", " + tuple[2] + " >");
					break;
				case NOT_OK_MESSAGE:
					//System.out.println("Good '" + good + "' (version '" + tuple[1] + "'): < " + tuple[0] + ", " + tuple[2] + " >");
					break;
				default:
					System.out.println("Error! Could not obtain a proper consensus from the system about the writeback...");
					break;
    		}    		
    	} else if(result.equals(NOT_OK_MESSAGE)) { 
    		System.out.println("Could not get the state of good.");
    	} else if(result.equals(ERROR)) {
    		System.out.println("Error! Could not obtain a proper consensus from the system.");
    	
    	}    
    	terminateClientLibraryBroadcast(clientLibraryBroadcast);
    }
    
    public void intentionToSell(String good) throws ClientBroadcastException {
    	String result;
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ClientLibrary clientLib;
    	ArrayList<ClientLibrary> clientLibraryBroadcast;
    	
    	clientLibraryBroadcast = initiateClientLibraryBroadcast();
    	for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {    		
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread("INTENTION-TO-SELL-PHASE-ONE", clientLib, 1120+counter+1, this.username);
			remoteCallThread.setGood(good);
			remoteCallThread.setCounter(counter);
			new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}    	
    	
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 10);
    	try {
	    	if(result.equals(OK_MESSAGE)) {  
	    		Object tuple[] = getHighestConsensusValue(threadArrayList);
	    		String message = (String) tuple[4];    	
	    		int version = (int) tuple[5];
	    		
	    		JSONObject contentJSONObject = UtilMethods.convertStringToJSON(message);
		        message = UtilMethods.changeVersionOfJSON(contentJSONObject, version);
	    		
	    		switch(sendWriteBack(message, good, "INTENTION-TO-SELL-PHASE-TWO", clientLibraryBroadcast)) {
					case OK_MESSAGE:
						System.out.println("Good: " + good + " is now on sale.");
						break;
					case NOT_OK_MESSAGE:
						System.out.println("Could not set good: " + good + " on sale.");
						break;
					default:
						System.out.println("Error! Could not obtain a proper consensus from the system about the writeback...");
						break;
	    		}       		    		
	    	} else if(result.equals(NOT_OK_MESSAGE)) { 
	    		System.out.println("Could not set good: " + good + " on sale.");
	    	} else if(result.equals(ERROR)) {
	    		System.out.println("Error! Could not obtain a proper consensus from the system.");
	    	
	    	}    	
	    	terminateClientLibraryBroadcast(clientLibraryBroadcast);  
    	} catch(UtilMethodsException ume) {
    		throw new ClientBroadcastException("intentionToSell(): something went wrong with the UtilMethods class...");
    	}
    }
    
    //This is always called within a thread so this doesn't make any sense at all
    public void transferGood() throws ClientBroadcastException {
    	
    	ClientLibrary clientLib;
    	String buyGoodEncapsulatedMessage, buyer, terminateMessage, versionString, status, cryptoPuzzleChallenge, cryptoPuzzleResponse;
    	String encapsulatedContent[], freshnessMessage[];
    	JSONObject contentJSONObject;
    	int currentVersion, serverPort;
    	
    	synchronized(this) {
			try {
				buyGoodEncapsulatedMessage = this.p2pLibrary.buyGoodSellerRelaySeller();
				encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(buyGoodEncapsulatedMessage, 1);    			
				contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);
				buyer = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject, "buyer");
				serverPort = (int) UtilMethods.jsonGetObjectByKey(contentJSONObject, "notary");    			
				
				clientLib = initiateNewClientLib(this.serverIP, serverPort, serverPort%10);
				
				cryptoPuzzleChallenge = clientLib.transferGoodCryptoPuzzlePhaseOne();
				cryptoPuzzleResponse = this.p2pLibrary.transferGoodCryptoPuzzlePhaseTwo(cryptoPuzzleChallenge, buyer);
				clientLib.transferGoodCryptoPuzzlePhaseThree(cryptoPuzzleResponse);
				
				freshnessMessage = clientLib.transferGoodPhaseOne(this.username, buyGoodEncapsulatedMessage);
				versionString = freshnessMessage[0];
	            status = freshnessMessage[1];
	            
	            if(status.equals(OK_MESSAGE)) {
	            	encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(versionString, 1);
	            	contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);
	            	currentVersion = (int) UtilMethods.jsonGetObjectByKey(contentJSONObject, "version");
	            	
	                this.p2pLibrary.getClientSecurity().setCurrentVersion(currentVersion);
	                this.p2pLibrary.getClientSecurity().setNotary(serverPort%10);
	                versionString = this.p2pLibrary.buyGoodRelayFreshnessSeller(versionString, buyer);
	                
	                clientLib.getClientSecurity().setCurrentVersion(currentVersion);
	                terminateMessage = clientLib.transferGoodPhaseTwo(this.username, versionString);
	                this.p2pLibrary.terminateBuyGood(terminateMessage);
	            } else {
	            	this.p2pLibrary.buyGoodSellerTerminateMidways(versionString);                		               
	            }
	            clientLib.closeConnection();
	            p2pLibrary.closeConnection();
			} catch(ClientLibraryException cle) {	
				throw new ClientBroadcastException("transferGood(): something went wrong with the ClientLibrary while doing a remote operation...", cle);
			} catch (UtilMethodsException ume) {
				throw new ClientBroadcastException("transferGood(): something went wrong with the UitlMethods class...", ume);			
			} catch (ClientEncapsulationException cee) {
				throw new ClientBroadcastException("transferGood(): something went wrong with the ClientEncapsulation class...", cee);
			}    	
    	}
    }
    public void buyGood(String seller, String good) throws ClientBroadcastException {
    	String result;
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();    	
    	ArrayList<RemoteCallThread> threadArrayList = new ArrayList<RemoteCallThread>();
    	ArrayList<ClientLibrary> clientLibraryBroadcast;
    	ClientLibrary clientLib;
    	
    	clientLibraryBroadcast = initiateClientP2PLibrary();    	    	
    	for(int counter = 0; counter < clientLibraryBroadcast.size(); counter++) {
    		clientLib = clientLibraryBroadcast.get(counter);  
    		RemoteCallThread remoteCallThread = new RemoteCallThread("BUY-GOOD-PHASE-ONE", clientLib, 1120+counter+1, this.username);
			remoteCallThread.setGood(good);
			remoteCallThread.setSeller(seller);
			remoteCallThread.setCounter(counter);
			new Thread(remoteCallThread).start();
			threadArrayList.add(remoteCallThread);
    	}    	    	
    	result = endOperation(acknowledgeArrayList, threadArrayList, good, 20);
    	try {
	    	if(result.equals(OK_MESSAGE)) {    		
	    		Object tuple[] = getHighestConsensusValue(threadArrayList);
	    		String message = (String) tuple[4];    	
	    		int version = (int) tuple[5];
	    		
	    		JSONObject contentJSONObject = UtilMethods.convertStringToJSON(message);
		        message = UtilMethods.changeVersionOfJSON(contentJSONObject, version);
		        
	    		System.out.println(message);
	    		
	    		switch(sendWriteBack(message, good, "BUY-GOOD-PHASE-TWO", clientLibraryBroadcast, seller)) {
					case OK_MESSAGE:
						System.out.println("Successfully bought: " + good);
						break;
					case NOT_OK_MESSAGE:
						System.out.println("Could not buy: " + good);
						break;
					default:
						System.out.println("Error! Could not obtain a proper consensus from the system about the writeback...");
						break;
	    		}
	    		
	    		//JSONObject contentJSONObject = UtilMethods.convertStringToJSON(message);
		        //message = UtilMethods.changeVersionOfJSON(contentJSONObject, version);
		        
	    		//System.out.println(message);
	    		
	    		
	    	} else if(result.equals(NOT_OK_MESSAGE)) { 
	    		System.out.println("Could not buy good: " + good);
	    	} else if(result.equals(ERROR)) {
	    		System.out.println("Error! Could not obtain a proper consensus from the system.");
	    	
	    	}    	
	    	terminateClientLibraryBroadcast(clientLibraryBroadcast);
    	} catch(UtilMethodsException ume) {
    		throw new ClientBroadcastException("buyGood(): something went wrong with the UtilMethods class...");
    	}
    }    
      
    
    private ArrayList<String> checkForAcknowledge(ArrayList<RemoteCallThread> threadArrayList, int sleep) throws ClientBroadcastException {
    	String ack;
    	ArrayList<String> acknowledgeArrayList = new ArrayList<String>();
    	RemoteCallThread currentThread;
		try {
			Thread.sleep(1000*sleep);
			
			for(int counter = 0; counter < threadArrayList.size(); counter++) {
				currentThread = threadArrayList.get(counter);
				ack = currentThread.getAck();
				acknowledgeArrayList.add(counter, ack);				
			}
			
			return acknowledgeArrayList;
		} catch (InterruptedException ie) {
			throw new ClientBroadcastException("checkForAcknowledge(): something went wrong with the current thread...", ie);
		}
    }
    
    private String proocedAccordingToAckowledges(ArrayList<String> acknowledgeList) {    	
    	int countOkAcks = 0;
    	int countNokAcks = 0;
    	System.out.println("###");
    	for(int counter = 0; counter < acknowledgeList.size(); counter++) {
    		String ack = acknowledgeList.get(counter);
    		System.out.println(ack);
    		if(ack.equals(OK_MESSAGE)) {
    			countOkAcks++;
    		}
    		if(ack.equals(NOT_OK_MESSAGE)) {
    			countNokAcks++;
    		}
    	}
    	//Duvida: em sec. SE todos os processos retornam um ACK, e o mesmo ACK? Nao, os bizantinos podem mandar coisas erradas
    	//Duvida: em sec. Se todos os processos correctos retornam um ACK, e o mesmo ACK? Sim
    	
    	float firstOperand = (float) this.numberOfNotaries; // N
    	float secondOperand = (float) this.numberOfFailures; // F
    	float divisor = (float) 2.0;

    	// threshold = (N+F)/2
    	float threshold = (firstOperand + secondOperand) / divisor;
    	float readListLength = (float)countOkAcks + (float)countNokAcks;

    	System.out.println(readListLength + "," + threshold + "," + countOkAcks + "," + countNokAcks);
    	if(readListLength > threshold) {
    		/*
    		v := highestval(readlist);
			readlist := [⊥]^N ;
			trigger  (bonrr, ReadReturn | v);
    		*/
    		boolean okOutput = countOkAcks>countNokAcks;
    		if(okOutput) {
    			return OK_MESSAGE;
    		} else {
    			return NOT_OK_MESSAGE;
    		}
    	} else {
    		return ERROR; 
    	}
    }
    
    private String endOperation(ArrayList<String> acknowledgeArrayList, ArrayList<RemoteCallThread> threadArrayList, String good, int sleep) throws ClientBroadcastException {
    	String result;
    	try {
			acknowledgeArrayList = checkForAcknowledge(threadArrayList, sleep);
		} catch (ClientBroadcastException cbe) {
			throw new ClientBroadcastException("endOperation(): something went wrong while trying to obtain the acknowledges");
		}
    	result = proocedAccordingToAckowledges(acknowledgeArrayList);
    	return result;
    }

    // Client Quorum: protects against byzantine notary
    private Object[] getHighestConsensusValue(ArrayList<RemoteCallThread> threadArrayList) throws ClientBroadcastException {
		Object[] tuple = getHighestConsensusValueAux(threadArrayList);
		//Object[] tuple = getHighestValue(threadArrayList);
		//List<Integer> writeVersionList = (List<Integer>) tuple[6];
		//int maxWriteVersion = (int) tuple[1];
		//String ownerId = (String) tuple[0];
		//String status = (String) tuple[2];
		//String lastStateReturn = (String) tuple[3];
		//String messageReturn = (String) tuple[4];
		//int maxFreshVersion = (int) tuple[5];
		/*
		boolean quorumSatisfied = verifyQuorum(writeVersionList, maxWriteVersion);
		// spaghetti code to achieve client quorum consensus with max 2 failures
		if (!quorumSatisfied) {
			// remove all obsolete versions and try again: f = 1
			int index;
			while((index = writeVersionList.indexOf(Integer.valueOf(maxWriteVersion)))!=-1) {
				// remove thread by index
				writeVersionList.remove(index);
				threadArrayList.remove(index);
			}
			// redo
			tuple = getHighestValue(threadArrayList);
			writeVersionList = (List<Integer>) tuple[6];
			maxWriteVersion = (int) tuple[1];
			quorumSatisfied = verifyQuorum(writeVersionList, maxWriteVersion);
			if (!quorumSatisfied) {
				throw new ClientBroadcastException("Client Quorum wasn't satisfied. Notaries de-synchronized.");
			}
		}*/

		return tuple;
    }

    private Object[] getHighestConsensusValueAux(ArrayList<RemoteCallThread> threadArrayList) {
		Object[] tuple = getHighestValue(threadArrayList);
		List<Integer> writeVersionList = (List<Integer>) tuple[6];
		int maxWriteVersion = (int) tuple[1];
		Object[] newTuple = tuple;

		boolean quorumSatisfied = verifyQuorum(writeVersionList, maxWriteVersion);
		if (!quorumSatisfied) {
			int index;
			while((index = writeVersionList.indexOf(Integer.valueOf(maxWriteVersion)))!=-1) {
				// remove thread by index
				writeVersionList.remove(index);
				threadArrayList.remove(index);
			}
			newTuple = getHighestConsensusValueAux(threadArrayList);
		}
		return newTuple;
	}

    private Object[] getHighestValue(ArrayList<RemoteCallThread> threadArrayList) {
		JSONObject jsonObject;
		String ownerId = "", status = "", lastState, lastStateReturn = "", messageReturn = "";
		int writeVersion, freshVersion, maxWriteVersion = -1, maxFreshVersion = -1;
		List<Integer> writeVersionList = new ArrayList<>();
		for(int i = 0; i < threadArrayList.size(); i++) {
			if(threadArrayList.get(i).getAck().equals(OK_MESSAGE)) {
				jsonObject = new JSONObject(threadArrayList.get(i).getMessage());
				freshVersion = (int) jsonObject.get("version");
				if(freshVersion>maxFreshVersion) {
					maxFreshVersion = freshVersion;
				}

				lastState = (String) jsonObject.get("lastState");

				JSONObject lastStateJSONObject = new JSONObject(lastState);
				writeVersion = (int) lastStateJSONObject.get("write-version");
				writeVersionList.add(writeVersion);
				// write version tem de ser maior que maxWriteVersion e, tambem,
				// (N+F)/2, ou seja, o numero de notarios que tem de concordar
				// com a writeVersion tem de ser maior do que F ou da erro
				if(writeVersion>maxWriteVersion) {
					maxWriteVersion = writeVersion;
					JSONObject valueJSONObject = (JSONObject) lastStateJSONObject.get("value");
					ownerId = (String) valueJSONObject.get("owner-id");
					status = (String) valueJSONObject.get("state");
					lastStateReturn = lastState;
					messageReturn = jsonObject.toString();
				}
			}
		}
		return new Object[]{ownerId, maxWriteVersion, status, lastStateReturn, messageReturn, maxFreshVersion, writeVersionList};
	}
    private boolean verifyQuorum(List<Integer> writeVersionList, int maxWriteVersion) {
		int listSize = writeVersionList.size();
		int n_maxVal = 0;
		for(int i = 0; i < listSize; ++i) {
			if (writeVersionList.get(i)==maxWriteVersion) {
				n_maxVal += 1;
			}
		}
		System.out.println("WTSs: " + writeVersionList);
		// 1. enough: n_max > f
		return (n_maxVal>this.numberOfFailures);
		// 2. exaggeration: n_max > (N+F)/2
		//return (n_maxVal>(this.numberOfNotaries + this.numberOfFailures)/2);
	}
}