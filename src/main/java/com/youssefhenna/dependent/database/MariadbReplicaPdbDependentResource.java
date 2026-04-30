package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetBuilder;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetSpecBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbReplicaPdbDependentResource extends CRUDKubernetesDependentResource<PodDisruptionBudget, SconeOsvScanner> {

    public MariadbReplicaPdbDependentResource() {
        super(PodDisruptionBudget.class);
    }

    @Override
    protected PodDisruptionBudget desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String primaryName = primary.getMetadata().getName();
        String name = Constants.getMariadbReplicaName(primaryName) + "-pdb";
        String namespace = primary.getMetadata().getNamespace();
        String statefulSetName = Constants.getMariadbReplicaName(primaryName);

        return new PodDisruptionBudgetBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(new PodDisruptionBudgetSpecBuilder()
                .withMinAvailable(new IntOrString(1))
                .withSelector(new LabelSelectorBuilder()
                    .addToMatchLabels("app", statefulSetName)
                    .build())
                .build())
            .build();
    }
}