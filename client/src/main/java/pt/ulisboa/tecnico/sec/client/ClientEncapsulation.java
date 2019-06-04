package pt.ulisboa.tecnico.sec.client;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientEncapsulationException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;

public class ClientEncapsulation {
    
	static int fetchVersion(String message) throws ClientEncapsulationException {
    	JSONObject messageJSON;
    	int version;    	
    	try {
			messageJSON = UtilMethods.convertStringToJSON(message);
			version = (int) UtilMethods.jsonGetObjectByKey(messageJSON, "version");
			return version;
		} catch (UtilMethodsException ume) {
			throw new ClientEncapsulationException("fetchVersion(): something went wrong with the UtilMethods class...");
		}
    }
    static String fetchStatus(String message) throws ClientEncapsulationException {
    	JSONObject messageJSON;
    	String status;    	
    	try {
			messageJSON = UtilMethods.convertStringToJSON(message);
			status = (String) UtilMethods.jsonGetObjectByKey(messageJSON, "status");
			return status;
		} catch (UtilMethodsException ume) {
			throw new ClientEncapsulationException("fetchVersion(): something went wrong with the UtilMethods class...");
		}
    }
    
    static String[] getEncapsulatedContent(String contentString, int numberOfEncapsulations) throws ClientEncapsulationException {
    	JSONObject firstEncapsulationJSONObject, secondEncapsulationJSONObject, thirdEncapsulationJSONObject, fourthEncapsulationJSONObject;
    	String firstEncapsulationString, secondEncapsulationString, thirdEncapsulationString;
    	String fetchedContent, fetchedSignature, encapsulatedContent[] = null;
    	// encapsulatedContent[
    	try {
	    	switch(numberOfEncapsulations) {
	    		case 1:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSON(contentString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    		case 2:    			
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSON(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSON(firstEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    		case 3:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSON(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSON(firstEncapsulationString);
	    			secondEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			
	    			thirdEncapsulationJSONObject = UtilMethods.convertStringToJSON(secondEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    			
	    		case 4:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSON(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSON(firstEncapsulationString);
	    			secondEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			
	    			thirdEncapsulationJSONObject = UtilMethods.convertStringToJSON(secondEncapsulationString);
	    			thirdEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "content");
	    			
	    			fourthEncapsulationJSONObject = UtilMethods.convertStringToJSON(thirdEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(fourthEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(fourthEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;    			
	    		default:
	    			System.out.println("There is not a case statement for that number of encapsulations");    			
	    	}
    	} catch(UtilMethodsException ume) {
    		throw new ClientEncapsulationException("getEncapsulatedContent(): something went wrong with the UtilMethods class...");
    	}
    	return encapsulatedContent;
    }
}
