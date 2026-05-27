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
    private static final String KYVERNO_INSTALL_URL = "https://github.com/kyverno/kyverno/releases/download/v1.16.2/install.yaml";
    private static final int KYVERNO_WAIT_TIMEOUT_MS = 60_000;
    private static final int KYVERNO_POLL_INTERVAL_MS = 2_000;

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

        System.setProperty("kubeconfig", kubeconfigFile.toAbsolutePath().toString());
        return Map.of();
    }

    private void installKyverno(String kubeConfigYaml) {
        Config config = Config.fromKubeconfig(kubeConfigYaml);
        try (KubernetesClient tempClient = new KubernetesClientBuilder().withConfig(config).build();
             InputStream manifest = new URL(KYVERNO_INSTALL_URL).openStream()) {
            tempClient.load(manifest).serverSideApply();
            waitForKyvernoCrd(tempClient);
        } catch (IOException e) {
            throw new RuntimeException("Failed to install Kyverno", e);
        }
    }

    private void waitForKyvernoCrd(KubernetesClient client) {
        long deadline = System.currentTimeMillis() + KYVERNO_WAIT_TIMEOUT_MS;
        while (!isKyvernoCrdAvailable(client)) {
            if (System.currentTimeMillis() > deadline) {
                throw new RuntimeException("Timed out waiting for Kyverno CRD (policies.kyverno.io)");
            }
            try {
                Thread.sleep(KYVERNO_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for Kyverno CRD", e);
            }
        }
    }

    private boolean isKyvernoCrdAvailable(KubernetesClient client) {
        return client.getApiGroups().getGroups().stream()
            .anyMatch(g -> "policies.kyverno.io".equals(g.getName()));
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