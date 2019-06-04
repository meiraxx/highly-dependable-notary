package pt.ulisboa.tecnico.sec;

import pt.ulisboa.tecnico.sec.exceptions.CitizenCardException;

import java.io.*;

import pt.ulisboa.tecnico.sec.exceptions.CryptoAuxException;
import pteidlib.PTEID_Certif;
import pteidlib.PTEID_ID;
import pteidlib.PTEID_PIC;
import pteidlib.PTEID_Pin;
import pteidlib.PTEID_TokenInfo;
import pteidlib.PteidException;
import pteidlib.pteid;

import java.nio.charset.Charset;
import java.lang.reflect.Method;

import sun.security.pkcs11.wrapper.*;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class ccUtility {
    public static void main(String[] args) {
        try {
            String b64encodedSignature;
            String testString;
            boolean verifiedSignature;
            X509Certificate cert;

            System.out.println("Supposed to have success:");
            // get signature provider
            SignatureProvider signatureProvider = usePTEID(false);

            // test string
            testString = "data";

            // generate signature with CC
            b64encodedSignature = generateCCSignature(signatureProvider, testString);

            // get CC certificate
            cert = getCertificateFromCC(true);

            // verify signature
            verifiedSignature = CryptoAux.verifySignature(cert, testString, b64encodedSignature);
            System.out.println("Verified Signature: " + verifiedSignature);

            System.out.println("Supposed to fail:");
            signatureProvider = usePTEID(true);
            // generate signature with CC
            b64encodedSignature = generateCCSignature(signatureProvider, "LOL");

            // verify signature
            verifiedSignature = CryptoAux.verifySignature(cert, testString, b64encodedSignature);
            System.out.println("Verified Signature: " + verifiedSignature);

            pteid.Exit(pteid.PTEID_EXIT_LEAVE_CARD); // leaves eID lib
        } catch (CitizenCardException | PteidException | CryptoAuxException e) {
            System.out.println("main() exception: " + e.getMessage());
        }
    }

    // public functions (called in notary)
    public static SignatureProvider usePTEID(boolean isLogged) throws CitizenCardException {
        try {
            System.loadLibrary("pteidlibj");
            pteid.Init(""); // Initializes the eID Lib
            pteid.SetSODChecking(false); // Don't check the integrity of the ID, address and photo (!)
        } catch (PteidException pe) {
            throw new CitizenCardException("usePTEID() exception: Couldn't open session", pe);
        }
        PKCS11 pkcs11;
        long p11_session;
        Class pkcs11Class;
        String osName, libName, javaVersion;
        osName = System.getProperty("os.name");
        javaVersion = System.getProperty("java.version");

        // linux/windows/mac compatibility
        libName = "libpteidpkcs11.so";
        if (osName.contains("Windows"))
            libName = "pteidpkcs11.dll";
        else if (osName.contains("Mac"))
            libName = "pteidpkcs11.dylib";
        try {
            pkcs11Class = Class.forName("sun.security.pkcs11.wrapper.PKCS11");
        } catch (ClassNotFoundException cnfe) {
            throw new CitizenCardException("getPKCS11Session() exception: Couldn't open session", cnfe);
        }

        // java version compatibility
        try {
            if (javaVersion.startsWith("1.5.")) {
                Method getInstanceMethode = pkcs11Class.getDeclaredMethod("getInstance", String.class, CK_C_INITIALIZE_ARGS.class, boolean.class);
                pkcs11 = (PKCS11) getInstanceMethode.invoke(null, new Object[]{libName, null, false});
            } else {
                Method getInstanceMethode = pkcs11Class.getDeclaredMethod("getInstance", String.class, String.class, CK_C_INITIALIZE_ARGS.class, boolean.class);
                pkcs11 = (PKCS11) getInstanceMethode.invoke(null, new Object[]{libName, "C_GetFunctionList", null, false});
            }
        } catch (Throwable e) {
            throw new CitizenCardException("usePTEID() exception: Problem with java version compatibility code.", e);
        }


        try {
            p11_session = pkcs11.C_OpenSession(0, PKCS11Constants.CKF_SERIAL_SESSION, null, null);
            if (!isLogged) {
                // Token login
                System.out.println("Asking for CC auth to login.");
                pkcs11.C_Login(p11_session, 1, null);
            }
            CK_SESSION_INFO info = pkcs11.C_GetSessionInfo(p11_session);
            // Get available keys
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[1];
            attributes[0] = new CK_ATTRIBUTE();
            attributes[0].type = PKCS11Constants.CKA_CLASS;
            attributes[0].pValue = new Long(PKCS11Constants.CKO_PRIVATE_KEY);
            pkcs11.C_FindObjectsInit(p11_session, attributes);
            long[] keyHandles = pkcs11.C_FindObjects(p11_session, 5);
            // use AUTH KEY
            long signatureKey = keyHandles[0];
            pkcs11.C_FindObjectsFinal(p11_session);
            // initialize the signature method
            CK_MECHANISM mechanism = new CK_MECHANISM();
            mechanism.mechanism = PKCS11Constants.CKM_SHA1_RSA_PKCS;
            mechanism.pParameter = null;
            pkcs11.C_SignInit(p11_session, mechanism, signatureKey);
        } catch (PKCS11Exception pkcs11e) {
            throw new CitizenCardException("usePTEID() exception: PKCS11 or its session are broken", pkcs11e);
        }
        SignatureProvider signatureProvider = new SignatureProvider(pkcs11, p11_session);
        return signatureProvider;
    }
    public static String generateCCSignature(SignatureProvider signatureProvider, String string) throws CitizenCardException {
        byte[] signature;
        String b64encodedSignature;
        PKCS11 pkcs11 = signatureProvider.getPKCS11Object();
        long p11_session = signatureProvider.getPKCS11Session();
        try {
            // sign
            System.out.println("Asking for CC auth to sign.");
            signature = pkcs11.C_Sign(p11_session, string.getBytes(Charset.forName("UTF-8")));
        } catch (PKCS11Exception pkcse) {
            throw new CitizenCardException("ccGenerateSignature(): There was an error signing the data", pkcse);
        }
        b64encodedSignature = Base64.getEncoder().encodeToString(signature);
        return b64encodedSignature;
    }

    // used private methods
    private static X509Certificate getCertificateFromCC(boolean printCert) throws CitizenCardException {
        X509Certificate cert;
        String certString;
        try{
            cert = getCertFromByteArray(getCertificateInBytes(0));
            // extract cc certificate to be used in clients as discussed
            certString = CryptoAux.certificateToString(cert);
        } catch (CertificateException ce) {
            throw new CitizenCardException("ccGetCCCertificate() exception: Could not get public key certificate from citizen card.", ce);
        } catch (CryptoAuxException cae) {
            throw new CitizenCardException("ccGetCertificate() exception: Could not convert certificate to string.", cae);
        }
        if(printCert)
            System.out.print(certString);
        return cert;
    }
    private static byte[] getCertificateInBytes(int n) {
        byte[] certificate_bytes = null;
        try {
            PTEID_Certif[] certs = pteid.GetCertificates();
            certificate_bytes = certs[n].certif; //gets the byte[] with the n-th certif

            // System.out.println("Number of certs found: " + certs.length);
            /*
            // print whole certificate chain
            int i = 0;
            for (PTEID_Certif cert : certs) {
                System.out.println("-------------------------------\nCertificate #" + (i++));
                System.out.println(cert.certifLabel);
            }
            */
        } catch (PteidException e) {
            System.out.println("Exception1");
            e.printStackTrace();
        }
        return certificate_bytes;
    }
    private static X509Certificate getCertFromByteArray(byte[] certificateEncoded) throws CertificateException {
        CertificateFactory f = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(certificateEncoded);
        X509Certificate cert = (X509Certificate) f.generateCertificate(in);
        return cert;
    }

    // unused private methods
    private static void releaseSDK() throws CitizenCardException {
        try {
            pteid.Exit(pteid.PTEID_EXIT_LEAVE_CARD); // leaves eID lib
        } catch (PteidException pe) {
            throw new CitizenCardException("Failed to release SDK.", pe);
        }
    }

    private static void PrintIDData(PTEID_ID idData) {
        System.out.println("DeliveryEntity : " + idData.deliveryEntity);
        System.out.println("PAN : " + idData.cardNumberPAN);
        System.out.println("...");
    }

    private static void showInfo() {
        try {
            int cardtype = pteid.GetCardType();
            switch (cardtype) {
                case pteid.CARD_TYPE_IAS07:
                    System.out.println("IAS 0.7 card\n");
                    break;
                case pteid.CARD_TYPE_IAS101:
                    System.out.println("IAS 1.0.1 card\n");
                    break;
                case pteid.CARD_TYPE_ERR:
                    System.out.println("Unable to get the card type\n");
                    break;
                default:
                    System.out.println("Unknown card type\n");
            }

            // Read ID Data
            PTEID_ID idData = pteid.GetID();
            if (null != idData)
                PrintIDData(idData);


            // Read Picture Data
            PTEID_PIC picData = pteid.GetPic();
            if (null != picData) {
                String photo = "photo.jp2";
                FileOutputStream oFile = new FileOutputStream(photo);
                oFile.write(picData.picture);
                oFile.close();
                System.out.println("Created " + photo);
            }

            // Read Pins
            PTEID_Pin[] pins = pteid.GetPINs();

            // Read TokenInfo
            PTEID_TokenInfo token = pteid.GetTokenInfo();

            // Read personal Data
            byte[] filein = {0x3F, 0x00, 0x5F, 0x00, (byte) 0xEF, 0x07};
            byte[] file = pteid.ReadFile(filein, (byte) 0x81);


        } catch (PteidException e) {
            System.out.println("Exception1");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getCitizenAuthCertInBytes() {
        return getCertificateInBytes(0); //certificado 0 no Cartao do Cidadao eh o de autenticacao
    }
}
