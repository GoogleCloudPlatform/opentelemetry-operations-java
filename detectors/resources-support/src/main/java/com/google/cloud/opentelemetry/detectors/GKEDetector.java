/*
 * Copyright 2023 Google LLC
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

import java.util.Optional;

/** Utility class to help add GKE specific attributes to a given resource */
public final class GKEDetector {
  public static final GKEDetector DEFAULT_INSTANCE = new GKEDetector();

  private final EnvironmentVariables environmentVariables;
  private final GCPMetadataConfig metadataConfig;

  GKEDetector() {
    this.environmentVariables = EnvironmentVariables.DEFAULT_INSTANCE;
    this.metadataConfig = GCPMetadataConfig.DEFAULT_INSTANCE;
  }

  // for testing only
  GKEDetector(EnvironmentVariables environmentVariables, GCPMetadataConfig metadataConfig) {
    this.environmentVariables = environmentVariables;
    this.metadataConfig = metadataConfig;
  }

  public String getPodName() {
    Optional<String> podName = Optional.ofNullable(this.environmentVariables.get("POD_NAME"));
    return podName.orElse(this.environmentVariables.get("HOSTNAME"));
  }

  public Optional<String> getNamespace() {
    return Optional.ofNullable(this.environmentVariables.get("NAMESPACE"));
  }

  public Optional<String> getContainerName() {
    return Optional.ofNullable(this.environmentVariables.get("CONTAINER_NAME"));
  }

  public Optional<String> getHostID() {
    return Optional.ofNullable(this.metadataConfig.getInstanceId());
  }

  public Optional<String> getClusterName() {
    return Optional.ofNullable(this.metadataConfig.getClusterName());
  }

  public GKEZoneOrRegion getGKEClusterLocation() {
    return new GKEZoneOrRegion(this.metadataConfig.getClusterLocation());
  }

  public enum LocationType {
    ZONE,
    REGION,
    UNDEFINED,
  }

  public static final class GKEZoneOrRegion {

    private final LocationType locationType;
    private final String clusterLocation;

    GKEZoneOrRegion(String clusterLocation) {
      this.clusterLocation = clusterLocation;
      this.locationType = determineClusterLocationType(clusterLocation);
    }

    public LocationType getLocationType() {
      return this.locationType;
    }

    public String getClusterLocation() {
      return this.clusterLocation;
    }

    private LocationType determineClusterLocationType(String clusterLocation) {
      long dashCount =
          (clusterLocation == null || clusterLocation.isEmpty())
              ? 0
              : clusterLocation.chars().filter(ch -> ch == '-').count();
      if (dashCount == 1) {
        return LocationType.REGION;
      } else if (dashCount == 2) {
        return LocationType.ZONE;
      }
      return LocationType.UNDEFINED;
    }
  }
}
