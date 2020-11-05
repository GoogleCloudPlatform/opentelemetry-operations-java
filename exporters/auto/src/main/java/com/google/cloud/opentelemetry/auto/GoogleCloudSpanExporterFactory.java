package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Properties;

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
}