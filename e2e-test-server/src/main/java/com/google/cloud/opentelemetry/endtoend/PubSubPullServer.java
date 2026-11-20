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
package com.google.cloud.opentelemetry.endtoend;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * A {@link PubSubServer} that can handle running the integration test scenarios when {@link
 * Constants#SUBSCRIPTION_MODE} points to 'pull' mode.
 *
 * <p>This kind of {@link PubSubServer} works well with Google Cloud's non-serverless compute
 * offerings like GCE and GKE. When {@link Constants#PUSH_PORT} or {@link Constants#PORT} is set, it
 * also serves HTTP health check endpoints ({@code /alive} and {@code /ready}) for environments such
 * as App Engine.
 *
 * <p>This class is responsible for the following:
 *
 * <ul>
 *   <li>Setting up a PubSub {@link Subscriber} with the appropriate subscription name and {@link
 *       com.google.cloud.pubsub.v1.MessageReceiver}.
 *   <li>Attaching a failure listener to subscriber to get causes for failures.
 *   <li>Optionally serving {@code /alive} and {@code /ready} HTTP health checks when a port is
 *       configured.
 * </ul>
 */
public class PubSubPullServer implements PubSubServer {

  private final PubSubMessageHandler pubSubMessageHandler;
  private final Subscriber subscriber;
  private final HttpServer httpServer;

  /**
   * Public constructor for {@link PubSubPullServer}.
   *
   * @param pubSubMessageHandler The {@link PubSubMessageHandler} that will be used to process
   *     incoming {@link com.google.pubsub.v1.PubsubMessage}s.
   */
  public PubSubPullServer(PubSubMessageHandler pubSubMessageHandler) {
    this(
        pubSubMessageHandler,
        createSubscriber(pubSubMessageHandler),
        createHealthCheckServerIfConfigured(Constants.PUSH_PORT, Constants.PORT));
  }

  PubSubPullServer(
      PubSubMessageHandler pubSubMessageHandler, Subscriber subscriber, HttpServer httpServer) {
    this.pubSubMessageHandler = pubSubMessageHandler;
    this.subscriber = subscriber;
    this.httpServer = httpServer;
  }

  private static Subscriber createSubscriber(PubSubMessageHandler pubSubMessageHandler) {
    Subscriber subscriber =
        Subscriber.newBuilder(
                Constants.getRequestSubscription(),
                (MessageReceiver)
                    (message, consumer) -> {
                      PubSubMessageHandler.PubSubMessageResponse response =
                          pubSubMessageHandler.handlePubSubMessage(message);
                      if (response == PubSubMessageHandler.PubSubMessageResponse.ACK) {
                        consumer.ack();
                      } else {
                        consumer.nack();
                      }
                    })
            .build();
    subscriber.addListener(
        new Subscriber.Listener() {
          @Override
          public void failed(Subscriber.State from, Throwable failure) {
            // Handle failure. This is called when the Subscriber encountered a fatal error and is
            // shutting down.
            System.err.println(failure.getMessage());
          }
        },
        MoreExecutors.directExecutor());
    return subscriber;
  }

  static HttpServer createHealthCheckServerIfConfigured(String pushPort, String port) {
    String portValue = resolvePort(pushPort, port);
    if (portValue.isEmpty()) {
      return null;
    }
    return createHealthCheckServer(Integer.parseInt(portValue));
  }

  static String resolvePort(String pushPort, String port) {
    if (pushPort != null && !pushPort.isEmpty()) {
      return pushPort;
    }
    if (port != null && !port.isEmpty()) {
      return port;
    }
    return "";
  }

  static HttpServer createHealthCheckServer(int port) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/ready", createStandardStringResponseHandler("Server Ready"));
      server.createContext("/alive", createStandardStringResponseHandler("Server Alive"));
      server.setExecutor(MoreExecutors.directExecutor());
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to create health check HTTP server", e);
    }
  }

  private static HttpHandler createStandardStringResponseHandler(String response) {
    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
    return httpExchange -> {
      httpExchange.sendResponseHeaders(200, responseBytes.length);
      try (OutputStream os = httpExchange.getResponseBody()) {
        os.write(responseBytes);
      }
      httpExchange.close();
    };
  }

  @Override
  public void close() {
    if (httpServer != null) {
      httpServer.stop(60);
    }
    if (subscriber != null) {
      subscriber.stopAsync();
      subscriber.awaitTerminated();
    }
    pubSubMessageHandler.cleanupMessageHandler();
  }

  @Override
  public void start() {
    if (subscriber != null) {
      subscriber.startAsync().awaitRunning();
    }
    if (httpServer != null) {
      httpServer.start();
    }
  }
}
