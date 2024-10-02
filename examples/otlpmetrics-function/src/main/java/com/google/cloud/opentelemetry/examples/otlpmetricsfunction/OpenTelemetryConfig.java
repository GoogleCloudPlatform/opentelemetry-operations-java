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
package com.google.cloud.opentelemetry.examples.otlpmetricsfunction;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.gcp.resource.GCPResourceProvider;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;

/**
 * A singleton class representing the OpenTelemetry instance that can be used to instrument the
 * application.
 */
public final class OpenTelemetryConfig {
  private static final OpenTelemetryConfig INSTANCE = new OpenTelemetryConfig();
  private static final int METRIC_EXPORT_DURATION_MILLIS = 10000;
  private final OpenTelemetrySdk openTelemetry;

  // prevent object creation
  private OpenTelemetryConfig() {
    this.openTelemetry = initOpenTelemetry();
  }

  public static OpenTelemetryConfig getInstance() {
    return INSTANCE;
  }

  public MeterProvider getMeterProvider() {
    return this.openTelemetry.getMeterProvider();
  }

  /** Closes the OpenTelemetry SDK instance, exporting any pending metrics. */
  public void closeSdk() {
    openTelemetry.close();
  }

  private OpenTelemetrySdk initOpenTelemetry() {
    // Enable proper resource detection within the application
    // This is used for the logging exporter.
    GCPResourceProvider resourceProvider = new GCPResourceProvider();
    Resource resource = Resource.getDefault().merge(resourceProvider.createResource(null));

    return OpenTelemetrySdk.builder()
        .setMeterProvider(
            SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                    PeriodicMetricReader.builder(LoggingMetricExporter.create())
                        .setInterval(Duration.ofMillis(METRIC_EXPORT_DURATION_MILLIS))
                        .build())
                .registerMetricReader(
                    PeriodicMetricReader.builder(OtlpGrpcMetricExporter.getDefault())
                        .setInterval(Duration.ofMillis(METRIC_EXPORT_DURATION_MILLIS))
                        .build())
                .build())
        .buildAndRegisterGlobal();
  }
}
