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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;

public class TraceExporter implements SpanExporter {

    private final TraceConfiguration.Builder customTraceConfigurationBuilder;
    private SpanExporter internalTraceExporter;

    private TraceExporter(TraceConfiguration.Builder configurationBuilder) {
        this.customTraceConfigurationBuilder = configurationBuilder;
        this.internalTraceExporter = null;
    }

    private static SpanExporter generateStubTraceExporter(TraceConfiguration.Builder configBuilder) {
        return new TraceExporter(configBuilder);
    }

    private SpanExporter createActualTraceExporter() throws IOException {
        return InternalTraceExporter.createWithConfiguration(this.customTraceConfigurationBuilder.build());
    }

    public static SpanExporter createWithDefaultConfiguration() {
        return generateStubTraceExporter(TraceConfiguration.builder());
    }

    public static SpanExporter createWithConfiguration(TraceConfiguration.Builder configBuilder) {
        return generateStubTraceExporter(configBuilder);
    }

    @Deprecated
    public static SpanExporter createWithConfiguration(TraceConfiguration configuration) throws IOException {
        return InternalTraceExporter.createWithConfiguration(configuration);
    }

    @Override
    public CompletableResultCode flush() {
        if (internalTraceExporter == null) {
            return CompletableResultCode.ofFailure();
        }
        return internalTraceExporter.flush();
    }

    @Override
    public CompletableResultCode export(@Nonnull Collection<SpanData> spanDataList) {
        try {
            if (internalTraceExporter == null) {
                internalTraceExporter = createActualTraceExporter();
            }
            return internalTraceExporter.export(spanDataList);
        } catch (IOException e) {
            e.printStackTrace();
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        if (internalTraceExporter != null) {
            return internalTraceExporter.shutdown();
        } else {
            return CompletableResultCode.ofFailure();
        }
    }
}
