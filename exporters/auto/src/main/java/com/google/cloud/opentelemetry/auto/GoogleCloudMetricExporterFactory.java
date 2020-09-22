package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.metric.MetricExporter;
import io.opentelemetry.javaagent.tooling.exporter.ExporterConfig;
import io.opentelemetry.javaagent.tooling.exporter.MetricExporterFactory;

import java.io.IOException;

@AutoService(MetricExporterFactory.class)
public class GoogleCloudMetricExporterFactory implements MetricExporterFactory {
    @Override
    public MetricExporter fromConfig(ExporterConfig _config) {
        try {
            return MetricExporter.createWithDefaultConfiguration();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}