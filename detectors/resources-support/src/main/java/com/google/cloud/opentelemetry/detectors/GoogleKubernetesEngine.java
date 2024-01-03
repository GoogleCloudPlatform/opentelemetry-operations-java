package com.google.cloud.opentelemetry.detectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CONTAINER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_HOST_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_NAMESPACE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_POD_NAME;

/** Utility class to help add GKE specific attributes to a given resource */
final class GoogleKubernetesEngine implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, Optional<String>> availableAttributes;

  GoogleKubernetesEngine() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
    this.availableAttributes = prepareAttributes();
  }

  // for testing only
  GoogleKubernetesEngine(
      EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, Optional<String>> prepareAttributes() {
    Map<String, Optional<String>> map = new HashMap<>();
    map.put(GKE_POD_NAME, Optional.ofNullable(getPodName()));
    map.put(GKE_NAMESPACE, Optional.ofNullable(this.environmentVariables.get("NAMESPACE")));
    map.put(
        GKE_CONTAINER_NAME, Optional.ofNullable(this.environmentVariables.get("CONTAINER_NAME")));
    map.put(GKE_CLUSTER_NAME, Optional.ofNullable(this.metadataConfig.getClusterName()));
    map.put(GKE_CLUSTER_LOCATION, Optional.of(this.metadataConfig.getClusterLocation()));
    map.put(GKE_CLUSTER_LOCATION_TYPE, Optional.of(this.getClusterLocationType()));
    map.put(GKE_HOST_ID, Optional.ofNullable(this.metadataConfig.getInstanceId()));
    return Collections.unmodifiableMap(map);
  }

  private String getPodName() {
    Optional<String> podName = Optional.ofNullable(this.environmentVariables.get("POD_NAME"));
    return podName.orElse(this.environmentVariables.get("HOSTNAME"));
  }

  private String getClusterLocationType() {
    String clusterLocation = this.metadataConfig.getClusterLocation();
    long dashCount =
        (clusterLocation == null || clusterLocation.isEmpty())
            ? 0
            : clusterLocation.chars().filter(ch -> ch == '-').count();
    if (dashCount == 1) {
      return GKE_LOCATION_TYPE_REGION;
    } else if (dashCount == 2) {
      return GKE_LOCATION_TYPE_ZONE;
    }
    return "";
  }

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_KUBERNETES_ENGINE;
  }

  @Override
  public Map<String, Optional<String>> getAttributes() {
    return this.availableAttributes;
  }
}
