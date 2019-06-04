package pt.ulisboa.tecnico.sec.notary;

public class Good {
    private String name;
    private String ownerId;
    private String state;
    private String signature = "default_signature";
    private String signer = "default_signer";
    private String lastState = "default_last_state";
    private int version;

    public Good(String name, String ownerId, String state, int version, String signature, String signer, String lastState) {
        this.name = name;
        this.ownerId = ownerId;
        this.state = state;
        this.version = version;
        this.signature = signature;
        this.signer = signer;
        this.lastState = lastState;
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    public String getSignature() {
        return this.signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getSigner() {
        return this.signer;
    }
    public void setSigner(String signer) {
        this.signer = signer;
    }
    public String getLastState() {
    	return this.lastState;
    }
    public void setLastState(String lastState) {
    	this.lastState = lastState;
    }
}
