package com.google.cloud.opentelemetry.detectors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.ResourceProvider;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;

public final class GCEResource extends ResourceProvider {
  private final GCPMetadataConfig metadata;

  public GCEResource() {
    this(GCPMetadataConfig.DEFAULT_INSTANCE);
  }

  // For testing only
  public GCEResource(GCPMetadataConfig metadataConfig) {
    this.metadata = metadataConfig;
  }

  @Override
  public Attributes getAttributes() {
    if (!metadata.isRunningOnGcp()) {
      return Attributes.empty();
    }

    AttributesBuilder attrBuilders = Attributes.builder();
    attrBuilders.put(SemanticAttributes.CLOUD_PROVIDER, "gcp");

    String projectId = metadata.getProjectId();
    if (!projectId.isEmpty()) {
      attrBuilders.put(SemanticAttributes.CLOUD_ACCOUNT_ID, projectId);
    }

    String zone = metadata.getZone();
    if (!zone.isEmpty()) {
      attrBuilders.put(SemanticAttributes.CLOUD_ZONE, zone);

      String[] splitArr = zone.split("-");
      if (splitArr.length > 2) {
        attrBuilders.put(
            SemanticAttributes.CLOUD_REGION, String.join("-", Arrays.copyOfRange(splitArr, 0, 2)));
      }
    }

    String instanceId = metadata.getInstanceId();
    if (!instanceId.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_ID, instanceId);
    }

    String instanceName = metadata.getInstanceName();
    if (!instanceName.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_NAME, instanceName);
    }

    String hostName = metadata.getInstanceHostname();
    if (!hostName.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_NAME, hostName);
    }

    String hostType = metadata.getMachineType();
    if (!hostType.isEmpty()) {
      attrBuilders.put(SemanticAttributes.HOST_TYPE, hostType);
    }

    return attrBuilders.build();
  }
}
