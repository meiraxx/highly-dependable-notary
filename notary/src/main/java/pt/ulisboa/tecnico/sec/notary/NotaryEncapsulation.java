package pt.ulisboa.tecnico.sec.notary;

import org.json.JSONObject;

import pt.ulisboa.tecnico.sec.notary.UtilMethods;
import pt.ulisboa.tecnico.sec.notary.exceptions.NotaryEncapsulationException;
import pt.ulisboa.tecnico.sec.notary.exceptions.UtilMethodsException;

public class NotaryEncapsulation {
	
	static int fetchVersion(String message) throws NotaryEncapsulationException {
    	JSONObject messageJSON;
    	int version;    	
    	try {
			messageJSON = UtilMethods.convertStringToJSONObject(message);
			version = (int) UtilMethods.jsonGetObjectByKey(messageJSON, "version");
			return version;
		} catch (UtilMethodsException ume) {
			throw new NotaryEncapsulationException("fetchVersion(): something went wrong with the UtilMethods class...");
		}
    }
	static String[] getEncapsulatedContent(String contentString, int numberOfEncapsulations) throws NotaryEncapsulationException {
    	JSONObject firstEncapsulationJSONObject, secondEncapsulationJSONObject, thirdEncapsulationJSONObject, fourthEncapsulationJSONObject;
    	String firstEncapsulationString, secondEncapsulationString, thirdEncapsulationString;
    	String fetchedContent, fetchedSignature, encapsulatedContent[] = null;
    	// encapsulatedContent[
    	try {
	    	switch(numberOfEncapsulations) {
	    		case 1:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(contentString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    		case 2:    			
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(firstEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    		case 3:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(firstEncapsulationString);
	    			secondEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			
	    			thirdEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(secondEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;
	    			
	    		case 4:
	    			firstEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(contentString);
	    			firstEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(firstEncapsulationJSONObject, "content");
	    			
	    			secondEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(firstEncapsulationString);
	    			secondEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(secondEncapsulationJSONObject, "content");
	    			
	    			thirdEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(secondEncapsulationString);
	    			thirdEncapsulationString = (String) UtilMethods.jsonGetObjectByKey(thirdEncapsulationJSONObject, "content");
	    			
	    			fourthEncapsulationJSONObject = UtilMethods.convertStringToJSONObject(thirdEncapsulationString);
	    			
	    			fetchedContent = (String) UtilMethods.jsonGetObjectByKey(fourthEncapsulationJSONObject, "content");
	    			fetchedSignature = (String) UtilMethods.jsonGetObjectByKey(fourthEncapsulationJSONObject, "signature");
	    			
	    			encapsulatedContent = new String[]{fetchedContent, fetchedSignature};
	    			break;    			
	    		default:
	    			System.out.println("There is not a case statement for that number of encapsulations");    			
	    	}
    	} catch(UtilMethodsException ume) {
    		throw new NotaryEncapsulationException("getEncapsulatedContent(): something went wrong with the UtilMethods class...");
    	}
    	return encapsulatedContent;
    }
}
