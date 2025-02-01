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

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
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
    logger.info("Initializing Autoconfigured OpenTelemetry SDK");
    AutoConfiguredOpenTelemetrySdk autoConfOTelSdk = AutoConfiguredOpenTelemetrySdk.initialize();
    return autoConfOTelSdk.getOpenTelemetrySdk();
  }
}
