package com.youssefhenna.integration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// Runs a testing local cluster using k3s and test containers
public class K3sTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName K3S_IMAGE = DockerImageName.parse("rancher/k3s:v1.32.2-k3s1");

    private K3sContainer k3s;
    private Path kubeconfigFile;

    @Override
    public Map<String, String> start() {
        k3s = new K3sContainer(K3S_IMAGE);
        k3s.start();

        try {
            kubeconfigFile = Files.createTempFile("k3s-kubeconfig", ".yaml");
            Files.writeString(kubeconfigFile, k3s.getKubeConfigYaml());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write K3s kubeconfig", e);
        }

        System.setProperty("kubeconfig", kubeconfigFile.toAbsolutePath().toString());
        return Map.of();
    }

    @Override
    public void stop() {
        System.clearProperty("kubeconfig");
        if (kubeconfigFile != null) {
            try { Files.deleteIfExists(kubeconfigFile); } catch (IOException ignored) {}
        }
        if (k3s != null) {
            k3s.stop();
        }
    }
}