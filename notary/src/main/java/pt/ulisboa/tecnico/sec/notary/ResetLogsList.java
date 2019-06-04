package pt.ulisboa.tecnico.sec.notary;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;

public class ResetLogsList {
		
	// to be run like a regular java file
	public static void main(String argv[]) {
		JSONArray goodJSONArray = new JSONArray();
		
		for(int i = 1; i <= 7; i++) {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter("logs" + i + ".json"));
				bw.write(goodJSONArray.toString());
				bw.close();
			} catch (IOException ioe) {
				System.out.println("main(): something went wrong while writing to a file");
			}
		}
	}

}
