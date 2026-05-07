package com.youssefhenna.policy.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// sessions have many other fields, only these are needed to be parsed
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionContents {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
