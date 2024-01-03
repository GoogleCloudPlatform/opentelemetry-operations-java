package com.google.cloud.opentelemetry.detectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class to help add attributes applicable to serverless compute in GCP to a given resource.
 */
abstract class GoogleServerlessCompute implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, Optional<String>> availableAttributes;

  GoogleServerlessCompute() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.availableAttributes = prepareAttributes();
  }

  // for testing only
  GoogleServerlessCompute(
      EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, Optional<String>> prepareAttributes() {
    Map<String, Optional<String>> map = new HashMap<>();
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_NAME,
        Optional.ofNullable(this.environmentVariables.get("K_SERVICE")));
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_REVISION,
        Optional.ofNullable(this.environmentVariables.get("K_REVISION")));
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_AVAILABILITY_ZONE,
        CloudLocationUtil.getAvailabilityZoneFromMetadata(this.metadataConfig));
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_CLOUD_REGION,
        CloudLocationUtil.getCloudRegionFromMetadataUsingZone(this.metadataConfig));
    map.put(
        AttributeKeys.SERVERLESS_COMPUTE_INSTANCE_ID,
        Optional.ofNullable(this.metadataConfig.getInstanceId()));
    return Collections.unmodifiableMap(map);
  }

  @Override
  public Map<String, Optional<String>> getAttributes() {
    return this.availableAttributes;
  }
}
