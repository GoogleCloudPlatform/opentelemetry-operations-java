package com.google.cloud.opentelemetry.auto;

import com.google.auto.service.AutoService;
import com.google.cloud.opentelemetry.metric.MetricExporter;
import io.opentelemetry.javaagent.spi.exporter.MetricExporterFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@AutoService(MetricExporterFactory.class)
public class GoogleCloudMetricExporterFactory implements MetricExporterFactory {
  @Override
  public MetricExporter fromConfig(Properties _config) {
    try {
      return MetricExporter.createWithDefaultConfiguration();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Set<String> getNames() {
    return new HashSet<>(Constants.CLOUD_MONITORING_EXPORTER_NAMES);
  }
}
