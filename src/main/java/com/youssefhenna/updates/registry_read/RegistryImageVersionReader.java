package com.youssefhenna.updates.registry_read;

import com.youssefhenna.spec.CommonRegistrySpec;

import java.util.List;

public interface RegistryImageVersionReader {
    List<String> getAvailableVersions(CommonRegistrySpec registrySpec, String imageName) throws Exception;
}
