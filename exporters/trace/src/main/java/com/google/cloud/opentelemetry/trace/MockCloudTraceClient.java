/*
 * Copyright 2021 Google
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

import com.google.devtools.cloudtrace.v2.BatchWriteSpansRequest;
import com.google.devtools.cloudtrace.v2.ProjectName;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TraceServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;

// A simplified version of TraceServiceClient, used ONLY for testing purposes.
class MockCloudTraceClient implements CloudTraceClient {

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
        BatchWriteSpansRequest.newBuilder().setName(name.toString()).addAllSpans(spans).build();
    blockingStub.batchWriteSpans(request);
  }

  // Empty because not being tested
  public final void shutdown() {}
}
