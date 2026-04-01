package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {

  public static void main(String[] args) throws IOException {
    int port = 8080;
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
      Request request = new Request.Builder()
          .url(targetUrl)
          .build();

      String responseBody = "";
      int responseCode = 200;

      try (Response response = client.newCall(request).execute()) {
        responseCode = response.code();
        responseBody = "Outbound request to " + targetUrl + " returned: " + responseCode + "\nBody: " + response.body().string();
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
