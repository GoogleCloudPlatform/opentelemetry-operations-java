package com.google.cloud.opentelemetry.metric;

import static java.time.Duration.ZERO;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.ServiceOptions;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Configurations for {@link MetricExporter}.
 */
@AutoValue
@Immutable
public abstract class MetricConfiguration {

  private static final String DEFAULT_PROJECT_ID =
      Strings.nullToEmpty(ServiceOptions.getDefaultProjectId());
  private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(10, 0);

  MetricConfiguration() {
  }

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

   * Returns a MetricsServiceStub instance used to make RPC calls.
   *
   * @return the metrics service stub.
   */
  @Nullable
  public abstract MetricServiceStub getMetricServiceStub();

  /**
   * Returns the deadline for exporting to Cloud Monitoring backend.
   *
   * <p>Default value is 10 seconds.
   *
   * @return the export deadline.
   */
  public abstract Duration getDeadline();

  public static Builder builder() {
    return new AutoValue_MetricConfiguration.Builder()
        .setProjectId(DEFAULT_PROJECT_ID)
        .setDeadline(DEFAULT_DEADLINE);
  }

  /**
   * Builder for {@link MetricConfiguration}.
   */
  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {
    }

    abstract String getProjectId();



    abstract Duration getDeadline();

    public abstract Builder setProjectId(String projectId);

    public abstract Builder setCredentials(Credentials newCredentials);



    public abstract Builder setMetricServiceStub(MetricServiceStub newMetricServiceStub);

    public abstract Builder setDeadline(Duration deadline);

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
