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
package com.google.cloud.opentelemetry.example.resource;

import com.google.cloud.opentelemetry.detectors.GKEResource;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class ResourceExample {
  public static void main(String[] args) {
    System.out.println("Detecting resource: Autoconfigure");
    io.opentelemetry.sdk.resources.Resource autoResource =
        AutoConfiguredOpenTelemetrySdk.builder()
            .setResultAsGlobal(false)
            .setServiceClassLoader(ResourceExample.class.getClassLoader())
            .build()
            .getResource();
    System.out.println(autoResource.getAttributes());
    System.out.println("Detecting resource: hardcoded");
    GKEResource resource = new GKEResource();
    System.out.println(resource.getAttributes());
  }
}
