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
    this.traceServiceClient.batchWriteSpans(name, spans);
  }

  public final void shutdown() {
    this.traceServiceClient.shutdown();
  }
}
