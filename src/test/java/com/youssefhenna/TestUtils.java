package com.youssefhenna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youssefhenna.utils.Common;
import org.bouncycastle.openpgp.api.OpenPGPKey;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.PGPainless;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.encryption_signing.SigningOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public class TestUtils {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static byte[] signInline(OpenPGPKey key, String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream plaintext = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        EncryptionStream stream = PGPainless.getInstance().generateMessage()
            .onOutputStream(output)
            .withOptions(ProducerOptions.sign(
                SigningOptions.get().addSignature(SecretKeyRingProtector.unprotectedKeys(), key)
            ));

        Streams.pipeAll(plaintext, stream);
        stream.close();
        return output.toByteArray();
    }

    public static String buildGpgAuthYaml(String repo, String notBefore, String notAfter, List<String> signers) {
        StringBuilder sb = new StringBuilder();
        sb.append("repo: \"").append(repo).append("\"\n");
        sb.append("validity:\n");
        sb.append("  notBefore: \"").append(notBefore).append("\"\n");
        sb.append("  notAfter: \"").append(notAfter).append("\"\n");
        sb.append("signers:\n");
        for (String signer : signers) {
            sb.append("  - |-\n");
            for (String line : signer.split("\n")) {
                sb.append("    ").append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static String buildSpolSignedYaml(String filename, String sha256, String repo) {
        return "filename: \"" + filename + "\"\n"
            + "sha256: \"" + sha256 + "\"\n"
            + "date: \"" + Common.dateToString(new Date()) + "\"\n"
            + "repo: \"" + repo + "\"\n";
    }

    public static byte[] buildSpolJson(String sessionName) throws Exception {
        String session = "name: " + sessionName + "\nversion: \"0.3\"\n";
        Map<String, Object> spol = new LinkedHashMap<>();
        spol.put("session", session);
        spol.put("signatures", List.of());
        return jsonMapper.writeValueAsBytes(spol);
    }

    public static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    public static String pastDate() {
        return pastDate(1);
    }

    public static String pastDate(int daysAgo) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return Common.dateToString(cal.getTime());
    }

    public static String futureDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.add(Calendar.DAY_OF_MONTH, 365);
        return Common.dateToString(cal.getTime());
    }

    public static void deleteDirectory(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> p.toFile().delete());
    }
}