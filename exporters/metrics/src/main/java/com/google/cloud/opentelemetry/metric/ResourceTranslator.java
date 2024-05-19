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
package com.google.cloud.opentelemetry.metric;

import com.google.api.MonitoredResource;
import com.google.cloud.opentelemetry.resource.GcpResource;
import com.google.common.base.Strings;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Set;

/** Translates from OpenTelemetry Resource into Google Cloud Monitoring's MonitoredResource. */
public class ResourceTranslator {
  private static final String CUSTOM_MR_KEY = "gcp.resource_type";
  private static final String MAPPING_MISMATCH_EXCEPTION_MSG =
      String.format(
          "Found %s key, but the OpenTelemetry Resource either does not have the expected attributes indicated by the MonitoredResource mappings or has extra attributes.",
          CUSTOM_MR_KEY);

  private ResourceTranslator() {}

  /** Converts a Java OpenTelemetry SDK resource into a MonitoredResource from GCP. */
  @Deprecated
  public static MonitoredResource mapResource(Resource resource) {
    GcpResource gcpResource =
        com.google.cloud.opentelemetry.resource.ResourceTranslator.mapResource(resource);
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(gcpResource.getResourceType());
    gcpResource.getResourceLabels().getLabels().forEach(mr::putLabels);
    return mr.build();
  }

  static MonitoredResource mapResource(Resource resource, MonitoredResourceMapping mrMappings) {
    String mrTypeToMap = resource.getAttributes().get(AttributeKey.stringKey(CUSTOM_MR_KEY));
    if (!Strings.isNullOrEmpty(mrTypeToMap)) {
      return mapResourceUsingCustomMappings(resource, mrTypeToMap, mrMappings);
    } else {
      return mapResourceUsingStandardMappings(resource);
    }
  }

  private static MonitoredResource mapResourceUsingCustomMappings(
      Resource resource, String mrTypeToMap, MonitoredResourceMapping monitoredResourceMapping) {
    if (!mrTypeToMap.equals(monitoredResourceMapping.getMonitoredResourceType())
        || resource.getAttributes().size()
            != monitoredResourceMapping.getMonitoredResourceLabels().size()) {
      throw new RuntimeException(MAPPING_MISMATCH_EXCEPTION_MSG);
    }
    Set<String> mrLabels = monitoredResourceMapping.getMonitoredResourceLabels();
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(mrTypeToMap);
    mrLabels.forEach(
        expectedLabel -> {
          String foundValue = resource.getAttributes().get(AttributeKey.stringKey(expectedLabel));
          if (foundValue != null) {
            mr.putLabels(expectedLabel, foundValue);
          } else {
            throw new RuntimeException(MAPPING_MISMATCH_EXCEPTION_MSG);
          }
        });
    return mr.build();
  }

  private static MonitoredResource mapResourceUsingStandardMappings(Resource resource) {
    GcpResource gcpResource =
        com.google.cloud.opentelemetry.resource.ResourceTranslator.mapResource(resource);
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(gcpResource.getResourceType());
    gcpResource.getResourceLabels().getLabels().forEach(mr::putLabels);
    return mr.build();
  }
}
