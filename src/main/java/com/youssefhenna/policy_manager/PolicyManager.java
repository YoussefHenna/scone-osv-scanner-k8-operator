package com.youssefhenna.policy_manager;

import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PolicyManager {

    private static String lastClonedGitUrl = null;
    private static String lastClonedGitBranch = null;
    private static Path lastClonedRepoPath = null;

    public static void syncPolicies(PolicyUpstreamSpec upstream) {
        try {
            ensureLatestRepoContents(upstream.getGitUrl(), upstream.getBranch());
            //TODO actual logic, now that we have up to date repo contents

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

}
