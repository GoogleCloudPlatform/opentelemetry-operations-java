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
import io.opentelemetry.sdk.resources.Resource;

/** Translates from OpenTelemetry Resource into Google Cloud Monitoring's MonitoredResource. */
public class ResourceTranslator {
  private ResourceTranslator() {}

  /** Converts a Java OpenTelemetry SDK resource into a MonitoredResource from GCP. */
  public static MonitoredResource mapResource(Resource resource) {
    GcpResource gcpResource =
        com.google.cloud.opentelemetry.resource.ResourceTranslator.mapResource(resource);
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    mr.setType(gcpResource.getResourceType());
    gcpResource.getResourceLabels().getLabels().forEach(mr::putLabels);
    return mr.build();
  }
}
