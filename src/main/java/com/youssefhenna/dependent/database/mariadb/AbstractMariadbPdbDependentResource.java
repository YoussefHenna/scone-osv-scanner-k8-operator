package com.youssefhenna.dependent.database.mariadb;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetBuilder;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

public abstract class AbstractMariadbPdbDependentResource extends CRUDKubernetesDependentResource<PodDisruptionBudget, SconeOsvScanner> {

    protected AbstractMariadbPdbDependentResource() {
        super(PodDisruptionBudget.class);
    }

    protected abstract String getStatefulSetName(String primaryName);

    @Override
    protected PodDisruptionBudget desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String primaryName = primary.getMetadata().getName();
        String statefulSetName = getStatefulSetName(primaryName);
        String name = statefulSetName + "-pdb";
        String namespace = primary.getMetadata().getNamespace();

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