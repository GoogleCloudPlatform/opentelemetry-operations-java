package com.google.cloud.opentelemetry.detectors;

public final class AttributeKeys {
  // GCE Attributes
  public static final String GCE_PROJECT_ID = AttributeKeys.PROJECT_ID;
  public static final String GCE_AVAILABILITY_ZONE = AttributeKeys.AVAILABILITY_ZONE;
  public static final String GCE_CLOUD_REGION = AttributeKeys.CLOUD_REGION;
  public static final String GCE_INSTANCE_ID = AttributeKeys.INSTANCE_ID;
  public static final String GCE_INSTANCE_NAME = AttributeKeys.INSTANCE_NAME;
  public static final String GCE_MACHINE_TYPE = AttributeKeys.MACHINE_TYPE;

  // GKE Attributes
  public static final String GKE_POD_NAME = "gke_pod_name";
  public static final String GKE_NAMESPACE = "gke_namespace";
  public static final String GKE_CONTAINER_NAME = "gke_container_name";
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

  static final String PROJECT_ID = "project_id";
  static final String AVAILABILITY_ZONE = "availability_zone";
  static final String CLOUD_REGION = "cloud_region";
  static final String INSTANCE_ID = "instance_id";
  static final String INSTANCE_NAME = "instance_name";
  static final String MACHINE_TYPE = "machine_type";
}
