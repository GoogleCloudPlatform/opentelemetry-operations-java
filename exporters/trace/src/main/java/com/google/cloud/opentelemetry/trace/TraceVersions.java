/*
 * Copyright 2023 Google
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
package com.google.cloud.opentelemetry.trace;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_VERSION;
// import static
// io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_VERSION;

import io.opentelemetry.sdk.resources.Resource;
import java.util.Properties;
import javax.annotation.Nullable;

/** Helper to grab version numbers from builds. */
public class TraceVersions {

  public static final String SDK_VERSION = readSdkVersion();
  public static final String EXPORTER_VERSION = readVersion();

  @Nullable
  private static String readSdkVersion() {
    return Resource.getDefault().getAttributes().get(TELEMETRY_SDK_VERSION);
  }

  @Nullable
  private static String readVersion() {
    Properties properties = new Properties();
    try {
      properties.load(
          TraceVersions.class.getResourceAsStream(
              "/com/google/cloud/opentelemetry/trace/version.properties"));
    } catch (Exception e) {
      // we left the attribute empty
      return "unknown";
    }
    String result = properties.getProperty("exporter.version");
    if (result != null && result.endsWith("-SNAPSHOT")) {
      // Lame hack:  Mock server doesn't allow interesting version strings.
      return result.substring(0, result.length() - 9);
    }
    return result;
  }
}
