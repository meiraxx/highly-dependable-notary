package pt.ulisboa.tecnico.sec.notary;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryBroadcastException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryEncapsulationException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;

public class NotaryBroadcast {
	
	private NotaryLibrary notaryLibrary;
	private String option;
	private int notary;
	private int numberOfNotaries;
	private int numberOfFailures;
	
	public NotaryBroadcast(NotaryLibrary notaryLibrary, String option, int notary, int numberOfNotaries, int numberOfFailures) {
		this.notaryLibrary = notaryLibrary;
		this.option = option;
		this.notary = notary;
		this.numberOfNotaries = numberOfNotaries;
		this.numberOfFailures = numberOfFailures;
	}
	
	private NotaryLibrary initiateNewNotaryClientLib(String serverIP, int serverPortNumber) throws NotaryBroadcastException {
        Socket notaryClientSocket = initiateNotaryClientSocket(serverIP, serverPortNumber);
        Communications notaryClientCommunication = new Communications(notaryClientSocket);
        return new NotaryLibrary(notaryClientCommunication, this.option, this.notary);
    }
    
    private Socket initiateNotaryClientSocket(String serverIP, int serverPortNumber) throws NotaryBroadcastException {
        Socket notaryClientSocket;
        try {
        	notaryClientSocket = new Socket(serverIP, serverPortNumber);
        } catch (UnknownHostException uhe) {
            throw new NotaryBroadcastException("Could not find a listening port in '" + serverIP + "' host machine, port '"
                    + serverPortNumber + "'. Client might be down.", uhe);
        } catch (IOException ioe) {
            throw new NotaryBroadcastException("initiateClientSocket(): Could not initialize my client socket. Aborting...", ioe);
        }
        return notaryClientSocket;
    }
    private ArrayList<NotaryLibrary> initiateNotaryClientLibraryBroadcast(String serverIP, int port, int myPort) throws NotaryBroadcastException {    	
    	NotaryLibrary notaryClientLib;    
    	ArrayList<NotaryLibrary> notaryClientLibraryBroadcast = new ArrayList<NotaryLibrary>();    	
    	for(int counter = 0; counter < this.numberOfNotaries; counter++) {
    		if(myPort!=(counter+1)) {
	    		try {
	    			notaryClientLib = initiateNewNotaryClientLib(serverIP, port);
	    			notaryClientLibraryBroadcast.add(notaryClientLib);
				} catch (NotaryBroadcastException nbe) {
					//If you can't open a connection with the current one, open one with the next node
					System.out.println("initiateClientLibraryBroadcast(): something went wrong while initiating a ClientLibrary connection with Notary on port: " + port + "...");
				}	    		
    		}
    		port++;
    	}    	    	    	
    	return notaryClientLibraryBroadcast;
    }
    private void runNotaryClientLibraryBroadcast(ArrayList<NotaryLibrary> notaryClientLibraryBroadcast, String command, String message) {
    	NotaryLibrary notaryClientLib;    	
    	BroadcastThread broadcastThread;
    	for(int counter = 0; counter < notaryClientLibraryBroadcast.size(); counter++) {
    		notaryClientLib = notaryClientLibraryBroadcast.get(counter);
    		switch(command) {
	    		case "GET-STATE-OF-GOOD-ECHO":
					broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;
	    		case "GET-STATE-OF-GOOD-READY":
	    			broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;
				case "INTENTION-TO-SELL-ECHO":
					broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;
				case "INTENTION-TO-SELL-READY":
					broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;		
				case "TRANSFER-GOOD-ECHO":
					broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;
				case "TRANSFER-GOOD-READY":
					broadcastThread = new BroadcastThread(command, notaryClientLib, "Notary" + this.notary);
					broadcastThread.setMessage(message);
					new Thread(broadcastThread).start();	    					    				
					break;	
				default:
					System.out.println("That option does not exist! Aborting...");
					return;
			}
    	}
    }
	String receiveCommand() throws NotaryBroadcastException {
		String command;
		try {
			command = this.notaryLibrary.receiveCommand();
			return command;
		} catch (NotaryLibraryException nle) {
			throw new NotaryBroadcastException("receiveCommand(): something went wrong with NotaryLibrary class...", nle);
		}		
	}	
	void getStateOfGood() throws NotaryLibraryException {
		ArrayList<NotaryLibrary> notaryClientLibraryBroadcast;
		String receivedJSONString;		
		this.notaryLibrary.getStateOfGood();
		receivedJSONString = this.notaryLibrary.receiveWriteBack();
		try {
			notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1130+1, this.notary);			
			runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "GET-STATE-OF-GOOD-ECHO", receivedJSONString);
			
			Thread.sleep(1000*10);
			
			ArrayList<ReceiveEchoThread> arrayListReceiveEcho = Notary.waitForEchoMessages.workerThreadArrayList;
			String mostCommonEchoMessage = getEchoMessage(arrayListReceiveEcho, receivedJSONString);
			if(mostCommonEchoMessage!=null) {
				//sentReady true
				
				notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
				runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "GET-STATE-OF-GOOD-READY", mostCommonEchoMessage);
				
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.writeBackWriteState(mostCommonReadyMessageWithReadyTrue);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}
			} else {
				//sentReadyFalse
				//timers tem de ser trocados
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				// este parametro era o que tinha de ser passado abaixo para evitar a excepcao do crypto
				String mostCommonReadyMessageWithReadyFalse = getReadyMessageWithSentReadyFalse(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyFalse!=null) {
					notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
					runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "GET-STATE-OF-GOOD-READY", mostCommonReadyMessageWithReadyFalse);
				} 
				Thread.sleep(1000*5);
				arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.writeBackWriteState(mostCommonReadyMessageWithReadyTrue);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}				
			}			
		} catch (NotaryBroadcastException nbe) {
			throw new NotaryLibraryException("getStateOfGood(): something went wrong "
					+ "with the NotaryBroadcast class...", nbe);
		} catch (InterruptedException ie) {
			throw new NotaryLibraryException("getStateOfGood(): something went wrong"
					+ "with the Interrupted class...", ie);
		} 		
	}
	void intentionToSell() throws NotaryLibraryException {
		ArrayList<NotaryLibrary> notaryClientLibraryBroadcast;
		String receivedJSONString, message;
		try {
			receivedJSONString = this.notaryLibrary.intentionToSellPhaseOne();
			
			notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1130+1, this.notary);			
			runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "INTENTION-TO-SELL-ECHO", receivedJSONString);
					
			Thread.sleep(1000*10);
			
			ArrayList<ReceiveEchoThread> arrayListReceiveEcho = Notary.waitForEchoMessages.workerThreadArrayList;
			String mostCommonEchoMessage = getEchoMessage(arrayListReceiveEcho, receivedJSONString);
			if(mostCommonEchoMessage!=null) {
				//sentReady true
				
				notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
				runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "INTENTION-TO-SELL-READY", mostCommonEchoMessage);
				
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.intentionToSellPhaseTwo(mostCommonReadyMessageWithReadyTrue);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}
			} else {
				//sentReadyFalse
				//timers tem de ser trocados
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyFalse = getReadyMessageWithSentReadyFalse(arrayListReceiveReady, receivedJSONString);
				// este parametro era o que tinha de ser passado abaixo para evitar a excepcao do crypto
				if(mostCommonReadyMessageWithReadyFalse!=null) {
					notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
					runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "INTENTION-TO-SELL-READY", mostCommonReadyMessageWithReadyFalse);
				} 
				Thread.sleep(1000*5);
				arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;				//
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.intentionToSellPhaseTwo(mostCommonReadyMessageWithReadyTrue);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}				
			}			
		} catch(NotaryLibraryException nle) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong "
					+ "with the NotaryLibrary class...", nle);
		} catch (NotaryBroadcastException nbe) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong "
					+ "with the NotaryBroadcast class...", nbe);
		} catch (InterruptedException ie) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong"
					+ "with the Interrupted class...", ie);
		} 
	}
	void transferGood() throws NotaryLibraryException {
		String receivedJSONString, message;
		ArrayList<NotaryLibrary> notaryClientLibraryBroadcast;
		
		try {
			receivedJSONString = this.notaryLibrary.transferGoodPhaseOne();
			
			notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1130+1, this.notary);			
			runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "TRANSFER-GOOD-ECHO", receivedJSONString);
					
			Thread.sleep(1000*10);
			
			ArrayList<ReceiveEchoThread> arrayListReceiveEcho = Notary.waitForEchoMessages.workerThreadArrayList;
			String mostCommonEchoMessage = getEchoMessage(arrayListReceiveEcho, receivedJSONString);
			if(mostCommonEchoMessage!=null) {
				//sentReady true
				//timers tem de ser trocados
				
				notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
				runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "TRANSFER-GOOD-READY", mostCommonEchoMessage);
				
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.transferGoodPhaseTwo(receivedJSONString);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}
			} else {
				//sentReadyFalse
				//timers tem de ser trocados
				Thread.sleep(1000*10);
				
				ArrayList<ReceiveEchoThread> arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyFalse = getReadyMessageWithSentReadyFalse(arrayListReceiveReady, receivedJSONString);
				// este parametro era o que tinha de ser passado abaixo para evitar a excepcao do crypto
				if(mostCommonReadyMessageWithReadyFalse!=null) {
					notaryClientLibraryBroadcast = initiateNotaryClientLibraryBroadcast("localhost", 1140+1, this.notary);
					runNotaryClientLibraryBroadcast(notaryClientLibraryBroadcast, "TRANSFER-GOOD-READY", mostCommonReadyMessageWithReadyFalse);
				} 
				Thread.sleep(1000*5);
				arrayListReceiveReady = Notary.waitForReadyMessages.workerThreadArrayList;
				String mostCommonReadyMessageWithReadyTrue = getReadyMessageWithSentReadyTrue(arrayListReceiveReady, receivedJSONString);
				if(mostCommonReadyMessageWithReadyTrue!=null) {
					this.notaryLibrary.transferGoodPhaseTwo(receivedJSONString);
				} else {
					System.out.println("not a true consensus found"
							+ " client might be byzantine, aborting");
					throw new NotaryBroadcastException("intentionToSell(): not a true consensus found"
							+ " client might be byzantine, aborting...");	
				}				
			}		
		} catch (NotaryLibraryException nle) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong "
					+ "with the NotaryLibrary class...", nle);
		} catch (InterruptedException ie) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong"
					+ "with the Interrupted class...", ie);
		} catch (NotaryBroadcastException nbe) {
			throw new NotaryLibraryException("intentionToSell(): something went wrong "
					+ "with the NotaryBroadcast class...", nbe);
		}
	}
	String getEchoMessage(ArrayList<ReceiveEchoThread> workerThreadArrayList, String currentMessage) {
		HashMap<String, Integer> messageMapping = null;
		messageMapping = getEchoMessageResult(workerThreadArrayList, currentMessage);
		Object tuple[];
		
		tuple = getMostCommonMessage(messageMapping);
		if(Integer.valueOf((String) tuple[1]) > ((this.numberOfNotaries + this.numberOfFailures)/2)) {
			return (String) tuple[0];
		} else {
			return null;
		}
	}
	String getReadyMessageWithSentReadyTrue(ArrayList<ReceiveEchoThread> workerThreadArrayList, String currentMessage) {
		HashMap<String, Integer> messageMapping = null;
		messageMapping = getEchoMessageResult(workerThreadArrayList, currentMessage);
		Object tuple[];
		
		tuple = getMostCommonMessage(messageMapping);
		if(Integer.valueOf((String) tuple[1]) > (2 * this.numberOfFailures)) {
			return (String) tuple[0];
		} else {
			return null;
		}
	}
	String getReadyMessageWithSentReadyFalse(ArrayList<ReceiveEchoThread> workerThreadArrayList, String currentMessage) {
		HashMap<String, Integer> messageMapping = null;
		messageMapping = getEchoMessageResult(workerThreadArrayList, currentMessage);
		Object tuple[];
		
		tuple = getMostCommonMessage(messageMapping);
		if(Integer.valueOf((String) tuple[1]) > (this.numberOfFailures)) {
			return (String) tuple[0];
		} else {
			return null;
		}
	}
	private HashMap<String, Integer> getEchoMessageResult(ArrayList<ReceiveEchoThread> workerThreadArrayList, String currentMessage) {
		Iterator<ReceiveEchoThread> iteratorWorkerThread = workerThreadArrayList.iterator();
		ReceiveEchoThread receiveEchoThread;
		String echoMessage;
		HashMap<String, Integer> messageMapping = new HashMap<String, Integer>();
		Integer count;
		
		Integer initialInt = new Integer(1);	
		messageMapping.put(currentMessage, initialInt);
		//getAllEchoMessages
		while(iteratorWorkerThread.hasNext()) {
			receiveEchoThread = iteratorWorkerThread.next();
			echoMessage = receiveEchoThread.getEchoMessage();
			if(echoMessage!=null) {								
				if(messageMapping.containsKey(echoMessage)) {
					int currentCount = messageMapping.get(echoMessage);
					Integer currentCountInteger = new Integer(currentCount+1);
					messageMapping.put(echoMessage, currentCountInteger);
				} else {
					count = new Integer(1);
					messageMapping.put(echoMessage, count);
				}
			}		
		}	
		
		return messageMapping;
	}
	
	private Object[] getMostCommonMessage(HashMap<String, Integer> messageMapping) {
		//getMostCommonMessage
		Iterator<Entry<String,Integer>> iterator = messageMapping.entrySet().iterator();
		Object tuple[] = new Object[]{"", ""};
	    while (iterator.hasNext()) {
	        HashMap.Entry<String,Integer> pair = (HashMap.Entry<String,Integer>)iterator.next();
	        
	        if(tuple[1].equals("")) {
	        	tuple[0] = (String) pair.getKey();
	        	tuple[1] = String.valueOf(pair.getValue());
	        } else if(Integer.valueOf((String) tuple[1])<(int) pair.getValue()) {
	        	tuple[0] = (String) pair.getKey();
	        	tuple[1] = String.valueOf(pair.getValue());
	        } 
	        iterator.remove();
	    }
		return tuple;
	}
}
