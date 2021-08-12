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
package com.google.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Creates a gRPC service on port 8080 that will be in our container. */
public class TestMain {

  private static final Logger logger = LogManager.getLogger();

  public static void main(String[] args) throws Exception {
    TestService service = new TestService();
    Server server =
        ServerBuilder.forPort(8080)
            .addService(ProtoReflectionService.newInstance())
            .addService(service)
            .directExecutor()
            .build()
            .start();
    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
    logger.info("Server started at port 8080.");
    server.awaitTermination();
  }
}
