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
package com.google.cloud.opentelemetry.examples.otlpspring;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

  static OpenTelemetrySdk openTelemetrySdk;
  private static GoogleCredentials googleCredentials;

  public static void main(String[] args) throws IOException {
    System.out.println("Starting OTLP with Spring Boot and Google Auth");
    googleCredentials = GoogleCredentials.getApplicationDefault();
    openTelemetrySdk = setupOpenTelemetry();
    SpringApplication.run(Main.class, args);
    flushBufferedTraces();
  }

  private static OpenTelemetrySdk setupOpenTelemetry() {
    AutoConfiguredOpenTelemetrySdk autoConfOTelSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addSpanExporterCustomizer(
                (exporter, configProperties) ->
                    addAuthorizationHeaders(exporter, googleCredentials))
            .build();
    return autoConfOTelSdk.getOpenTelemetrySdk();
  }

  private static SpanExporter addAuthorizationHeaders(
      SpanExporter exporter, GoogleCredentials credentials) {
    Map<String, String> authHeaders = new ConcurrentHashMap<>();
    authHeaders.put("Authorization", refreshToken(credentials));
    if (exporter instanceof OtlpHttpSpanExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpHttpSpanExporterBuilder builder =
            ((OtlpHttpSpanExporter) exporter).toBuilder().setHeaders(() -> authHeaders);
        return builder.build();
      } catch (IOException e) {
        System.out.println("error:" + e.getMessage());
        throw new RuntimeException(e);
      }
    } else if (exporter instanceof OtlpGrpcSpanExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpGrpcSpanExporterBuilder builder =
            ((OtlpGrpcSpanExporter) exporter).toBuilder().setHeaders(() -> authHeaders);
        return builder.build();
      } catch (IOException e) {
        System.out.println("error:" + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return exporter;
  }

  // Flush all buffered traces
  private static void flushBufferedTraces() {
    CompletableResultCode completableResultCode =
        openTelemetrySdk.getSdkTracerProvider().shutdown();
    // wait till export finishes
    completableResultCode.join(10000, TimeUnit.MILLISECONDS);
  }

  private static String refreshToken(GoogleCredentials credentials) {
    System.out.println("Refreshing Google Credentials");
    try {
      credentials.refreshIfExpired();
      return String.format("Bearer %s", credentials.getAccessToken().getTokenValue());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
