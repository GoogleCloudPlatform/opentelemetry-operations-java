package com.google.cloud.opentelemetry.detectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_APP_VERSION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GAE_MODULE_NAME;

final class GoogleAppEngine implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, Optional<String>> availableAttributes;

  GoogleAppEngine() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.availableAttributes = prepareAttributes();
  }

  // for testing only
  GoogleAppEngine(EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, Optional<String>> prepareAttributes() {
    Map<String, Optional<String>> map = new HashMap<>();
    map.put(GAE_MODULE_NAME, Optional.ofNullable(this.environmentVariables.get("GAE_SERVICE")));
    map.put(GAE_APP_VERSION, Optional.ofNullable(this.environmentVariables.get("GAE_VERSION")));
    map.put(GAE_INSTANCE_ID, Optional.ofNullable(this.environmentVariables.get("GAE_INSTANCE")));
    map.put(
        GAE_AVAILABILITY_ZONE,
        CloudLocationUtil.getAvailabilityZoneFromMetadata(this.metadataConfig));
    map.put(GAE_CLOUD_REGION, getCloudRegion());
    return Collections.unmodifiableMap(map);
  }

  private Optional<String> getCloudRegion() {
    if (this.environmentVariables.get("GAE_ENV") != null
        && this.environmentVariables.get("GAE_ENV").equals("standard")) {
      return CloudLocationUtil.getCloudRegionFromMetadataUsingRegion(this.metadataConfig);
    } else {
      return CloudLocationUtil.getCloudRegionFromMetadataUsingZone(this.metadataConfig);
    }
  }

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_APP_ENGINE;
  }

  @Override
  public Map<String, Optional<String>> getAttributes() {
    return this.availableAttributes;
  }
}
