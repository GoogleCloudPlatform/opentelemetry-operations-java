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
  private Map<String, String> cachedAttributes = new HashMap<>();

  // For testing only
  public GCPMetadataConfig(String url) {
    this.url = url;
  }

  boolean isRunningOnGcp() {
    return !getProjectId().isEmpty();
  }

  String getProjectId() {
    if (cachedAttributes.containsKey("project/project-id")) {
      return cachedAttributes.get("project/project-id");
    } else {
      String projectId = getAttribute("project/project-id");
      cachedAttributes.put("project/project-id", projectId);
      return projectId;
    }
  }

  // Example response: projects/640212054955/zones/australia-southeast1-a
  String getZone() {
    if (cachedAttributes.containsKey("instance/zone")) {
      return cachedAttributes.get("instance/zone");
    } else {
      String zone = getAttribute("instance/zone");
      if (zone.contains("/")) {
        zone = zone.substring(zone.lastIndexOf('/') + 1);
      }
      cachedAttributes.put("instance/zone", zone);
      return zone;
    }
  }

  // Example response: projects/640212054955/machineTypes/e2-medium
  String getMachineType() {
    if (cachedAttributes.containsKey("instance/machine-type")) {
      return cachedAttributes.get("instance/machine-type");
    } else {
      String machineType = getAttribute("instance/machine-type");
      if (machineType.contains("/")) {
        machineType = machineType.substring(machineType.lastIndexOf('/') + 1);
      }
      cachedAttributes.put("instance/machine-type", machineType);
      return machineType;
    }
  }

  String getInstanceId() {
    if (cachedAttributes.containsKey("instance/id")) {
      return cachedAttributes.get("instance/id");
    } else {
      String instanceId = getAttribute("instance/id");
      cachedAttributes.put("instance/id", instanceId);
      return instanceId;
    }
  }

  String getClusterName() {
    if (cachedAttributes.containsKey("instance/attributes/cluster-name")) {
      return cachedAttributes.get("instance/attributes/cluster-name");
    } else {
      String clusterName = getAttribute("instance/attributes/cluster-name");
      cachedAttributes.put("instance/attributes/cluster-name", clusterName);
      return clusterName;
    }
  }

  String getInstanceHostName() {
    if (cachedAttributes.containsKey("instance/hostname")) {
      return cachedAttributes.get("instance/hostname");
    } else {
      String instanceHostName = getAttribute("instance/hostname");
      cachedAttributes.put("instance/hostname", instanceHostName);
      return instanceHostName;
    }
  }

  String getInstanceName() {
    if (cachedAttributes.containsKey("instance/name")) {
      return cachedAttributes.get("instance/name");
    } else {
      String instanceName = getAttribute("instance/name");
      cachedAttributes.put("instance/name", instanceName);
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
