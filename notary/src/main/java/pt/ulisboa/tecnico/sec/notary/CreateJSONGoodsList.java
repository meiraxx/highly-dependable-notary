package pt.ulisboa.tecnico.sec.notary;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONObject;

public class CreateJSONGoodsList {
	
	// to be run like a regular java file
	public static void main(String argv[]) {
		JSONObject goodJSONObject = new JSONObject();
		
		for(int i = 1; i <= 7; i++) {
			for(int j = 1; j <= 5; j++) {
				JSONObject goodInfoJSONObject = new JSONObject();				
				JSONObject lastStateJSONObject = new JSONObject();				
				JSONObject valueJSONObject = new JSONObject();
				
				goodInfoJSONObject.put("owner-id", "User"+j);
				goodInfoJSONObject.put("signer", "default_signer");
				goodInfoJSONObject.put("signature", "default_signature");
				goodInfoJSONObject.put("state", "not-on-sale");
				
				lastStateJSONObject.put("version", 0);
				lastStateJSONObject.put("write-version", 0);
				valueJSONObject.put("owner-id", "User"+j);
				valueJSONObject.put("state", "not-on-sale");
				lastStateJSONObject.put("value", valueJSONObject);
				
				goodInfoJSONObject.put("lastState", lastStateJSONObject);
				goodInfoJSONObject.put("version", 0);
				
				goodJSONObject.put("Good" + j, goodInfoJSONObject);							
			}
			
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter("goods_list" + i + ".json"));
				bw.write(goodJSONObject.toString());
				bw.close();
			} catch (IOException ioe) {
				System.out.println("main(): something went wrong while writing to a file");
			}
		}
	}
}
