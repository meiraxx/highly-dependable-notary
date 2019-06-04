package pt.ulisboa.tecnico.sec.client;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.CryptoAux;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientEncapsulationException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientSecurityException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;
import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.exceptions.CryptoAuxException;

public class ClientSecurity {

    private static final String OK_MESSAGE = "OK";
    private static final String NOT_OK_MESSAGE = "NOT-OK";

	private Communications communication;
	private String keystoreFilePath;

	private String username;

    private int currentVersion;
    private String notaryCertPath;
    private String option;

    private int notary;

	public ClientSecurity(Communications communication, String keystoreFilePath, String username, String option, int notary) {
		this.keystoreFilePath = keystoreFilePath;
		this.communication = communication;
		this.username = username;
		this.option = option;
		this.notary = notary;
        if (option.equals("cc")) {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryJoao.pem";
        } else {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryTest" + notary + ".pem";
        }
	}

	public int getCurrentVersion() {
		return this.currentVersion;
	}

	public void setCurrentVersion(int currentVersion) {
		this.currentVersion = currentVersion;
	}

	public int getNotary() {
		return this.notary;
	}

	public void setNotary(int notary) {
        if (option.equals("cc")) {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryJoao.pem";
        } else {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryTest" + notary + ".pem";
        }
	}

	// security
    void sendSignedMessage(String content) throws ClientSecurityException {
        PrivateKey userPrivateKey;
        String b64encodedSignature, signedMessageData;
        JSONObject signedMessageJSON;
        try {
            userPrivateKey = CryptoAux.loadPrivateKeyFromFile(this.keystoreFilePath, "abcdef", "123456");
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("sendSignedMessage(): Couldn't load private key from keystore file...", cae);
        }
        try {
            b64encodedSignature = CryptoAux.generateSignature(userPrivateKey, content);
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("sendSignedMessage(): Wasn't able to generate digital signature.", cae);
        }
        signedMessageJSON = new JSONObject();

        signedMessageJSON.put("content", content);
        signedMessageJSON.put("signature", b64encodedSignature);
        signedMessageData = signedMessageJSON.toString();
        try {
            UtilMethods.sendMessage(this.communication, signedMessageData);
        } catch (UtilMethodsException ume) {
            throw new ClientSecurityException("sendSignedMessage(): Failed to send message", ume);
        }

    }
    private boolean verifyNotarySignature(String content, String signature) throws ClientSecurityException {
        boolean verifiedSignature;
        X509Certificate notaryCert;
        try {
            // load notary cert from file
            notaryCert = CryptoAux.loadCertificateFromFile(this.notaryCertPath);
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("verifyNotarySignature(): problem loading notary cert.", cae);
        }
        try {
            // verify signature
            verifiedSignature = CryptoAux.verifySignature(notaryCert, content, signature);
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("verifyNotarySignature(): problem verifying signature.", cae);
        }
        return verifiedSignature;
    }
    private boolean verifyClientSignature(String content, String signature, String clientName) throws ClientSecurityException {
        boolean verifiedSignature;
        X509Certificate clientCert;
        try {
            clientCert = CryptoAux.loadCertificateFromFile("usersPublicKeyCertificates/" + clientName + ".pem");
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("verifyClientSignature(): problem loading client cert.", cae);
        }
        try {
            // verify signature
            verifiedSignature = CryptoAux.verifySignature(clientCert, content, signature);
        } catch (CryptoAuxException cae) {
            throw new ClientSecurityException("verifyClientSignature(): problem verifying signature.", cae);
        }
        return verifiedSignature;
    }
    boolean validatePreviousContent(JSONObject messageJSONObject) throws ClientSecurityException {
    	String signature, signer, lastState, pattern = "Notary.";
    	boolean verifiedClient, verifiedNotary;
    	try {
    		Pattern r = Pattern.compile(pattern);

			signature = (String) UtilMethods.jsonGetObjectByKey(messageJSONObject, "signature");
			signer = (String) UtilMethods.jsonGetObjectByKey(messageJSONObject, "signer");
			lastState = (String) UtilMethods.jsonGetObjectByKey(messageJSONObject, "lastState");

			Matcher m = r.matcher(signer);
			if(m.find()) {
				verifiedNotary = verifyNotarySignature(lastState, signature);
				verifiedClient = false;
			}
			else {
				verifiedNotary = false;
				verifiedClient = verifyClientSignature(lastState, signature, signer);
			}

			if(verifiedClient) {
				return true;
			} else if(verifiedNotary) {
				return true;
			} else {
				return false;
			}
    	} catch(UtilMethodsException ume) {
    		throw new ClientSecurityException("validatePreviousContent(): something went wrong with the UtilMethods class...");
    	} catch (ClientSecurityException cse) {
    		throw new ClientSecurityException("validatePreviousContent(): something went wrong with the verifyClientSignature() method...");
		}
    }
    String authenticatedClientReceive(String method, String expectedClientName) throws ClientSecurityException {
        // authenticatedClientReceive(): returns content only
        // p2p-client is authenticated here
        // authentication method: digital signature
        String receivedJSONString, clientName, encapsulatedContent[];
        JSONObject  contentJSONObject;
        boolean verifiedSignature;
        try {
            // 1st VERIFICATION: user is who he's supposed to be!
            switch(method) {            	
            	case "callCommand":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                    contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);

            		// need to verify user has the received username
                    clientName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"user");

                    // verify the signature of supposed client
                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], clientName);

                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    return encapsulatedContent[0];
            	case "buyGood":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                    contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);

                    clientName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"buyer");

                    // verify the signature of supposed client
                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], clientName);

                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    return receivedJSONString;
            	case "relayFreshnessBetweenClientNotary":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClientName);
                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    return receivedJSONString;
            	case "relayFreshnessBetweenClients":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 2);

                    contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);
                    String seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");

                    verifiedSignature = verifyNotarySignature(encapsulatedContent[0], encapsulatedContent[1]);
                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);

                    if(!seller.equals("")) {
                    	verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);
                    } else {
                    	verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClientName);
                    }
                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    return receivedJSONString;
            	case "terminateBuyGood":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);

                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClientName);
	                if (!verifiedSignature)
	                    throw new ClientSecurityException("authenticatedClientReceive: We received an untrusted message. Aborting...");

	                encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 2);

	                verifiedSignature = verifyNotarySignature(encapsulatedContent[0], encapsulatedContent[1]);
	                if (!verifiedSignature)
	                    throw new ClientSecurityException("authenticatedClientReceive: We received an untrusted message. Aborting...");

	                return receivedJSONString;
            	case "cryptoPuzzle":
            		// receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);
                    
                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);

                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClientName);
	                if (!verifiedSignature)
	                    throw new ClientSecurityException("authenticatedClientReceive: We received an untrusted message. Aborting...");

	                encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 2);

	                verifiedSignature = verifyNotarySignature(encapsulatedContent[0], encapsulatedContent[1]);
	                if (!verifiedSignature)
	                    throw new ClientSecurityException("authenticatedClientReceive: We received an untrusted message. Aborting...");

	                
                    return encapsulatedContent[0];
            	case "cryptoPuzzlePhaseTwo":
                    // receive message
                    receivedJSONString = UtilMethods.receiveMessage(this.communication);

                    encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                    contentJSONObject = UtilMethods.convertStringToJSON(encapsulatedContent[0]);

                    // verify the signature of supposed client
                    verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClientName);

                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedClientReceive(): We received an untrusted message. Aborting...");

                    return receivedJSONString;
	             default:
	            	 throw new ClientSecurityException("authenticatedClientReceive(): No such method called '" + method + "'.");
            }
        } catch (UtilMethodsException ume) {
            throw new ClientSecurityException("authenticatedClientReceive(): Something went wrong with a UtilMethods function." +
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (ClientEncapsulationException cee) {
        	throw new ClientSecurityException("authenticatedClientReceive(): Something went wrong with a UtilMethods function." +
                    "Message: '" + cee.getMessage() + "'.", cee);
        }
    }
    String authenticatedReceiveFromNotary(String method) throws ClientSecurityException {
        // authenticatedReceiveFromNotary(): returns content only
        // notary is authenticated here
        // authentication method: digital signature
        String receivedJSONString, contentString, signatureString;
        JSONObject receivedJSONObject;
        boolean verifiedSignature;

        try {
            // receive message
            receivedJSONString = UtilMethods.receiveMessage(this.communication);
            // get content and signature
            receivedJSONObject = UtilMethods.convertStringToJSON(receivedJSONString);
            contentString = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject,"content");
            signatureString = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "signature");

            switch(method) {
            	case "ping":

            	case "getStateOfGood":

            	case "intentionToSell":

            	case "intentionToSellFresh":
            	
            	case "writeBack":            		
            		// verify the signature
                    verifiedSignature = verifyNotarySignature(contentString, signatureString);
                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedReceiveFromNotary(): We received an untrusted message. Aborting...");

                    // return the usual content
                    return contentString;
                
				case "cryptoPuzzle":
					
            	case "transferGood":

            		verifiedSignature = verifyNotarySignature(contentString, signatureString);
                    if (!verifiedSignature)
                        throw new ClientSecurityException("authenticatedReceiveFromNotary(): We received an untrusted message. Aborting...");

                    // return the message envelope
                    return receivedJSONString;
            	default:
            		throw new ClientSecurityException("authenticatedReceiveFromNotary(): the method you chose to receive is not available...");
            }
        } catch (UtilMethodsException ume) {
            throw new ClientSecurityException("authenticatedReceiveFromNotary(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        }
    }

	String receiveCryptoPuzzleChallenge() throws ClientSecurityException {
		String cryptoPuzzleReceivedMessage, cryptoPuzzleChallenge, cryptoPuzzleHint, receivedMessage;
		JSONObject receivedJSONObject;

		try {
			cryptoPuzzleReceivedMessage = authenticatedReceiveFromNotary("cryptoPuzzle");
		} catch (ClientSecurityException cse) {
			throw new ClientSecurityException("Didn't receive crypto puzzle.", cse);
		}

		try {
			receivedJSONObject = UtilMethods.convertStringToJSON(cryptoPuzzleReceivedMessage);
			receivedMessage = receivedJSONObject.toString();			
		} catch (UtilMethodsException ume) {
			throw new ClientSecurityException("receiveAndBruteforceCryptoPuzzleChallenge(): UtilMethods" +
					" error.", ume);
		}
		
		return receivedMessage;
	}
	
    String solveCryptoPuzzle(String cryptoPuzzleMessage) throws ClientSecurityException {
    	String cryptoPuzzleChallenge, cryptoPuzzleHint;    	
    	JSONObject receivedJSONObject;
    	
    	try {
	    	receivedJSONObject = UtilMethods.convertStringToJSON(cryptoPuzzleMessage);
	    	cryptoPuzzleChallenge = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject,"cryptoPuzzleChallenge");
			cryptoPuzzleHint = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject,"cryptoPuzzleHint");
			
			System.out.println(cryptoPuzzleChallenge);
			System.out.println(cryptoPuzzleHint);
			
			return bruteforceCryptoPuzzle(cryptoPuzzleChallenge, cryptoPuzzleHint);
    	} catch(UtilMethodsException ume) {
    		throw new ClientSecurityException("solveCryptoPuzzle(): something went wrong with the UtilMethods exception...");
    	}

    }

	String auxRecursiveBruteforce(String cryptoPuzzleChallenge, String cryptoPuzzleHint) throws ClientSecurityException {
		String possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		//String possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String sha256result;
		StringBuilder possibleAnswer = new StringBuilder(cryptoPuzzleHint);

		try {
			int possibleCharactersLength = possibleCharacters.length();
			for(int i = 0; i < possibleCharactersLength; i++) {
				possibleAnswer.setCharAt(28, possibleCharacters.charAt(i));
				for(int j = 0; j < possibleCharactersLength; j++) {
					possibleAnswer.setCharAt(29, possibleCharacters.charAt(j));
					for(int k = 0; k < possibleCharactersLength; k++) {
						possibleAnswer.setCharAt(30, possibleCharacters.charAt(k));
						for(int l = 0; l < possibleCharactersLength; l++) {
							possibleAnswer.setCharAt(31, possibleCharacters.charAt(l));
							sha256result = UtilMethods.getStringDigest("SHA-256", possibleAnswer.toString());
							if (sha256result.equals(cryptoPuzzleChallenge)) {
								return possibleAnswer.toString();
							}
						}
					}
				}
			}
		} catch (UtilMethodsException ume) {
			throw new ClientSecurityException("bruteforceCryptoPuzzle(): UtilMethods error...", ume);
		}
		return possibleAnswer.toString();
	}

	String bruteforceCryptoPuzzle(String cryptoPuzzleChallenge, String cryptoPuzzleHint) throws ClientSecurityException {
		String possibleAnswer = auxRecursiveBruteforce(cryptoPuzzleChallenge, cryptoPuzzleHint + "AAAA");
		return possibleAnswer;
	}

	// receives and verifies received crypto-puzzle answer is correct
	void sendCryptoPuzzleAnswer(String cryptoPuzzleAnswer, String user) throws ClientSecurityException {
		JSONObject sentJSONObject;
		String sentJSONString;
		// send good with version information
		sentJSONObject = new JSONObject();
		sentJSONObject.put("user", user);
		sentJSONObject.put("cryptoPuzzleAnswer", cryptoPuzzleAnswer);
		sentJSONString = sentJSONObject.toString();
		sendSignedMessage(sentJSONString);
	}

    String ensureFreshnessNotary(String method) throws ClientSecurityException {
    	int receivedVersion;
        String versionString = null, status, encapsulatedContent[]; 
        
        switch(method) {
        	case "intentionToSell":
        		versionString = authenticatedReceiveFromNotary("intentionToSell");
        		break;
        	case "intentionToSellFresh":
        		versionString = authenticatedReceiveFromNotary("intentionToSellFresh");
        		break;
        	case "transferGood":
        		versionString = authenticatedReceiveFromNotary("transferGood");
        		break;
        	case "sendOk":
        		versionString = authenticatedReceiveFromNotary("transferGood");
        		
        		try {
	            	encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(versionString, 1);
	            	receivedVersion = ClientEncapsulation.fetchVersion(encapsulatedContent[0]);
	            	status = ClientEncapsulation.fetchStatus(encapsulatedContent[0]);
        		} catch (ClientEncapsulationException cee) {
                	throw new ClientSecurityException("ensureFreshnessNotary(): Something went wrong with a UtilMethods function." +
                            "Message: '" + cee.getMessage() + "'.", cee);
                }
    	        // confirm specific good and version
    	        if (status.equals(OK_MESSAGE) && !(this.currentVersion<receivedVersion)) {
    	            throw new ClientSecurityException("ensureFreshnessNotary(): Someone tried to perform a replay attack!");
    	        } else if(status.equals(NOT_OK_MESSAGE) && !(this.currentVersion>=receivedVersion)) {
    	            throw new ClientSecurityException("ensureFreshnessNotary(): Someone tried to perform a replay attack!");
    	        }   
    	        break;
        	case "writeBack":
        		versionString = authenticatedReceiveFromNotary("writeBack");
        		
        		try {
	            	receivedVersion = ClientEncapsulation.fetchVersion(versionString);
	            	status = ClientEncapsulation.fetchStatus(versionString);
        		} catch (ClientEncapsulationException cee) {
                	throw new ClientSecurityException("ensureFreshnessNotary(): Something went wrong with a UtilMethods function." +
                            "Message: '" + cee.getMessage() + "'.", cee);
                }
    	        // confirm specific good and version
    	        if (status.equals(OK_MESSAGE) && this.currentVersion>receivedVersion) {
    	            throw new ClientSecurityException("ensureFreshnessNotary(): Someone tried to perform a replay attack!");
    	        } else if(status.equals(NOT_OK_MESSAGE) && this.currentVersion<=receivedVersion) {
    	            throw new ClientSecurityException("ensureFreshnessNotary(): Someone tried to perform a replay attack!");
    	        }
    	        return versionString;
        	default:
        		throw new ClientSecurityException("ensureFreshnessNotary(): No such method called '" + method + "'.");
        }        
        return versionString;
    }   
    String ensureFreshnessClient(String method, String expectedClientName) throws ClientSecurityException {
    	JSONObject contentJSONObject = null, lastStateJSON;
    	int receivedVersion, writtenVersion;
    	String versionString, contentString, status, encapsulatedContent[] = null, lastState; 
    			
    	versionString = authenticatedClientReceive(method, expectedClientName);
    	try {
	    	switch(method) {
	    		//seller uses this method on sendOk method to the buyer
		    	case "terminateBuyGood":    			
					encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(versionString, 2);
		    		receivedVersion = ClientEncapsulation.fetchVersion(encapsulatedContent[0]);
		    		status = ClientEncapsulation.fetchStatus(encapsulatedContent[0]);
	        		if (status.equals(OK_MESSAGE) && !(this.currentVersion<=receivedVersion)) {
	    	            throw new ClientSecurityException("ensureFreshnessClient(): Someone tried to perform a replay attack!");
	    	        } else if(status.equals(NOT_OK_MESSAGE) && !(this.currentVersion>receivedVersion)) {
	    	            throw new ClientSecurityException("ensureFreshnessClient(): Someone tried to perform a replay attack!");
	        		} else {
	        			return encapsulatedContent[0];
	        		}	        	
	        	//seller uses this method on the way-back relay to the notary
	    		case "relayFreshnessBetweenClientNotary":
	    			encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(versionString, 1);
	        		receivedVersion = ClientEncapsulation.fetchVersion(encapsulatedContent[0]);
	        		if (!(this.currentVersion<receivedVersion)) {
	    	            throw new ClientSecurityException("ensureFreshnessClient(): Someone tried to perform a replay attack!");
	    	        }
	        		return versionString;
	    		case "relayFreshnessBetweenClients":
	        		encapsulatedContent = ClientEncapsulation.getEncapsulatedContent(versionString, 2);
	        		status = ClientEncapsulation.fetchStatus(encapsulatedContent[0]);
	    	    	if(status.equals(OK_MESSAGE)) {							
		    	    	return encapsulatedContent[0];

	    	    	} else {
	    	    		return encapsulatedContent[0];
	    	    	}
	    		default:
	    			sendSignedMessage(versionString); 
	    			return versionString;
	    	}
    	} catch (ClientEncapsulationException cee) {
        	throw new ClientSecurityException("ensureFreshnessClient(): Something went wrong with a UtilMethods function." +
                    "Message: '" + cee.getMessage() + "'.", cee);
        }
    }  
}
