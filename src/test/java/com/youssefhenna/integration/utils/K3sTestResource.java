package com.youssefhenna.integration.utils;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// Runs a testing local cluster using k3s and test containers
public class K3sTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName K3S_IMAGE = DockerImageName.parse("rancher/k3s:v1.32.2-k3s1");
    private static final String KYVERNO_INSTALL_URL = "https://github.com/kyverno/kyverno/releases/download/v1.18.1/install.yaml";

    private K3sContainer k3s;
    private Path kubeconfigFile;

    @Override
    public Map<String, String> start() {
        k3s = new K3sContainer(K3S_IMAGE);
        k3s.start();

        String kubeConfigYaml = k3s.getKubeConfigYaml();
        try {
            kubeconfigFile = Files.createTempFile("k3s-kubeconfig", ".yaml");
            Files.writeString(kubeconfigFile, kubeConfigYaml);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write K3s kubeconfig", e);
        }

        installKyverno(kubeConfigYaml);

        String kubeconfigPath = kubeconfigFile.toAbsolutePath().toString();
        System.setProperty("kubeconfig", kubeconfigPath);
        return Map.of("quarkus.kubernetes-client.kubeconfig", kubeconfigPath);
    }

    private void installKyverno(String kubeConfigYaml) {
        Config config = Config.fromKubeconfig(kubeConfigYaml);
        try (KubernetesClient tempClient = new KubernetesClientBuilder().withConfig(config).build();
             InputStream manifest = new URL(KYVERNO_INSTALL_URL).openStream()) {
            tempClient.load(manifest).serverSideApply();
        } catch (IOException e) {
            throw new RuntimeException("Failed to install Kyverno", e);
        }
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