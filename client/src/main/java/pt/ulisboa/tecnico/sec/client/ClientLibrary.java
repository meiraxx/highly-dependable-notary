package pt.ulisboa.tecnico.sec.client;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.CryptoAux;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientEncapsulationException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientSecurityException;
import pt.ulisboa.tecnico.sec.client.exceptions.RoutingOperationsException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;
import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.communications.exceptions.CommunicationsException;
import pt.ulisboa.tecnico.sec.exceptions.CryptoAuxException;

class ClientLibrary {

    private static final String OK_MESSAGE = "OK";
    private static final String NOT_OK_MESSAGE = "NOT-OK";        
    
    private Communications communication;
    
    private ClientSecurity clientSec;
    private RoutingOperations routingOps;
    
    private String keystoreFilePath;
    //private String clientCertPath;
    private String username;
    private String option;
    
    private int notary;   

    ClientLibrary(Communications communication, String username, String option, int notary) {
        this.communication = communication;
        this.username = username;
        this.keystoreFilePath = "userKeystore/" + username + "keystore.jks";
        //this.clientCertPath = "userKeystore/" + username + ".pem";
        this.option = option;
        this.notary = notary;
        initializeModules();
    }
    
    private void initializeModules() {
    	this.clientSec = new ClientSecurity(this.communication, this.keystoreFilePath, this.username, this.option, this.notary);
    	this.routingOps = new RoutingOperations(this.clientSec);
    }
    
    public ClientSecurity getClientSecurity() {
    	return this.clientSec;
    }
    
    private void setClientSecurity(ClientSecurity clientSec) {
    	this.clientSec = clientSec;
    }
    
    public RoutingOperations getRoutingOperations() {
    	return this.routingOps;
    }
    
    private void setRoutingOperations(RoutingOperations routingOps) {
    	this.routingOps = routingOps;
    }
    
    void closeConnection() {
        try {
            this.communication.end();
        } catch (CommunicationsException ce) {
            // no problem
        }
    }
    
    String receiveCommand() throws ClientLibraryException {
        String contentString, command;
        JSONObject contentJSONObject;
        try {
        	contentString = this.clientSec.authenticatedClientReceive("callCommand", null);
        
            contentJSONObject = UtilMethods.convertStringToJSON(contentString);
            command = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"command");
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("receiveCommand(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("receiveCommand(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
        return command;
    }
    	
    // error handling
    private boolean handleStatus(JSONObject contentJSONObject) throws ClientLibraryException {
        String error, status;

        try {
            status = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"status");
            if (status.equals(OK_MESSAGE)) {
                // operation successful
                return true;
            }
            else if (status.equals(NOT_OK_MESSAGE)) {
                error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
                System.out.println(error);
                return false;
            }
            else
                throw new ClientLibraryException("handleStatus(): Something went terribly wrong, didn't receive OK or NOT_OK message.");
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("handleStatus(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        }
    }
    private String removeLastState(String contentString) throws ClientLibraryException {
    	JSONObject contentJSONObject; 
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        contentJSONObject.remove("lastState");
	        contentString = contentJSONObject.toString();
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("addSignerJSONField(): something went wrong with the UtilMethods class...");
    	}
    	
    	return contentString;
    }
    private String incrementVersion(String contentString) throws ClientLibraryException {
    	JSONObject contentJSONObject; 
    	int version; 		
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        version = ClientEncapsulation.fetchVersion(contentString);
	        contentString = UtilMethods.changeVersionOfJSON(contentJSONObject, version);
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("incrementVersion(): something went wrong with the UtilMethods class...");
    	} catch(ClientEncapsulationException cee) {
    		throw new ClientLibraryException("incrementVersion(): something went wrong with the ClientEncapsulation class...");
    	}
    	
    	return contentString;
    }
    private String incrementWriteVersion(String contentString) throws ClientLibraryException {
    	JSONObject contentJSONObject, lastStateJSONObject;
    	String lastStateString;
    	int writeVersion; 		
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	    	
	    	lastStateString = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject, "lastState");
	    	lastStateJSONObject = UtilMethods.convertStringToJSON(lastStateString);
	    	
	    	writeVersion = (int) UtilMethods.jsonGetObjectByKey(lastStateJSONObject, "write-version");
	    		    	
	    	contentString = UtilMethods.changeWriteVersionOfJSON(contentJSONObject, writeVersion);
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("incrementVersion(): something went wrong with the UtilMethods class...");
    	}
    	
    	return contentString;
    }
    private String addSignerJSONField(String contentString, String signer) throws ClientLibraryException {
    	JSONObject contentJSONObject; 		
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        contentJSONObject.put("signer", signer);
	        contentString = contentJSONObject.toString();
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("addSignerJSONField(): something went wrong with the UtilMethods class...");
    	}
    	
    	return contentString;
    }
    private String addOperationToJSONField(String contentString, String operation) throws ClientLibraryException {
    	JSONObject contentJSONObject; 
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        contentJSONObject.put("operation", operation);
	        contentString = contentJSONObject.toString();
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("addSignerJSONField(): something went wrong with the UtilMethods class...");
    	}
    	
    	return contentString;
    }
    private String addWriteValueToJSONField(String contentString, JSONObject writeValue) throws ClientLibraryException {
    	JSONObject contentJSONObject; 
    	
    	try {
	    	contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        contentJSONObject.put("value", writeValue);
	        contentString = contentJSONObject.toString();
    	} catch(UtilMethodsException ume) {
    		throw new ClientLibraryException("addSignerJSONField(): something went wrong with the UtilMethods class...");
    	}
    	
    	return contentString;
    }

    private boolean checkForSentGood(JSONObject contentJSONObject, String name) throws ClientLibraryException {
    	String receivedName;
    	
    	try {
    		receivedName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject, "good");
    		
    		if(receivedName.equals(name)) {
    			return true;
    		} else {
    			System.out.println("checkForSentGood(): the received good is not the one you wanted to buy!");
    			return false; 
    		}
		} catch (UtilMethodsException ume) {
			throw new ClientLibraryException("checkForSentGood(): something went wrong with the UtilMethods class...");
		}
    }

    // aux
    void sendCommand(String user, String command) throws ClientLibraryException {
        JSONObject sentJSONObject;
        String sentJSONString;

        sentJSONObject = new JSONObject();
        sentJSONObject.put("user", user);
        sentJSONObject.put("command", command);
        sentJSONString = sentJSONObject.toString();
        try {
        	this.clientSec.sendSignedMessage(sentJSONString);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("sendCommand(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }
    
    Object[] ping(String user) throws ClientLibraryException {
    	String message;
    	JSONObject jsonObject = new JSONObject();
    	try {
			sendCommand(user, "PING");
			
			jsonObject.put("user", user);
			message = jsonObject.toString();
			this.clientSec.sendSignedMessage(message);
			
			message = this.clientSec.authenticatedReceiveFromNotary("ping");
			jsonObject = new JSONObject(message);
			
			if(handleStatus(jsonObject)) {
				System.out.println("ping(): success!");
				return new Object[]{true, ""};
			} 
			
			return new Object[]{false, ""};
		} catch (ClientSecurityException cse) {
			throw new ClientLibraryException("ping(): something went wrong with sendCommand() method...", cse);
		}    		
    }

    Object[] getStateOfGood(String user, String good) throws ClientLibraryException {
        JSONObject sentJSONObject, contentJSONObject;
        String sentJSONString, contentString, stateOfGood, ownerId, error;
        int version;        

        try {
            sendCommand(user, "GET-STATE-OF-GOOD");

            // send good with version information
            sentJSONObject = new JSONObject();
            sentJSONObject.put("user", user);
            sentJSONObject.put("good", good);
            sentJSONString = sentJSONObject.toString();
            this.clientSec.sendSignedMessage(sentJSONString);

            // receive state of good
            contentString = this.clientSec.authenticatedReceiveFromNotary("getStateOfGood");
            contentJSONObject = UtilMethods.convertStringToJSON(contentString);
            
            if(!handleStatus(contentJSONObject)) {
            	error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
            	return new Object[]{false, error};
            }
            if(!this.clientSec.validatePreviousContent(contentJSONObject)) {
            	return new Object[]{false, "Error: Previous write was not state verified"};
            }
            if(handleStatus(contentJSONObject)) {
	            // parse and show state of good
	            stateOfGood = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"state");
	            ownerId = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"owner-id");
	            version = (int) UtilMethods.jsonGetObjectByKey(contentJSONObject,"version");
	            //System.out.println("Good '" + good + "' (version '" + version + "'): < " + ownerId + ", " + stateOfGood + " >");
	            
	            //returns true plus the original String
	            return new Object[]{true, contentString};
            }else {
		        error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
		        return new Object[]{false, error};
            }
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("getStateOfGood(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("getStateOfGood(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }

    Object[] intentionToSellPhaseOne(String seller, String good) throws ClientLibraryException {
        JSONObject sentJSONObject, contentJSONObject;
        String sentJSONString, contentString, error;

        try {
            sendCommand(seller,"INTENTION-TO-SELL");

            sentJSONObject = new JSONObject();
            sentJSONObject.put("seller", seller);
            sentJSONObject.put("good", good);
            sentJSONString = sentJSONObject.toString();
            
            // send seller name and good name
            this.clientSec.sendSignedMessage(sentJSONString);

            // ensure freshness
            contentString = this.clientSec.ensureFreshnessNotary("intentionToSellFresh");

            contentJSONObject = UtilMethods.convertStringToJSON(contentString);
            
            if(!handleStatus(contentJSONObject)) {            	
            	error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
            	return new Object[]{false, error};
            }            
            if(!checkForSentGood(contentJSONObject, good)) {
            	return new Object[]{false, "Error: Not the good previously sent!"};
            }
            if(!this.clientSec.validatePreviousContent(contentJSONObject)) {
            	return new Object[]{false, "Error: Previous write was not state verified!"};
            }
            
            return new Object[]{true, contentString};
            
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("intentionToSell(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("intentionToSell(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }
    
    Object[] intentionToSellPhaseTwo(String contentString, String good) throws ClientLibraryException {
    	String error;
    	JSONObject contentJSONObject;
    	
    	try {
	        contentString = incrementWriteVersion(contentString);
	        contentString = addSignerJSONField(contentString, this.username);
	        contentString = addOperationToJSONField(contentString, "ITS");
	        contentString = removeLastState(contentString);
	        
	        JSONObject writeValueJSONObject = new JSONObject();
	        writeValueJSONObject.put("owner-id", this.username);
	        writeValueJSONObject.put("state", "on-sale");
	        
	        contentString = addWriteValueToJSONField(contentString, writeValueJSONObject);
	        
	        
	        this.clientSec.sendSignedMessage(contentString);
	
	        // receive ok
	        contentString = this.clientSec.authenticatedReceiveFromNotary("intentionToSell");
	        contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        
	        if(handleStatus(contentJSONObject)) {
	        	System.out.println("The state of good '" + good + "' has been successfully changed to 'on-sale'.");
	        	return new Object[]{true, "The state of good '" + good + "' has been successfully changed to 'on-sale'."};
	        }else {
		        error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
		        return new Object[]{false, error};
	        }
    	} catch (UtilMethodsException ume) {
            throw new ClientLibraryException("intentionToSell(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("intentionToSell(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }
    
    String transferGoodCryptoPuzzlePhaseOne() throws ClientLibraryException {
    	String cryptoPuzzleChallenge;
    	
    	try {
			sendCommand(this.username, "TRANSFER-GOOD");
			// communicate w/ notary
	        cryptoPuzzleChallenge = this.clientSec.receiveCryptoPuzzleChallenge();
	        
	        return cryptoPuzzleChallenge;
    	} catch(ClientSecurityException cse) {
    		throw new ClientLibraryException("transferGoodCryptoPuzzlePhaseOne(): something went wrong with the ClientSecurity class...", cse);
    	}
        //this.clientSec.sendCryptoPuzzleAnswer(cryptoPuzzleAnswer, seller);
    }
    
    String transferGoodCryptoPuzzlePhaseTwo(String cryptoPuzzleChallenge, String buyer) throws ClientLibraryException {
    	String cryptoPuzzleResponse;
    	try {
    		this.clientSec.sendSignedMessage(cryptoPuzzleChallenge);
    		
    		cryptoPuzzleResponse = this.clientSec.authenticatedClientReceive("cryptoPuzzlePhaseTwo", buyer);
    		
    		return cryptoPuzzleResponse;
    	} catch(ClientSecurityException cse) {
    		throw new ClientLibraryException("transferGoodCryptoPuzzlePhaseTwo(): something went wrong with the ClientSecurity class...", cse);
    	}
    }
    void transferGoodCryptoPuzzlePhaseThree(String cryptoPuzzleResponse) throws ClientLibraryException {
    	try {
    		this.clientSec.sendSignedMessage(cryptoPuzzleResponse);    		    	
    	} catch(ClientSecurityException cse) {
    		throw new ClientLibraryException("transferGoodCryptoPuzzlePhaseTwo(): something went wrong with the ClientSecurity class...", cse);
    	}
    }
    
    // receives and verifies received crypto-puzzle answer is correct
 	void sendCryptoPuzzleAnswer(String cryptoPuzzleAnswer, String user, String seller) throws ClientSecurityException {
 		JSONObject sentJSONObject;
 		String sentJSONString;
 		// send good with version information
 		sentJSONObject = new JSONObject();
 		sentJSONObject.put("user", user);
 		sentJSONObject.put("seller", seller);
 		sentJSONObject.put("cryptoPuzzleAnswer", cryptoPuzzleAnswer);
 		sentJSONString = sentJSONObject.toString();
 		this.clientSec.sendSignedMessage(sentJSONString);
 	}

    String[] transferGoodPhaseOne(String seller, String sentJSONString) throws ClientLibraryException {    
    	JSONObject contentJSONObject = null;
        String contentString, versionString, freshnessMessage[];
        //devido ao cripto puzzle o sendCommand agora esta no broadcast thread 
        //sendCommand(seller, "TRANSFER-GOOD");
        try {                    	       
			this.clientSec.sendSignedMessage(sentJSONString);
	
			// ensure freshness
			versionString = this.clientSec.ensureFreshnessNotary("transferGood");
			contentString = ClientEncapsulation.getEncapsulatedContent(versionString, 1)[0];
        
			contentJSONObject = UtilMethods.convertStringToJSON(contentString);
		} catch (UtilMethodsException ume) {
			throw new ClientLibraryException("transferGoodPhaseOne: something went wrong with the UtilMethods class");
		} catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("transferGoodPhaseOne(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        } catch (ClientEncapsulationException cee) {
        	throw new ClientLibraryException("transferGoodPhaseOne(): Something went wrong with the ClientEncapsulation module."+
                    "Message: '" + cee.getMessage() + "'.", cee);
        }
        
        if(!handleStatus(contentJSONObject)) {            	
        	freshnessMessage = new String[]{versionString, NOT_OK_MESSAGE};
        	return freshnessMessage;
        } else {
        	freshnessMessage = new String[]{versionString, OK_MESSAGE};
        	return freshnessMessage;
        }		
    }
    
    String transferGoodPhaseTwo(String seller, String versionString) throws ClientLibraryException {    	
    	try {
    		//System.out.println(versionString);
			this.clientSec.sendSignedMessage(versionString);
			
			// phase two
			versionString = this.clientSec.ensureFreshnessNotary("sendOk");
    	} catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("transferGoodPhaseTwo(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }	
		return versionString;
    }

    // buyGood (between clients)
    Object[] buyGoodP2PClientPhaseOne(String buyer, String seller, String good, int notary) throws ClientLibraryException {
        JSONObject sentJSONObject, contentJSONObject = null;
        String sentJSONString, contentString, versionString, error, receivedCryptoPuzzleChallenge, cryptoPuzzleChallengeResponse;
        try {
        	sendCommand(this.username, "BUY-GOOD");            

            sentJSONObject = new JSONObject();
            sentJSONObject.put("buyer", buyer);
            sentJSONObject.put("seller", seller);
            sentJSONObject.put("good", good);
            sentJSONObject.put("notary", notary);
            sentJSONString = sentJSONObject.toString();

            this.clientSec.sendSignedMessage(sentJSONString);

            receivedCryptoPuzzleChallenge = this.clientSec.authenticatedClientReceive("cryptoPuzzle", seller);
            cryptoPuzzleChallengeResponse = this.clientSec.solveCryptoPuzzle(receivedCryptoPuzzleChallenge);
            this.sendCryptoPuzzleAnswer(cryptoPuzzleChallengeResponse, buyer, seller);
            
            //check freshness
            versionString = this.clientSec.ensureFreshnessClient("relayFreshnessBetweenClients", seller);
            contentJSONObject = UtilMethods.convertStringToJSON(versionString);
            
            if(!handleStatus(contentJSONObject)) {            	
            	error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
            	return new Object[]{false, error};
            }
            if(!checkForSentGood(contentJSONObject, good)) {
            	return new Object[]{false, "Error: Not the good previously sent!"};
            }         
            if(!this.clientSec.validatePreviousContent(contentJSONObject)) {
            	return new Object[]{false, "Error: Previous write was not state verified!"};
            }

            contentString = contentJSONObject.toString();
    		return new Object[]{true, contentString};
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("buyGoodP2PClient(): '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("buyGoodP2PClient(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }
    
    Object[] buyGoodP2PClientPhaseTwo(String versionString, String seller, String good) throws ClientLibraryException {
    	JSONObject lastStateJSON, contentJSONObject;
    	String contentString, error, lastState;      
        int writtenVersion, receivedVersion;
		String receivedCryptoPuzzleChallenge, cryptoPuzzleChallengeResponse;

		try {
			contentJSONObject = UtilMethods.convertStringToJSON(versionString);

			receivedCryptoPuzzleChallenge = ((JSONObject) contentJSONObject.get("cryptoPuzzle")).toString();
			cryptoPuzzleChallengeResponse = this.clientSec.solveCryptoPuzzle(receivedCryptoPuzzleChallenge);

			lastState = (String) contentJSONObject.get("lastState");
			lastStateJSON = UtilMethods.convertStringToJSON(lastState);
			writtenVersion = (int) lastStateJSON.get("write-version");

			contentJSONObject.put("cryptoPuzzleAnswer", cryptoPuzzleChallengeResponse);
			contentJSONObject.put("write-version", writtenVersion+1);
			contentJSONObject.put("signer", this.username);
			contentJSONObject.put("operation", "BG");
			
			JSONObject writeValueJSONObject = new JSONObject();
			writeValueJSONObject.put("owner-id", this.username);
			writeValueJSONObject.put("state", "not-on-sale");
			
			contentJSONObject.put("value", writeValueJSONObject);
	        
			receivedVersion = ClientEncapsulation.fetchVersion(versionString);		    		        		    		       
	        contentString = UtilMethods.changeVersionOfJSON(contentJSONObject, receivedVersion);		    		        
	        	        
	        this.clientSec.setCurrentVersion(receivedVersion+1);
	        
	        contentJSONObject = new JSONObject(contentString);
			contentJSONObject.remove("lastState");
			
			//System.out.println("!!!");
			//System.out.println(contentJSONObject.toString());
			//System.out.println("!!!");
			
	 		this.clientSec.sendSignedMessage(contentJSONObject.toString());     
	 		
	    	contentJSONObject = UtilMethods.convertStringToJSON(versionString);
	        contentString = this.clientSec.ensureFreshnessClient("terminateBuyGood", seller);
	        contentJSONObject = UtilMethods.convertStringToJSON(contentString);
	        
	        if(handleStatus(contentJSONObject)) {
	        	System.out.println("You have successfully bought good '" + good + "' from user '" + seller + "'.");
	        	return new Object[]{true, "You have successfully bought good '" + good + "' from user '" + seller + "'."};
	        } else {
	        	error = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"error");
	        	return new Object[]{false, error};
	        }
		} catch (UtilMethodsException ume) {
			throw new ClientLibraryException("buyGoodP2PClientPhaseTwo(): Something went wrong with the UtilMethods module."+
                    "Message: '" + ume.getMessage() + "'.", ume);
		} catch (ClientSecurityException cse) {
			throw new ClientLibraryException("buyGoodP2PClientPhaseTwo(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
		} catch (ClientEncapsulationException cee) {
			throw new ClientLibraryException("buyGoodP2PClientPhaseTwo(): Something went wrong with the ClientEncapsulation module."+
                    "Message: '" + cee.getMessage() + "'.", cee);
		}		
    }
    
    String buyGoodSellerRelaySeller() throws ClientLibraryException {
    	String buyGoodEncapsulatedMessage;
    	try {
			buyGoodEncapsulatedMessage = this.clientSec.authenticatedClientReceive("buyGood", null);
		} catch (ClientSecurityException cse) {
			throw new ClientLibraryException("buyGoodSellerRelaySeller(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
		}
    	return buyGoodEncapsulatedMessage;
    }
    
    String buyGoodRelayFreshnessSeller(String versionString, String buyer) throws ClientLibraryException {
    	String relayedVersionString;
    	try {
			relayedVersionString = this.routingOps.relayFreshnessBetweenClientNotary(versionString, buyer);
		} catch (RoutingOperationsException roe) {
			throw new ClientLibraryException("buyGoodRelayFreshnessSeller(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + roe.getMessage() + "'.", roe);
		}
		return relayedVersionString;
    }

    void buyGoodSellerTerminateMidways(String versionString) throws ClientLibraryException {
    	try {
			this.routingOps.terminateBuyGoodMidways(versionString);
		} catch (RoutingOperationsException roe) {
			throw new ClientLibraryException("buyGoodSellerTerminateMidways(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + roe.getMessage() + "'.", roe);
		}
    }

    void terminateBuyGood(String message) throws ClientLibraryException {
        JSONObject contentJSONObject;
        String contentString;        
        try {
        	this.clientSec.sendSignedMessage(message);
        	contentString = ClientEncapsulation.getEncapsulatedContent(message, 1)[0];
            contentJSONObject = UtilMethods.convertStringToJSON(contentString);
        } catch (UtilMethodsException ume) {
            throw new ClientLibraryException("terminateBuyGood(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientSecurityException cse) {
        	throw new ClientLibraryException("buyGoodP2PClient(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        } catch (ClientEncapsulationException cee) {
        	throw new ClientLibraryException("transferGoodPhaseOne(): Something went wrong with the ClientEncapsulation module."+
                    "Message: '" + cee.getMessage() + "'.", cee);
        }
        handleStatus(contentJSONObject);
    }
    
    Object[] writeBack(String sentJSONString) throws ClientLibraryException {
    	JSONObject contentJSONObject;
    	String versionString;    	
    	int currentVersion;
    	
    	try {
    		
    		currentVersion = ClientEncapsulation.fetchVersion(sentJSONString);
    		this.clientSec.setCurrentVersion(currentVersion);
	
			this.clientSec.sendSignedMessage(sentJSONString);
			
			versionString = this.clientSec.ensureFreshnessNotary("writeBack");
			
			contentJSONObject = UtilMethods.convertStringToJSON(versionString);
			if(handleStatus(contentJSONObject)) {
				return new Object[]{true, "writeBack(): sucessfull!"};
			}else {
				return new Object[]{true, "writeBack(): not sucessfull..."};
			}
		} catch (ClientLibraryException cle) {
			throw new ClientLibraryException("writeBack(): Something went wrong with the ClientLibrary module."+
                    "Message: '" + cle.getMessage() + "'.", cle);
		} catch (ClientSecurityException cse) {
			throw new ClientLibraryException("writeBack(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
		} catch (ClientEncapsulationException cee) {
			throw new ClientLibraryException("writeBack(): Something went wrong with the ClientEncapsulation module."+
                    "Message: '" + cee.getMessage() + "'.", cee);
		} catch (UtilMethodsException ume) {
			throw new ClientLibraryException("writeBack(): Something went wrong with the UtilMethods module."+
                    "Message: '" + ume.getMessage() + "'.", ume);
		}   
    }
}