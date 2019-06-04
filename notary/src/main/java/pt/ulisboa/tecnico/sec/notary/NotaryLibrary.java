package pt.ulisboa.tecnico.sec.notary;

import java.util.Iterator;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryEncapsulationException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryLibraryException;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotarySecurityException;
import pt.ulisboa.tecnico.sec.notary.exceptions.UtilMethodsException;

class NotaryLibrary {
    private static final String CLIENTS_INFO = "clients_info.json";    
    
    private static final String ON_SALE = "on-sale";
    private static final String NOT_ON_SALE = "not-on-sale";    
    
    private Communications communication;

    private NotarySecurity notarySec;
    private String option;

    private int notary;
    private String receivedEchoMessage; 
    
    private String goodsList;
    
    NotaryLibrary(Communications communication, String option, int notary) {
        this.communication = communication;
        this.option = option;
        this.notary = notary;        
        this.goodsList = "goods_list" + notary + ".json";
        
        try {
			initializeModules();
		} catch (NotaryLibraryException nle) {
			System.out.println("Could not initiate the NotaryLibrary module! Aborting...");
		}        
    }
    
    public String getReceivedEchoMessage() {
    	return this.receivedEchoMessage;
    }
    
    private void initializeModules() throws NotaryLibraryException {
    	this.notarySec = new NotarySecurity(this.communication, this.option, this.notary);
    	
    	try {
    		setAllDefaultSignaturesToMySignature(this.notarySec);
    	} catch(NotaryLibraryException nle) {
    		throw new NotaryLibraryException("initializeModules(): something went wrong while initializing the modules...");    		
    	}
    }
    private void setAllDefaultSignaturesToMySignature(NotarySecurity notarySec) throws NotaryLibraryException {
    	JSONObject goodsJSON, updatedJSON, innerJSON; 
    	String currentKey, signedString;
    	Good updateGood;
    	synchronized(this) {
	    	try {
				notarySec.databaseRecoverNormalState();
				goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);			
				
				Iterator<String> iter = goodsJSON.keys();
				
				while(iter.hasNext()) {
					currentKey = iter.next();
					innerJSON = (JSONObject) goodsJSON.get(currentKey);
					
					if(innerJSON.get("signature").equals("default_signature")) {
						updateGood = this.notarySec.setGoodInfo(innerJSON, currentKey);
											
						updateGood.setSigner("Notary" + this.notary);
						signedString = notarySec.signString(updateGood.getLastState());
						updateGood.setSignature(signedString);
						
						updatedJSON = notarySec.updateJSONWithGood(goodsJSON, updateGood);
						notarySec.atomicWriteJSONToFile(updatedJSON, goodsList);
					}
				}
				
			} catch (NotarySecurityException nse) {
				throw new NotaryLibraryException("setAllDefaultSignaturesToMySignature(): something went wrong with the NotarySecurityException class...");
			} catch (UtilMethodsException ume) {
				throw new NotaryLibraryException("setAllDefaultSignaturesToMySignature(): something went wrong with the UtilMethods class...");
			}
    	}     
    }
    private void setSentReadInfo(String sentMessage, JSONObject goodsJSON, Good good) throws NotarySecurityException {
    	JSONObject updatedGoodsJSON;
    	String encapsulatedContent[], signature, content;
    	try {    		
    		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(sentMessage, 1);
    		
    		content = encapsulatedContent[0];
    		signature = encapsulatedContent[1];
    		
    		// version is updated
            good.setVersion(good.getVersion());
            // update JSON with new good
            updatedGoodsJSON =  this.notarySec.updateJSONWithGood(goodsJSON, good);
            // write new JSON to file
            this.notarySec.atomicWriteJSONToFile(updatedGoodsJSON, goodsList);
            
    	} catch (NotarySecurityException nse) {
    		throw new NotarySecurityException("setSentReadInfo(): something went "
    				+ "wrong with the NotarySecurity exception...", nse);
		} catch (NotaryEncapsulationException nee) {
    		throw new NotarySecurityException("setSentReadInfo(): something went "
    				+ "wrong with the NotaryEncapsulation exception...", nee);
		}
    }
    // write-back getStateOfGood
    void writeBackWriteState(String receivedJSONString) throws NotaryLibraryException {
    	String encapsulatedContent[], ownerId, signature, state, signer, goodName, lastState;
    	JSONObject updatedGoodsJSON, receivedJSONObject, goodsJSON;
    	Good goodToWrite;
    	int version, writeVersion;
    	
    	try {
    		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 1);
	    	receivedJSONObject = UtilMethods.convertStringToJSONObject(encapsulatedContent[0]);
	    	
	    	goodName = (String) receivedJSONObject.get("good");
    		
    		notarySec.databaseRecoverNormalState();
            goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
            goodToWrite = notarySec.getGoodFromJSONByName(goodsJSON, goodName);
                        
            ownerId = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "owner-id");
	    	version = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "version");
	    	writeVersion = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "write-version");
	    	signature = encapsulatedContent[1];
	    	signer = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "signer");
	    	state = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "state");
	    	lastState = encapsulatedContent[0];
	    				
    	
    		// atomic register: write-back
			//if(writeVersion>fetchLastWrittenVersion(goodToWrite)) {
			// atomic register: write-back, stronger condition
			if(writeVersion>fetchLastWrittenVersion(goodToWrite)) {
				goodToWrite.setLastState(lastState);
			 	goodToWrite.setOwnerId(ownerId);
			 	goodToWrite.setSignature(signature);
			 	goodToWrite.setSigner(signer);
			 	goodToWrite.setState(state);
			 	goodToWrite.setVersion(version);
		    	// update JSON with new good    	
			    updatedGoodsJSON = notarySec.updateJSONWithGood(goodsJSON, goodToWrite);
			    // write new JSON to file
			    notarySec.atomicWriteJSONToFile(updatedGoodsJSON, goodsList);
			    
			    // notify success to client
		        this.notarySec.sendOk(goodToWrite);
			} else {
				//this.notarySec.sendNotOk("Already had that written version on the goods list...", goodToWrite);
				this.notarySec.sendNotOk("Already had that written version on the goods list, or" +
						" you sent me a weird WTS...", goodToWrite);
			}
    	} catch(UtilMethodsException ume) {
    		throw new NotaryLibraryException("goodTransaction(): something went wrong with the UtilMethods class...", ume);
    	} catch (NotaryEncapsulationException nee) {
    		throw new NotaryLibraryException("goodTransaction(): something went wrong with the NotaryEncapsulation class...", nee);
		} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("goodTransaction(): something went wrong with the NotarySecurity class...", nse);
		}
    }

    // write-back intentionToSell
    private void putOnSale(String receivedJSONString, String signature, Good goodToSell, JSONObject goodsJSON) throws NotaryLibraryException {
    	String signer;
    	JSONObject receivedJSONObject, updatedGoodsJSON;
    	int version, writeVersion;

    	try {            
	    	receivedJSONObject = UtilMethods.convertStringToJSONObject(receivedJSONString);
	    	version = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "version");
	    	writeVersion = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "write-version");
	    	signer = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "signer");

			// atomic register: write-back
	    	//if(writeVersion>fetchLastWrittenVersion(goodToSell)) {
			// atomic register: write-back, stronger condition
			if(writeVersion>fetchLastWrittenVersion(goodToSell)) {
		    	// now on sale
		        goodToSell.setState(ON_SALE);
		        // add signer
		        goodToSell.setSigner(signer);
		        // set signature
		        goodToSell.setSignature(signature);
		        // set last state
		        goodToSell.setLastState(receivedJSONString);
		        // version is updated
		        goodToSell.setVersion(version);        
		        // update JSON with new good
		        updatedGoodsJSON = notarySec.updateJSONWithGood(goodsJSON, goodToSell);
		        // write new JSON to file
		        notarySec.atomicWriteJSONToFile(updatedGoodsJSON, goodsList);
		        		        		        
                // notify success to client
                this.notarySec.sendOk(goodToSell);
                System.out.println("intentionToSell() function: Success!");
	    	} else {
	    		this.notarySec.sendNotOk("Already had that written version on the goods list...", goodToSell);
	    	}
    	} catch(UtilMethodsException ume) {
    		throw new NotaryLibraryException("putOnSale(): something went wrong with the UtilMethods class...");
    	} catch (NotarySecurityException nse) {
    		throw new NotaryLibraryException("putOnSale(): something went wrong with the NotarySecurity class...");
		}
    }

    // write-back transferGood
    private void goodTransaction(String receivedJSONString, String signature, Good goodToSell, String buyerName, JSONObject goodsJSON) throws NotaryLibraryException {
    	String encapsulatedContent[], signer;
    	JSONObject updatedGoodsJSON, receivedJSONObject;
    	int version, writeVersion;

    	try {                		
	    	receivedJSONObject = UtilMethods.convertStringToJSONObject(receivedJSONString);
	    	version = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "version");
	    	writeVersion = (int) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "write-version");
	    	signer = (String) UtilMethods.jsonGetObjectByKey(receivedJSONObject, "signer");

	    	// atomic register: write-back
			//if(writeVersion>fetchLastWrittenVersion(goodToSell)) {
			// atomic register: write-back, stronger condition
			if(writeVersion>fetchLastWrittenVersion(goodToSell)) {
			 	// not on sale anymore
		    	goodToSell.setState(NOT_ON_SALE);
		        // add signer
		        goodToSell.setSigner(signer);
		        // set signature
		        goodToSell.setSignature(signature);
		        // set last state
		        goodToSell.setLastState(receivedJSONString);
			    // now belongs to buyer
		    	goodToSell.setOwnerId(buyerName);	    
			    // version is updated
		    	goodToSell.setVersion(version);
		    	// update JSON with new good    	
			    updatedGoodsJSON = notarySec.updateJSONWithGood(goodsJSON, goodToSell);
			    // write new JSON to file
			    notarySec.atomicWriteJSONToFile(updatedGoodsJSON, goodsList);
			    			    			    
			    // notify success to client
                this.notarySec.sendOk(goodToSell);
                System.out.println("transferGood() function: Success!");
	    	} else {
	    		this.notarySec.sendNotOk("Already had that written version on the goods list...", goodToSell);
	    	}	    	
    	} catch(UtilMethodsException ume) {
    		throw new NotaryLibraryException("goodTransaction(): something went wrong with the UtilMethods class...", ume);
    	} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("goodTransaction(): something went wrong with the NotarySecurity class...", nse);
		}    	

    }
    // package-private methods
    String receiveCommand() throws NotaryLibraryException {
        String contentString, command;
        JSONObject contentJSONObject;
        try {
        	contentString = this.notarySec.authenticatedReceive("callCommand", null);
        
            contentJSONObject = UtilMethods.convertStringToJSONObject(contentString);
            command = (String) UtilMethods.jsonGetObjectByKey(contentJSONObject,"command");
        } catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("receiveCommand(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("receiveCommand(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        }
        return command;
    }
        
    void ping() throws NotaryLibraryException {
    	String receivedString, clientName;
    	JSONObject receivedJSON, clientsJSON;
    	try {
			// verifies user is who he claims to be and receives string
            receivedString = this.notarySec.authenticatedReceive("getStateOfGood", null);
            // validates and converts received string to JSON
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);

            // user can be any user
            clientName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"user");
			synchronized(this) {	            
				clientsJSON = UtilMethods.convertFileToJSONObject(CLIENTS_INFO);
	            this.notarySec.verifyUserExists(clientsJSON, clientName);
	            
				this.notarySec.sendOk();
				System.out.println("ping(): success!");
			}
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("ping(): something went wrong with the NotarySecurity class...", nse);
    	} catch (UtilMethodsException ume) {
    		throw new NotaryLibraryException("ping(): something went wrong with the UtilMethods class...", ume);
		}
    }

    void getStateOfGood() throws NotaryLibraryException {
        String receivedString, clientName, goodName, sentMessage;
        JSONObject receivedJSON, clientsJSON, goodsJSON, updatedGoodsJSON;
        Good good;

        try {
            // verifies user is who he claims to be and receives string
            receivedString = this.notarySec.authenticatedReceive("getStateOfGood", null);
            // validates and converts received string to JSON
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);

            // user can be any user
            clientName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"user");
            goodName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"good");

            clientsJSON = UtilMethods.convertFileToJSONObject(CLIENTS_INFO);
            this.notarySec.verifyUserExists(clientsJSON, clientName);

            // CPU-ATOMICITY: read-from/write-to database
            synchronized(this) {
                notarySec.databaseRecoverNormalState();
                goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
                good = notarySec.getGoodFromJSONByName(goodsJSON, goodName);  
                
                // version is updated
                good.setVersion(good.getVersion()+1);
                
                sentMessage = this.notarySec.sendOk(good);
                setSentReadInfo(sentMessage, goodsJSON, good);
            }            
            System.out.println("getStateOfGood() function: Success!");
        } catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("getStateOfGood(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("getStateOfGood(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        }

    }
    String intentionToSellPhaseOne() throws NotaryLibraryException {
        String receivedString, sellerName, goodName;
        JSONObject receivedJSON, clientsJSON, goodsJSON, updatedGoodsJSON;
        Good good;

        try {
            // verifies user is who he claims to be
            receivedString = this.notarySec.authenticatedReceive("intentionToSellTransaction", null);

            // validates and converts received string to JSON
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);

            // user must be seller
            sellerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"seller");
            goodName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"good");

            clientsJSON = UtilMethods.convertFileToJSONObject(CLIENTS_INFO);
            this.notarySec.verifyUserExists(clientsJSON, sellerName);

            // CPU-ATOMICITY: read-from/write-to database
            synchronized(this) {
                notarySec.databaseRecoverNormalState();
                goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
                good = notarySec.getGoodFromJSONByName(goodsJSON, goodName);
                
                // version is updated
                good.setVersion(good.getVersion()+1);                
                
                // freshness protocol
                receivedString = this.notarySec.ensureFreshness(good, sellerName, null, "intentionToSellFreshness", goodsJSON);
                
                return receivedString;
            }
        } catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        } catch (NotaryEncapsulationException nee) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotaryEncapsulation function."+
                    "Message: '" + nee.getMessage() + "'.", nee);
		}

    }    
    void intentionToSellPhaseTwo(String receivedStringFromNotaries) throws NotaryLibraryException {
    	JSONObject receivedJSON, goodsJSON;
    	String sellerName, goodName, receivedString, signature, encapsulatedContent[];    	
    	Good good;
    	
    	try {
    		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedStringFromNotaries, 1);
    		receivedString = encapsulatedContent[0];
    		signature = encapsulatedContent[1];
	    	// validates and converts received string to JSON
	        receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);
	    	// user must be seller
	        sellerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"seller");
	        goodName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"good");
	        
	        // CPU-ATOMICITY: read-from/write-to database
	        synchronized(this) {
	            notarySec.databaseRecoverNormalState();
	            goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
	            good = notarySec.getGoodFromJSONByName(goodsJSON, goodName);	            	                       
	            
	          	// if owner is not seller
	            if (!good.getOwnerId().equals(sellerName)) {
	                String errorMsg = "('" + sellerName + "') is not the owner of this good.";
	                this.notarySec.sendNotOk(errorMsg);
	                throw new NotaryLibraryException("intentionToSell(): " + "Client '"
	                        + sellerName + "' is not the owner of the specified good.");
	            }
	            // else if good is already on sale
	            else if(good.getState().equals(ON_SALE)) {
	                String errorMsg = "Good '" + goodName + "' is already on sale.";
	                this.notarySec.sendNotOk(errorMsg);
	                throw new NotaryLibraryException("intentionToSell(): " + errorMsg);	
	            }
	            // else it's not on sale and the owner is the seller, so it's ok
	            else {
	            	putOnSale(receivedString, signature, good, goodsJSON);                    
	            }
	            
	        }
    	} catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        } catch (NotaryEncapsulationException nee) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotaryEncapsulation function."+
                    "Message: '" + nee.getMessage() + "'.", nee);
		}
  
    }

    String transferGoodPhaseOne() throws NotaryLibraryException {
        String receivedString, sellerName, buyerName, goodName, cryptoPuzzleCorrectAnswer;
        boolean cryptoPuzzleOk;
        JSONObject receivedJSON, clientsJSON, goodsJSON, updatedGoodsJSON;
        Good good;

        try {
            // send user the crypto puzzle
            cryptoPuzzleCorrectAnswer = this.notarySec.sendCryptoPuzzleChallenge();
            cryptoPuzzleOk = this.notarySec.receiveAndVerifyCryptoPuzzle(cryptoPuzzleCorrectAnswer);
            System.out.println("Crypto Puzzle answer: " + cryptoPuzzleOk);
            if (!cryptoPuzzleOk) {
                throw new NotaryLibraryException("Received crypto-puzzle answer is WRONG.");
            }
            // verifies user is who he claims to be
            receivedString = this.notarySec.authenticatedReceive("transferGoodPhaseOne", null);

            // validates received JSON
            receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);

            // user must be seller
            sellerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"seller");
            buyerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"buyer");
            goodName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"good");

            clientsJSON = UtilMethods.convertFileToJSONObject(CLIENTS_INFO);
            this.notarySec.verifyUserExists(clientsJSON, sellerName);
            this.notarySec.verifyUserExists(clientsJSON, buyerName);

            // CPU-ATOMICITY: read-from/write-to database
            synchronized (this) {
                notarySec.databaseRecoverNormalState();
                goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
                good = notarySec.getGoodFromJSONByName(goodsJSON, goodName);

				// version is updated
				good.setVersion(good.getVersion()+1);

                // freshness protocol
                receivedString = this.notarySec.ensureFreshness(good, sellerName, buyerName, "transferGoodPhaseTwo", goodsJSON);
                
                return receivedString;
            }
        } catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("transferGood(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("transferGood(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        } catch (NotaryEncapsulationException nee) {
        	throw new NotaryLibraryException("transferGood(): Something went wrong with a NotaryEncapsulation function."+
                    "Message: '" + nee.getMessage() + "'.", nee);
		}
    }    
    void transferGoodPhaseTwo(String receivedJSONString) throws NotaryLibraryException {
	    	
    	JSONObject receivedJSON, goodsJSON;
    	String sellerName, buyerName, goodName, receivedString, signature, encapsulatedContent[];    	
    	Good good;
    	
    	try {
    		encapsulatedContent = NotaryEncapsulation.getEncapsulatedContent(receivedJSONString, 2);
    		receivedString = encapsulatedContent[0];
    		signature = encapsulatedContent[1];
	    	// validates and converts received string to JSON
	        receivedJSON = UtilMethods.convertStringToJSONObject(receivedString);
	    	// user must be seller
	        buyerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"signer");
	        sellerName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"seller");
	        goodName = (String) UtilMethods.jsonGetObjectByKey(receivedJSON,"good");
	        
	        // CPU-ATOMICITY: read-from/write-to database
	        synchronized(this) {
	            notarySec.databaseRecoverNormalState();
	            goodsJSON = UtilMethods.convertFileToJSONObject(goodsList);
	            good = notarySec.getGoodFromJSONByName(goodsJSON, goodName);	            	                       
	            
	        	// if owner is not seller
	            if (!good.getOwnerId().equals(sellerName)) {
	                String errorMsg = "('" + sellerName + "') is not the owner of this good.";
	                this.notarySec.sendNotOk(errorMsg, good);
	                throw new NotaryLibraryException("transferGood(): " + "Client '"
	                        + sellerName + "' is not the owner of the specified good.");
	            }
	            // else if seller is the same as buyer
	            else if (sellerName.equals(buyerName)) {
	                String errorMsg = "('" + sellerName + "') can't transfer a good to itself.";
	                this.notarySec.sendNotOk(errorMsg, good);
	                throw new NotaryLibraryException("transferGood(): " + errorMsg);
	            }
	            // else if good is not on sale
	            else if (good.getState().equals(NOT_ON_SALE)) {
	                String errorMsg = "Good '" + goodName + "' is not on sale.";
	                this.notarySec.sendNotOk(errorMsg, good);
	                throw new NotaryLibraryException("transferGood(): " + errorMsg);
	            }
	            // else, the owner is the seller, seller != buyer and good is on sale
	            else {
	    			// version is updated
	    			//good.setVersion(good.getVersion()+1);
	                goodTransaction(receivedString, signature, good, buyerName, goodsJSON);                                        
	            }	            		            
	        }
    	} catch (UtilMethodsException ume) {
            throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a UtilMethods function."+
                    "Message: '" + ume.getMessage() + "'.", ume);
        } catch (NotarySecurityException nse) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
        } catch (NotaryEncapsulationException nee) {
        	throw new NotaryLibraryException("intentionToSellPhaseOne(): Something went wrong with a NotaryEncapsulation function."+
                    "Message: '" + nee.getMessage() + "'.", nee);
		}
    }
    String receiveWriteBack() throws NotaryLibraryException {
    	String receivedString;
    	
    	try {
    		receivedString = this.notarySec.authenticatedReceive("writeBack", null);
    		return receivedString;    		
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("writeBack(): Something went wrong with a NotarySecurity function."+
                    "Message: '" + nse.getMessage() + "'.", nse);
    	}
    }    
    private int fetchLastWrittenVersion(Good good) throws NotaryLibraryException {
    	String lastState;
    	JSONObject lastStateJSONObject;
    	int lastVersion;
    	
    	try {
	    	lastState = good.getLastState();
	    	lastStateJSONObject = UtilMethods.convertStringToJSONObject(lastState);
	    	
	    	lastVersion = (int) UtilMethods.jsonGetObjectByKey(lastStateJSONObject, "write-version");
	    	return lastVersion;
    	} catch(UtilMethodsException ume) {
    		throw new NotaryLibraryException("fetchLastWrittenVersion(): something went wrong with the UtilMethods class...", ume);
    	}
    }        
    String broadcastConsensusEcho() throws NotaryLibraryException {
    	String receivedEchoMessage;
		try {
			receivedEchoMessage = this.notarySec.authenticatedReceive("ECHO", null);
			
			return receivedEchoMessage;
		} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("broadcastConsensus(): something went wrong with"
					+ " the NotarySecurity class...");
		}    	
    }
    
    String broadcastConsensusReady() throws NotaryLibraryException {
    	String receivedEchoMessage;
		try {
			receivedEchoMessage = this.notarySec.authenticatedReceive("ECHO", null);
			
			return receivedEchoMessage;
		} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("broadcastConsensus(): something went wrong with"
					+ " the NotarySecurity class...");
		}    	
    }
    String broadcastConsensusEchoTG() throws NotaryLibraryException {
    	String receivedEchoMessage;
		try {
			receivedEchoMessage = this.notarySec.authenticatedReceive("ECHO-TG", null);
			
			return receivedEchoMessage;
		} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("broadcastConsensus(): something went wrong with"
					+ " the NotarySecurity class...");
		}    	
    }
    
    String broadcastConsensusReadyTG() throws NotaryLibraryException {
    	String receivedEchoMessage;
		try {
			receivedEchoMessage = this.notarySec.authenticatedReceive("ECHO-TG", null);
			
			return receivedEchoMessage;
		} catch (NotarySecurityException nse) {
			throw new NotaryLibraryException("broadcastConsensus(): something went wrong with"
					+ " the NotarySecurity class...");
		}    	
    }
    void sendBroadcast(String receivedJSONString) throws NotaryLibraryException {
    	try {
    		this.notarySec.sendCommand("Notary" + String.valueOf(this.notary), "BROADCAST");
    		this.notarySec.sendBroadcastMessage(receivedJSONString);
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("intentionToSellPhaseTwo(): something went wrong with"
    				+ " the NotarySecurity class...");
    	}
    }
    void sendReady(String receivedJSONString) throws NotaryLibraryException {
    	try {
    		this.notarySec.sendCommand("Notary" + String.valueOf(this.notary), "READY");
    		this.notarySec.sendBroadcastMessage(receivedJSONString);
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("intentionToSellPhaseThree(): something went wrong with"
    				+ " the NotarySecurity class...");
    	}
    }
    void sendBroadcastTG(String receivedJSONString) throws NotaryLibraryException {
    	try {
    		this.notarySec.sendCommand("Notary" + String.valueOf(this.notary), "BROADCAST-TG");
    		this.notarySec.sendBroadcastMessage(receivedJSONString);
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("intentionToSellPhaseTwo(): something went wrong with"
    				+ " the NotarySecurity class...");
    	}
    }
    void sendReadyTG(String receivedJSONString) throws NotaryLibraryException {
    	try {
    		this.notarySec.sendCommand("Notary" + String.valueOf(this.notary), "READY-TG");
    		this.notarySec.sendBroadcastMessage(receivedJSONString);
    	} catch(NotarySecurityException nse) {
    		throw new NotaryLibraryException("intentionToSellPhaseThree(): something went wrong with"
    				+ " the NotarySecurity class...");
    	}
    }
}
