package com.youssefhenna.policy_manager;

import com.youssefhenna.policy_manager.model.FileWithSignature;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.api.OpenPGPCertificate;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.PGPainless;
import org.pgpainless.decryption_verification.ConsumerOptions;
import org.pgpainless.decryption_verification.DecryptionStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class PGP {
    public record SignatureVerificationResult(String signedContent, boolean isVerified) {
    }

    public static SignatureVerificationResult verifyTrustedFileSignature(FileWithSignature file, ArrayList<String> trustedGpgKeys) throws IOException, PGPException {
        PGPainless pgp = PGPainless.getInstance();
        ByteArrayOutputStream signedContentOutputStream = new ByteArrayOutputStream();

        ArrayList<OpenPGPCertificate> keysAsCertificates = new ArrayList<>();
        for (String key : trustedGpgKeys) {
            keysAsCertificates.add(pgp.readKey().parseCertificate(key.strip()));
        }

        boolean isVerified;

        try (InputStream signatureFileInputStream = Files.newInputStream(file.signatureFilePath())) {
            DecryptionStream consumerStream = pgp.processMessage()
                .onInputStream(signatureFileInputStream)
                .withOptions(ConsumerOptions.get().addVerificationCerts(keysAsCertificates));

            Streams.pipeAll(consumerStream, signedContentOutputStream);
            consumerStream.close();
            isVerified = consumerStream.getMetadata().isVerifiedSigned();
        }

        return new SignatureVerificationResult(signedContentOutputStream.toString(StandardCharsets.UTF_8), isVerified);
    }
}
