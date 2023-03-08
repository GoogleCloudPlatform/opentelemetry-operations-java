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
package com.google.cloud.opentelemetry.example.metrics;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.metric.MetricConfiguration;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MetricsExporterOpenTelemetryExample {
  private static final String INSTRUMENTATION_SCOPE_NAME =
      MetricsExporterOpenTelemetryExample.class.getName();
  private static final Random RANDOM = new Random();

  private static Meter meter;

  private static OpenTelemetrySdk setupOpenTelemetryWithMetricsExporter() {
    MetricExporter metricExporter =
        GoogleCloudMetricExporter.createWithConfiguration(MetricConfiguration.builder().build());
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .registerMetricReader(
                PeriodicMetricReader.builder(metricExporter)
                    .setInterval(java.time.Duration.ofSeconds(10))
                    .build())
            .build();
    meter =
        sdkMeterProvider
            .meterBuilder(INSTRUMENTATION_SCOPE_NAME)
            .setInstrumentationVersion("server:1.0.0")
            .build();
    return OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).buildAndRegisterGlobal();
  }

  private static void doWork(LongCounter counter) {
    try {
      for (int i = 0; i < 10; i++) {
        counter.add(RANDOM.nextInt(100));
        Thread.sleep(RANDOM.nextInt(1000));
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void myUseCase() {
    LongCounter counter =
        meter
            .counterBuilder("example_counter")
            .setDescription("Processed jobs")
            .setUnit("1")
            .build();
    doWork(counter);
  }

  // To run this from command line, execute `gradle runMetricExporterWithOtel`
  public static void main(String[] args) {
    System.out.println("Starting the metrics-otel-example application");
    OpenTelemetrySdk openTelemetrySdk = setupOpenTelemetryWithMetricsExporter();

    myUseCase();
    myUseCase();
    myUseCase();

    System.out.println("Shutting down the metrics-otel-example application");

    CompletableResultCode shutdownResult = openTelemetrySdk.getSdkMeterProvider().shutdown();
    shutdownResult.join(10000, TimeUnit.MILLISECONDS);

    System.out.println("Shutdown complete");
  }
}
