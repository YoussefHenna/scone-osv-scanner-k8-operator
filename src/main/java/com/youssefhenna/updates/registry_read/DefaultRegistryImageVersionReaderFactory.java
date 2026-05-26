package com.youssefhenna.updates.registry_read;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultRegistryImageVersionReaderFactory implements RegistryImageVersionReaderFactory {

    @Override
    public RegistryImageVersionReader create(KubernetesClient client, String namespace) {
        return new RegistryImageVersionReaderImpl(client, namespace);
    }
}