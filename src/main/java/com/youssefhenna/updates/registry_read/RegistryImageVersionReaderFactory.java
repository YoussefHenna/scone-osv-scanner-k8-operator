package com.youssefhenna.updates.registry_read;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface RegistryImageVersionReaderFactory {
    RegistryImageVersionReader create(KubernetesClient client, String namespace);
}