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
package com.google.cloud.opentelemetry.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.FaasIncubatingAttributes;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import io.opentelemetry.semconv.incubating.K8sIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Translates from OpenTelemetry Resource into Google Cloud's notion of resource. */
public class ResourceTranslator {

  private static final String UNKNOWN_SERVICE_PREFIX = "unknown_service";

  private ResourceTranslator() {}

  @com.google.auto.value.AutoValue
  public abstract static class AttributeMapping {
    /** The label name used in GCP's MonitoredResource. */
    public abstract String getLabelName();
    /** The list of OTEL keys that can be used for this resource label, in priority order. */
    public abstract java.util.List<AttributeKey<?>> getOtelKeys();
    /** A fallback value to set the resource. */
    public abstract Optional<String> fallbackLiteral();

    public void fill(Resource resource, GcpResource.Builder builder) {
      for (AttributeKey<?> key : getOtelKeys()) {
        Object value = resource.getAttribute(key);
        if (value != null) {
          String stringValue = value.toString();
          // for monitored resource types that have service.name, ignore it
          // if its unknown_service in favor of a valid value in faas.name.
          // if faas.name is also empty/unset use the ignored value from before.
          if (key.equals(ServiceAttributes.SERVICE_NAME)
              && stringValue.startsWith(UNKNOWN_SERVICE_PREFIX)) {
            continue;
          }
          builder.addResourceLabels(getLabelName(), stringValue);
          return;
        }
      }
      if (getOtelKeys().contains(ServiceAttributes.SERVICE_NAME)
          && Objects.nonNull(resource.getAttribute(ServiceAttributes.SERVICE_NAME))) {
        builder.addResourceLabels(
            getLabelName(), resource.getAttribute(ServiceAttributes.SERVICE_NAME));
        return;
      }
      fallbackLiteral().ifPresent(value -> builder.addResourceLabels(getLabelName(), value));
    }

    public static AttributeMapping create(String labelName, AttributeKey<?> otelKey) {
      return new AutoValue_ResourceTranslator_AttributeMapping(
          labelName, java.util.Collections.singletonList(otelKey), Optional.empty());
    }

    public static AttributeMapping create(
        String labelName, AttributeKey<?> otelKey, String fallbackLiteral) {
      return new AutoValue_ResourceTranslator_AttributeMapping(
          labelName, java.util.Collections.singletonList(otelKey), Optional.of(fallbackLiteral));
    }

    public static AttributeMapping create(
        String labelName, java.util.List<AttributeKey<?>> otelKeys) {
      return new AutoValue_ResourceTranslator_AttributeMapping(
          labelName, otelKeys, Optional.empty());
    }

    public static AttributeMapping create(
        String labelName, java.util.List<AttributeKey<?>> otelKeys, String fallbackLiteral) {
      return new AutoValue_ResourceTranslator_AttributeMapping(
          labelName, otelKeys, Optional.of(fallbackLiteral));
    }
  }

  private static final List<AttributeMapping> GCE_INSTANCE_LABELS =
      Arrays.asList(
          AttributeMapping.create("zone", CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE),
          AttributeMapping.create("instance_id", HostIncubatingAttributes.HOST_ID));
  private static final List<AttributeMapping> K8S_CONTAINER_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION)),
          AttributeMapping.create("cluster_name", K8sIncubatingAttributes.K8S_CLUSTER_NAME),
          AttributeMapping.create("namespace_name", K8sIncubatingAttributes.K8S_NAMESPACE_NAME),
          AttributeMapping.create("container_name", K8sIncubatingAttributes.K8S_CONTAINER_NAME),
          AttributeMapping.create("pod_name", K8sIncubatingAttributes.K8S_POD_NAME));
  private static final List<AttributeMapping> K8S_POD_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION)),
          AttributeMapping.create("cluster_name", K8sIncubatingAttributes.K8S_CLUSTER_NAME),
          AttributeMapping.create("namespace_name", K8sIncubatingAttributes.K8S_NAMESPACE_NAME),
          AttributeMapping.create("pod_name", K8sIncubatingAttributes.K8S_POD_NAME));
  private static final List<AttributeMapping> K8S_NODE_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION)),
          AttributeMapping.create("cluster_name", K8sIncubatingAttributes.K8S_CLUSTER_NAME),
          AttributeMapping.create("node_name", K8sIncubatingAttributes.K8S_NODE_NAME));
  private static final List<AttributeMapping> K8S_CLUSTER_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION)),
          AttributeMapping.create("cluster_name", K8sIncubatingAttributes.K8S_CLUSTER_NAME));
  private static final List<AttributeMapping> AWS_EC2_INSTANCE_LABELS =
      Arrays.asList(
          AttributeMapping.create("instance_id", HostIncubatingAttributes.HOST_ID),
          AttributeMapping.create("region", CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE),
          AttributeMapping.create("aws_account", CloudIncubatingAttributes.CLOUD_ACCOUNT_ID));
  private static final List<AttributeMapping> GOOGLE_CLOUD_APP_ENGINE_INSTANCE_LABELS =
      Arrays.asList(
          AttributeMapping.create("module_id", FaasIncubatingAttributes.FAAS_NAME),
          AttributeMapping.create("version_id", FaasIncubatingAttributes.FAAS_VERSION),
          AttributeMapping.create("instance_id", FaasIncubatingAttributes.FAAS_INSTANCE),
          AttributeMapping.create("location", CloudIncubatingAttributes.CLOUD_REGION));
  private static final List<AttributeMapping> GENERIC_TASK_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION),
              "global"),
          AttributeMapping.create("namespace", ServiceIncubatingAttributes.SERVICE_NAMESPACE, ""),
          AttributeMapping.create(
              "job",
              Arrays.asList(ServiceAttributes.SERVICE_NAME, FaasIncubatingAttributes.FAAS_NAME),
              ""),
          AttributeMapping.create(
              "task_id",
              Arrays.asList(
                  ServiceIncubatingAttributes.SERVICE_INSTANCE_ID,
                  FaasIncubatingAttributes.FAAS_INSTANCE),
              ""));
  private static final List<AttributeMapping> GENERIC_NODE_LABELS =
      Arrays.asList(
          AttributeMapping.create(
              "location",
              Arrays.asList(
                  CloudIncubatingAttributes.CLOUD_AVAILABILITY_ZONE,
                  CloudIncubatingAttributes.CLOUD_REGION),
              "global"),
          AttributeMapping.create("namespace", ServiceIncubatingAttributes.SERVICE_NAMESPACE, ""),
          AttributeMapping.create(
              "node_id",
              Arrays.asList(HostIncubatingAttributes.HOST_ID, HostIncubatingAttributes.HOST_NAME),
              ""));

  /** Converts a Java OpenTelemetry SDK resource into a GCP resource. */
  public static GcpResource mapResource(Resource resource) {
    String platform = resource.getAttribute(CloudIncubatingAttributes.CLOUD_PLATFORM);
    if (platform == null) {
      return mapK8sResourceOrGenericTaskOrNode(resource);
    }
    switch (platform) {
      case CloudIncubatingAttributes.CloudPlatformIncubatingValues.GCP_COMPUTE_ENGINE:
        return mapBase(resource, "gce_instance", GCE_INSTANCE_LABELS);
      case CloudIncubatingAttributes.CloudPlatformIncubatingValues.AWS_EC2:
        return mapBase(resource, "aws_ec2_instance", AWS_EC2_INSTANCE_LABELS);
      case CloudIncubatingAttributes.CloudPlatformIncubatingValues.GCP_APP_ENGINE:
        return mapBase(resource, "gae_instance", GOOGLE_CLOUD_APP_ENGINE_INSTANCE_LABELS);
      default:
        return mapK8sResourceOrGenericTaskOrNode(resource);
    }
  }

  private static GcpResource mapK8sResourceOrGenericTaskOrNode(Resource resource) {
    // if k8s.cluster.name is set, pattern match for various k8s resources.
    // this will also match non-cloud k8s platforms like minikube.
    if (resource.getAttribute(K8sIncubatingAttributes.K8S_CLUSTER_NAME) != null) {
      if (resource.getAttribute(K8sIncubatingAttributes.K8S_CONTAINER_NAME) != null) {
        return mapBase(resource, "k8s_container", K8S_CONTAINER_LABELS);
      } else if (resource.getAttribute(K8sIncubatingAttributes.K8S_POD_NAME) != null) {
        return mapBase(resource, "k8s_pod", K8S_POD_LABELS);
      } else if (resource.getAttribute(K8sIncubatingAttributes.K8S_NODE_NAME) != null) {
        return mapBase(resource, "k8s_node", K8S_NODE_LABELS);
      } else {
        return mapBase(resource, "k8s_cluster", K8S_CLUSTER_LABELS);
      }
    }
    return genericTaskOrNode(resource);
  }

  private static GcpResource genericTaskOrNode(Resource resource) {
    Map<AttributeKey<?>, Object> attrMap = resource.getAttributes().asMap();
    if ((attrMap.containsKey(ServiceAttributes.SERVICE_NAME)
            || attrMap.containsKey(FaasIncubatingAttributes.FAAS_NAME))
        && (attrMap.containsKey(ServiceIncubatingAttributes.SERVICE_INSTANCE_ID)
            || attrMap.containsKey(FaasIncubatingAttributes.FAAS_INSTANCE))) {
      return mapBase(resource, "generic_task", GENERIC_TASK_LABELS);
    } else {
      return mapBase(resource, "generic_node", GENERIC_NODE_LABELS);
    }
  }

  private static GcpResource mapBase(
      Resource resource, String mrType, List<AttributeMapping> mappings) {
    GcpResource.Builder mr = GcpResource.builder();
    mr.setResourceType(mrType);
    for (AttributeMapping mapping : mappings) {
      mapping.fill(resource, mr);
    }
    return mr.build();
  }
}
