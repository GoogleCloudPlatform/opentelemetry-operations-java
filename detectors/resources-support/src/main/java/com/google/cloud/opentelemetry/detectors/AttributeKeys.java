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
package com.google.cloud.opentelemetry.detectors;

/**
 * Contains constants that act as keys for the known attributes for {@link
 * com.google.cloud.opentelemetry.detectors.GCPPlatformDetector.SupportedPlatform}s.
 */
public final class AttributeKeys {
  // GCE Attributes
  public static final String GCE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String GCE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;
  public static final String GCE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;
  public static final String GCE_INSTANCE_NAME = AttributeKeys.INSTANCE_NAME;
  public static final String GCE_MACHINE_TYPE = AttributeKeys.MACHINE_TYPE;
  public static final String GCE_INSTANCE_HOSTNAME = "instance_hostname";

  // GKE Attributes
  public static final String GKE_CLUSTER_NAME = "gke_cluster_name";
  public static final String GKE_CLUSTER_LOCATION_TYPE = "gke_cluster_location_type";
  public static final String GKE_CLUSTER_LOCATION = "gke_cluster_location";
  public static final String GKE_HOST_ID = AttributeKeys.INSTANCE_ID;

  // GKE Location Constants
  public static final String GKE_LOCATION_TYPE_ZONE = "ZONE";
  public static final String GKE_LOCATION_TYPE_REGION = "REGION";

  // GAE Attributes
  public static final String GAE_MODULE_NAME = "gae_module_name";
  public static final String GAE_APP_VERSION = "gae_app_version";
  public static final String GAE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;
  public static final String GAE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String GAE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;

  // Google Serverless Compute Attributes
  public static final String SERVERLESS_COMPUTE_NAME = "serverless_compute_name";
  public static final String SERVERLESS_COMPUTE_REVISION = "serverless_compute_revision";
  public static final String SERVERLESS_COMPUTE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String SERVERLESS_COMPUTE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;
  public static final String SERVERLESS_COMPUTE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;

  static final String AVAILABILITY_ZONE = "availability_zone";
  static final String CLOUD_REGION = "cloud_region";
  static final String INSTANCE_ID = "instance_id";
  static final String INSTANCE_NAME = "instance_name";
  static final String MACHINE_TYPE = "machine_type";
}
