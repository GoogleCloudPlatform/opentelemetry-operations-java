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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;

public class DeferredTraceExporter implements SpanExporter {

    private final TraceConfiguration customTraceConfiguration;
    private SpanExporter traceExporter;

    private DeferredTraceExporter(TraceConfiguration configuration) {
        this.customTraceConfiguration = configuration;
        this.traceExporter = null;

    }

    private static DeferredTraceExporter generateStubTraceExporter(TraceConfiguration configuration) {
        return new DeferredTraceExporter(configuration);
    }

    public static SpanExporter createWithDefaultConfiguration() throws IOException {
        return generateStubTraceExporter(TraceConfiguration.builder().build());
    }

    public static SpanExporter createWithConfiguration(TraceConfiguration configuration) {
        return generateStubTraceExporter(configuration);
    }

    private SpanExporter createActualTraceExporter() throws IOException {
        return TraceExporter.createWithConfiguration(this.customTraceConfiguration);
    }

    @Override
    public CompletableResultCode flush() {
        if (traceExporter == null) {
            return CompletableResultCode.ofFailure();
        }
        return traceExporter.flush();
    }

    @Override
    public CompletableResultCode export(@Nonnull Collection<SpanData> spanDataList) {
        try {
            if (traceExporter == null) {
                traceExporter = createActualTraceExporter();
            }
            return traceExporter.export(spanDataList);
        } catch (IOException e) {
            e.printStackTrace();
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        if (traceExporter != null) {
            return traceExporter.shutdown();
        } else {
            return CompletableResultCode.ofFailure();
        }
    }
}
