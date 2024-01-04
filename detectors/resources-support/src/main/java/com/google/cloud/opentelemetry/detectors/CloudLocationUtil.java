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

import java.util.Optional;

/**
 * Contains utility functions to retrieve appropriate zone and regions representing the location of
 * certain cloud resources.
 */
final class CloudLocationUtil {
  /**
   * Utility method to extract cloud availability zone from passed {@link GCPMetadataConfig}. The
   * method modifies the passed attributesBuilder by adding the extracted property to it. If the
   * zone cannot be found, calling this method has no effect.
   *
   * <ul>
   *   <li>If the availability zone cannot be found, calling this method has no effect.
   * </ul>
   *
   * <p>Example zone: australia-southeast1-a
   *
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud availability zone
   *     value is extracted.
   */
  static Optional<String> getAvailabilityZoneFromMetadata(GCPMetadataConfig metadataConfig) {
    return Optional.ofNullable(metadataConfig.getZone());
  }

  /**
   * Utility method to extract the cloud region from passed {@link GCPMetadataConfig}. The method
   * modifies the passed attributesBuilder by adding the extracted property to it.
   *
   * <ul>
   *   <li>If the cloud region cannot be found, calling this method has no effect.
   *   <li>This method uses zone attribute to parse region from it.
   * </ul>
   *
   * <p>Example region: australia-southeast1
   *
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud region value is
   *     extracted.
   */
  static Optional<String> getCloudRegionFromMetadataUsingZone(GCPMetadataConfig metadataConfig) {
    Optional<String> optZone = getAvailabilityZoneFromMetadata(metadataConfig);
    if (optZone.isPresent()) {
      // Parsing required to scope up to a region
      String[] splitArr = optZone.get().split("-");
      if (splitArr.length > 2) {
        return Optional.of(String.join("-", splitArr[0], splitArr[1]));
      }
    }
    return Optional.empty();
  }

  /**
   * Utility method to extract the cloud region from passed {@link GCPMetadataConfig}. The method
   * modifies the passed attributesBuilder by adding the extracted property to it.
   *
   * <ul>
   *   <li>If the cloud region cannot be found, calling this method has no effect.
   *   <li>This method directly uses the region attribute from the metadata config.
   * </ul>
   *
   * <p>Example region: australia-southeast1
   *
   * @param metadataConfig The {@link GCPMetadataConfig} from which the cloud region value is
   *     extracted.
   */
  static Optional<String> getCloudRegionFromMetadataUsingRegion(GCPMetadataConfig metadataConfig) {
    return Optional.ofNullable(metadataConfig.getRegion());
  }
}
