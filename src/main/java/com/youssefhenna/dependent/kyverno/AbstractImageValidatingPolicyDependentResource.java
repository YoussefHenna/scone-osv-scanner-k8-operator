package com.youssefhenna.dependent.kyverno;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonDependantSpec;
import com.youssefhenna.spec.CommonRegistrySpec;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import java.util.List;
import java.util.Map;

// kyverno image validating policy using cosign validation
// based on https://kyverno.io/policies/other/verify-image-ivpol/verify-image-ivpol/
public abstract class AbstractImageValidatingPolicyDependentResource
    extends CRUDKubernetesDependentResource<NamespacedImageValidatingPolicy, SconeOsvScanner> {

    private static final String ATTESTOR_NAME = "cosign";
    private static final String VALIDATION_EXPRESSION =
        "images.containers.map(image, verifyImageSignatures(image, [attestors." + ATTESTOR_NAME + "])).all(e, e > 0)";

    protected AbstractImageValidatingPolicyDependentResource() {
        super(NamespacedImageValidatingPolicy.class);
    }

    protected abstract CommonDependantSpec getDependantSpec(SconeOsvScanner primary);

    protected abstract String getPolicyName(String primaryName);

    protected abstract CommonRegistrySpec resolveRegistrySpec(SconeOsvScanner primary);

    @Override
    protected NamespacedImageValidatingPolicy desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        CommonDependantSpec spec = getDependantSpec(primary);
        CommonRegistrySpec registrySpec = resolveRegistrySpec(primary);

        if (registrySpec == null) {
            throw new RuntimeException("Cannot resolve image registry for cosign policy: " + getPolicyName(primary.getMetadata().getName()));
        }

        String policyName = getPolicyName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();
        String imageGlob = Common.buildImage(registrySpec.getRegistryUrl(), spec.getImageName(), "*");

        NamespacedImageValidatingPolicy policy = new NamespacedImageValidatingPolicy();
        policy.getMetadata().setName(policyName);
        policy.getMetadata().setNamespace(namespace);
        policy.getMetadata().setLabels(Map.of(
            Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE
        ));
        policy.setSpec(Map.of(
            "webhookConfiguration", Map.of("timeoutSeconds", 30),
            "evaluation", Map.of("background", Map.of("enabled", false)),
            "validationActions", List.of("Deny"),
            "matchConstraints", Map.of(
                "resourceRules", List.of(Map.of(
                    "apiGroups", List.of(""),
                    "apiVersions", List.of("v1"),
                    "operations", List.of("CREATE", "UPDATE"),
                    "resources", List.of("pods")
                ))
            ),
            "matchImageReferences", List.of(Map.of("glob", imageGlob)),
            "attestors", List.of(Map.of(
                "name", ATTESTOR_NAME,
                "cosign", Map.of("key", Map.of("data", spec.getCosignPublicKey()))
            )),
            "validations", List.of(Map.of(
                "expression", VALIDATION_EXPRESSION,
                "message", "failed image signature verification"
            ))
        ));

        return policy;
    }

    // Only create validating policy when cosign public key defined
    public abstract static class CosignKeyPresentCondition implements Condition<NamespacedImageValidatingPolicy, SconeOsvScanner> {

        protected abstract CommonDependantSpec getDependantSpec(SconeOsvScanner primary);

        @Override
        public boolean isMet(DependentResource<NamespacedImageValidatingPolicy, SconeOsvScanner> dr,
                             SconeOsvScanner primary,
                             Context<SconeOsvScanner> context) {
            return getDependantSpec(primary).getCosignPublicKey() != null;
        }
    }
}