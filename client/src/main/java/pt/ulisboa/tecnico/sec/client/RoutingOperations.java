package pt.ulisboa.tecnico.sec.client;

import pt.ulisboa.tecnico.sec.client.exceptions.ClientLibraryException;
import pt.ulisboa.tecnico.sec.client.exceptions.ClientSecurityException;
import pt.ulisboa.tecnico.sec.client.exceptions.RoutingOperationsException;

public class RoutingOperations {
	
	ClientSecurity clientSec;
	
	public RoutingOperations(ClientSecurity clientSec) {
		this.clientSec = clientSec;
	}
    String relayFreshnessBetweenClientNotary(String versionString, String buyer) throws RoutingOperationsException {
        try {
			this.clientSec.sendSignedMessage(versionString);
			versionString = this.clientSec.ensureFreshnessClient("relayFreshnessBetweenClientNotary", buyer);
		} catch (ClientSecurityException cse) {
			throw new RoutingOperationsException("relayFreshnessBetweenClientNorary(): something went wrong with the ClientsSecurity module...", cse);
		}
        
        return versionString;
    }
    void terminateBuyGoodMidways(String versionString) throws RoutingOperationsException {
    	try {
    		this.clientSec.sendSignedMessage(versionString);
    	} catch (ClientSecurityException cse) {
			throw new RoutingOperationsException("terminateBuyGoodMidways(): something went wrong with the ClientsSecurity module...", cse);
		}
    }

	
	
}
