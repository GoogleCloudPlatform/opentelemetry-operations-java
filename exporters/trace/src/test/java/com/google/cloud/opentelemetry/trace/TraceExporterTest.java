package com.google.cloud.opentelemetry.trace;

import com.google.devtools.cloudtrace.v2.AttributeValue;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.Map;
import java.util.HashMap;
import org.junit.Test;

@RunWith(JUnit4.class)
public class TraceExporterTest {

  private final String PROJECT_ID = "project";
  private final Map<String, AttributeValue> FIXED_ATTRIBUTES = new HashMap<>();

  @Test
  public void exporterConstructorTest() {
    final TraceExporter exporter = new TraceExporter(PROJECT_ID, null, FIXED_ATTRIBUTES);
    assertNotNull(exporter);
  }
}
