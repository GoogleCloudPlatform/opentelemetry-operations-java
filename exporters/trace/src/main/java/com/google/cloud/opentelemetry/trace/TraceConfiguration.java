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
package com.google.cloud.opentelemetry.trace;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.cloud.ServiceOptions;
import com.google.cloud.trace.v2.stub.TraceServiceStub;
import com.google.cloud.trace.v2.stub.TraceServiceStubSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/** Configurations for {@link TraceExporter}. */
@AutoValue
@Immutable
public abstract class TraceConfiguration {

  private static final String DEFAULT_PROJECT_ID =
      Strings.nullToEmpty(ServiceOptions.getDefaultProjectId());

  @VisibleForTesting static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(10, 0);

  @VisibleForTesting
  static final ImmutableMap<String, String> DEFAULT_ATTRIBUTE_MAPPING =
      ImmutableMap.<String, String>builder()
          .put("http.host", "/http/host")
          .put("http.method", "/http/method")
          .put("http.target", "/http/path")
          .put("http.status_code", "/http/status_code")
          .put("http.url", "/http/url")
          .put("http.request_content_length", "/http/request/size")
          .put("http.response_content_length", "/http/response/size")
          .put("http.scheme", "/http/client_protocol")
          .put("http.route", "/http/route")
          .put("http.user_agent", "/http/user_agent")
          .put("exception.type", "/error/name")
          .put("exception.message", "/error/message")
          .put("thread.id", "/tid")
          .build();

  TraceConfiguration() {}

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
   * Returns a TraceServiceStub instance used to make RPC calls.
   *
   * @return the trace service stub.
   */
  @Nullable
  @Deprecated
  public abstract TraceServiceStub getTraceServiceStub();

  /**
   * Returns the endpoint where to write traces.
   *
   * <p>The default is tracing.googleapis.com:443
   */
  @Nullable
  public abstract String getTraceServiceEndpoint();

  /**
   * Returns a map of attributes that is added to all the exported spans.
   *
   * @return the map of attributes that is added to all the exported spans.
   */
  public abstract Map<String, AttributeValue> getFixedAttributes();

  /**
   * Returns a map of attribute renames that will be applied to all attributes of exported spans.
   *
   * @return A map of OTEL name to GCP name for spans.
   */
  public abstract ImmutableMap<String, String> getAttributeMapping();

  /**
   * Returns the deadline for exporting to Trace backend.
   *
   * <p>Default value is 10 seconds.
   *
   * @return the export deadline.
   */
  public abstract Duration getDeadline();

  @VisibleForTesting
  abstract boolean getInsecureEndpoint();

  /**
   * Returns a new {@link Builder}.
   *
   * @return a {@code Builder}.
   */
  public static Builder builder() {
    return new AutoValue_TraceConfiguration.Builder()
        .setProjectId(DEFAULT_PROJECT_ID)
        .setFixedAttributes(Collections.emptyMap())
        .setDeadline(DEFAULT_DEADLINE)
        .setTraceServiceEndpoint(TraceServiceStubSettings.getDefaultEndpoint())
        .setInsecureEndpoint(false)
        .setAttributeMapping(DEFAULT_ATTRIBUTE_MAPPING);
  }

  /** Builder for {@link TraceConfiguration}. */
  @AutoValue.Builder
  public abstract static class Builder {

    @VisibleForTesting static final Duration ZERO = Duration.ZERO;

    Builder() {}

    /**
     * Sets the {@link Credentials} used to authenticate API calls.
     *
     * @param credentials the {@code Credentials}.
     * @return this.
     */
    public abstract Builder setCredentials(Credentials credentials);

    /**
     * Sets the cloud project id.
     *
     * @param projectId the cloud project id.
     * @return this.
     */
    public abstract Builder setProjectId(String projectId);

    /**
     * Sets the trace service stub used to send gRPC calls.
     *
     * @deprecated("Use setTraceServiceEndpoint")
     * @param traceServiceStub the {@code TraceServiceStub}.
     * @return this.
     */
    @Deprecated
    public abstract Builder setTraceServiceStub(TraceServiceStub traceServiceStub);

    /** Sets the endpoint where to write traces. Defaults to tracing.googleapis.com:443. */
    public abstract Builder setTraceServiceEndpoint(String endpoint);

    /**
     * Sets the map of attributes that is added to all the exported spans.
     *
     * @param fixedAttributes the map of attributes that is added to all the exported spans.
     * @return this.
     */
    public abstract Builder setFixedAttributes(Map<String, AttributeValue> fixedAttributes);

    /**
     * Sets the map of attribute keys that will be renamed.
     *
     * @param attributeMapping the map of attribute OTEL key to GCP attribute name.
     * @return this.
     */
    public abstract Builder setAttributeMapping(ImmutableMap<String, String> attributeMapping);

    /** Returns the builder for the attribute mapping. */
    public abstract ImmutableMap.Builder<String, String> attributeMappingBuilder();

    /**
     * Adds an attribute mapping that replaces a key in OTEL with the given key name for GCP.
     *
     * @param otelKey the attribute name from OTEL.
     * @param gcpKey the attribute name to use in GCP
     * @return this.
     */
    public final Builder addAttributeMapping(String otelKey, String gcpKey) {
      attributeMappingBuilder().put(otelKey, gcpKey);
      return this;
    }

    /**
     * Sets the deadline for exporting to Trace backend.
     *
     * <p>If both {@code TraceServiceStub} and {@code Deadline} are set, {@code TraceServiceStub}
     * takes precedence and {@code Deadline} will not be respected.
     *
     * @param deadline the export deadline.
     * @return this
     */
    public abstract Builder setDeadline(Duration deadline);

    abstract String getProjectId();

    abstract Map<String, AttributeValue> getFixedAttributes();

    abstract Duration getDeadline();

    @VisibleForTesting
    abstract Builder setInsecureEndpoint(boolean value);

    abstract TraceConfiguration autoBuild();

    /**
     * Builds a {@link TraceConfiguration}.
     *
     * @return a {@code TraceConfiguration}.
     */
    public TraceConfiguration build() {
      // Make a defensive copy of fixed attributes.
      setFixedAttributes(Collections.unmodifiableMap(new LinkedHashMap<>(getFixedAttributes())));
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(getProjectId()),
          "Cannot find a project ID from either configurations or application default.");
      for (Map.Entry<String, AttributeValue> fixedAttribute : getFixedAttributes().entrySet()) {
        Preconditions.checkNotNull(fixedAttribute.getKey(), "attribute key");
        Preconditions.checkNotNull(fixedAttribute.getValue(), "attribute value");
      }
      Preconditions.checkArgument(getDeadline().compareTo(ZERO) > 0, "Deadline must be positive.");
      return autoBuild();
    }
  }
}
