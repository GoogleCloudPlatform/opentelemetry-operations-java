package com.google.cloud.operations.opentelemetry.exporter.trace;

import com.google.cloud.operations.opentelemetry.exporter.util.ResourceUtils;
import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CloudTraceExporter implements SpanExporter {

  // Only initialize once.
  private static final Map<String, AttributeValue> RESOURCE_LABELS =
      CloudTraceTranslator.getResourceLabels(ResourceUtils.detectResourceLabels());

  private final TraceServiceClient traceServiceClient;
  private final ProjectName projectName;
  private final String projectId;
  private final Map<String, AttributeValue> fixedAttributes;

  public CloudTraceExporter(String projectId, TraceServiceClient traceServiceClient,
      Map<String, AttributeValue> fixedAttributes) {
    this.projectId = projectId;
    this.traceServiceClient = traceServiceClient;
    this.projectName = ProjectName.of(projectId);
    this.fixedAttributes = fixedAttributes;
  }

  @Override
  public ResultCode export(Collection<SpanData> spanDataList) {
    List<Span> spans = new ArrayList<>(spanDataList.size());
    for (SpanData spanData : spanDataList) {
      spans.add(CloudTraceTranslator.generateSpan(spanData, projectId, RESOURCE_LABELS, fixedAttributes));
    }

    traceServiceClient.batchWriteSpans(projectName, spans);
    return ResultCode.SUCCESS;
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException();
  }


}
