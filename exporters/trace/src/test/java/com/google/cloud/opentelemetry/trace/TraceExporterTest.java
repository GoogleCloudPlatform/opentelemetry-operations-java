package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TraceExporterTest {

  @Test
  public void createWithConfiguration() {
    TraceConfiguration configuration = TraceConfiguration.builder().setProjectId("test").build();
    try {
      TraceExporter exporter = TraceExporter.createWithConfiguration(configuration);

      assertNotNull(exporter);
    } catch (IOException ignored) {
    }
  }
}
