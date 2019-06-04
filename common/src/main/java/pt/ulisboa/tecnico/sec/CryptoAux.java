package pt.ulisboa.tecnico.sec;

import pt.ulisboa.tecnico.sec.exceptions.CryptoAuxException;

import java.io.*;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

public class CryptoAux {
    public CryptoAux() {
    }

    // static methods in this class
    // very good article on java crypto:
    // http://evverythingatonce.blogspot.com/2014/07/tech-talkcryptography-or-encryption_4233.html
    private static KeyStore loadKeyStoreFromFile(String keyStoreFilePath, String keystorePasswordString) throws CryptoAuxException {
        // filepath, abcdef, 123456, UserX
        char[] keystorePasswordCharArray = keystorePasswordString.toCharArray();
        InputStream readStream;
        KeyStore ks;
        try {
            // JKS for key-stores that include only public keys and certificates
            // JCEKS for key-stores that include private keys too
            ks = KeyStore.getInstance("JKS");
        } catch (KeyStoreException ke) {
            throw new CryptoAuxException("loadKeyStore() exception: Failed to get an instance of keystore from string 'JKS'.", ke);
        }
        try {
            readStream = new FileInputStream(keyStoreFilePath);
        } catch (FileNotFoundException fnfe) {
            throw new CryptoAuxException("loadKeyStore() exception: Didn't find file '" + keyStoreFilePath + "'.", fnfe);
        }
        try {
            ks.load(readStream, keystorePasswordCharArray);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new CryptoAuxException("loadKeyStore() exception: Couldn't load keystore.", e);
        }
        try {
            readStream.close();
        } catch (IOException ioe) {
            throw new CryptoAuxException("loadKeyStore() exception: Couldn't close stream.", ioe);
        }
        return ks;
    }
    private static PrivateKey loadPrivateKeyFromKeyStore (KeyStore keyStore, String keyPasswordString) throws CryptoAuxException {
        PrivateKey privateKey;
        char[] keyPasswordCharArray = keyPasswordString.toCharArray();
        Enumeration<String> aliases;
        String alias;
        KeyStore.PasswordProtection passwordProtection;
        KeyStore.PrivateKeyEntry privateKeyEntry;
        try {
            aliases = keyStore.aliases();
        } catch (KeyStoreException kse) {
            throw new CryptoAuxException("loadPrivateKeyFromKeyStore() exception: Couldn't get key-store aliases.", kse);
        }
        //? does this work?
        alias = aliases.nextElement();
        passwordProtection = new KeyStore.PasswordProtection(keyPasswordCharArray);
        try {
            privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, passwordProtection);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            throw new CryptoAuxException("loadPrivateKeyFromKeyStore() exception: Couldn't fetch private key entry from keystore.", e);
        }
        privateKey = privateKeyEntry.getPrivateKey();
        return privateKey;
    }

    // used
    public static PrivateKey loadPrivateKeyFromFile(String keyStoreFilePath, String keystorePasswordString, String keyPasswordString) throws CryptoAuxException {
        KeyStore keyStore = loadKeyStoreFromFile(keyStoreFilePath, keystorePasswordString);
        PrivateKey privateKey = loadPrivateKeyFromKeyStore(keyStore, keyPasswordString);
        return privateKey;
    }
    public static X509Certificate loadCertificateFromFile(String pemFilePath) throws CryptoAuxException {
        CertificateFactory certFactory;
        FileInputStream fileInputStream;
        X509Certificate certificate;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException ce) {
            throw new CryptoAuxException("loadCertificateFromFile() exception: Could not get X.509" +
                    "instance from Certificate Factory",ce);
        }
        try {
            fileInputStream = new FileInputStream(pemFilePath);
        } catch (FileNotFoundException fnfe) {
            throw new CryptoAuxException("loadCertificateFromFile() exception: Could not find file '" +
                    pemFilePath + "'.",fnfe);
        }
        try {
            certificate = (X509Certificate) certFactory.generateCertificate(fileInputStream);
        } catch (CertificateException ce) {
            throw new CryptoAuxException("loadCertificateFromFile() exception: Failed to generate" +
                    "a certificate from file '" + pemFilePath + "'.",ce);
        }
        //PublicKey key = certificate.getPublicKey();
        return certificate;
    }
    public synchronized static String generateSignature(PrivateKey privateKey, String data) throws CryptoAuxException {
        byte[] signature = data.getBytes(Charset.forName("UTF-8"));
        Signature sigInfo;
        byte[] signatureBytes;
        String signatureString;

        try {
            sigInfo = Signature.getInstance("SHA1WithRSA");
            sigInfo.initSign(privateKey);
            sigInfo.update(signature);
            signatureBytes = sigInfo.sign();
        } catch (NoSuchAlgorithmException nsae) {
            throw new CryptoAuxException("generateSignature() exception: Could not find 'SHA1withRSA' algorithm in Signature class.", nsae);
        } catch (InvalidKeyException ike) {
            throw new CryptoAuxException("generateSignature() exception: The given public key is invalid " +
                    "(maybe your citizen card doesn't support 'SHA1withRSA' algorithm.", ike);
        } catch (SignatureException se) {
            throw new CryptoAuxException("generateSignature() exception: I wasn't able to update Signature object.", se);
        }
        signatureString = Base64.getEncoder().encodeToString(signatureBytes);
        return signatureString;
    }
    public synchronized static boolean verifySignature(X509Certificate cert, String receivedString, String b64encodedSignature) throws CryptoAuxException {
        Signature sigInfo;
        PublicKey publicKey = cert.getPublicKey();
        byte[] signatureBytes = Base64.getDecoder().decode(b64encodedSignature);
        boolean verifiedSignature;

        try {
            sigInfo = Signature.getInstance("SHA1withRSA");
            sigInfo.initVerify(publicKey);
            sigInfo.update(receivedString.getBytes(Charset.forName("UTF-8")));
            verifiedSignature = sigInfo.verify(signatureBytes);
        } catch (NoSuchAlgorithmException nsae) {
            throw new CryptoAuxException("verifySignature() exception: Could not find 'SHA1withRSA' algorithm in Signature class.", nsae);
        } catch (InvalidKeyException ike) {
            throw new CryptoAuxException("verifySignature() exception: The given public key is invalid " +
                    "(maybe your citizen card doesn't support 'SHA1withRSA' algorithm.", ike);
        } catch (SignatureException se) {
            throw new CryptoAuxException("verifySignature() exception: I wasn't able to update Signature object.", se);
        }

        return verifiedSignature;
    }
    public synchronized static String certificateToString(X509Certificate cert) throws CryptoAuxException {
        Base64.Encoder base64Encoder = Base64.getEncoder();
        byte[] derCert;
        try {
            derCert = cert.getEncoded();
        } catch (CertificateEncodingException cee) {
            throw new CryptoAuxException("certificateToString() exception: Invalid certificate encoding.", cee);
        }
        String pemCertPrefix = "-----BEGIN CERTIFICATE-----" + System.lineSeparator();
        String pemCertContent = new String(base64Encoder.encode(derCert));
        int pemCertContentLength = pemCertContent.length();
        StringBuilder stringBuilder = new StringBuilder();
        String substr = null;
        // Loop and append values.
        int i = 0;
        int diff = 0;
        int summer = 0;
        String linesep = System.lineSeparator();
        while (i < pemCertContentLength) {
            diff = pemCertContentLength-i;
            if (64%diff==0)
                summer = diff;
            else
                summer = 64;
            substr = pemCertContent.substring(i,i+summer);
            stringBuilder.append(substr);
            stringBuilder.append(linesep);
            i+=64;
        }
        String pemCertSuffix = "-----END CERTIFICATE-----" + System.lineSeparator();
        String pemCertString = pemCertPrefix + stringBuilder.toString() + pemCertSuffix;
        return pemCertString;
    }

    // unused
    public synchronized static X509Certificate stringToCertificate(String certString) throws CryptoAuxException {
        Base64.Decoder base64Decoder = Base64.getDecoder();
        X509Certificate certObject;
        byte[] decoded = base64Decoder.decode(certString);
        try {
            certObject = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
        } catch (CertificateException ce) {
            throw new CryptoAuxException("stringToCertificate() exception: Failed to generate certificate object from string.", ce);
        }
        return certObject;
    }
}
