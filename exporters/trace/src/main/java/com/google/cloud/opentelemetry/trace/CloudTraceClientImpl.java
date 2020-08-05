package com.google.cloud.opentelemetry.trace;

import com.google.cloud.trace.v2.TraceServiceClient;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;

import java.util.List;

public class CloudTraceClientImpl implements CloudTraceClient {
    private final TraceServiceClient traceServiceClient;

    public CloudTraceClientImpl(TraceServiceClient traceServiceClient) {
        this.traceServiceClient = traceServiceClient;
    }

    public final void batchWriteSpans(ProjectName name, List<Span> spans) {
        traceServiceClient.batchWriteSpans(name, spans);
    }
}
