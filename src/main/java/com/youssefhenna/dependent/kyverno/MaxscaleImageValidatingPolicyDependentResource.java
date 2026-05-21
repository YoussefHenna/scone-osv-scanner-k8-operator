package com.youssefhenna.dependent.kyverno;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonDependantSpec;
import com.youssefhenna.spec.CommonRegistrySpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.utils.Constants;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(informer = @Informer(name = Constants.IMAGE_VALIDATING_POLICY_INFORMER, labelSelector = Constants.DEPENDANT_SELECTOR))
public class MaxscaleImageValidatingPolicyDependentResource extends AbstractImageValidatingPolicyDependentResource {

    @Override
    protected CommonDependantSpec getDependantSpec(SconeOsvScanner primary) {
        return primary.getSpec().getDatabase().getMaxscale();
    }

    @Override
    protected String getPolicyName(String primaryName) {
        return Constants.getMaxscaleImageValidatingPolicyName(primaryName);
    }

    @Override
    protected CommonRegistrySpec resolveRegistrySpec(SconeOsvScanner primary) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        DatabaseSpec databaseSpec = primarySpec.getDatabase();
        return databaseSpec.getMaxscale().resolveRegistrySpec(databaseSpec, primarySpec);
    }

    public static class Condition extends CosignKeyPresentCondition {
        @Override
        protected CommonDependantSpec getDependantSpec(SconeOsvScanner primary) {
            return primary.getSpec().getDatabase().getMaxscale();
        }
    }
}
