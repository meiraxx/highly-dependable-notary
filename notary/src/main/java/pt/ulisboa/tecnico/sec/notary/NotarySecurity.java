package pt.ulisboa.tecnico.sec.notary;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.CryptoAux;
import pt.ulisboa.tecnico.sec.SignatureProvider;
import pt.ulisboa.tecnico.sec.ccUtility;
import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.exceptions.CitizenCardException;
import pt.ulisboa.tecnico.sec.exceptions.CryptoAuxException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryEncapsulationException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotarySecurityException;
import pt.ulisboa.tecnico.sec.notary.exceptions.UtilMethodsException;
import java.security.MessageDigest;

public class NotarySecurity {
	
    private static final String OK_MESSAGE = "OK";
    private static final String NOT_OK_MESSAGE = "NOT-OK";
        
	private Communications communication;
	private String option;
    private String notaryCertPath;
    private String notaryKeyStorePath;
    private String clientNotary;
    private int notary;
      
    private String goodsList;
    private String notaryLogs;
	public NotarySecurity(Communications communication, String option, int notary) {
		this.communication = communication;
		this.option = option;
		this.notary = notary;
		this.goodsList = "goods_list" + notary + ".json";
		this.notaryLogs = "logs" + notary + ".json";
        if (option.equals("cc")) {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryJoao.pem";
        } else {
            this.notaryCertPath = "notaryPublicKeyCertificates/notaryTest" + notary + ".pem";
            this.notaryKeyStorePath = "notaryPublicKeyCertificates/notaryTest" + notary + "keystore.jks";
        }
	}

    void setSentMessageInfo(String sentMessage, JSONObject goodsJSON, Good good) throws NotarySecurityException {
        JSONObject updatedGoodsJSON;
        String encapsulatedContent[], signature, content;
        try {
            encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(sentMessage, 1);

            content = encapsulatedContent[0];
            signature = encapsulatedContent[1];

            // version is updated
            good.setVersion(good.getVersion());
            // update JSON with new good
            updatedGoodsJSON = updateJSONWithGood(goodsJSON, good);
            // write new JSON to file
            atomicWriteJSONToFile(updatedGoodsJSON, goodsList);

        } catch (NotarySecurityException nse) {
            throw new NotarySecurityException("getSentMessageInfo(): something went "
                    + "wrong with the NotarySecurity exception...", nse);
        } catch (NotaryEncapsulationException nee) {
            throw new NotarySecurityException("getSentMessageInfo(): something went "
                    + "wrong with the NotaryEncapsulation exception...", nee);
        }
    }

    //1. comms
    void sendNotOk(String errorMsg) throws NotarySecurityException {
        JSONObject errorJSON = new JSONObject();
        errorJSON.put("status", NOT_OK_MESSAGE);
        errorJSON.put("seller", "");
        errorJSON.put("error", errorMsg);
        String errorData = errorJSON.toString();
        sendSignedMessage(this.communication, errorData);
    }    
    void sendNotOk(String errorMsg, Good good) throws NotarySecurityException {
    	String stateOfGoodData, message;
        
        stateOfGoodData = getGoodInfo(good);
        try {
			JSONObject errorMessageJSON = UtilMethods.convertStringToJSONObject(stateOfGoodData);
			errorMessageJSON.put("status", NOT_OK_MESSAGE);
			errorMessageJSON.put("error", errorMsg);			
			message = errorMessageJSON.toString();
		} catch (UtilMethodsException ume) {
			throw new NotarySecurityException("sendNotOk(): something went wrong with the Utils method class...");
		}

        sendSignedMessage(this.communication, message);
    }
    void sendOk() throws NotarySecurityException {    	
    	String message;
		JSONObject sucessMessageJSON = new JSONObject();
		
		sucessMessageJSON.put("status", OK_MESSAGE);		
		message = sucessMessageJSON.toString();
		
        sendSignedMessage(this.communication, message);
    }
    String sendOk(Good good) throws NotarySecurityException {
    	String stateOfGoodData, message;
        
        stateOfGoodData = getGoodInfo(good);
        try {
			JSONObject sucessMessageJSON = UtilMethods.convertStringToJSONObject(stateOfGoodData);
			sucessMessageJSON.put("status", OK_MESSAGE);			
			message = sucessMessageJSON.toString();
		} catch (UtilMethodsException ume) {
			throw new NotarySecurityException("sendNotOk(): something went wrong with the Utils method class...");
		}
        message = sendSignedMessage(this.communication, message);
        
        return message;
    }

    private void sendString(String content) throws NotarySecurityException {
        sendSignedMessage(this.communication, content);
    }
    // aux
    // aux
    void sendCommand(String user, String command) throws NotaryLibraryException {
        JSONObject sentJSONObject;
        String sentJSONString;

        sentJSONObject = new JSONObject();
        sentJSONObject.put("user", user);
        sentJSONObject.put("command", command);
        sentJSONString = sentJSONObject.toString();
        try {
        	sendSignedMessage(this.communication, sentJSONString);
        } catch (NotarySecurityException cse) {
        	throw new NotaryLibraryException("sendCommand(): Something went wrong with the ClientSecurity module."+
                    "Message: '" + cse.getMessage() + "'.", cse);
        }
    }
    // receives and verifies received crypto-puzzle answer is correct
    boolean receiveAndVerifyCryptoPuzzle(String cryptoPuzzleCorrectAnswer) throws NotarySecurityException {
        String receivedString, cryptoPuzzleAnswer;
	    receivedString = authenticatedReceive("cryptoPuzzle", null);
        JSONObject receivedJSON;
        try {
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);
            cryptoPuzzleAnswer = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"cryptoPuzzleAnswer");
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("receiveAndVerifyCryptoPuzzle(): Util methods exception...", ume);
        }

        return cryptoPuzzleAnswer.equals(cryptoPuzzleCorrectAnswer);
    }

    // receives and verifies received crypto-puzzle answer is correct
    boolean verifyCryptoPuzzle(String receivedString, String cryptoPuzzleCorrectAnswer) throws NotarySecurityException {
        String cryptoPuzzleAnswer;
        JSONObject receivedJSON;
        try {
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);
            cryptoPuzzleAnswer = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"cryptoPuzzleAnswer");
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("verifyCryptoPuzzle(): Util methods exception...", ume);
        }
        //System.out.println(cryptoPuzzleAnswer);
        //System.out.println(cryptoPuzzleCorrectAnswer);
        return cryptoPuzzleAnswer.equals(cryptoPuzzleCorrectAnswer);
    }

    // returns correct answer
    String sendCryptoPuzzleChallenge() throws NotarySecurityException {
	    JSONObject sentJSONObject;
	    String sentJSONString;

        String[] cryptoPuzzle = generateRandomCryptoPuzzle();
        String cryptoPuzzleCorrectAnswer = cryptoPuzzle[0];
        String cryptoPuzzleChallenge = cryptoPuzzle[1];
        String cryptoPuzzleHint = cryptoPuzzleCorrectAnswer.substring(0, 28);

        sentJSONObject = new JSONObject();
        sentJSONObject.put("cryptoPuzzleChallenge", cryptoPuzzleChallenge);
        sentJSONObject.put("cryptoPuzzleHint", cryptoPuzzleHint);
        sentJSONString = sentJSONObject.toString();
        sendString(sentJSONString);
        return cryptoPuzzleCorrectAnswer;
    }

    // returns correct answer
    Object[] addCryptoPuzzleChallengeToJSON(JSONObject jsonObject) throws NotarySecurityException {
        JSONObject sentJSONObject;
	    String[] cryptoPuzzle = generateRandomCryptoPuzzle();
        String cryptoPuzzleCorrectAnswer = cryptoPuzzle[0];
        String cryptoPuzzleChallenge = cryptoPuzzle[1];
        String cryptoPuzzleHint = cryptoPuzzleCorrectAnswer.substring(0, 28);

        sentJSONObject = new JSONObject();
        sentJSONObject.put("cryptoPuzzleChallenge", cryptoPuzzleChallenge);
        sentJSONObject.put("cryptoPuzzleHint", cryptoPuzzleHint);

        jsonObject.put("cryptoPuzzle", sentJSONObject);

        return new Object[]{cryptoPuzzleCorrectAnswer, jsonObject};
    }

    private String[] generateRandomCryptoPuzzle() throws NotarySecurityException {
        String sha256;
        String randomAlphaNum;

        try {
            randomAlphaNum = UtilMethods.generateRandomAlphaNumeric(32, true);
            //randomAlphaNum = "A";
            sha256 = UtilMethods.getStringDigest("SHA-256", randomAlphaNum);
        } catch(UtilMethodsException ume) {
            throw new NotarySecurityException("sendCryptoPuzzle(): Failed to generate random " +
                    "alpha-numeric string or to get digest from it.", ume);
        }
        return new String[]{randomAlphaNum, sha256};
    }

    //2. security
    private String sendSignedMessage(Communications communication, String content) throws NotarySecurityException {
        String signedMessageData, b64encodedSignature;
        JSONObject signedMessageJSON;

        b64encodedSignature = signString(content);
        signedMessageJSON = new JSONObject();
        signedMessageJSON.put("content", content);
        signedMessageJSON.put("signature", b64encodedSignature);
        signedMessageData = signedMessageJSON.toString();
        try {
            UtilMethods.sendMessage(communication, signedMessageData);
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("sendSignedMessage(): Failed to send message.", ume);
        }
        return signedMessageData;
    }
    private boolean verifyNotarySignature(String content, String signature) throws NotarySecurityException {
        boolean verifiedSignature;
        X509Certificate notaryCert;
        try {
            // load notary cert from file
            notaryCert = CryptoAux.loadCertificateFromFile(this.notaryCertPath);
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("verifyNotarySignature(): problem loading notary cert.", cae);
        }
        try {
            // verify signature
            verifiedSignature = CryptoAux.verifySignature(notaryCert, content, signature);
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("verifyNotarySignature(): problem verifying signature.", cae);
        }
        return verifiedSignature;
    }
    private boolean verifyClientSignature(String content, String signature, String clientName) throws NotarySecurityException {
        boolean verifiedSignature;
        String pattern = "Notary.";
        X509Certificate clientCert;        
        try {
        	Pattern r = Pattern.compile(pattern);

        	Matcher m = r.matcher(clientName);
			if(m.find()) {				
				// load client cert from file based on his username
	            clientCert = CryptoAux.loadCertificateFromFile("notaryPublicKeyCertificates/" + 
	            		"notaryTest" + clientName.substring(clientName.length() - 1) + ".pem");
	            
			} else {
				// load client cert from file based on his username
	            clientCert = CryptoAux.loadCertificateFromFile("usersPublicKeyCertificates/" +
	                    clientName + ".pem");
			}            
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("verifyClientSignature(): problem loading notary cert.", cae);
        }
        try {
            // verify signature
        	verifiedSignature = CryptoAux.verifySignature(clientCert, content, signature);
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("verifyClientSignature(): problem verifying signature.", cae);
        }
        return verifiedSignature;
    }
    void sendBroadcastMessage(String message) throws NotarySecurityException {
    	try {
			sendSignedMessage(this.communication, message);
		} catch (NotarySecurityException nse) {
			throw new NotarySecurityException("sendBroadcastMessage(): something went wrong"
					+ " with the NotarySecurity class...");
		}
    }
    private boolean verifyIfNotary(String clientName) {
    	String pattern = "Notary.";
    	
    	Pattern r = Pattern.compile(pattern);

    	Matcher m = r.matcher(clientName);
    	
    	if(m.find()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    String authenticatedReceive(String method, String expectedClient) throws NotarySecurityException {
        // client is authenticated here
        // authentication method: content digital signature
        // authenticatedReceive(): returns content only
        String receivedJSONString, contentString, signatureString, clientName, buyer, seller, encapsulatedContent[], signer;
        JSONObject receivedJSONObject, contentJSONObject;
        boolean verifiedSignature;
        
        
        try {
            // receive message
            receivedJSONString = UtilMethods.receiveMessage(this.communication);
            // It is important for part of the code to be synchronized since the logger can't have unsynchronized states
            synchronized(this) {
            	logsRecoverNormalState();
            	writeLog(receivedJSONString);
            }            
            // 1st VERIFICATION: user is who he's supposed to be!
            if (method.equals("callCommand") || method.equals("getStateOfGood")){
                // get content and signature
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);                
                contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
                // need to verify user has the received username
                clientName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"user");                
                if(verifyIfNotary(clientName)) {
                	this.clientNotary = clientName;
                }                                
                // verify the signature of supposed client
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], clientName);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");

                return encapsulatedContent[0];
            } else if (method.equals("intentionToSellTransaction")) {
            	// get content and signature
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);                
                contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);            	
                // need to verify user has the received seller name
            	clientName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
            	
            	// verify the signature of supposed client
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], clientName);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");

                return encapsulatedContent[0];
            } else if (method.equals("intentionToSellFreshness")) {
            	// get content and signature
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);     
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
            	
                seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
                     
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                                
                return receivedJSONString;       
            } else if (method.equals("transferGoodPhaseOne")) {
            	// get content and signature
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);     
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
                
            	seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
            	buyer = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"buyer");
            	contentString = encapsulatedContent[0];
            	
            	verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], buyer);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                return contentString;                
            } else if (method.equals("transferGoodPhaseTwo")) {            
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);

            	seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
            	                
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], expectedClient);                
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
            	
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);               
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                return receivedJSONString;                       	            	            	            	            	            	            	           
            } else if (method.equals("writeBack")) {
            	// get content and signature
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);     
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
            	
                signer = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"signer");
                     
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], signer);
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                                
                return receivedJSONString;       
            } else if(method.equals("cryptoPuzzle")) {
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
            	            	
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);
            	contentString = encapsulatedContent[0];
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
            	
            	clientName = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"user");
            	seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
            	                
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], clientName);                
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
            	
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);               
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                return contentString;
            } else if(method.equals("ECHO")) {            	
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);    	
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
            	
            	signer = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"signer");
            	                
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], signer);                
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
            	
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                contentString = encapsulatedContent[0];
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], this.clientNotary);               
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                return contentString;
            }  else if(method.equals("ECHO-TG")) {            	
            	encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 3);          	
            	contentJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
            	
            	signer = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"signer");
            	
            	seller = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"seller");
            	                
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], signer);                
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], seller);               
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                                
                encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
                contentString = encapsulatedContent[0];
                verifiedSignature = verifyClientSignature(encapsulatedContent[0], encapsulatedContent[1], this.clientNotary);               
                if (!verifiedSignature)
                    throw new NotarySecurityException("authenticatedReceive(): We received an untrusted message. Aborting...");
                
                return contentString;
            } else {
                throw new NotarySecurityException("authenticatedReceive(): No such method: '" + method + "'.");
            }
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("authenticatedReceive(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotaryEncapsulationException nee) {
        	throw new NotarySecurityException("authenticatedReceive(): Something went wrong with a NotarySecurity class."+
                    "Message: '" + nee.getMessage() + "'.", nee);
		}
    }
    String ensureFreshness(Good good, String sellerName, String buyerName, String method, JSONObject goodsJSON) throws NotarySecurityException, NotaryEncapsulationException {
        // freshness protocol
        // 1. send specific good and version
        String realVersionJSONString, receivedVersionJSONString, encapsulatedContent[], returnVersionJSONString, sentMessage;
        Object[] cryptoPuzzleTuple;
        String correctAnswer = "";

        JSONObject realVersionJSONObject = new JSONObject();
        realVersionJSONObject.put("good", good.getName());
        realVersionJSONObject.put("version", good.getVersion());
        realVersionJSONObject.put("seller", sellerName);
        realVersionJSONObject.put("status", OK_MESSAGE);
        realVersionJSONObject.put("lastState", good.getLastState());
        realVersionJSONObject.put("signature", good.getSignature());
        realVersionJSONObject.put("signer", good.getSigner());
        if (method.equals("transferGoodPhaseTwo")) {
            cryptoPuzzleTuple = addCryptoPuzzleChallengeToJSON(realVersionJSONObject);
            correctAnswer = (String) cryptoPuzzleTuple[0];
            realVersionJSONObject = (JSONObject) cryptoPuzzleTuple[1];
        }
        realVersionJSONString = realVersionJSONObject.toString();
        
        sentMessage = sendSignedMessage(this.communication, realVersionJSONString);        
        setSentMessageInfo(sentMessage, goodsJSON, good);
        
        // 2. receive specific good and version
        receivedVersionJSONString = authenticatedReceive(method, buyerName);
        returnVersionJSONString = receivedVersionJSONString;
        // 3. confirm specific good and version
        if(method.equals("intentionToSellFreshness")) {
        	try {
        		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedVersionJSONString, 1);
        		returnVersionJSONString = receivedVersionJSONString;
        		receivedVersionJSONString = encapsulatedContent[0];
        	} catch(NotaryEncapsulationException nee) {
        		throw new NotaryEncapsulationException("ensureFreshness(): something went wrong with the NotaryEncapsulation class...");
        	}
        } else if(method.equals("transferGoodPhaseTwo")) {
        	try {
        		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedVersionJSONString, 2);
        		returnVersionJSONString = receivedVersionJSONString;
        		receivedVersionJSONString = encapsulatedContent[0];
        	} catch(NotaryEncapsulationException nee) {
        		throw new NotaryEncapsulationException("ensureFreshness(): something went wrong with the NotaryEncapsulation class...");
        	}
        }
        
        versionGreaterThanCurrent(realVersionJSONString, receivedVersionJSONString);
        System.out.println("Client '" + sellerName + "': communication freshness verified");

        // get unique puzzle: need Double-Echo for notaries to agree on answer
        // BONUS POINTS
        /*try {
            if(method.equals("transferGoodPhaseTwo")) {
                JSONObject receivedVersionJSONObject =
                        UtilMethods.convertStringToJSONObject(receivedVersionJSONString);
                boolean cryptoPuzzleOk;
                // verify crypto puzzle
                //cryptoPuzzleOk = verifyCryptoPuzzle(receivedVersionJSONObject.toString(), correctAnswer);
                /*
                System.out.println("Crypto Puzzle answer: " + cryptoPuzzleOk);
                if (!cryptoPuzzleOk) {
                    throw new NotarySecurityException("ensureFreshness():" +
                            "Received crypto-puzzle answer is WRONG.");
                }
            }
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("ensureFreshness():" +
                    "Deus me perdoe. UtilMethods down, again... please refactor me ffs");
        }*/

        return returnVersionJSONString;
    }
    
    //verifica a freshness version
    private void versionGreaterThanCurrent(String realVersionJSONString, String receivedVersionJSONString) throws NotarySecurityException {
    	JSONObject realVersionJSONObject, receivedVersionJSONObject;
    	int realVersion, receivedVersion;
    	String realGood, receivedGood;
    	try {
    		realVersionJSONObject = UtilMethods.convertStringToJSONObject(realVersionJSONString);
    		realVersion = (int) UtilMethods.jsonGetObjectByKey(realVersionJSONObject, "version");
    		realGood = (String) UtilMethods.jsonGetObjectByKey(realVersionJSONObject, "good");
    		
    		receivedVersionJSONObject = UtilMethods.convertStringToJSONObject(receivedVersionJSONString);
    		receivedVersion = (int) UtilMethods.jsonGetObjectByKey(receivedVersionJSONObject, "version");
    		receivedGood = (String) UtilMethods.jsonGetObjectByKey(receivedVersionJSONObject, "good");

    		if(!realGood.equals(receivedGood)) {
    			throw new NotarySecurityException("versionGreaterThanCurrent(): "
    					+ "something went terribly wrong "
    					+ "since the good name doesn't match the good name the client sent previously!");
    		}
    		
    		if(receivedVersion<=realVersion) {
    			throw new NotarySecurityException("versionGreaterThanCurrent(): Someone tried to perform a replay attack!");
    		}

    	} catch(UtilMethodsException ume) {
    		throw new NotarySecurityException("versionGreaterThanCurrent(): something went wrong with the UtilMethods class...");
    	}
    }
    void verifyUserExists(JSONObject clientsJSON, String clientName) throws NotarySecurityException {
        if (!clientsJSON.has(clientName)) {
            String errorMsg = clientName + " does not exist on the system...";
            sendNotOk(errorMsg);
            // verifyUserExists causes an exception if user doesn't exist
            throw new NotarySecurityException("verifyUserExists(): Client '" + clientName + "' does not exist on the system...");
        }
    }

    String signString(String content) throws NotarySecurityException {
        if (this.option.equals("cc")) {
            return ccSignString(content);
        } else {
            return signTestString(content);
        }
    }

    private String signTestString(String content) throws NotarySecurityException {
        PrivateKey userPrivateKey;
        String b64encodedSignature;
        String keystoreFilePath = this.notaryKeyStorePath;
        try {
            userPrivateKey = CryptoAux.loadPrivateKeyFromFile(keystoreFilePath, "abcdef", "123456");
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("signTestString(): Couldn't load private key from keystore file...", cae);
        }
        try {
            b64encodedSignature = CryptoAux.generateSignature(userPrivateKey, content);
        } catch (CryptoAuxException cae) {
            throw new NotarySecurityException("signTestString(): Wasn't able to generate digital signature.", cae);
        }
        return b64encodedSignature;
    }

    String ccSignString(String message) throws NotarySecurityException {
    	String b64encodedSignature;
    	SignatureProvider signatureProvider;
        try {
            signatureProvider = ccUtility.usePTEID(true);
        } catch (CitizenCardException cae) {
            throw new NotarySecurityException("ccSignString(): I wasn't able to get a signature provider from citizen card. Aborting...");
        }
        try {
            b64encodedSignature = ccUtility.generateCCSignature(signatureProvider, message);
        } catch (CitizenCardException cce) {
            throw new NotarySecurityException("ccSignString(): Failed to generate signature with citizen card.", cce);
        }
        
        return b64encodedSignature;
    }
    
    //3. json, good, string operations
    Good getGoodFromJSONByName(JSONObject jsonObject, String goodName) throws NotarySecurityException {
        JSONObject nestedJSONObject;
        String ownerId, state, signature, signer, lastState;
        int version;
        try {
            nestedJSONObject = (JSONObject) UtilMethods.jsonGetObjectByKey(jsonObject, goodName);
            ownerId = (String) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "owner-id");
            state = (String) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "state");
            version = (int) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "version");
            signer = (String) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "signer");
            signature = (String) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "signature");
            lastState = (String) UtilMethods.jsonGetObjectByKey(nestedJSONObject, "lastState");
        } catch (ClassCastException cce) {
            // this should never happen in any scenario, but just in case
            throw new NotarySecurityException("getGoodFromJSONByName(): Something went terribly wrong...", cce);
        } catch (UtilMethodsException ume) {
            // throw same exception but send error msg
            sendNotOk("Good '" + goodName +  "' not found");
            throw new NotarySecurityException("getGoodFromJSONByName(): Good not found");
        }
        return new Good(goodName, ownerId, state, version, signature, signer, lastState);
    }
    JSONObject updateJSONWithGood(JSONObject jsonObject, Good good) throws NotarySecurityException {
        String goodName = good.getName();
        JSONObject updatedGoodDatabaseJSON = jsonObject;
        JSONObject updatedGoodJSON;
        try {
            updatedGoodJSON = (JSONObject) UtilMethods.jsonGetObjectByKey(updatedGoodDatabaseJSON, goodName);
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("updateJSONWithGood(): Good not found.", ume);
        }

        updatedGoodJSON.put("state", good.getState());
        updatedGoodJSON.put("owner-id", good.getOwnerId());
        updatedGoodJSON.put("version", good.getVersion());
    	updatedGoodJSON.put("signature", good.getSignature());
    	updatedGoodJSON.put("signer", good.getSigner());
    	updatedGoodJSON.put("lastState", good.getLastState());

        updatedGoodDatabaseJSON.put(goodName, updatedGoodJSON);

        return updatedGoodDatabaseJSON;
    }

    //a. atomicity
    void atomicMoveFile(String originFilePath, String newFilePath) throws NotarySecurityException {
        Path originFilePathObject = FileSystems.getDefault().getPath(originFilePath);
        Path newFilePathObject = FileSystems.getDefault().getPath(newFilePath);
        try{
            Files.move(originFilePathObject, newFilePathObject, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioe) {
            throw new NotarySecurityException("atomicMoveFile(): Couldn't move temporary database file into main database file.", ioe);
        }
    }
    void atomicWriteJSONToFile(JSONObject updatedDatabaseJSON, String databaseFilePath) throws NotarySecurityException {
        // FILE-WRITE-ATOMICITY
        String updatedDatabaseString = updatedDatabaseJSON.toString();
        String databaseTempFilePath = databaseFilePath + ".tmp";

        try {
            // write updated database to our temporary file
            UtilMethods.writeFile(databaseTempFilePath, updatedDatabaseString);
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("atomicWriteJSONToFile(): Failed to create temp file.", ume);
        }


        // ATOMIC-WRITE to our main json file
        // based on move operation:
        // https://stackoverflow.com/questions/774098/atomicity-of-file-move
        // https://stackoverflow.com/questions/29923008/how-to-create-then-atomically-rename-file-in-java-on-windows?rq=1
        atomicMoveFile(databaseTempFilePath, databaseFilePath);
    }
    void atomicWriteJSONToFile(JSONArray updatedDatabaseJSON, String databaseFilePath) throws NotarySecurityException {
        // FILE-WRITE-ATOMICITY
        String updatedDatabaseString = updatedDatabaseJSON.toString();
        String databaseTempFilePath = databaseFilePath + ".tmp";

        try {
            // write updated database to our temporary file
            UtilMethods.writeFile(databaseTempFilePath, updatedDatabaseString);
        } catch (UtilMethodsException ume) {
            throw new NotarySecurityException("atomicWriteJSONToFile(): Failed to create temp file.", ume);
        }


        // ATOMIC-WRITE to our main json file
        // based on move operation:
        // https://stackoverflow.com/questions/774098/atomicity-of-file-move
        // https://stackoverflow.com/questions/29923008/how-to-create-then-atomically-rename-file-in-java-on-windows?rq=1
        atomicMoveFile(databaseTempFilePath, databaseFilePath);
    }
    void databaseRecoverNormalState() throws NotarySecurityException {
        // this is a case that never happened and should never happen
        // if main file (goodsList) doesn't exist, something
        // went terribly wrong in the atomic move operation
        // solution: re-move tmpfile to our main file so that
        // we can load it successfully
        boolean mainDatabaseFileExists = new File(goodsList).isFile();
        if (!mainDatabaseFileExists){
            atomicMoveFile(goodsList  + ".tmp",goodsList);
            System.out.println("Database recovered.");
        }        
    }
    void logsRecoverNormalState() throws NotarySecurityException {
    	// this is a case that never happened and should never happen
        // if main file (notaryLogs) doesn't exist, something
        // went terribly wrong in the atomic move operation
        // solution: re-move tmpfile to our main file so that
        // we can load it successfully
    	boolean mainLogsFileExists = new File(notaryLogs).isFile();
        if (!mainLogsFileExists){
            atomicMoveFile(notaryLogs  + ".tmp",notaryLogs);
            System.out.println("Database recovered.");
        }
    }
    void writeLog(String receivedMessage) throws NotarySecurityException {
    	JSONObject logJSON = new JSONObject();
    	JSONArray logsJSONArray;
    	String unsignedLog, signature;
    	
        try {
			logsJSONArray = UtilMethods.convertFileToJSONArray(notaryLogs);
		} catch (UtilMethodsException ume) {
			 throw new NotarySecurityException("writeLog(): Something went wrong with a UtilMethods function."+
	                    "Message: '" + ume.getMessage() + "'.", ume);
	    }
    	
    	logJSON.put("timestamp", UtilMethods.now());
    	logJSON.put("transaction", receivedMessage);
    	
    	unsignedLog = logJSON.toString();    	    	
    	signature = signString(unsignedLog);
        logJSON.put("signature", signature);    
        
    	logsJSONArray.put(logJSON);
    	synchronized(this) {
    		atomicWriteJSONToFile(logsJSONArray, notaryLogs);
    	}
    }
    
    String getGoodInfo(Good good) {
    	String stateOfGoodData;
        String state = good.getState();
        String ownerId = good.getOwnerId();
        String signer = good.getSigner();
        String signature = good.getSignature();
        String lastState = good.getLastState();        
        int version = good.getVersion();        

        JSONObject stateOfGoodJSON = new JSONObject();
        stateOfGoodJSON.put("owner-id", ownerId);
        stateOfGoodJSON.put("state", state);
        stateOfGoodJSON.put("version", version);
        stateOfGoodJSON.put("signer", signer);
        stateOfGoodJSON.put("signature", signature);
        stateOfGoodJSON.put("lastState", lastState);
        stateOfGoodData = stateOfGoodJSON.toString();
                
        return stateOfGoodData;
    }
    
    Good setGoodInfo(JSONObject stateOfGoodJSON, String goodName) {
    	String ownerId, state, signer, signature;
    	JSONObject lastState;
    	int version;
    	    	    	
    	ownerId = (String) stateOfGoodJSON.get("owner-id");
        state = (String) stateOfGoodJSON.get("state");
        version = (int) stateOfGoodJSON.get("version");
        signer = (String) stateOfGoodJSON.get("signer");
        signature = (String) stateOfGoodJSON.get("signature");
        lastState = (JSONObject) stateOfGoodJSON.get("lastState");
        
        return new Good(goodName, ownerId, state, version, signature, signer, lastState.toString());
    }
    
}
