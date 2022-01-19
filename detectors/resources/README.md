# Resource Detectors for OpenTelemetry

This package provides resource detectors for OpenTelemetry.

The following OpenTelemetry semantic conventions will be detected:

| Resource attribute | GCE | GKE | Cloud Run |
| ------------------ | --- | --- | ----------|
| cloud.platform | gce_compute_engine | gce_kubernetes_engine | |
| cloud.provider | gcp | gcp | |
| cloud.account.id | auto | auto | |
| cloud.availability_zone | auto | auto | |
| cloud.region | auto | auto | |
| host.id | auto | auto | |
| host.name | auto | auto | |
| host.type | auto | auto | |
| k8s.pod.name | | downward API or auto | |
| k8s.namespace.name | | downward API | |
| k8s.container.name | | hardcoded | |
| k8s.cluster.name | | auto | |

## Downward API

For GKE applications, some values most be passed via the environment variable using k8s
"downward API".  For example, the following spec will ensure `k8s.namespace.name` and
`k8s.pod.name` are correctly discovered:

```yaml
spec:
  containers:
    - name: my-application
      image: gcr.io/my-project/my-image:latest
      env:
      - name: POD_NAME
        valueFrom:
          fieldRef:
            fieldPath: metadata.name
      - name: NAMESPACE
        valueFrom:
          fieldRef:
            fieldPath: metadata.namespace
      - name: CONTAINER_NAME
        value: my-application
```

Additionally, the container name will only be discovered via the environment variable `CONTAINER_NAME`
which much be included in the environment.
