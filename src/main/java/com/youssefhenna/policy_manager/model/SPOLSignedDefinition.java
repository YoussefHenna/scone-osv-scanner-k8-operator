package com.youssefhenna.policy_manager.model;

import io.fabric8.generator.annotation.Required;

public class SPOLSignedDefinition {
    @Required
    private String filename;

    @Required
    private String sha256;

    @Required
    private String date;

    @Required
    private String repo;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }
}
