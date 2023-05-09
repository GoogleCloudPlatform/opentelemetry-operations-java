/*
 * Copyright 2023 Google LLC
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
package com.google.cloud.opentelemetry.example.appengine;

import com.google.appengine.api.utils.SystemProperty;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "HelloAppEngine", value = "/hello")
public class HelloAppEngine extends HttpServlet {

  // Backed by Logback
  private static final Logger logger = LoggerFactory.getLogger(HelloAppEngine.class.getName());

  // These are only required when doing manual instrumentation with OpenTelemetry
  private Random random;
  private AttributeKey<String> DESCRIPTION_KEY;
  private Tracer tracer;
  private Meter meter;
  private LongCounter useCaseCount;
  private DoubleHistogram fakeLatency;

  @Override
  public void init() throws ServletException {
    super.init();
    logger.info("Init called - HelloAppEngine");
    // Uncomment the below line to generate telemetry from manual instrumentation
    //performInitialization();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    logger.info("/hello called with " + request.getQueryString() + " queryString");
    // Uncomment the below line to generate telemetry from manual instrumentation
    // generateTelemetryFromManualInstrumentation();
    Properties properties = System.getProperties();

    response.setContentType("text/plain");
    response.getWriter().println("Hello App Engine - Standard using "
        + SystemProperty.version.get() + " Java " + properties.get("java.specification.version"));
  }

  // annotation reference - https://opentelemetry.io/docs/instrumentation/java/automatic/annotations/
  @WithSpan
  public static String getInfo() {
    return "Version: " + System.getProperty("java.version")
          + " OS: " + System.getProperty("os.name")
          + " User: " + System.getProperty("user.name");
  }

  private void performInitialization() {
    Objects.requireNonNull(GlobalOpenTelemetry.get(), "Failed to autoconfigure opentelemetry");
    DESCRIPTION_KEY = AttributeKey.stringKey("description");
    tracer = GlobalOpenTelemetry.get().tracerBuilder("example-auto").build();
    meter = GlobalOpenTelemetry.get().meterBuilder("example-auto").build();
    useCaseCount = meter.counterBuilder("use_case_gae").build();
    fakeLatency = meter.histogramBuilder("fakeLatency_gae").build();
    random = new Random();
  }

  private void generateTelemetryFromManualInstrumentation() {
    logger.info("Doing OTel stuff");
    myUseCase("GaeOne");
    myUseCase("GaeTwo");
    logger.info("OTEL Stuff done");
  }

  private void myUseCase(String description) {
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      useCaseCount.add(1, Attributes.of(DESCRIPTION_KEY, description));
      fakeLatency.record(1, Attributes.of(DESCRIPTION_KEY, description));
      span.addEvent("Event A");
      // Do some work for the use case
      for (int i = 0; i < 3; i++) {
        String work = String.format("%s - Work #%d", description, (i + 1));
        doWork(work);
      }
      logger.info("Log correlation with a span " + description);
      span.addEvent("Event B");
    } finally {
      span.end();
    }
  }

  private void doWork(String description) {
    // Child span
    Span span = tracer.spanBuilder(description).startSpan();
    try (Scope scope = span.makeCurrent()) {
      // Simulate work: this could be simulating a network request or an expensive disk operation
      long millis = 100 + random.nextInt(5) * 100;
      fakeLatency.record(millis, Attributes.of(DESCRIPTION_KEY, description.substring(0, 3)));
      Thread.sleep(millis);
      logger.info("Finished work for " + description);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
  }

}
