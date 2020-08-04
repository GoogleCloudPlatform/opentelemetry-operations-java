package com.google.cloud.opentelemetry.trace;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.cloud.trace.v2.TraceServiceSettings;
import com.google.cloud.trace.v2.stub.TraceServiceStub;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.api.client.util.Preconditions.checkNotNull;

public class TraceExporter implements SpanExporter {

  private final TraceServiceClient traceServiceClient;
  private final ProjectName projectName;
  private final String projectId;
  private final Map<String, AttributeValue> fixedAttributes;

  public static TraceExporter createWithDefaultConfiguration() throws IOException {
    TraceConfiguration configuration = TraceConfiguration.builder().build();
    return TraceExporter.createWithConfiguration(configuration);
  }

  public static TraceExporter createWithConfiguration(TraceConfiguration configuration)
      throws IOException {
    String projectId = configuration.getProjectId();
    TraceServiceStub stub = configuration.getTraceServiceStub();

    if (stub == null) {
      Credentials credentials =
          configuration.getCredentials() == null
              ? GoogleCredentials.getApplicationDefault()
              : configuration.getCredentials();

      return TraceExporter.createWithCredentials(
          projectId, credentials, configuration.getFixedAttributes(), configuration.getDeadline());
    }
    return TraceExporter.createWithClient(
        projectId, TraceServiceClient.create(stub), configuration.getFixedAttributes());
  }

  private static TraceExporter createWithClient(
      String projectId,
      TraceServiceClient traceServiceClient,
      Map<String, AttributeValue> fixedAttributes) {
    return new TraceExporter(projectId, traceServiceClient, fixedAttributes);
  }

  private static TraceExporter createWithCredentials(
      String projectId,
      Credentials credentials,
      Map<String, AttributeValue> fixedAttributes,
      Duration deadline)
      throws IOException {
    TraceServiceSettings.Builder builder =
        TraceServiceSettings.newBuilder()
            .setCredentialsProvider(
                FixedCredentialsProvider.create(checkNotNull(credentials, "credentials")));
    // We only use the batchWriteSpans API in this exporter.
    builder
        .batchWriteSpansSettings()
        .setSimpleTimeoutNoRetries(org.threeten.bp.Duration.ofMillis(deadline.toMillis()));
    return new TraceExporter(
        projectId, TraceServiceClient.create(builder.build()), fixedAttributes);
  }

  TraceExporter(
      String projectId,
      TraceServiceClient traceServiceClient,
      Map<String, AttributeValue> fixedAttributes) {
    this.projectId = projectId;
    this.traceServiceClient = traceServiceClient;
    this.projectName = ProjectName.of(projectId);
    this.fixedAttributes = fixedAttributes;
  }

  // TODO @imnoahcook add support for flush
  @Override
  public ResultCode flush() {
    return ResultCode.FAILURE;
  }

  @Override
  public ResultCode export(Collection<SpanData> spanDataList) {
    List<Span> spans = new ArrayList<>(spanDataList.size());
    for (SpanData spanData : spanDataList) {
      spans.add(TraceTranslator.generateSpan(spanData, projectId, fixedAttributes));
    }

    traceServiceClient.batchWriteSpans(projectName, spans);
    return ResultCode.SUCCESS;
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }
}
