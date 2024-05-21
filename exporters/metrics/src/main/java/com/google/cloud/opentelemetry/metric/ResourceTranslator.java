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
import java.util.Objects;
import java.util.Set;

/** Translates from OpenTelemetry Resource into Google Cloud Monitoring's MonitoredResource. */
public class ResourceTranslator {
  private static final String CUSTOM_MR_KEY = "gcp.resource_type";
  private static final String BLANK_STRING = " ";

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

  /**
   * Converts a Java OpenTelemetry SDK {@link Resource} into a Google specific {@link
   * MonitoredResource}.
   *
   * @param resource The OpenTelemetry {@link Resource} to be converted.
   * @param mrDescription The {@link MonitoredResourceDescription} in case the OpenTelemetry SDK
   *     {@link Resource} needs to be converted to a custom {@link MonitoredResource}. For use-cases
   *     not requiring custom {@link MonitoredResource}s, use the {@link
   *     MetricConfiguration#DEFAULT_MONITORED_RESOURCE_DESCRIPTION}.
   * @return The converted {@link MonitoredResource} based on the provided {@link
   *     MonitoredResourceDescription}.
   */
  static MonitoredResource mapResource(
      Resource resource, MonitoredResourceDescription mrDescription) {
    String mrTypeToMap = resource.getAttributes().get(AttributeKey.stringKey(CUSTOM_MR_KEY));
    if (Strings.isNullOrEmpty(mrTypeToMap)) {
      return mapResourceUsingCustomerMappings(resource);
    } else {
      return mapResourceUsingPlatformMappings(resource, mrTypeToMap, mrDescription);
    }
  }

  private static MonitoredResource mapResourceUsingPlatformMappings(
      Resource resource,
      String mrTypeToMap,
      MonitoredResourceDescription monitoredResourceDescription) {
    Set<String> expectedMRLabels = monitoredResourceDescription.getMonitoredResourceLabels();
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(mrTypeToMap);
    expectedMRLabels.forEach(
        expectedLabel -> {
          String foundValue = resource.getAttribute(AttributeKey.stringKey(expectedLabel));
          mr.putLabels(expectedLabel, Objects.requireNonNullElse(foundValue, BLANK_STRING));
        });
    return mr.build();
  }

  private static MonitoredResource mapResourceUsingCustomerMappings(Resource resource) {
    GcpResource gcpResource =
        com.google.cloud.opentelemetry.resource.ResourceTranslator.mapResource(resource);
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(gcpResource.getResourceType());
    gcpResource.getResourceLabels().getLabels().forEach(mr::putLabels);
    return mr.build();
  }
}
