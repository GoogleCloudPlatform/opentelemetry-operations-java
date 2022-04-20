/*
 * Copyright 2022 Google
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

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

/**
 * Autoconfigurable propagator for OpenTelemetry Java SDK that only attaches to existing
 * X-Cloud-Trace-Context traces and does not create downstream ones.
 *
 * <p>Note: This is the preferred mechanism of propagation as X-Cloud-Trace-Context sampling flag
 * behaves subtly different from expectations in both w3c traceparent *and* opentelemetry
 * propagation.
 */
@AutoService(ConfigurablePropagatorProvider.class)
public class OneWayXCloudTraceConfigurablePropagatorProvider
    implements ConfigurablePropagatorProvider {
  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return new XCloudTraceContextPropagator(true);
  }

  @Override
  public String getName() {
    return "oneway-gcp";
  }
}
