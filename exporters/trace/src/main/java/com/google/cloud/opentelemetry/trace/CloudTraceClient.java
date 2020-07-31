package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;

import java.util.List;

public interface CloudTraceClient {
    void batchWriteSpans(ProjectName name, List<Span> spans);
}
