package com.youssefhenna.dependent.database;

import com.youssefhenna.dependent.database.mariadb.AbstractMariadbPdbDependentResource;
import com.youssefhenna.utils.Constants;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbPrimaryPdbDependentResource extends AbstractMariadbPdbDependentResource {

    @Override
    protected String getStatefulSetName(String primaryName) {
        return Constants.getMariadbPrimaryName(primaryName);
    }
}