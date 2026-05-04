package com.youssefhenna.spec.policy;

import com.youssefhenna.model.PollConfig;
import io.fabric8.generator.annotation.Required;

import java.util.ArrayList;

public class PolicyUpstreamSpec {

    @Required
    private String gitUrl;

    @Required
    private String branch;

    @Required
    private ArrayList<String> gpgKeys;

    @Required
    private PollConfig poll;

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public ArrayList<String> getGpgKeys() {
        return gpgKeys;
    }

    public void setGpgKeys(ArrayList<String> gpgKeys) {
        this.gpgKeys = gpgKeys;
    }

    public PollConfig getPoll() {
        return poll;
    }

    public void setPoll(PollConfig poll) {
        this.poll = poll;
    }
}
