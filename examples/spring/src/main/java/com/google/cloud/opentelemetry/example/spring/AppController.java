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
package com.google.cloud.opentelemetry.example.spring;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController {
  private final LongCounter counter_greeting;
  private final LongCounter counter_home;
  private final Tracer tracer;

  @Autowired
  public AppController(Meter meter, Tracer tracer) {
    counter_greeting =
        meter
            .counterBuilder("greeting_counter")
            .setDescription("Hit /greeting endpoint")
            .setUnit("1")
            .build();
    counter_home =
        meter
            .counterBuilder("home_counter")
            .setDescription("Hit root endpoint")
            .setUnit("1")
            .build();
    this.tracer = tracer;
  }

  @GetMapping("/greeting")
  public String greeting() {
    Span span = tracer.spanBuilder("greeting_call").startSpan();
    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Event A");
      span.setAttribute("test_api", true);
      counter_greeting.add(1);
    } finally {
      span.end();
    }
    return "Hello World";
  }

  @GetMapping("/")
  public String home() {
    counter_home.add(1);
    return "Welcome to Spring Demo";
  }
}
