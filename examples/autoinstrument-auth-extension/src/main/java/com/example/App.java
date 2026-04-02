/*
 * Copyright 2026 Google LLC
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
package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class App {

  public static void main(String[] args) throws IOException {
    String portEnv = System.getenv("PORT");
    int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", new MyHandler());
    server.createContext("/makeRequest", new MakeRequestHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
    System.out.println("Server started on port " + port);
  }

  static class MyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response = "Hello from OpenTelemetry Autoinstrumentation Sample App!\n";
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class MakeRequestHandler implements HttpHandler {
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void handle(HttpExchange t) throws IOException {
      String targetUrl = "https://httpbin.org/get";
      Request request = new Request.Builder().url(targetUrl).build();

      String responseBody = "";
      int responseCode = 200;

      try (Response response = client.newCall(request).execute()) {
        responseCode = response.code();
        responseBody =
            "Outbound request to "
                + targetUrl
                + " returned: "
                + responseCode
                + "\nBody: "
                + response.body().string();
      } catch (IOException e) {
        responseCode = 500;
        responseBody = "Error making outbound request: " + e.getMessage();
      }

      t.sendResponseHeaders(responseCode, responseBody.length());
      try (OutputStream os = t.getResponseBody()) {
        os.write(responseBody.getBytes());
      }
    }
  }
}
