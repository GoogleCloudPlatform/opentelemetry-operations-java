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
package com.google.cloud.opentelemetry.metric;

import static java.time.Duration.ZERO;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.ServiceOptions;
import com.google.cloud.monitoring.v3.stub.MetricServiceStubSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.ResourceAttributes;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Configurations for {@link GoogleCloudMetricExporter}.
 *
 * <p>See {@link #builder()} for usage.
 */
@AutoValue
@Immutable
public abstract class MetricConfiguration {
  /** Resource attribute filter that disables addition of resource attributes to metric labels. */
  public static final Predicate<AttributeKey<?>> NO_RESOURCE_ATTRIBUTES = attributeKey -> false;

  public static final MonitoredResourceDescription DEFAULT_MONITORED_RESOURCE_DESCRIPTION =
      new MonitoredResourceDescription("", Collections.emptySet());

  /**
   * Default resource attribute filter that adds recommended resource attributes to metric labels.
   */
  public static final Predicate<AttributeKey<?>> DEFAULT_RESOURCE_ATTRIBUTES_FILTER =
      attributeKey ->
          (attributeKey.equals(ResourceAttributes.SERVICE_NAME)
                  || attributeKey.equals(ResourceAttributes.SERVICE_NAMESPACE)
                  || attributeKey.equals(ResourceAttributes.SERVICE_INSTANCE_ID))
              && !attributeKey.getKey().isEmpty();

  static final String DEFAULT_PREFIX = "workload.googleapis.com";

  private static final Duration DEFAULT_DEADLINE =
      Duration.ofSeconds(12, 0); // Consistent with Cloud Monitoring's timeout

  MetricConfiguration() {}

  /**
   * Package private method to get a {@link Supplier} for the project ID. The value supplied depends
   * on the user-provided value via {@link MetricConfiguration.Builder#setProjectId(String)}. If
   * user does not provide a project ID via {@link
   * MetricConfiguration.Builder#setProjectId(String)}, this method returns a {@link Supplier} that
   * supplies the default Project ID.
   *
   * @see ServiceOptions#getDefaultProjectId()
   * @return a {@link Supplier} for the GCP project ID.
   */
  abstract Supplier<String> getProjectIdSupplier();

  /**
   * Returns the {@link Credentials}.
   *
   * <p>Defaults to the application default credential's project.
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
  public final String getProjectId() {
    return getProjectIdSupplier().get();
  }

  /**
   * Returns the prefix prepended to metric names.
   *
   * @see <a href="https://cloud.google.com/monitoring/custom-metrics#identifier">Custom Metrics
   *     Identifiers</a>
   *     <p>Defaults to workload.googleapis.com.
   * @return the prefix to attach to metrics.
   */
  public abstract String getPrefix();

  /**
   * Returns the deadline for exporting to Cloud Monitoring backend.
   *
   * <p>Default value is {{@link MetricConfiguration#DEFAULT_DEADLINE}.
   *
   * @return the export deadline.
   */
  public abstract Duration getDeadline();

  /**
   * Returns the strategy for how to send metric descriptors to Cloud Monitoring.
   *
   * <p>The Default is to only send descriptors once per process/classloader.
   *
   * @return the configured strategy.
   */
  public abstract MetricDescriptorStrategy getDescriptorStrategy();

  /**
   * Returns the endpoint where to write metrics.
   *
   * <p>The default is monitoring.googleapis.com:443
   */
  @Nullable
  public abstract String getMetricServiceEndpoint();

  /**
   * Returns the {@link Predicate} based filter that determines which resource attributes to add as
   * metric labels.
   *
   * <p>The default filter adds {@link ResourceAttributes#SERVICE_NAME}, {@link
   * ResourceAttributes#SERVICE_NAMESPACE}, and {@link ResourceAttributes#SERVICE_INSTANCE_ID} as
   * metric labels.
   *
   * @return a {@link Predicate} that acts as a resource attribute filter.
   * @see Builder#setResourceAttributesFilter(Predicate) for details.
   */
  public abstract Predicate<AttributeKey<?>> getResourceAttributesFilter();

  /**
   * Returns a boolean indicating if the {@link MetricConfiguration} is configured to write to a
   * metric generated from a Google Cloud Service.
   *
   * @return true if the {@link MetricConfiguration} is configured to write to a metric generated
   *     from a Google Cloud Service, false otherwise.
   */
  public abstract boolean getUseServiceTimeSeries();

  /**
   * Returns the custom {@link MonitoredResourceDescription} that is used to map the OpenTelemetry
   * {@link io.opentelemetry.sdk.resources.Resource} to Google specific {@link
   * com.google.api.MonitoredResource}.
   *
   * <p>This returns the {@link MetricConfiguration#DEFAULT_MONITORED_RESOURCE_DESCRIPTION} if not
   * set through exporter configuration.
   *
   * @return The {@link MonitoredResourceDescription} object containing the MonitoredResource type
   *     and its expected labels.
   */
  public abstract MonitoredResourceDescription getMonitoredResourceDescription();

  @VisibleForTesting
  abstract boolean getInsecureEndpoint();

  /**
   * Constructs a {@link Builder} with default values.
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
        .setProjectIdSupplier(Suppliers.memoize(ServiceOptions::getDefaultProjectId))
        .setPrefix(DEFAULT_PREFIX)
        .setDeadline(DEFAULT_DEADLINE)
        .setDescriptorStrategy(MetricDescriptorStrategy.SEND_ONCE)
        .setInsecureEndpoint(false)
        .setUseServiceTimeSeries(false)
        .setResourceAttributesFilter(DEFAULT_RESOURCE_ATTRIBUTES_FILTER)
        .setMonitoredResourceDescription(DEFAULT_MONITORED_RESOURCE_DESCRIPTION)
        .setMetricServiceEndpoint(MetricServiceStubSettings.getDefaultEndpoint());
  }

  /** Builder for {@link MetricConfiguration}. */
  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {}

    abstract Duration getDeadline();

    /**
     * Package private method to set the {@link Supplier} that supplies the project ID. The project
     * ID value that is supplied depends on the value set using {@link
     * MetricConfiguration.Builder#setProjectId(String)}}.
     *
     * @param projectIdSupplier the cloud project id supplier.
     * @return this.
     */
    abstract Builder setProjectIdSupplier(Supplier<String> projectIdSupplier);

    /**
     * Sets the GCP project id where the metrics should be written. The project ID should be a
     * valid, non-null and non-empty String.
     *
     * @param projectId the cloud project id.
     * @return this.
     */
    public final Builder setProjectId(String projectId) {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(projectId), "Project ID cannot be null or empty.");
      setProjectIdSupplier(() -> projectId);
      return this;
    }

    /** Set the prefix prepended to metric names. */
    public abstract Builder setPrefix(String prefix);

    /** Set the credentials to use when writing metrics. */
    public abstract Builder setCredentials(Credentials newCredentials);

    /** Set the deadline for exporting batches of metric timeseries. */
    public abstract Builder setDeadline(Duration deadline);

    /** Set the policy for sending metric descriptors, e.g. always, never or once. */
    public abstract Builder setDescriptorStrategy(MetricDescriptorStrategy strategy);

    /** Sets the endpoint where to write Metrics. Defaults to monitoring.googleapis.com:443. */
    public abstract Builder setMetricServiceEndpoint(String endpoint);

    /**
     * Sets the {@link MetricConfiguration} to configure the exporter to write metrics via {@link
     * com.google.cloud.monitoring.v3.MetricServiceClient#createServiceTimeSeries(String, List)}
     * method. By default, this is false.
     *
     * @param useServiceTimeSeries a boolean indicating whether to use {@link
     *     com.google.cloud.monitoring.v3.MetricServiceClient#createServiceTimeSeries(String, List)}
     *     method for writing metrics to Google Cloud Monitoring.
     * @return this
     */
    public abstract Builder setUseServiceTimeSeries(boolean useServiceTimeSeries);

    /**
     * Sets the {@link MonitoredResourceDescription} that is used to map OpenTelemetry {@link
     * io.opentelemetry.sdk.resources.Resource}s to Google specific {@link
     * com.google.api.MonitoredResource}s.
     *
     * @param monitoredResourceDescription the {@link MonitoredResourceDescription} object
     *     responsible for providing mapping between the custom {@link
     *     com.google.api.MonitoredResource} and the expected labels.
     * @return this.
     */
    public abstract Builder setMonitoredResourceDescription(
        MonitoredResourceDescription monitoredResourceDescription);

    /**
     * Set a filter to determine which resource attributes to add to metrics as metric labels. By
     * default, it adds service.name, service.namespace, and service.instance.id. This is
     * recommended to avoid writing duplicate timeseries against the same monitored resource. Use
     * setResourceAttributesFilter(NO_RESOURCE_ATTRIBUTES) to disable the addition of resource
     * attributes to metric labels.
     *
     * @param filter A {@link Predicate} that determines if a resource attribute would be added as a
     *     metric label
     * @return this.
     */
    public abstract Builder setResourceAttributesFilter(Predicate<AttributeKey<?>> filter);

    @VisibleForTesting
    abstract Builder setInsecureEndpoint(boolean value);

    abstract MetricConfiguration autoBuild();

    /**
     * Builds a {@link MetricConfiguration}.
     *
     * @return a {@code MetricsConfiguration}.
     */
    public MetricConfiguration build() {
      Preconditions.checkArgument(getDeadline().compareTo(ZERO) > 0, "Deadline must be positive.");
      return autoBuild();
    }
  }
}
