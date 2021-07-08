/*
 * Copyright 2021 Google
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

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableFeignClients
@RestController
@SpringBootApplication
public class Main {
  @Autowired
  private GreetingClient greetingClient;

  public static void main(String[] args) throws IOException {
    SpringApplication.run(Main.class, args);
  }

  @Bean
  public SpanExporter googleTraceExporter() throws IOException {
    return TraceExporter.createWithDefaultConfiguration();
  }

  @GetMapping("/greeting")
  public String greeting() {
    return "Hello";
  }

  @GetMapping("/")
  public String home() {
    return greetingClient.greeting() + " World";
  }
}
