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
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_HOSTNAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_ID;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_INSTANCE_NAME;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_MACHINE_TYPE;
import static com.google.cloud.opentelemetry.detectors.AttributeKeys.GCE_PROJECT_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class GoogleComputeEngine implements DetectedPlatform {
  private final GCPMetadataConfig metadataConfig;
  private final Map<String, String> availableAttributes;

  GoogleComputeEngine(GCPMetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
    this.availableAttributes = prepareAttributes();
  }

  private Map<String, String> prepareAttributes() {
    Map<String, String> map = new HashMap<>();
    map.put(GCE_PROJECT_ID, this.metadataConfig.getProjectId());
    map.put(GCE_AVAILABILITY_ZONE, this.metadataConfig.getZone());
    map.put(GCE_CLOUD_REGION, this.metadataConfig.getRegionFromZone());
    map.put(GCE_INSTANCE_ID, this.metadataConfig.getInstanceId());
    map.put(GCE_INSTANCE_NAME, this.metadataConfig.getInstanceName());
    map.put(GCE_INSTANCE_HOSTNAME, this.metadataConfig.getInstanceHostName());
    map.put(GCE_MACHINE_TYPE, this.metadataConfig.getMachineType());
    return Collections.unmodifiableMap(map);
  }

  @Override
  public GCPPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GCPPlatformDetector.SupportedPlatform.GOOGLE_COMPUTE_ENGINE;
  }

  @Override
  public Map<String, String> getAttributes() {
    return this.availableAttributes;
  }
}
