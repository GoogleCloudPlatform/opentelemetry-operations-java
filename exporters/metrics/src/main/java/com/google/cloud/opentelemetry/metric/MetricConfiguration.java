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
package com.google.cloud.opentelemetry.metric;

import static java.time.Duration.ZERO;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.ServiceOptions;
import com.google.cloud.monitoring.v3.stub.MetricServiceStubSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Configurations for {@link MetricExporter}.
 *
 * <p>See {@link #builder()} for usage.
 */
@AutoValue
@Immutable
public abstract class MetricConfiguration {

  private static final String DEFAULT_PROJECT_ID =
      Strings.nullToEmpty(ServiceOptions.getDefaultProjectId());
  private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(10, 0);

  MetricConfiguration() {}

  /**
   * Returns the {@link Credentials}.
   *
   * @return the {@code Credentials}.
   */
  @Nullable
  public abstract Credentials getCredentials();

  /**
   * Returns the cloud project id.
   *
   * @return the cloud project id.
   */
  public abstract String getProjectId();

  /**
   * Returns the deadline for exporting to Cloud Monitoring backend.
   *
   * <p>Default value is 10 seconds.
   *
   * @return the export deadline.
   */
  public abstract Duration getDeadline();

  /**
   * Returns the strategy for how to send metric descriptors to Cloud Monitoring.
   *
   * <p>The Default is to only send descriptors once per process/classloader.
   *
   * @return thhe configured strategy.
   */
  public abstract MetricDescriptorStrategy getDescriptorStrategy();

  /**
   * Returns the endpoint where to write metrics.
   *
   * <p>The default is monitoring.googleapis.com:443
   */
  @Nullable
  public abstract String getMetricServiceEndpoint();

  @VisibleForTesting
  abstract boolean getInsecureEndpoint();

  /**
   * Constructs a {@link MetricConfiguration.Builder} with default values.
   *
   * <p>This will construct a builder with the following default configuration:
   *
   * <ul>
   *   <li>Project ID will be discovered/derived from the environment
   *   <li>Metric export deadline will 10 seconds
   *   <li>Metric descriptors will only be sent once for the lifetime of the exporter
   * </ul>
   *
   * @return the configuration builder.
   */
  public static Builder builder() {
    return new AutoValue_MetricConfiguration.Builder()
        .setProjectId(DEFAULT_PROJECT_ID)
        .setDeadline(DEFAULT_DEADLINE)
        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
        .setInsecureEndpoint(false)
        .setMetricServiceEndpoint(MetricServiceStubSettings.getDefaultEndpoint());
  }

  /** Builder for {@link MetricConfiguration}. */
  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {}

    abstract String getProjectId();

    abstract Duration getDeadline();

    /** Set the GCP project where metrics should be writtten. */
    public abstract Builder setProjectId(String projectId);

    /** Set the credentials to use when writing metrics. */
    public abstract Builder setCredentials(Credentials newCredentials);

    /** Set the deadline for exporting batches of metric timeseries. */
    public abstract Builder setDeadline(Duration deadline);

    /** Set the policy for sending metric descriptors, e.g. always, never or once. */
    public abstract Builder setDescriptorStrategy(MetricDescriptorStrategy strategy);

    /** Sets the endpoint where to write Metrics. Defaults to monitoring.googleapis.com:443. */
    public abstract Builder setMetricServiceEndpoint(String endpoint);

    @VisibleForTesting
    abstract Builder setInsecureEndpoint(boolean value);

    abstract MetricConfiguration autoBuild();

    /**
     * Builds a {@link MetricConfiguration}.
     *
     * @return a {@code MetricsConfiguration}.
     */
    public MetricConfiguration build() {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(getProjectId()),
          "Cannot find a project ID from either configurations or application default.");
      Preconditions.checkArgument(getDeadline().compareTo(ZERO) > 0, "Deadline must be positive.");
      return autoBuild();
    }
  }
}
