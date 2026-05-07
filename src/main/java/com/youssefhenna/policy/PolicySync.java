package com.youssefhenna.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.policy.cas.CASClient;
import com.youssefhenna.policy.model.*;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.status.PolicyUpdateRunStatus;
import com.youssefhenna.status.PolicyUploadStatusItem;
import com.youssefhenna.utils.Common;
import org.bouncycastle.openpgp.PGPException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;
import io.quarkus.logging.Log;


public class PolicySync {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    private static String lastClonedGitUrl = null;
    private static String lastClonedGitBranch = null;
    private static Path lastClonedRepoPath = null;

    public record SyncPoliciesResult(PolicyUpdateRunStatus overallStatus, ArrayList<PolicyUploadStatusItem> statuses){}

    public static SyncPoliciesResult syncPolicies(PolicyUpstreamSpec upstream, CASClient casClient) {
        try {
            ensureLatestRepoContents(upstream.getGitUrl(), upstream.getBranch());

            ArrayList<FileWithSignature> spolFiles = findHighestVersionSPOLS();
            ArrayList<FileWithSignature> gpgKeyAuthorizationFiles = findGpgAuthorizationFiles();

            ArrayList<String> authorizedGpgKeys = extractAuthorizedGpgKeys(gpgKeyAuthorizationFiles, upstream.getGpgKeys(), upstream.getGitUrl());
            ArrayList<FileWithSignature> verifiedSpolFiles = filterVerifiedSPOLS(spolFiles, authorizedGpgKeys, upstream.getGitUrl());

            //TODO: tests to verify correct behavior, use pgp lib for generating keys, provide mocked local available repo
            //TODO: add logs with  io.quarkus.logging.Log
            ArrayList<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, verifiedSpolFiles);
            return new SyncPoliciesResult(PolicyUpdateRunStatus.SUCCESSFUL, statuses);
        } catch (Exception e) {
            Log.error("Error while syncing policies", e);
            return new SyncPoliciesResult(PolicyUpdateRunStatus.FAILED, new ArrayList<>());
        }
    }


    private static void ensureLatestRepoContents(String gitUrl, String gitBranch) throws IOException, GitAPIException {
        if (lastClonedRepoPath != null && lastClonedRepoPath.toFile().exists() && lastClonedGitUrl.equals(gitUrl) && lastClonedGitBranch.equals(gitBranch)) {
            try (Git git = Git.open(lastClonedRepoPath.toFile())) {
                git.fetch().setRefSpecs("refs/heads/" + gitBranch + ":refs/remotes/origin/" + gitBranch).call();
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + gitBranch).call();
            }
        } else {
            Path clonePath = Files.createTempDirectory("policies");

            CloneCommand command = Git.cloneRepository()
                .setURI(gitUrl)
                .setBranch(gitBranch)
                .setDirectory(clonePath.toFile());

            try (Git _git = command.call()) {
                lastClonedRepoPath = clonePath;
                lastClonedGitUrl = gitUrl;
                lastClonedGitBranch = gitBranch;
            }
        }
    }

    private static ArrayList<FileWithSignature> findHighestVersionSPOLS() throws IOException {
        if (lastClonedRepoPath == null) {
            return new ArrayList<>();
        }

        Map<String, Integer> highestVersions = new HashMap<>();
        Map<String, FileWithSignature> highestSPOLS = new HashMap<>();

        Files.walkFileTree(lastClonedRepoPath, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();

                // files in the format <version>-<file-name>.json, ex: 0-osv-policy.json, 1-osv-policy.json, etc.
                int dashIndex = fileName.indexOf('-');
                if (dashIndex > 0 && fileName.endsWith(".json")) {
                    try {
                        int version = Integer.parseInt(fileName.substring(0, dashIndex));
                        String baseName = fileName.substring(dashIndex + 1);
                        Path signatureFile = file.resolveSibling(fileName + ".asc");
                        if (!Files.exists(signatureFile)) {
                            Log.warn("No signature file found for '" +fileName+ "', skipping...");
                            return FileVisitResult.CONTINUE;
                        }
                        Integer currentHighest = highestVersions.get(baseName);
                        if (currentHighest == null || version > currentHighest) {
                            highestVersions.put(baseName, version);
                            highestSPOLS.put(baseName, new FileWithSignature(baseName, file, signatureFile));
                        }
                    } catch (NumberFormatException ignored) {
                        // skip file if it does not start with version number
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return new ArrayList<>(highestSPOLS.values());
    }

    private static ArrayList<FileWithSignature> findGpgAuthorizationFiles() throws IOException {
        if (lastClonedRepoPath == null) {
            return new ArrayList<>();
        }

        ArrayList<FileWithSignature> authorizationFiles = new ArrayList<>();

        Files.walkFileTree(lastClonedRepoPath, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();

                // files in the format gpg-key-authorization-<version>.yml/yaml.asc, ex: gpg-key-authorization-0.yml.asc
                if (fileName.startsWith("gpg-key-authorization") && (fileName.endsWith(".yml.asc") || fileName.endsWith(".yaml.asc"))) {
                    authorizationFiles.add(new FileWithSignature(fileName, null /* contents in signed file, no separate file */, file));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return authorizationFiles;
    }

    private static ArrayList<String> extractAuthorizedGpgKeys(ArrayList<FileWithSignature> gpgKeyAuthorizationFiles, ArrayList<String> rootTrustedGpgKeys, String gitUrl) throws PGPException, IOException, ParseException {
        ArrayList<String> authorizedGpgKeys = new ArrayList<>();

        for (FileWithSignature file : gpgKeyAuthorizationFiles) {
            PGP.SignatureVerificationResult verificationResult = PGP.verifyTrustedFileSignature(file, rootTrustedGpgKeys);
            if (verificationResult.isVerified()) {
                GPGAuthorizationSignedDefinition gpgAuthDefinition = yamlMapper.readValue(verificationResult.signedContent(), GPGAuthorizationSignedDefinition.class);

                if (!gpgAuthDefinition.getRepo().equals(gitUrl)) {
                    continue;
                }

                Date notBefore = Common.parseDate(gpgAuthDefinition.getValidity().getNotBefore());
                Date notAfter = Common.parseDate(gpgAuthDefinition.getValidity().getNotAfter());

                Date now = new Date();
                boolean valid = now.after(notBefore) && now.before(notAfter);

                if (valid) {
                    authorizedGpgKeys.addAll(gpgAuthDefinition.getSigners());
                }
            } else {
                Log.warn("GPG file '"+file.baseName()+"' has an invalid signature, skipping...");
            }
        }

        if(authorizedGpgKeys.isEmpty()){
            Log.warn("No authorized GPG keys found in git repo that match given root GPG keys, no policies will be updated");
        }
        return authorizedGpgKeys;
    }

    private static ArrayList<FileWithSignature> filterVerifiedSPOLS(ArrayList<FileWithSignature> spols, ArrayList<String> gpgKeys, String gitUrl) throws PGPException, IOException, NoSuchAlgorithmException {
        ArrayList<FileWithSignature> verifiedSPOLS = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (FileWithSignature file : spols) {
            PGP.SignatureVerificationResult verificationResult = PGP.verifyTrustedFileSignature(file, gpgKeys);
            if (verificationResult.isVerified()) {
                SPOLSignedDefinition spolSignedDefinition = yamlMapper.readValue(verificationResult.signedContent(), SPOLSignedDefinition.class);

                if (!spolSignedDefinition.getRepo().equals(gitUrl)) {
                    continue;
                }

                if (file.filePath() != null) {
                    if (!spolSignedDefinition.getFilename().equals(file.filePath().getFileName().toString())) {
                        continue;
                    }

                    byte[] fileHash = digest.digest(Files.readAllBytes(file.filePath()));
                    String fileHashHex = HexFormat.of().formatHex(fileHash);

                    if (!spolSignedDefinition.getSha256().equals(fileHashHex)) {
                        continue;
                    }

                    verifiedSPOLS.add(file);
                }
            }
        }

        return verifiedSPOLS;
    }
}
