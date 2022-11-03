/*
 * Copyright 2022 Google
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

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * A utility class that contains method that facilitate extraction of attributes from environment
 * variables and metadata configurations.
 */
public class AttributesExtractorUtil {

  public static void addAvailabilityZoneFromMetadata(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    // Example zone: australia-southeast1-a
    String zone = metadataConfig.getZone();
    if (zone != null) {
      attributesBuilder.put(ResourceAttributes.CLOUD_AVAILABILITY_ZONE, zone);
    }
  }

  public static void addCloudRegionFromMetadata(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String zone = metadataConfig.getZone();
    if (zone != null) {
      // Parsing required to scope up to a region
      String[] splitArr = zone.split("-");
      if (splitArr.length > 2) {
        attributesBuilder.put(ResourceAttributes.CLOUD_REGION, splitArr[0] + "-" + splitArr[1]);
      }
    }
  }

  public static void addInstanceIdFromMetadata(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String instanceId = metadataConfig.getInstanceId();
    if (instanceId != null) {
      attributesBuilder.put(ResourceAttributes.FAAS_ID, instanceId);
    }
  }
}
