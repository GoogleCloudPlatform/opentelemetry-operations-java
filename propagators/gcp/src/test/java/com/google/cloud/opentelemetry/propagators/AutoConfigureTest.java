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
package com.google.cloud.opentelemetry.propagators;

import static org.junit.Assert.assertTrue;

import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AutoConfigureTest {

  @Test
  public void findsWithServiceLoader() {
    ServiceLoader<ConfigurablePropagatorProvider> services =
        ServiceLoader.load(ConfigurablePropagatorProvider.class, getClass().getClassLoader());
    assertTrue(
        "Could not load gcp_oneway propagator using serviceloader, found: " + services,
        services.stream()
            .anyMatch(
                provider ->
                    provider.type().equals(OneWayXCloudTraceConfigurablePropagatorProvider.class)));

    assertTrue(
        "Could not load gcp propagator using serviceloader, found: " + services,
        services.stream()
            .anyMatch(
                provider ->
                    provider.type().equals(XCloudTraceConfigurablePropagatorProvider.class)));
  }

  @Test
  public void findsWithAutoConfigure() {
    assertTrue(
        findsWithAutoConfigure("oneway-gcp").getTextMapPropagator()
            instanceof XCloudTraceContextPropagator);
    assertTrue(
        findsWithAutoConfigure("gcp").getTextMapPropagator()
            instanceof XCloudTraceContextPropagator);
  }

  private static ContextPropagators findsWithAutoConfigure(String propagator) {
    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .disableShutdownHook()
            .addPropertiesSupplier(
                () ->
                    Map.of(
                        "otel.propagators",
                        propagator,
                        "otel.traces.exporter",
                        "none",
                        "otel.metrics.exporter",
                        "none",
                        "otel.logs.exporter",
                        "none"))
            .build();
    return sdk.getOpenTelemetrySdk().getPropagators();
  }
}
