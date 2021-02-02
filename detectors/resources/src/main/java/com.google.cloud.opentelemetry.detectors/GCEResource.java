package com.google.cloud.opentelemetry.detectors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.opentelemetry.sdk.resources.ResourceProvider;

import java.util.Arrays;

public final class GCEResource extends ResourceProvider {
    @Override
    public Attributes getAttributes() {
        if (!GCPMetadataConfig.isRunningOnGcp()) {
            return Attributes.empty();
        }

        AttributesBuilder attrBuilders = Attributes.builder();
        attrBuilders.put(SemanticAttributes.CLOUD_PROVIDER, "gcp");

        String projectId = GCPMetadataConfig.getProjectId();
        if (!projectId.isEmpty()) {
            attrBuilders.put(SemanticAttributes.CLOUD_ACCOUNT_ID, projectId);
        }

        String zone = GCPMetadataConfig.getZone();
        if (!zone.isEmpty()) {
            attrBuilders.put(SemanticAttributes.CLOUD_ZONE, zone);

            String[] splitArr = zone.split("-");
            if (splitArr.length > 2) {
                attrBuilders.put(SemanticAttributes.CLOUD_REGION, String.join("-", Arrays.copyOfRange(splitArr, 0, 2)));
            }
        }

        String instanceId = GCPMetadataConfig.getInstanceId();
        if (!instanceId.isEmpty()) {
            attrBuilders.put(SemanticAttributes.HOST_ID, instanceId);
        }

        String instanceName = GCPMetadataConfig.getInstanceName();
        if (!instanceName.isEmpty()) {
            attrBuilders.put(SemanticAttributes.HOST_NAME, instanceName);
        }

        String hostName = GCPMetadataConfig.getInstanceHostname();
        if (!hostName.isEmpty()) {
            attrBuilders.put(SemanticAttributes.HOST_NAME, hostName);
        }

        String hostType = GCPMetadataConfig.getMachineType();
        if (!hostType.isEmpty()) {
            attrBuilders.put(SemanticAttributes.HOST_TYPE, hostType);
        }

        Attributes res = attrBuilders.build();
        res.forEach((key, value) -> {
            System.out.println(key +" - " + value);
        });
        return res;
    }
}