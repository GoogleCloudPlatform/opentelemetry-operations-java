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
import java.util.Map;

/** Translates from OpenTelemetry Resource into Google Cloud Monitoring's MonitoredResource. */
public class ResourceTranslator {
  private static final String MR_CLOUD_RUN_REVISION = "cloud_run_revision";
  private static final String MR_GENERIC_TASK = "generic_task";

  private ResourceTranslator() {}

  /** Converts a Java OpenTelemetry SDK resource into a MonitoredResource from GCP. */
  public static MonitoredResource mapResource(Resource resource) {
    GcpResource gcpResource =
        com.google.cloud.opentelemetry.resource.ResourceTranslator.mapResource(resource);
    MonitoredResource.Builder mr = MonitoredResource.newBuilder();
    if (!mapCloudRunRevisionToGenericTask(mr, gcpResource)) {
      // the gcpResource was not cloud_run_revision, so no explicit mapping performed
      mr.setType(gcpResource.getResourceType());
      gcpResource.getResourceLabels().getLabels().forEach(mr::putLabels);
    }
    return mr.build();
  }

  /**
   * Helper function to map cloud_run_revision {@link MonitoredResource} to generic_task. This is
   * done because custom metrics are not yet supported on cloud_run_revision. For details see <a
   * href="https://cloud.google.com/monitoring/custom-metrics/creating-metrics#create-metric-desc">Manual
   * creation of metric descriptors</a>.
   *
   * @param monitoredResourceBuilder Builder object for {@link MonitoredResource} which needs to be
   *     mapped to generic_task.
   * @param cloudRunResource The actual Cloud Run resource which is detected.
   * @return True if the mapping operation was performed, indicating that the passed {@link
   *     GcpResource} was cloud_run_revision. False otherwise.
   */
  private static boolean mapCloudRunRevisionToGenericTask(
      MonitoredResource.Builder monitoredResourceBuilder, GcpResource cloudRunResource) {
    if (cloudRunResource.getResourceType().equals(MR_CLOUD_RUN_REVISION)) {
      monitoredResourceBuilder.setType(MR_GENERIC_TASK);
      Map<String, String> cloudRunLabels = cloudRunResource.getResourceLabels().getLabels();
      monitoredResourceBuilder.putLabels("location", cloudRunLabels.get("location"));
      monitoredResourceBuilder.putLabels("namespace", cloudRunLabels.get("configuration_name"));
      monitoredResourceBuilder.putLabels("job", cloudRunLabels.get("service_name"));
      monitoredResourceBuilder.putLabels("task_id", cloudRunLabels.get("instance_id"));
      return true;
    }
    return false;
  }
}
