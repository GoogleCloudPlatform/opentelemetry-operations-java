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

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.FaasIncubatingAttributes;

/**
 * A utility class that contains method that facilitate extraction of attributes from environment
 * variables and metadata configurations.
 *
 * <p>This class only adds helper methods to extract {@link CloudIncubatingAttributes}
 * and {@link FaasIncubatingAttributes} that are common
 * across all the supported compute environments.
 *
 * @deprecated Not for public use. This class is expected to be retained only as package private.
 */
@Deprecated
public class AttributesExtractorUtil {

  /**
   * Utility method to extract cloud availability zone from passed {@link GCPMetadataConfig}. The
   * method modifies the passed attributesBuilder by adding the extracted property to it. If the
   * zone cannot be found, calling this method has no effect.
   *
   * <ul>
   *   <li>If the availability zone cannot be found, calling this method has no effect.
   *   <li>Calling this method will update {@link CloudIncubatingAttributes#CLOUD_AVAILABILITY_ZONE}
   *       attribute.
   * </ul>
   *
   * <p>Example zone: australia-southeast1-a
   *
   * @param attributesBuilder The {@link AttributesBuilder} to which the extracted property needs to
   *     be added.
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud availability zone
   *     value is extracted.
   */
  public static void addAvailabilityZoneFromMetadata(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String zone = metadataConfig.getZone();
    if (zone != null) {
      attributesBuilder.put(CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE, zone);
    }
  }

  /**
   * Utility method to extract the cloud region from passed {@link GCPMetadataConfig}. The method
   * modifies the passed attributesBuilder by adding the extracted property to it.
   *
   * <ul>
   *   <li>If the cloud region cannot be found, calling this method has no effect.
   *   <li>Calling this method will update {@link CloudIncubatingAttributes#CLOUD_REGION} attribute.
   *   <li>This method uses zone attribute to parse region from it.
   * </ul>
   *
   * <p>Example region: australia-southeast1
   *
   * @param attributesBuilder The {@link AttributesBuilder} to which the extracted property needs to
   *     be added.
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud region value is
   *     extracted.
   */
  public static void addCloudRegionFromMetadataUsingZone(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String zone = metadataConfig.getZone();
    if (zone != null) {
      // Parsing required to scope up to a region
      String[] splitArr = zone.split("-");
      if (splitArr.length > 2) {
        attributesBuilder.put(CloudIncubatingAttributes.CLOUD_REGION, splitArr[0] + "-" + splitArr[1]);
      }
    }
  }

  /**
   * Utility method to extract the cloud region from passed {@link GCPMetadataConfig}. The method
   * modifies the passed attributesBuilder by adding the extracted property to it.
   *
   * <ul>
   *   <li>If the cloud region cannot be found, calling this method has no effect.
   *   <li>Calling this method will update {@link CloudIncubatingAttributes#CLOUD_REGION} attribute.
   *   <li>This method directly uses the region attribute from the metadata config.
   * </ul>
   *
   * <p>Example region: australia-southeast1
   *
   * @param attributesBuilder The {@link AttributesBuilder} to which the extracted property needs to
   *     be added.
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud region value is
   *     extracted.
   */
  public static void addCloudRegionFromMetadataUsingRegion(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String region = metadataConfig.getRegion();
    if (region != null) {
      attributesBuilder.put(CloudIncubatingAttributes.CLOUD_REGION, region);
    }
  }

  /**
   * Utility method to extract the current compute instance ID from the passed {@link
   * GCPMetadataConfig}. The method modifies the passed attributesBuilder by adding the extracted
   * property to it.
   *
   * <ul>
   *   <li>If the instance ID cannot be found, calling this method has no effect.
   *   <li>Calling this method will update {@link FaasIncubatingAttributes#FAAS_INSTANCE} attribute.
   * </ul>
   *
   * @param attributesBuilder The {@link AttributesBuilder} to which the extracted property needs to
   *     be added.
   * @param metadataConfig The {@link GCPMetadataConfig} from which the instance ID value is
   *     extracted.
   */
  public static void addInstanceIdFromMetadata(
      AttributesBuilder attributesBuilder, GCPMetadataConfig metadataConfig) {
    String instanceId = metadataConfig.getInstanceId();
    if (instanceId != null) {
      attributesBuilder.put(FaasIncubatingAttributes.FAAS_INSTANCE, instanceId);
    }
  }
}
