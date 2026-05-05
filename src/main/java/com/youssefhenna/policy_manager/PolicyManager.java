package com.youssefhenna.policy_manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.policy_manager.model.FileWithSignature;
import com.youssefhenna.policy_manager.model.GPGAuthorizationSignedDefinition;
import com.youssefhenna.policy_manager.model.SPOLSignedDefinition;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
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
import java.text.SimpleDateFormat;
import java.util.*;

public class PolicyManager {

    private static String lastClonedGitUrl = null;
    private static String lastClonedGitBranch = null;
    private static Path lastClonedRepoPath = null;

    public static void syncPolicies(PolicyUpstreamSpec upstream) {
        try {
            ensureLatestRepoContents(upstream.getGitUrl(), upstream.getBranch());

            ArrayList<FileWithSignature> spolFiles = findHighestVersionSPOLS();
            ArrayList<FileWithSignature> gpgKeyAuthorizationFiles = findGpgAuthorizationFiles();

            ArrayList<String> authorizedGpgKeys = extractAuthorizedGpgKeys(gpgKeyAuthorizationFiles, upstream.getGpgKeys(), upstream.getGitUrl());
            ArrayList<FileWithSignature> verifiedSpolFiles = filterVerifiedSPOLS(spolFiles, authorizedGpgKeys, upstream.getGitUrl());

            //TODO: List of verified SPOLS, proceed to CAS submission
            //TODO: tests to verify correct behavior, use pgp lib for generating keys, provide mocked local available repo

        } catch (Exception e) {
            throw new RuntimeException("Unknown error while syncing policies", e);
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

        for(FileWithSignature file: gpgKeyAuthorizationFiles){
            PGP.SignatureVerificationResult verificationResult = PGP.verifyTrustedFileSignature(file, rootTrustedGpgKeys);
            if(verificationResult.isVerified()){
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                GPGAuthorizationSignedDefinition gpgAuthDefinition = mapper.readValue(verificationResult.signedContent(), GPGAuthorizationSignedDefinition.class);

                if(!gpgAuthDefinition.getRepo().equals(gitUrl)){
                    continue;
                }

                Date notBefore = parseDate(gpgAuthDefinition.getValidity().getNotBefore());
                Date notAfter = parseDate(gpgAuthDefinition.getValidity().getNotAfter());

                Date now = new Date();
                boolean valid = now.after(notBefore) && now.before(notAfter);

                if(valid){
                    authorizedGpgKeys.addAll(gpgAuthDefinition.getSigners());
                }
            }
        }

        return authorizedGpgKeys;
    }

    private static ArrayList<FileWithSignature> filterVerifiedSPOLS(ArrayList<FileWithSignature> spols, ArrayList<String> gpgKeys, String gitUrl) throws PGPException, IOException, NoSuchAlgorithmException {
        ArrayList<FileWithSignature> verifiedSPOLS = new ArrayList<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for(FileWithSignature file: spols){
            PGP.SignatureVerificationResult verificationResult = PGP.verifyTrustedFileSignature(file, gpgKeys);
            if(verificationResult.isVerified()){
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                SPOLSignedDefinition spolSignedDefinition = mapper.readValue(verificationResult.signedContent(), SPOLSignedDefinition.class);

                if(!spolSignedDefinition.getRepo().equals(gitUrl)){
                    continue;
                }

                if(file.filePath() != null) {
                    if(!spolSignedDefinition.getFilename().equals(file.filePath().getFileName().toString())){
                        continue;
                    }

                    byte[] fileHash = digest.digest(Files.readAllBytes(file.filePath()));
                    String fileHashHex = HexFormat.of().formatHex(fileHash);

                    if(!spolSignedDefinition.getSha256().equals(fileHashHex)){
                        continue;
                    }

                    verifiedSPOLS.add(file);
                }
            }
        }

        return verifiedSPOLS;
    }


    private static Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateString);
    }


    public static void main(String[] args) {
        PolicyUpstreamSpec spec = new PolicyUpstreamSpec();
        spec.setBranch("main");
        spec.setGitUrl("https://github.com/YoussefHenna/scone-osv-scanner-policies");

        ArrayList<String> gpgKeys = new ArrayList<>();
        gpgKeys.add(" -----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "\n" +
            "        mQINBGn3kf0BEADRQ4LBRxjIsRQBG/p5t8JC3f/nj4w8RksnZc2NYxM7Zo7LV1j1\n" +
            "        Am+aFNubOyiqxzzKy/6NBZYJxRfKzgCNZNNqYFyEk6E/FoHuuKVY/qW2Gw2neEZP\n" +
            "        YInip9mRrupgJpYK3wj/vpkBfjy0RAFt/17v2EVzEcwijZceoPDwdZDhUQURiELE\n" +
            "        HTolLfd50tRtQe9alX3+ttW3z0JJMWo5MQchNFYDQQZoOWEGD7kkOBXRZcmXGGjD\n" +
            "        DMYNtHdk7vlZ9nmYu+zOKaK6V0Hl+NKcXO2embMmJaZRa6CyE7Q90fwzxhj5G08u\n" +
            "        rTEBzo1hL6gXx2l1HAoFb6/BPXbUCgTL5bK/Z3No9POv6gpM2jVuVp3iqXYFNhAH\n" +
            "        H/6KvIpNc5Sodc4jUfjgzYiGLFZmsts4Mr7WpObGGKdgQa7/4I2gOhjF1CGRqyzW\n" +
            "        j0TXggnlldBBatL+15H5yMPD1pfzyQhu/P4+LgSJ/utXwZxPxxMQMXvph1FyQ84U\n" +
            "        ucfV8HzKvdeXeR2LokFxjo954poGn1IeFau88eN6xw3Lvb7YIYUROVv03zUv0jvB\n" +
            "        bbjLJfQdumuRLU4ZUTPjF273jFGZ2clvq1MyG1VCP4XN1lRtVhsMrnyJoijnKUC8\n" +
            "        jMGsQVYld2m7PKUToBrRWFOIEqgQAYV9w8OAG8HYfWqrxKUyoduJD6203wARAQAB\n" +
            "        tFxZb3Vzc2VmIEhlbm5hIChPU1YgU2Nhbm5lciBQb2xpY2llcyBBdXRob3JpemVk\n" +
            "        IHNpZ25lciwgdGVzdGluZykgPHlvdXNzZWYuaGlzaGFtMTRAZ21haWwuY29tPokC\n" +
            "        VAQTAQoAPhYhBGOtIFNwDwT6RlT9g3evEYOVHfVkBQJp95H9AhsDBQkB4TOABQsJ\n" +
            "        CAcCBhUKCQgLAgQWAgMBAh4BAheAAAoJEHevEYOVHfVkmOsP/1GHPQHzhrZIVTiv\n" +
            "        DQDKIKml2TzwfFcKdvLnQxDFdI4REQablCmp/I6SgggpDcpJEus3qUTS1LTgh79F\n" +
            "        hh8lI+M0Lbv0VI6yoTlYiV+kFkq+EwxQa7xmcRzGfy7T4ZS/YBKLVhi8w2DAEfik\n" +
            "        sgsuNhx4xAgTQcloY44yfjviH+/FRW94RgjYUAOKEQYTLJII9Tn6lyc4cNRfzOxV\n" +
            "        TgnRjDuU0ycCrWZN//B9EokZGDI0o+zY0w8MCCo4iZRUgyqg6kejQEd0MiK5Zq4R\n" +
            "        gsGjsTCIS0fx0IaKS2qq9ysZSAaQUi0+LzDzHYL42hHto29q8xIL2jlQIdpBImsn\n" +
            "        V1XT2ZTI6HBRqRp59hBsACz1JT4Uqqs4fakTyG0zaVMsetKjEHt6JQ0ElXJcaPOi\n" +
            "        rALZSbkI7FZ6fWcNXnFdDG4nI3KdrYhU8lFoS8RtQka5gg3DK/fAVv4Ku3EpBGvz\n" +
            "        3cMIWa4WOKd21GOGUO+san0x2fHWdQtId3B1e7pSRjsYcN/s+M6i2BmqYAqaD82j\n" +
            "        Ik4+Ue5eTZp4mbQcfqzhhOKWXxmdh2vOXitI3Ke7nbF7+aYqgrmSpacT9UH5qZXb\n" +
            "        UpqNr41B8WEm/KX67LcDM5u1H6KvWFCipe7evfVmQLiu9Mv4p7oILfYh27V8RMd3\n" +
            "        jSppVCd+nZ/EFJskPEp9Ws4hRAJPuQINBGn3kf0BEADM/0XiqiKsBMoiUNbLw2CL\n" +
            "        GqdUG2nu8IGn5JleP0w478gc0mAtOisUuLfZX5e7sH5NDIdTKy8lRgMfOm0R078P\n" +
            "        SODOdUUlaUZ/KBh/E0XfgSohUTYChQ/DGGDZKyrbN+rMi2MuVH30lNixZBnLPLHV\n" +
            "        ayt7ynP05VgarrQR5f2tOg+PgflA8T7LOy5D3O7Zizl1OabRiJT2boZWBYmkzUcT\n" +
            "        myMLbrSYGc6AOyf+Q1JA2r/MNIimPn2aMPsfWyzd9C296HuTlLwDny6pc1qA1uko\n" +
            "        lbOkByXp8Drbl8Aocni/o7BuQu9qkP0wQKK4oRxr0gT79uoNkNZk2oqDQMKR7Ig0\n" +
            "        amUhjdabmhMYFZd/JASM0bC7c+8pnmVI0tBGPI/plE7epsEcrQmHBg770wssdgL8\n" +
            "        uRqOfYMM1RP+aRSGNRMGq/3ry9yQM9AJ8HTkEjKQF3JQFgB6cu7vFg0ezTFFD6Gm\n" +
            "        8gDn2y2V/01mkRtg1WmVTKuHBkHH74It4T0vRNMWWdIwtgCivMoQaTBq45dj6D5e\n" +
            "        KDWNSneNZB36jzDqPKB3IMRGq9O/LAGDQJ2txOEGAnIc12AVib61g4cuX1LniAQi\n" +
            "        MLGNQUJU84YP44pdIh3wUe400LGKN7+qmQ/OUoGZMe7sNEjXjRM5ocGa3PpsbYmL\n" +
            "        /Yb+JbVB+upGF/344pxA4QARAQABiQI8BBgBCgAmFiEEY60gU3APBPpGVP2Dd68R\n" +
            "        g5Ud9WQFAmn3kf0CGwwFCQHhM4AACgkQd68Rg5Ud9WSWBw//QMrlflNwJ3Ij+sHj\n" +
            "        q1H/6u1J7+aC8KJaNAKP0y4knxn7xE6J2WxeFW75wG1TTCscSeddJLxgQdwBkZFS\n" +
            "        LkOZRs8S2MXwH6/zLse2NHUAtQJL3a4UKY4lD6EnfQ976nUCp5niEuRrL2FV95GS\n" +
            "        FyH7w2pxAQ/ygyo71dYnUcdEjMrbai/Q1Szr4Td7xJ8fYmrqGha/Xv3QoDdz7tbg\n" +
            "        h/ebl20f488oQ7nm6Nrc6A2j59SA9OU/kDO2ltIThtOwLmsF2V/eyRxAv8kCFe1d\n" +
            "        VIlckJfGeD7BbqfJ7sA0CUeiFQ0apjj6UWHvPX3V3vq3Nwz0ZzYTG0KY1UTnbsjs\n" +
            "        APmFPS7B5IS469ghech8IKeOp3z1BW/dnKC0YOJ6v3vo7Uee4A/Cwn4qKQ9pSxyz\n" +
            "        3YZf1NRAQl1GwsSTj1oOQbtwGDDFg+r2/sqVn1R4laUbuJ6/XfTydu+bB84ByT2e\n" +
            "        +G1vxmhYIT2/BdEUU3pGphE60pzRBglB8w+6zc97yzyZGMTGZF8sA8QIfobepidk\n" +
            "        dpdVyhYxoA3Jobks/GxZ9iMjTVlRCGBxuHoxj2e8e8yrCIUa+8Um/dSA6QJ+Qieo\n" +
            "        GfV2x6oeKHkMLo0zMYEDtK1ThMyOwtnlVPQ4sVv0c0XgbrONrWqvrugbk0mUMxNH\n" +
            "        n/JPwE6bObP7aHAQr6e5a1Vxk+8=\n" +
            "        =ilcr\n" +
            "        -----END PGP PUBLIC KEY BLOCK-----");
        spec.setGpgKeys(gpgKeys);

        PollConfig config = new PollConfig();
        config.setEvery(10);
        config.setUnit(PollConfig.Unit.MINUTES);
        spec.setPoll(config);

        syncPolicies(spec);
    }

}
