package com.youssefhenna.dependent.kyverno;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.Map;

@Group("policies.kyverno.io")
@Version("v1")
public class NamespacedImageValidatingPolicy extends CustomResource<Map<String, Object>, Void> implements Namespaced {

    public static boolean isCrdAvailable(KubernetesClient client) {
        return client.getApiGroups().getGroups().stream()
            .anyMatch(g -> "policies.kyverno.io".equals(g.getName()));
    }
}