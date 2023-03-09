/*
 * Copyright 2023 Google
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
package com.google.cloud.opentelemetry.metric;

import com.google.cloud.ServiceOptions;
import com.google.common.base.Suppliers;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudMetricExporter implements MetricExporter {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudMetricExporter.class);

  private final Supplier<MetricExporter> internalMetricExporterSupplier;

  private GoogleCloudMetricExporter(MetricConfiguration configuration) {
    this.internalMetricExporterSupplier =
        Suppliers.memoize(
            () -> {
              try {
                return InternalMetricExporter.createWithConfiguration(configuration);
              } catch (IOException e) {
                logger.warn(
                    "Unable to initialize GoogleCloudMetricExporter. Export operation failed, switching to NoopMetricExporter.",
                    e);
                return new NoopMetricExporter();
              }
            });
  }

  /**
   * Method that generates an instance of {@link GoogleCloudMetricExporter} using a minimally
   * configured {@link MetricConfiguration} object that requires no input from the user. Since no
   * project ID is specified, default project ID is used instead. See {@link
   * ServiceOptions#getDefaultProjectId()} for details.
   *
   * <p>This method defers the initialization of an actual {@link GoogleCloudMetricExporter} to a
   * point when it is actually needed - which is when the metrics need to be exported. As a result,
   * while this method does not throw any exception, an exception may still be thrown during the
   * attempt to generate the actual {@link GoogleCloudMetricExporter}.
   *
   * @return A configured instance of {@link GoogleCloudMetricExporter} as a {@link MetricExporter}
   *     which gets initialized lazily once {@link GoogleCloudMetricExporter#export(Collection)} is
   *     called.
   */
  public static MetricExporter createWithDefaultConfiguration() {
    return new GoogleCloudMetricExporter(MetricConfiguration.builder().build());
  }

  /**
   * Method that generates an instance of {@link GoogleCloudMetricExporter} using a {@link
   * MetricConfiguration} that allows the user to provide custom configuration for Traces.
   *
   * <p>This method defers the initialization of an actual {@link GoogleCloudMetricExporter} to a
   * point when it is actually needed - which is when the metrics need to be exported. As a result,
   * while this method does not throw any exception, an exception may still be thrown during the
   * attempt to generate the actual {@link GoogleCloudMetricExporter}.
   *
   * @param configuration The {@link MetricConfiguration} object that determines the user
   *     preferences for metrics.
   * @return An instance of {@link GoogleCloudMetricExporter} as a {@link MetricExporter} object.
   */
  public static MetricExporter createWithConfiguration(MetricConfiguration configuration) {
    return new GoogleCloudMetricExporter(configuration);
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<MetricData> metrics) {
    return this.internalMetricExporterSupplier.get().export(metrics);
  }

  @Override
  public CompletableResultCode flush() {
    return this.internalMetricExporterSupplier.get().flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return this.internalMetricExporterSupplier.get().shutdown();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(@Nonnull InstrumentType instrumentType) {
    return this.internalMetricExporterSupplier.get().getAggregationTemporality(instrumentType);
  }
}
