package com.youssefhenna.policy.model.http;

import java.util.ArrayList;

public class UploadSessionResponse {
    private String hash;

    private ArrayList<ArrayList<String>> warnings;

    public String getHash() { return hash; }

    public void setHash(String hash) { this.hash = hash; }

    public ArrayList<ArrayList<String>> getWarnings() {
        return warnings;
    }

    public void setWarnings(ArrayList<ArrayList<String>> warnings) {
        this.warnings = warnings;
    }
}
