package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@AutoService(SpanExporterFactory.class)
public class GoogleCloudSpanExporterFactory implements SpanExporterFactory {
    @Override
    public SpanExporter fromConfig(Properties _config) {
        try {
            return TraceExporter.createWithDefaultConfiguration();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Set<String> getNames() {
        return new HashSet<>(Arrays.asList("google_cloud", "google_cloud_span"));
    }
}
