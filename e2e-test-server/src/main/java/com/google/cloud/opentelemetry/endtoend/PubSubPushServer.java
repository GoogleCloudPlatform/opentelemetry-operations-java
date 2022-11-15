/*
 * Copyright 2022 Google
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

import com.google.cloud.opentelemetry.endtoend.PubSubMessageHandler.PubSubMessageResponse;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.pubsub.v1.PubsubMessage;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A {@link PubSubServer} that can handle running the integration test scenarios when {@link
 * Constants#SUBSCRIPTION_MODE} points to 'push' mode.
 *
 * <p>Google cloud's serverless offerings like CloudRun recommend using 'push' mode instead of
 * 'pull' for subscriptions in pub/sub since CloudRun requires running code (the instance spun up is
 * like a daemon when no request is processing). This means we need a dedicated server actively
 * listening on a port instead of waiting on messages being pushed to a topic which can then be
 * pulled (this is what happens in 'pull' subscription mode). This server sets up an {@link
 * HttpServer} on a specified port which actively listens for incoming requests.
 *
 * <p>More information on why 'push' mode is preferred can be seen <a
 * href="https://cloud.google.com/run/docs/triggering/pubsub-push#:~:text=Note%3A%20Google,minimum%20instances">here</a>.
 *
 * <p>This class is responsible for the following:
 *
 * <ul>
 *   <li>Setting up a {@link HttpServer} bound to a specified port, listening for incoming requests.
 *   <li>Validate incoming HTTP requests and parse them into {@link PubsubMessage}s.
 *   <li>Hand off the parsed {@link PubsubMessage}s to a specified {@link PubSubMessageHandler}.
 * </ul>
 */
public class PubSubPushServer implements PubSubServer {

  private static final String POST_REQUEST = "POST";

  private final int port;
  private final HttpServer httpServer;
  private final HttpHandler rootRequestHandler;
  private final PubSubMessageHandler pubsubMessageHandler;

  /**
   * Public constructor for the {@link PubSubPushServer}.
   *
   * @param port The port on which the HTTP server should listen for incoming requests.
   * @param pubSubMessageHandler The {@link PubSubMessageHandler} responsible for handling the
   *     incoming HTTP requests.
   */
  public PubSubPushServer(int port, PubSubMessageHandler pubSubMessageHandler) {
    this.port = port;
    this.pubsubMessageHandler = pubSubMessageHandler;
    this.rootRequestHandler = createRootRequestHandler();
    this.httpServer = createHttpServer();
  }

  @Override
  public void start() {
    this.httpServer.start();
  }

  @Override
  public void close() {
    httpServer.stop(60);
    pubsubMessageHandler.cleanupMessageHandler();
  }

  private PubsubMessage parseIncomingMessage(HttpExchange httpExchange) {
    InputStreamReader inputStreamReader =
        new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
    JsonElement jsonRoot = JsonParser.parseReader(inputStreamReader);
    String msgStr = jsonRoot.getAsJsonObject().get("message").toString();
    Gson gson = new Gson();
    Message message = gson.fromJson(msgStr, Message.class);
    return PubsubMessage.newBuilder().putAllAttributes(message.getAttributes()).build();
  }

  private HttpServer createHttpServer() {
    HttpServer httpServer = null;
    try {
      httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
      httpServer.createContext("/", this.rootRequestHandler);
      httpServer.createContext("/ready", createStandardStringResponseHandler("Server Ready"));
      httpServer.createContext("/alive", createStandardStringResponseHandler("Server Alive"));
      httpServer.setExecutor(MoreExecutors.directExecutor());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return httpServer;
  }

  private HttpHandler createRootRequestHandler() {
    return httpExchange -> {
      if (httpExchange.getRequestMethod().equals(POST_REQUEST)) {
        if (!isRequestJSON(httpExchange)) {
          String response = "Expecting request in JSON format";
          httpExchange.sendResponseHeaders(400, response.length());
          OutputStream os = httpExchange.getResponseBody();
          os.write(response.getBytes(StandardCharsets.UTF_8));
          os.close();
          httpExchange.close();
          return;
        }
        PubsubMessage message = parseIncomingMessage(httpExchange);
        PubSubMessageResponse ackOrNack = pubsubMessageHandler.handlePubSubMessage(message);
        String finalResponse = "";
        if (ackOrNack.equals(PubSubMessageResponse.ACK)) {
          finalResponse = "Success";
          httpExchange.sendResponseHeaders(200, finalResponse.length());
        } else {
          finalResponse = "Failure";
          httpExchange.sendResponseHeaders(500, finalResponse.length());
        }
        OutputStream os = httpExchange.getResponseBody();
        os.write(finalResponse.getBytes(StandardCharsets.UTF_8));
        os.close();
        httpExchange.close();
      } else {
        String response = "Only expecting POST requests";
        httpExchange.sendResponseHeaders(400, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
        httpExchange.close();
      }
    };
  }

  private HttpHandler createStandardStringResponseHandler(String response) {
    return httpExchange -> {
      httpExchange.sendResponseHeaders(200, response.length());
      OutputStream os = httpExchange.getResponseBody();
      os.write(response.getBytes(StandardCharsets.UTF_8));
      os.close();
      httpExchange.close();
    };
  }

  private boolean isRequestJSON(HttpExchange httpExchange) {
    Headers headers = httpExchange.getRequestHeaders();
    String contentType = headers.getFirst("Content-type").split(";")[0].trim();
    return contentType.equalsIgnoreCase("application/json");
  }

  /**
   * A POJO class containing equivalent Java representation of the incoming HTTP request's (to
   * {@link PubSubPushServer}) JSON form.
   */
  public static class Message {
    private Map<String, String> attributes;
    private String messageId;
    private String publishTime;

    /**
     * Parameterized constructor for the class.
     *
     * @param attributes A mapping of String key-value pairs containing custom fields in the
     *     request.
     * @param messageId The unique ID associated with the incoming request.
     * @param publishTime The timestamp at which the request was issued.
     */
    public Message(Map<String, String> attributes, String messageId, String publishTime) {
      this.attributes = attributes;
      this.messageId = messageId;
      this.publishTime = publishTime;
    }

    public Message() {
      // default constructor
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }

    public String getMessageId() {
      return messageId;
    }

    public void setMessageId(String messageId) {
      this.messageId = messageId;
    }

    public String getPublishTime() {
      return publishTime;
    }

    public void setPublishTime(String publishTime) {
      this.publishTime = publishTime;
    }

    @Override
    public String toString() {
      return "Message{"
          + "attributes="
          + attributes
          + ", messageId='"
          + messageId
          + '\''
          + ", publishTime='"
          + publishTime
          + '\''
          + '}';
    }
  }
}
