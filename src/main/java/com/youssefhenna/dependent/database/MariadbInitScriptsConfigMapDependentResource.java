package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbInitScriptsConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, SconeOsvScanner> {

    private static final String SCRIPTS_PATH = "scripts/mariadb/";

    public MariadbInitScriptsConfigMapDependentResource() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String name = Constants.getMariadbInitScriptsConfigMapName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();

        return new ConfigMapBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withData(Map.of(
                "primary-entrypoint.sh", loadScript("primary-entrypoint.sh"),
                "replica-entrypoint.sh", loadScript("replica-entrypoint.sh"),
                "replica-setup.sh", loadScript("replica-setup.sh")
            ))
            .build();
    }

    private static String loadScript(String name) {
        try (InputStream stream = MariadbInitScriptsConfigMapDependentResource.class
            .getClassLoader()
            .getResourceAsStream(SCRIPTS_PATH + name)) {
            if (stream == null) {
                throw new IllegalStateException("Script not found on classpath: " + SCRIPTS_PATH + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
