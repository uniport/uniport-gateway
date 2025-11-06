package ch.uniport.gateway.core.entrypoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class CertGenerator {
    public static class PemFiles {
        public final File certFile;
        public final File keyFile;

        public PemFiles(File certFile, File keyFile) {
            this.certFile = certFile;
            this.keyFile = keyFile;
        }
    }

    public static PemFiles generateTempPemFiles(String commonName) throws Exception {
        // 1. Add Bouncy Castle Provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 2. Generate Key Pair
        KeyPair keyPair = generateKeyPair();

        // 3. Generate Self-Signed Certificate
        Certificate selfCert = generateSelfSignedCertificate(keyPair, String.format("CN=%s", commonName));

        // 4. Write Private Key to .pem file
        File keyFile = File.createTempFile("temp-private-", ".pem");
        keyFile.deleteOnExit();
        writePemFile(keyFile, "PRIVATE KEY", keyPair.getPrivate().getEncoded());

        // 5. Write Certificate to .pem file
        File certFile = File.createTempFile("temp-cert-", ".pem");
        certFile.deleteOnExit();
        writePemFile(certFile, "CERTIFICATE", selfCert.getEncoded());

        System.out.println("Generated PEM Certificate at: " + certFile.getAbsolutePath());
        System.out.println("Generated PEM Private Key at: " + keyFile.getAbsolutePath());

        return new PemFiles(certFile, keyFile);
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    private static Certificate generateSelfSignedCertificate(KeyPair keyPair, String commonName)
        throws OperatorCreationException, CertificateException, IOException {

        Date notBefore = new Date(System.currentTimeMillis() - 86400000); // Yesterday
        Date notAfter = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)); // 1 year validity

        X500Name dnName = new X500Name(commonName);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider("BC")
            .build(keyPair.getPrivate());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            dnName, serial, notBefore, notAfter, dnName, keyPair.getPublic());

        return new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certBuilder.build(signer));
    }

    private static void writePemFile(File file, String type, byte[] content)
        throws IOException {

        try (PemWriter pemWriter = new PemWriter(new FileWriter(file))) {
            pemWriter.writeObject(new PemObject(type, content));
        }
    }
}