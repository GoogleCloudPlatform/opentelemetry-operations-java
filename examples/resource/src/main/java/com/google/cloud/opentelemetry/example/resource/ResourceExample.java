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
package com.google.cloud.opentelemetry.example.resource;

import com.google.cloud.opentelemetry.detectors.GCPResource;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.resources.Resource;

public class ResourceExample {
  public static void main(String[] args) {
    System.out.println("Detecting resource: Autoconfigure");
    Resource autoResource = ResourceConfiguration.createEnvironmentResource();
    System.out.println(autoResource.getAttributes());
    System.out.println("Detecting resource: hardcoded");
    GCPResource resource = new GCPResource();
    System.out.println(resource.getAttributes());
  }
}
