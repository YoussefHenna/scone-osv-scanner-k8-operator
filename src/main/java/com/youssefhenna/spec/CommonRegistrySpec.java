package com.youssefhenna.spec;

import com.youssefhenna.model.RegistryCredentials;
import org.jspecify.annotations.Nullable;

public class CommonRegistrySpec {
    private String registryUrl;

    private RegistryCredentials registryCredentials;

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public RegistryCredentials getRegistryCredentials() {
        return registryCredentials;
    }

    public void setRegistryCredentials(RegistryCredentials registryCredentials) {
        this.registryCredentials = registryCredentials;
    }


    // resolves the innermost registry spec that is defined
    @Nullable
    public CommonRegistrySpec resolveRegistrySpec(CommonRegistrySpec... parents) {
        if (this.registryUrl != null && this.registryCredentials != null) return this;
        for (CommonRegistrySpec parent : parents) {
            if (parent != null && parent.registryUrl != null && parent.registryCredentials != null) return parent;
        }
        return null;
    }


}
