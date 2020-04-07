package com.google.cloud.operations.opentelemetry.exporter.util;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class ResourceUtils {
  static final Map<String, String>  EMPTY_RESOURCE_LABELS = Collections.<String, String>emptyMap();

  /**
   * Returns a set of labels for detected resource.
   * Detector sequentially runs resource detection from environment
   * variables, K8S, GCE and AWS.
   *
   * @return a set of resource labels.
   */
  public static Map<String, String> detectResourceLabels() {
    List<Map<String, String>> resourceList = new ArrayList<>();
    if (System.getenv("KUBERNETES_SERVICE_HOST") != null) {
      resourceList.add(ContainerResource.detect());
      resourceList.add(K8sResource.detect());
    }
    resourceList.add(HostResource.detect());
    resourceList.add(CloudResource.detect());
    return firstNonNull(mergeResources(resourceList), EMPTY_RESOURCE_LABELS);
  }


  /**
   * Runs through all input resources sequentially and merges their results.
   * In case a type of label key is already set, the first set value takes precedence.
   *
   * @param resources a list of resources.
   * @return a {@code Resource}.
   */
  @Nullable
  public static Map<String, String> mergeResources(List<Map<String, String>> resources) {
    Map<String, String> currentResource = null;
    for (Map<String, String> resource : resources) {
      currentResource = merge(currentResource, resource);
    }
    return currentResource;
  }


  /**
   * Returns a new, merged set of labels by merging two resources. In case of a collision, first
   * resource takes precedence.
   */
  @Nullable
  private static Map<String, String> merge(@Nullable Map<String, String> resource, @Nullable Map<String, String> otherResource) {
    if (otherResource == null) {
      return resource;
    }
    if (resource == null) {
      return otherResource;
    }

    Map<String, String> mergedLabelMap =
        new LinkedHashMap<String, String>(otherResource);

    // Labels from resource overwrite labels from otherResource.
    for (Map.Entry<String, String> entry : resource.entrySet()) {
      mergedLabelMap.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(mergedLabelMap);
  }

  private ResourceUtils() {}
}
