package pt.ulisboa.tecnico.sec.client;

import org.json.JSONException;
import org.json.JSONObject;
import pt.ulisboa.tecnico.sec.communications.Communications;
import pt.ulisboa.tecnico.sec.communications.exceptions.CommunicationsException;
import pt.ulisboa.tecnico.sec.client.exceptions.UtilMethodsException;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Random;
import java.util.Scanner;

public class UtilMethods {
    public UtilMethods() {
    }

    static String now() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		return dateFormat.format(date);
    }
    static void writeFile(String filepath, String writeString) throws UtilMethodsException {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath));
            bw.write(writeString);
            bw.close();
        } catch (FileNotFoundException fnfe) {
            throw new UtilMethodsException("writeFile(): Could not find file '" + filepath + "'.", fnfe);
        } catch (IOException ioe) {
            throw new UtilMethodsException("writeFile(): Could not write to file '" + filepath + "'.", ioe);
        }
    }
    static String readFile(String filepath) throws UtilMethodsException {
        BufferedReader br;
        StringBuilder stringBuilder  = new StringBuilder();
        String line;
        String ls = System.lineSeparator();
        try {
            br = new BufferedReader(new FileReader(filepath));
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            // delete the last new line separator
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            br.close();
        } catch (FileNotFoundException fnfe) {
            throw new UtilMethodsException("readFile(): Could not find file '" + filepath + "'.", fnfe);
        } catch (IOException ioe) {
            throw new UtilMethodsException("readFile(): Could not read file '" + filepath + "'.", ioe);
        }
        String content = stringBuilder.toString();
        return content;
    }
    static String getInput(Scanner scanner) {
        String input = scanner.nextLine();
        return input;
    }
    static void sendMessage(Communications communication, String msg) throws UtilMethodsException {
        try {
            communication.sendInChunks(msg);
        } catch (CommunicationsException ce) {
            throw new UtilMethodsException("sendMessage(): Communications module broke down.", ce);
        }
    }
    static String receiveMessage(Communications communication) throws UtilMethodsException {
        String receivedString;
        try {
            receivedString = (String) communication.receiveInChunks();
        } catch (CommunicationsException ce) {
            throw new UtilMethodsException("receiveMessage(): Communications module broke down...", ce);
        }
        return receivedString;
    }
    static JSONObject convertStringToJSON(String jsonString) throws UtilMethodsException {
        // throws JSONException
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException je) {
            throw new UtilMethodsException("convertStringToJSON(): String '" + jsonString + "' is not a valid JSON string.", je);
        }
        return jsonObject;
    }
    static JSONObject convertFileToJSON(String jsonFilePath) throws UtilMethodsException {
        String jsonString;
        jsonString = readFile(jsonFilePath);
        return convertStringToJSON(jsonString);
    }
    private static Boolean jsonKeyExists(JSONObject jsonObject, String key) {
        return jsonObject.has(key);
    }
    static Object jsonGetObjectByKey(JSONObject jsonObject, String key) throws UtilMethodsException {
        if (!jsonKeyExists(jsonObject, key)) {
            throw new UtilMethodsException("jsonGetObjectByKey(): Key '" + key + "'  does not exist.");
        }
        return jsonObject.get(key);
    }    
    static String changeVersionOfJSON(JSONObject jsonObject, int version) {
    	String updatedContentString;
    	jsonObject.put("version", version+1);
    	updatedContentString = jsonObject.toString();
    	return updatedContentString;
    }
    static String changeWriteVersionOfJSON(JSONObject jsonObject, int version) {
    	String updatedContentString;
    	jsonObject.put("write-version", version+1);
    	updatedContentString = jsonObject.toString();
    	return updatedContentString;
    }
    static String changeLastStateOfJSON(JSONObject jsonObject, String lastState) {
    	String updatedContentString;
    	jsonObject.put("lastState", lastState);
    	updatedContentString = jsonObject.toString();
    	return updatedContentString;
    }
    static String byteToHex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
    static String getStringDigest(String alg, String string) throws UtilMethodsException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException nsae) {
            throw new UtilMethodsException("getDigestFromString(): " + alg + " not found");
        }
        md.reset();
        try {
            md.update(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            throw new UtilMethodsException("getDigestFromString(): UTF-8 not supported");
        }

        return byteToHex(md.digest());
    }
    static String generateRandomAlphaNumeric(final int numberOfChars, boolean alphabetOnly) throws UtilMethodsException {
        String characters = alphabetOnly?"ABCDEFGHIJKLMNOPQRSTUVWXYZ":
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new SecureRandom();

        if (numberOfChars <= 0) {
            throw new UtilMethodsException("generateRandomAlphaNumeric(): Illegal argument, " +
                    "numberOfChars must be greater or equal than 0.");
        }

        StringBuilder randomAlphaNumeric = new StringBuilder(numberOfChars);
        for (int i = 0; i < numberOfChars; i++) {
            randomAlphaNumeric.append(characters.charAt(random.nextInt(characters.length())));
        }


        return randomAlphaNumeric.toString();
    }
}