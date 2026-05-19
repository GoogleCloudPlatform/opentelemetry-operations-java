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
package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ConfigurableSpanExporterProvider.class)
@Deprecated
public class GoogleCloudSpanExporterFactory implements ConfigurableSpanExporterProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(GoogleCloudSpanExporterFactory.class);

  static {
    logger.warn(
        "Google Cloud OpenTelemetry Auto exporter for Java is deprecated and will be archived after September 30th, 2026. Please migrate to the OpenTelemetry OTLP exporters. For migration details, see https://github.com/GoogleCloudPlatform/opentelemetry-operations-java/blob/main/MIGRATION.md");
  }

  @Override
  public SpanExporter createExporter(ConfigProperties config) {
    return TraceExporter.createWithDefaultConfiguration();
  }

  @Override
  public String getName() {
    return Constants.CLOUD_TRACE_NAME;
  }
}
