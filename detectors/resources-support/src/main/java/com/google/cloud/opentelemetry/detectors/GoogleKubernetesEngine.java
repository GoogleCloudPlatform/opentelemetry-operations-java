/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.opentelemetry.detectors;

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_LOCATION_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CLUSTER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_CONTAINER_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_HOST_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_LOCATION_TYPE_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_NAMESPACE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GKE_POD_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class GoogleKubernetesEngine implements DetectedPlatform {
  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleKubernetesEngine(
      EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(GKE_POD_NAME, getPodName());
    map.put(GKE_NAMESPACE, this.environmentVariables.get("NAMESPACE"));
    map.put(GKE_CONTAINER_NAME, this.environmentVariables.get("CONTAINER_NAME"));
    map.put(GKE_CLUSTER_NAME, this.metadataConfig.getClusterName());
    map.put(GKE_CLUSTER_LOCATION, this.metadataConfig.getClusterLocation());
    map.put(GKE_CLUSTER_LOCATION_TYPE, this.getClusterLocationType());
    map.put(GKE_HOST_ID, this.metadataConfig.getInstanceId());
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
  public Map<String, String> getAttributes() {
    return this.availableAttributes;
  }
}
