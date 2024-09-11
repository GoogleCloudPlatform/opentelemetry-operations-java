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
package com.google.cloud.opentelemetry.example.spring;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfiguration {

  private static final String INSTRUMENTATION_SCOPE_NAME = "spring-demo";
  private static final String INSTRUMENTATION_VERSION = "semver:1.0.0";

  @Bean
  public OpenTelemetrySdk getOpenTelemetrySdk() {
    return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  }

  @Bean
  public Tracer getTracer(OpenTelemetrySdk openTelemetrySdk) {
    return openTelemetrySdk
        .tracerBuilder(INSTRUMENTATION_SCOPE_NAME)
        .setInstrumentationVersion(INSTRUMENTATION_VERSION)
        .build();
  }

  @Bean
  public Meter getMeter(OpenTelemetrySdk openTelemetrySdk) {
    return openTelemetrySdk
        .meterBuilder(INSTRUMENTATION_SCOPE_NAME)
        .setInstrumentationVersion(INSTRUMENTATION_VERSION)
        .build();
  }
}
