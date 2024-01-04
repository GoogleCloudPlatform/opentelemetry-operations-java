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

import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_AVAILABILITY_ZONE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_CLOUD_REGION;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_MACHINE_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_PROJECT_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    map.put(GCE_AVAILABILITY_ZONE, Optional.ofNullable(this.metadataConfig.getZone()));
    map.put(GCE_CLOUD_REGION, Optional.ofNullable(this.metadataConfig.getRegionFromZone()));
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
