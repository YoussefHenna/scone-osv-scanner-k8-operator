package com.youssefhenna.policy_manager.model;

import io.fabric8.generator.annotation.Required;

import java.util.ArrayList;

public class GPGAuthorizationSignedDefinition {

    @Required
    private String repo;

    @Required
    private ArrayList<String> signers;

    @Required
    private Validity validity;

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public ArrayList<String> getSigners() {
        return signers;
    }

    public void setSigners(ArrayList<String> signers) {
        this.signers = signers;
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    public static class Validity {
        @Required
        private String notBefore;

        @Required
        private String notAfter;

        public String getNotBefore() {
            return notBefore;
        }

        public void setNotBefore(String notBefore) {
            this.notBefore = notBefore;
        }

        public String getNotAfter() {
            return notAfter;
        }

        public void setNotAfter(String notAfter) {
            this.notAfter = notAfter;
        }
    }
}
