package com.youssefhenna.integration.utils;

import com.youssefhenna.updates.registry_read.RegistryImageVersionReader;
import com.youssefhenna.updates.registry_read.RegistryImageVersionReaderFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestRegistryImageVersionReaderFactory implements RegistryImageVersionReaderFactory {

    private volatile RegistryImageVersionReader reader = (spec, imageName) -> List.of();

    public void setReader(RegistryImageVersionReader reader) {
        this.reader = reader;
    }

    public void reset() {
        this.reader = (spec, imageName) -> List.of();
    }

    @Override
    public RegistryImageVersionReader create(KubernetesClient client, String namespace) {
        return reader;
    }
}