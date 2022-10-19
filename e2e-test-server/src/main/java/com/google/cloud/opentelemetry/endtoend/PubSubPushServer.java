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

import com.google.cloud.pubsub.v1.AckReplyConsumer;
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

public class PubSubPushServer implements AutoCloseable {
  public static final String POST_REQUEST = "POST";

  private final int port;
  private final HttpServer httpServer;
  private final HttpHandler rootRequestHandler;
  private final Server mainPullServer;

  // We require main pull server to get access to the handle message functionality
  // TODO: Refactor class to take out the handle function responsibility into a different 'Main'
  // class
  public PubSubPushServer(int port, Server mainPullServer) {
    this.port = port;
    this.mainPullServer = mainPullServer;
    this.rootRequestHandler = createRootRequestHandler();
    this.httpServer = createHttpServer();
  }

  /** Starts the Google cloud pub-sub push server. */
  public void start() {
    this.httpServer.start();
  }

  @Override
  public void close() throws Exception {
    System.out.println("Server is closed");
  }

  private PubsubMessage parseIncomingMessage(HttpExchange httpExchange) {
    InputStreamReader inputStreamReader =
        new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
    JsonElement jsonRoot = JsonParser.parseReader(inputStreamReader);
    String msgStr = jsonRoot.getAsJsonObject().get("message").toString();
    System.out.println("Parsing Incoming message " + msgStr);
    Gson gson = new Gson();
    Message message = gson.fromJson(msgStr, Message.class);
    System.out.println("Message parsed, generated message " + message);
    PubsubMessage pubsubMessage =
        PubsubMessage.newBuilder().putAllAttributes(message.getAttributes()).build();
    System.out.println("Pubsub message after parsing is " + pubsubMessage);
    return pubsubMessage;
  }

  private HttpServer createHttpServer() {
    HttpServer httpServer = null;
    try {
      httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
      httpServer.createContext("/", this.rootRequestHandler);
      httpServer.setExecutor(MoreExecutors.directExecutor());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return httpServer;
  }

  private HttpHandler createRootRequestHandler() {
    return httpExchange -> {
      System.out.println("Handling the response");
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
        System.out.println("PubSub Message parsed " + message);
        String ack_or_nack =
            mainPullServer.handleMessage(
                message,
                new AckReplyConsumer() {
                  @Override
                  public void ack() {
                    System.out.println("Ack");
                  }

                  @Override
                  public void nack() {
                    System.out.println("Nack");
                  }
                });
        String finalResponse = "";
        System.out.println("Final ack or nack " + ack_or_nack);
        if (ack_or_nack.equals("ack")) {
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

  private boolean isRequestJSON(HttpExchange httpExchange) {
    Headers headers = httpExchange.getRequestHeaders();
    String contentType = headers.getFirst("Content-type").split(";")[0].trim();
    return contentType.equalsIgnoreCase("application/json");
  }

  private static class Message {
    private Map<String, String> attributes;
    private String messageId;
    private String publishTime;
    private String data;

    public Message(
        Map<String, String> attributes, String messageId, String publishTime, String data) {
      this.attributes = attributes;
      this.messageId = messageId;
      this.publishTime = publishTime;
      this.data = data;
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

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
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
          + ", data='"
          + data
          + '\''
          + '}';
    }
  }
}
