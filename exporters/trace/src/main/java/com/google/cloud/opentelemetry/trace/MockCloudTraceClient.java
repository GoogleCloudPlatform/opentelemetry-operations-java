package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.BatchWriteSpansRequest;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TraceServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudTraceClient implements CloudTraceClient{

    private final TraceServiceGrpc.TraceServiceBlockingStub blockingStub;

    MockCloudTraceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    private MockCloudTraceClient(ManagedChannelBuilder<?> channelBuilder) {
        Channel channel = channelBuilder.build();
        blockingStub = TraceServiceGrpc.newBlockingStub(channel);
    }

    public final void batchWriteSpans(ProjectName name, List<Span> spans) {
        BatchWriteSpansRequest request =
                BatchWriteSpansRequest.newBuilder()
                        .setName(name.toString())
                        .addAllSpans(spans)
                        .build();
        blockingStub.batchWriteSpans(request);
    }
}
