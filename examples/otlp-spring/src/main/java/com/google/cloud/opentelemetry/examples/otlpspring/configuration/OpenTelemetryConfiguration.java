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
package com.google.cloud.opentelemetry.examples.otlpspring.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OpenTelemetryConfiguration {
  private final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfiguration.class);

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public OpenTelemetrySdk getOpenTelemetrySdk() {
    GoogleCredentials googleCredentials = getCredentials();
    AutoConfiguredOpenTelemetrySdk autoConfOTelSdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addSpanExporterCustomizer(
                (exporter, configProperties) ->
                    addAuthorizationHeaders(exporter, googleCredentials))
            .build();
    return autoConfOTelSdk.getOpenTelemetrySdk();
  }

  private GoogleCredentials getCredentials() {
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private SpanExporter addAuthorizationHeaders(
      SpanExporter exporter, GoogleCredentials credentials) {
    Map<String, String> authHeaders = new ConcurrentHashMap<>();
    if (exporter instanceof OtlpHttpSpanExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpHttpSpanExporterBuilder builder =
            ((OtlpHttpSpanExporter) exporter)
                .toBuilder()
                    .setHeaders(
                        () -> {
                          authHeaders.put("Authorization", refreshToken(credentials));
                          return authHeaders;
                        });
        return builder.build();
      } catch (IOException e) {
        logger.error("Error while adding headers : {}", e.getMessage());
        throw new RuntimeException(e);
      }
    } else if (exporter instanceof OtlpGrpcSpanExporter) {
      try {
        credentials.refreshIfExpired();
        OtlpGrpcSpanExporterBuilder builder =
            ((OtlpGrpcSpanExporter) exporter)
                .toBuilder()
                    .setHeaders(
                        () -> {
                          authHeaders.put("Authorization", refreshToken(credentials));
                          return authHeaders;
                        });
        return builder.build();
      } catch (IOException e) {
        logger.error("Error while adding headers: {}", e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return exporter;
  }

  private String refreshToken(GoogleCredentials credentials) {
    logger.info("Refreshing Google Credentials");
    try {
      logger.info(
          "Current access token expires at {}", credentials.getAccessToken().getExpirationTime());
      credentials.refreshIfExpired();
      logger.info("Credential refresh check complete");
      return String.format("Bearer %s", credentials.getAccessToken().getTokenValue());
    } catch (IOException e) {
      logger.error("Error while refreshing credentials: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
