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

import static java.net.http.HttpResponse.BodyHandlers;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client application that runs for ~ 2 hours and make HTTP requests to the spring application
 * running on localhost:8080.
 */
class MainClient {
  private static final String BASE_URL = "http://localhost:8080";
  private static final String[] ROUTES = new String[] {"/", "/work"};
  private static final Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(Main.class.getSimpleName());
  // total time for which this client app runs
  private static final int CLIENT_RUN_DURATION_MIN = 120;

  public static void main(String[] args) throws InterruptedException {
    System.out.println("Starting client");
    HttpClient client = HttpClient.newHttpClient();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduleApplicationCalls(client, scheduler);
    boolean allTasksComplete =
        scheduler.awaitTermination(CLIENT_RUN_DURATION_MIN + 5, TimeUnit.MINUTES);
    if (allTasksComplete) {
      logger.info("All scheduled calls finished successfully.");
    } else {
      logger.info("Scheduled calls timed out.");
    }
  }

  private static void scheduleApplicationCalls(
      HttpClient client, ScheduledExecutorService scheduler) {
    Runnable requestIssuer =
        () -> {
          String response = issueRequest(client);
          logger.info("Response: {}", response);
        };

    final ScheduledFuture<?> requestHandle =
        scheduler.scheduleAtFixedRate(requestIssuer, 10, 10, TimeUnit.of(SECONDS));
    scheduler.schedule(
        () -> {
          requestHandle.cancel(true);
        },
        CLIENT_RUN_DURATION_MIN,
        TimeUnit.of(MINUTES));
  }

  private static String issueRequest(HttpClient client) {
    HttpRequest httpRequest =
        HttpRequest.newBuilder(URI.create(constructUrl()))
            .GET()
            .timeout(Duration.of(8, SECONDS))
            .build();
    try {
      HttpResponse<String> response = client.send(httpRequest, BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      logger.error("Unable to complete request: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private static String constructUrl() {
    String route = ROUTES[random.nextInt(2)];
    String additionalParams = "";
    if (route.equals("/work")) {
      int randomWorkId = random.nextInt(100);
      String workDescription = "test" + randomWorkId;
      additionalParams = String.format("?desc=%s", workDescription);
    }
    return BASE_URL + route + additionalParams;
  }
}
