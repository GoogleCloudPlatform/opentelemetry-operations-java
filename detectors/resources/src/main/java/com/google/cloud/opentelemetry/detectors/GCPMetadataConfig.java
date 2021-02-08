/*
 * Copyright 2021 Google
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

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieves Google Cloud project-id and a limited set of instance attributes from Metadata server.
 *
 * @see <a href="https://cloud.google.com/compute/docs/storing-retrieving-metadata">
 *     https://cloud.google.com/compute/docs/storing-retrieving-metadata</a>
 */
final class GCPMetadataConfig {
  private static final String DEFAULT_URL = "http://metadata.google.internal/computeMetadata/v1/";
  public static final GCPMetadataConfig DEFAULT_INSTANCE = new GCPMetadataConfig(DEFAULT_URL);

  private final String url;
  private final Map<String, String> cachedAttributes = new HashMap<>();

  public enum AttributeEndpoints {
    projectId("project/project-id"),
    zone("instance/zone"),
    machineType("instance/machine-type"),
    instanceId("instance/id"),
    clusterName("instance/attributes/cluster-name"),
    instanceHostName("instance/hostname"),
    instanceName("instance/name");

    private final String endpointString;

    AttributeEndpoints(String s) {
      endpointString = s;
    }

    public String toString() {
      return this.endpointString;
    }
  }

  // For testing only
  public GCPMetadataConfig(String url) {
    this.url = url;
  }

  boolean isRunningOnGcp() {
    return !getProjectId().isEmpty();
  }

  String getProjectId() {
    if (cachedAttributes.containsKey(AttributeEndpoints.projectId.toString())) {
      return cachedAttributes.get(AttributeEndpoints.projectId.toString());
    } else {
      String projectId = getAttribute(AttributeEndpoints.projectId.toString());
      if (!projectId.equals("")) cachedAttributes.put(AttributeEndpoints.projectId.toString(), projectId);
      return projectId;
    }
  }

  // Example response: projects/640212054955/zones/australia-southeast1-a
  String getZone() {
    if (cachedAttributes.containsKey(AttributeEndpoints.zone.toString())) {
      return cachedAttributes.get(AttributeEndpoints.zone.toString());
    } else {
      String zone = getAttribute(AttributeEndpoints.zone.toString());
      if (zone.contains("/")) {
        zone = zone.substring(zone.lastIndexOf('/') + 1);
      }
      if (!zone.equals("")) cachedAttributes.put(AttributeEndpoints.zone.toString(), zone);
      return zone;
    }
  }

  // Example response: projects/640212054955/machineTypes/e2-medium
  String getMachineType() {
    if (cachedAttributes.containsKey(AttributeEndpoints.machineType.toString())) {
      return cachedAttributes.get(AttributeEndpoints.machineType.toString());
    } else {
      String machineType = getAttribute(AttributeEndpoints.machineType.toString());
      if (machineType.contains("/")) {
        machineType = machineType.substring(machineType.lastIndexOf('/') + 1);
      }
      if (!machineType.equals("")) cachedAttributes.put(AttributeEndpoints.machineType.toString(), machineType);
      return machineType;
    }
  }

  String getInstanceId() {
    if (cachedAttributes.containsKey(AttributeEndpoints.instanceId.toString())) {
      return cachedAttributes.get(AttributeEndpoints.instanceId.toString());
    } else {
      String instanceId = getAttribute(AttributeEndpoints.instanceId.toString());
      if (!instanceId.equals(""))cachedAttributes.put(AttributeEndpoints.instanceId.toString(), instanceId);
      return instanceId;
    }
  }

  String getClusterName() {
    if (cachedAttributes.containsKey(AttributeEndpoints.clusterName.toString())) {
      return cachedAttributes.get(AttributeEndpoints.clusterName.toString());
    } else {
      String clusterName = getAttribute(AttributeEndpoints.clusterName.toString());
      if (!clusterName.equals("")) cachedAttributes.put(AttributeEndpoints.clusterName.toString(), clusterName);
      return clusterName;
    }
  }

  String getInstanceHostName() {
    if (cachedAttributes.containsKey(AttributeEndpoints.instanceHostName.toString())) {
      return cachedAttributes.get(AttributeEndpoints.instanceHostName.toString());
    } else {
      String instanceHostName = getAttribute(AttributeEndpoints.instanceHostName.toString());
      if (!instanceHostName.equals("")) cachedAttributes.put(AttributeEndpoints.instanceHostName.toString(), instanceHostName);
      return instanceHostName;
    }
  }

  String getInstanceName() {
    if (cachedAttributes.containsKey(AttributeEndpoints.instanceName.toString())) {
      return cachedAttributes.get(AttributeEndpoints.instanceName.toString());
    } else {
      String instanceName = getAttribute(AttributeEndpoints.instanceName.toString());
      if (!instanceName.equals("")) cachedAttributes.put(AttributeEndpoints.instanceName.toString(), instanceName);
      return instanceName;
    }
  }

  String getAttribute(String attributeName) {
    try {
      URL url = new URL(this.url + attributeName);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Metadata-Flavor", "Google");
      if (connection.getResponseCode() == 200
          && ("Google").equals(connection.getHeaderField("Metadata-Flavor"))) {
        InputStream input = connection.getInputStream();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
          return firstNonNull(reader.readLine(), "");
        }
      }
    } catch (IOException ignore) {
      // ignore
    }
    return "";
  }
}
