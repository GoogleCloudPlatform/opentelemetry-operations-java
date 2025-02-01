/*
 * Copyright 2024 Google LLC
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
package com.google.cloud.opentelemetry.examples.otlpspring;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController {
  private static final String INSTRUMENTATION_SCOPE_NAME = ApplicationController.class.getName();
  private static final Random random = new Random();

  private static final String INDEX_GREETING =
      "Welcome to OTLP Trace sample with Google Auth on Spring";
  private static final String WORK_RESPONSE_FMT = "Work finished in %d ms";

  private final Logger logger = LoggerFactory.getLogger(ApplicationController.class);
  private final OpenTelemetrySdk openTelemetrySdk;

  @Autowired
  public ApplicationController(OpenTelemetrySdk openTelemetrySdk) {
    this.openTelemetrySdk = openTelemetrySdk;
  }

  @GetMapping("/")
  public String index() {
    return INDEX_GREETING;
  }

  @GetMapping("/work")
  public String simulateWork(@RequestParam(name = "desc") Optional<String> description) {
    String desc = description.orElse("generic");
    // Generate a span
    Span span =
        openTelemetrySdk.getTracer(INSTRUMENTATION_SCOPE_NAME).spanBuilder(desc).startSpan();
    long workDurationMillis;
    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Event A");
      // Do some work for the use case
      // Simulate work: this could be simulating a network request or an expensive disk operation
      int randomSleepDuration = random.nextInt(5) * 100;
      workDurationMillis = 100 + randomSleepDuration;
      Thread.sleep(workDurationMillis);
      span.addEvent("Event B");
      span.setAttribute("RandomSleep", randomSleepDuration);
    } catch (InterruptedException e) {
      logger.debug("Error while sleeping: {}", e.getMessage());
      throw new RuntimeException(e);
    } finally {
      span.end();
    }
    return String.format(WORK_RESPONSE_FMT, workDurationMillis);
  }
}
