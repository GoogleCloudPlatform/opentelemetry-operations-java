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
package com.google.cloud.opentelemetry.example.resource;

import io.opentelemetry.contrib.gcp.resource.GCPResourceProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

public class ResourceExample {
  public static void main(String[] args) {
    // Get the autoconfigured OpenTelemetry SDK
    OpenTelemetrySdk openTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

    // Shows the resource attributes detected from the environment variables
    // and system properties.
    System.out.println("Detecting resource: Environment");
    Resource autoResource = ResourceConfiguration.createEnvironmentResource();
    System.out.println(autoResource.getAttributes() + "\n");

    // Shows the resource attributes detected by the GCP Resource Provider
    System.out.println("Detecting resource: hardcoded");
    GCPResourceProvider resourceProvider = new GCPResourceProvider();
    System.out.println(resourceProvider.getAttributes() + "\n");

    // Shows the attributes attached to the Resource that was set for TracerProvider
    // via the autoconfiguration SPI.
    // This works similarly for MeterProvider and LoggerProvider.
    System.out.println("Detecting resource: Autoconfigure");
    SdkTracerProvider autoConfTracerProvider = openTelemetrySdk.getSdkTracerProvider();
    System.out.println(autoConfTracerProvider.toString() + "\n");
  }
}
