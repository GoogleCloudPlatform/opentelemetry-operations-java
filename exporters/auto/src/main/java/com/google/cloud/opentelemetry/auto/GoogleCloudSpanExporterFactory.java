package com.google.cloud.opentelemetry.auto;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.javaagent.tooling.TracerInstaller;
import io.opentelemetry.javaagent.tooling.exporter.ExporterConfig;
import io.opentelemetry.javaagent.tooling.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudSpanExporterFactory implements SpanExporterFactory {

    private static final Logger log = LoggerFactory.getLogger(TracerInstaller.class);

    @Override
    public SpanExporter fromConfig(ExporterConfig _config) {
        try {
            return TraceExporter.createWithDefaultConfiguration();
        } catch (IOException ex) {
            log.error("Unable to create Google Trace exporter.", ex);
            return null;
        }
    }
}