/*
 * Copyright 2025 Google LLC
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
package com.google.cloud.opentelemetry.example.otlpmetric;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OTLPMetricExample {
  private static final String INSTRUMENTATION_SCOPE_NAME = OTLPMetricExample.class.getName();
  private static final Random RANDOM = new Random();

  private static OpenTelemetrySdk openTelemetrySdk;

  private static OpenTelemetrySdk setupMetricExporter() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

    // Update the SDK configured using environment variables and system properties
    // TODO: Replace this with the use of gcp-auth-extension
    AutoConfiguredOpenTelemetrySdk autoConfOTelSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addMetricExporterCustomizer(
                (exporter, configProperties) -> addAuthorizationHeaders(exporter, credentials))
            .build();
    return autoConfOTelSdk.getOpenTelemetrySdk();
  }

  // Modifies the metric exporter initially auto-configured using environment variables
  // Note: This adds static authorization headers which are set only at initialization time.
  // This will stop working after the token expires, since the token is not refreshed.
  private static MetricExporter addAuthorizationHeaders(
      MetricExporter exporter, GoogleCredentials credentials) {
    if (exporter instanceof OtlpHttpMetricExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpHttpMetricExporterBuilder builder =
            ((OtlpHttpMetricExporter) exporter)
                .toBuilder()
                    .addHeader(
                        "Authorization", "Bearer " + credentials.getAccessToken().getTokenValue());

        if (credentials.getQuotaProjectId() != null) {
          builder.addHeader("x-goog-user-project", credentials.getQuotaProjectId());
        }
        return builder.build();
      } catch (IOException e) {
        System.out.println("error:" + e.getMessage());
      }
    } else if (exporter instanceof OtlpGrpcMetricExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpGrpcMetricExporterBuilder builder =
            ((OtlpGrpcMetricExporter) exporter)
                .toBuilder()
                    .addHeader(
                        "Authorization", "Bearer " + credentials.getAccessToken().getTokenValue());

        if (credentials.getQuotaProjectId() != null) {
          builder.addHeader("x-goog-user-project", credentials.getQuotaProjectId());
        }
        return builder.build();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return exporter;
  }

  private static void myUseCase() {
    LongCounter counter =
        openTelemetrySdk
            .getMeter(INSTRUMENTATION_SCOPE_NAME)
            .counterBuilder("example_counter")
            .setDescription("Processed jobs")
            .setUnit("1")
            .build();
    doWork(counter);
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

  public static void main(String[] args) throws IOException {
    // Configure the OpenTelemetry pipeline with CloudMetric exporter
    openTelemetrySdk = setupMetricExporter();

    // Application-specific logic
    myUseCase();

    // Flush all buffered metrics
    CompletableResultCode completableResultCode = openTelemetrySdk.getSdkMeterProvider().shutdown();
    // wait till export finishes
    completableResultCode.join(10000, TimeUnit.MILLISECONDS);
  }
}
