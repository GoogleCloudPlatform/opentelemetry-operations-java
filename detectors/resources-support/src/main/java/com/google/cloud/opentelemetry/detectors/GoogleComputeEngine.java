package com.google.cloud.opentelemetry.detectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_MACHINE_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_PROJECT_ID;

final class GoogleComputeEngine implements DetectedPlatform {
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, Optional<String>> availableAttributes;

  GoogleComputeEngine() {
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.availableAttributes = prepareAttributes();
  }

  // for testing only
  GoogleComputeEngine(
      GCPMetadataConfig metadataConfig, Map<String, Optional<String>> availableAttributes) {
    this.metadataConfig = metadataConfig;
    this.availableAttributes = availableAttributes;
  }

  private Map<String, Optional<String>> prepareAttributes() {
    Map<String, Optional<String>> map = new HashMap<>();
    map.put(GCE_PROJECT_ID, Optional.ofNullable(this.metadataConfig.getProjectId()));
    map.put(
        GCE_AVAILABILITY_ZONE,
        CloudLocationUtil.getAvailabilityZoneFromMetadata(this.metadataConfig));
    map.put(
        GCE_CLOUD_REGION,
        CloudLocationUtil.getCloudRegionFromMetadataUsingZone(this.metadataConfig));
    map.put(GCE_INSTANCE_ID, Optional.ofNullable(this.metadataConfig.getInstanceId()));
    map.put(GCE_INSTANCE_NAME, Optional.ofNullable(this.metadataConfig.getInstanceName()));
    map.put(GCE_MACHINE_TYPE, Optional.ofNullable(this.metadataConfig.getMachineType()));
    return Collections.unmodifiableMap(map);
  }

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE;
  }

  @Override
  public Map<String, Optional<String>> getAttributes() {
    return this.availableAttributes;
  }
}
