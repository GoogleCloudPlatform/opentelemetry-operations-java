package com.google.cloud.opentelemetry.trace;

import static org.junit.Assert.assertNotNull;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import org.junit.Test;

@RunWith(JUnit4.class)
public class TraceExporterTest {

    @Test
    public void createWithConfiguration() {
        TraceConfiguration configuration = TraceConfiguration.builder().setProjectId("test").build();
        try {
            TraceExporter exporter = TraceExporter.createWithConfiguration(configuration);

            assertNotNull(exporter);
        } catch (IOException e) {
        }
    }
}
