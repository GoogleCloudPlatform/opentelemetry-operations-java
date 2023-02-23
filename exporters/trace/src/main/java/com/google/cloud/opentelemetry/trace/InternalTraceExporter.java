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
package com.google.cloud.opentelemetry.trace;

import static com.google.api.client.util.Preconditions.checkNotNull;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.cloud.trace.v2.TraceServiceSettings;
import com.google.cloud.trace.v2.stub.TraceServiceStub;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates internal implementation details for exporting spans for applications
 * running on Google cloud environment. This class should not be exposed to the end user for direct
 * usage.
 */
class InternalTraceExporter implements SpanExporter {

  private final CloudTraceClient cloudTraceClient;
  private final ProjectName projectName;
  private final String projectId;
  private final TraceTranslator translator;

  private static final Map<String, String> HEADERS =
      Map.of("User-Agent", "opentelemetry-operations-java/" + TraceVersions.EXPORTER_VERSION);
  private static final HeaderProvider HEADER_PROVIDER = () -> HEADERS;

  private static InternalTraceExporter createWithClient(
      String projectId,
      CloudTraceClient cloudTraceClient,
      ImmutableMap<String, String> attributeMappings,
      Map<String, AttributeValue> fixedAttributes) {
    return new InternalTraceExporter(
        projectId, cloudTraceClient, attributeMappings, fixedAttributes);
  }

  static SpanExporter createWithConfiguration(TraceConfiguration configuration) throws IOException {
    String projectId = configuration.getProjectIdProvider().get();
    TraceServiceStub stub = configuration.getTraceServiceStub();

    // TODO: Remove stub - tracked in issue #198
    if (stub == null) {
      TraceServiceSettings.Builder builder = TraceServiceSettings.newBuilder();

      // We only use the batchWriteSpans API in this exporter.
      builder
          .batchWriteSpansSettings()
          .setSimpleTimeoutNoRetries(
              org.threeten.bp.Duration.ofMillis(configuration.getDeadline().toMillis()));
      // For testing, we need to hack around our gRPC config.
      if (configuration.getInsecureEndpoint()) {
        builder.setCredentialsProvider(NoCredentialsProvider.create());
        builder.setTransportChannelProvider(
            FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                    ManagedChannelBuilder.forTarget(configuration.getTraceServiceEndpoint())
                        .usePlaintext()
                        .build())));
      } else {
        Credentials credentials =
            configuration.getCredentials() == null
                ? GoogleCredentials.getApplicationDefault()
                : configuration.getCredentials();
        builder.setCredentialsProvider(
            FixedCredentialsProvider.create(checkNotNull(credentials, "credentials")));
        builder.setEndpoint(configuration.getTraceServiceEndpoint());
        builder.setHeaderProvider(HEADER_PROVIDER);
      }

      return new InternalTraceExporter(
          projectId,
          new CloudTraceClientImpl(TraceServiceClient.create(builder.build())),
          configuration.getAttributeMapping(),
          configuration.getFixedAttributes());
    }
    return InternalTraceExporter.createWithClient(
        projectId,
        new CloudTraceClientImpl(TraceServiceClient.create(stub)),
        configuration.getAttributeMapping(),
        configuration.getFixedAttributes());
  }

  /**
   * A noop implementation of {@link TraceExporter}. Should be used when {@link TraceExporter}
   * cannot be initialized due to some error/exception.
   *
   * @return noop implementation of {@link TraceExporter} as a {@link SpanExporter}.
   */
  static SpanExporter noop() {
    return NoopTraceExporter.getNoopTraceExporter();
  }

  InternalTraceExporter(
      String projectId,
      CloudTraceClient cloudTraceClient,
      ImmutableMap<String, String> attributeMappings,
      Map<String, AttributeValue> fixedAttributes) {
    this.projectId = projectId;
    this.cloudTraceClient = cloudTraceClient;
    this.projectName = ProjectName.of(projectId);
    this.translator = new TraceTranslator(attributeMappings, fixedAttributes);
  }

  @Override
  public CompletableResultCode flush() {
    // We do no exporter buffering of spans, so we're always flushed.
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spanDataList) {
    List<Span> spans = new ArrayList<>(spanDataList.size());
    for (SpanData spanData : spanDataList) {
      spans.add(translator.generateSpan(spanData, projectId));
    }

    cloudTraceClient.batchWriteSpans(projectName, spans);
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    this.cloudTraceClient.shutdown();
    return CompletableResultCode.ofSuccess();
  }
}
