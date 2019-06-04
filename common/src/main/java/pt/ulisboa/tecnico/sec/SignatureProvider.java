package pt.ulisboa.tecnico.sec;

import sun.security.pkcs11.wrapper.PKCS11;

public class SignatureProvider {
    private PKCS11 pkcs11;
    private long p11_session;

    public SignatureProvider(PKCS11 pkcs11, long p11_session) {
        this.pkcs11 = pkcs11;
        this.p11_session = p11_session;
    }

    public PKCS11 getPKCS11Object() {
        return this.pkcs11;
    }

    public long getPKCS11Session() {
        return this.p11_session;
    }

    public void setPKCS11Object(PKCS11 pkcs11) {
        this.pkcs11 = pkcs11;
    }

    public void setPKCS11Session(long p11_session) {
        this.p11_session = p11_session;
    }
}