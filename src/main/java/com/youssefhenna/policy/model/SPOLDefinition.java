package com.youssefhenna.policy.model;

import java.util.ArrayList;

public class SPOLDefinition {
    private String session;

    private ArrayList<SessionSigner> signatures;

    public String getSession() { return session; }

    public void setSession(String session) { this.session = session; }

    public ArrayList<SessionSigner> getSignatures() { return signatures; }

    public void setSignatures(ArrayList<SessionSigner> signatures) { this.signatures = signatures; }
}
