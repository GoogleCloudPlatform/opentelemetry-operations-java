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

  // For testing only
  public GCPMetadataConfig(String url) {
    this.url = url;
  }

  boolean isRunningOnGcp() {
    return getProjectId() != null && !getProjectId().isEmpty();
  }

  // Returns null on failure to retrieve from metadata server
  String getProjectId() {
    return getAttribute("project/project-id");
  }

  // Example response: projects/640212054955/zones/australia-southeast1-a
  // Returns null on failure to retrieve from metadata server
  String getZone() {
    String zone = getAttribute("instance/zone");
    if (zone != null && zone.contains("/")) {
      zone = zone.substring(zone.lastIndexOf('/') + 1);
    }
    return zone;
  }

  // Example response: projects/640212054955/machineTypes/e2-medium
  String getMachineType() {
    String machineType = getAttribute("instance/machine-type");
    if (machineType != null && machineType.contains("/")) {
      machineType = machineType.substring(machineType.lastIndexOf('/') + 1);
    }
    return machineType;
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceId() {
    return getAttribute("instance/id");
  }

  // Returns null on failure to retrieve from metadata server
  String getClusterName() {
    return getAttribute("instance/attributes/cluster-name");
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceHostName() {
    return getAttribute("instance/hostname");
  }

  // Returns null on failure to retrieve from metadata server
  String getInstanceName() {
    return getAttribute("instance/name");
  }

  // Returns null on failure to retrieve from metadata server
  private String getAttribute(String attributeName) {
    return cachedAttributes.computeIfAbsent(attributeName, this::fetchAttribute);
  }

  // Return the attribute received at <attributeName> relative path or null on failure
  private String fetchAttribute(String attributeName) {
    try {
      URL url = new URL(this.url + attributeName);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty("Metadata-Flavor", "Google");
      if (connection.getResponseCode() == 200
          && ("Google").equals(connection.getHeaderField("Metadata-Flavor"))) {
        InputStream input = connection.getInputStream();
        try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
          return reader.readLine();
        }
      }
    } catch (IOException ignore) {
      // ignore
    }
    return null;
  }
}
