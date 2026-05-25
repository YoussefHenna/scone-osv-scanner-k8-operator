package com.youssefhenna.updates.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerConfigJson {
    private Map<String, DockerRegistryAuth> auths;

    public Map<String, DockerRegistryAuth> getAuths() {
        return auths;
    }

    public void setAuths(Map<String, DockerRegistryAuth> auths) {
        this.auths = auths;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DockerRegistryAuth {
        private String username;
        private String password;
        private String auth;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}