package com.google.cloud.operations.opentelemetry.exporter.util;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import io.opentelemetry.sdk.resources.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper class for K8S container {@code Resource}.
 */
public class ContainerResource {
  /**
   * Kubernetes resources key that represents a type of the resource.
   */
  public static final String TYPE = "container";

  /**
   * Key for the container name.
   */
  public static final String NAME_KEY = "container.name";

  /**
   * Key for the container image name.
   */
  public static final String IMAGE_NAME_KEY = "container.image.name";

  /**
   * Key for the container image tag.
   */
  public static final String IMAGE_TAG_KEY = "container.image.tag";

  /**
   * Returns a {@link Resource} that describes a container.
   *
   * @param name the container name.
   * @param imageName the container image name.
   * @param imageTag the container image tag.
   * @return a {@link Resource} that describes a k8s container.
   */
  public static Map<String, String> create(String name, String imageName, String imageTag) {
    Map<String, String> labels = new LinkedHashMap<String, String>();
    labels.put(NAME_KEY, checkNotNull(name, "name"));
    labels.put(IMAGE_NAME_KEY, checkNotNull(imageName, "imageName"));
    labels.put(IMAGE_TAG_KEY, checkNotNull(imageTag, "imageTag"));
    return labels;
  }

  static Map<String, String> detect() {
    // TODO: Add support to auto-detect IMAGE_NAME_KEY and IMAGE_TAG_KEY.
    return create(firstNonNull(System.getenv("CONTAINER_NAME"), ""), "", "");
  }

  private ContainerResource() {}
}
