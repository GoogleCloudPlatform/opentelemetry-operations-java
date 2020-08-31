package com.google.cloud.opentelemetry.metric;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.ServiceOptions;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.stub.MetricServiceStub;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** Configurations for {@link MetricExporter}. */
@AutoValue
@Immutable
public abstract class MetricConfiguration {

    private static final String DEFAULT_PROJECT_ID =
            Strings.nullToEmpty(ServiceOptions.getDefaultProjectId());
    private static final boolean DEFAULT_ADD_UNIQUE_IDENTIFIER = false;

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
     * Returns the client.
     *
     * @return the client.
     */
    public abstract MetricServiceClient getClient();

    /**
     * Returns whether a unique identifier will be added.
     *
     * @return whether a unique identifier will be added.
     */
    public abstract boolean getAddUniqueIdentifier();

    /**
     * Returns a MetricsServiceStub instance used to make RPC calls.
     *
     * @return the metrics service stub.
     */
    @Nullable
    public abstract MetricServiceStub getMetricServiceStub();

    public static Builder builder() {
        return new AutoValue_MetricConfiguration.Builder();
    }

    /** Builder for {@link MetricConfiguration}. */
    @AutoValue.Builder
    public abstract static class Builder {

        Builder() {}

        abstract String getProjectId();

        abstract MetricServiceClient getClient();

        abstract boolean getAddUniqueIdentifier();

        abstract MetricConfiguration autoBuild();

        public abstract Builder setProjectId(String newProjectId);

        public abstract Builder setClient(MetricServiceClient newClient);

        /**
         * Builds a {@link MetricConfiguration}.
         *
         * @return a {@code MetricsConfiguration}.
         */
        public MetricConfiguration build() {
            Preconditions.checkArgument(
                    !Strings.isNullOrEmpty(getProjectId()),
                    "Cannot find a project ID from either configurations or application default.");

            return autoBuild();
        }
    }

}
