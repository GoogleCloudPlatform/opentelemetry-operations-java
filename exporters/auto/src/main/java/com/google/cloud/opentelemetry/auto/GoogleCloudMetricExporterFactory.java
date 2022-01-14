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
package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.metric.MetricExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import java.io.IOException;

@AutoService(ConfigurableMetricExporterProvider.class)
public class GoogleCloudMetricExporterFactory implements ConfigurableMetricExporterProvider {
  @Override
  public MetricExporter createExporter(ConfigProperties config) {
    try {
      return MetricExporter.createWithDefaultConfiguration();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String getName() {
    return Constants.CLOUD_MONITORING_NAME;
  }
}
