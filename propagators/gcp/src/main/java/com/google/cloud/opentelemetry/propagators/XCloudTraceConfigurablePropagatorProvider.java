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
 * Autoconfigurable propagator for OpenTelemetry Java SDK that will make use of
 * X-Cloud-Trace-Context header.
 *
 * <p>Note: This is *NOT* the recommended propagation as X-Cloud-Trace-Context because the sampling
 * flag behaves subtly different from expectations in both w3c traceparent *and* opentelemetry
 * propagation.
 *
 * @see {@link OneWayXCloudTraceConfigurablePropagatorProvider}
 */
@AutoService(ConfigurablePropagatorProvider.class)
public class XCloudTraceConfigurablePropagatorProvider implements ConfigurablePropagatorProvider {
  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return new XCloudTraceContextPropagator(false);
  }

  @Override
  public String getName() {
    return "gcp";
  }
}
