# Autoinstrumentation Example

An example spring webapp deployed and instrumented using the OpenTelemetry Java Auto-instrumentation agent deployed to GKE.

To spin it up on your own GKE cluster, run the following:
```bash
export GOOGLE_CLOUD_PROJECT={your-project}

./gradlew :examples-autoinstrument:jib --image="gcr.io/$GOOGLE_CLOUD_PROJECT/hello-autoinstrument-java"

kubectl create deployment hello-autoinstrument-java \
  --image=gcr.io/$GOOGLE_CLOUD_PROJECT/hello-autoinstrument-java

kubectl expose deployment  hello-autoinstrument-java --type LoadBalancer --port 80 --target-port 8080
```


This will expose the simple http server at port 80.   You can try out the tracing instrumentation via:

```bash
curl ${cluster_ip}
```

Or, if you'd like to synthesize a parent trace:

```bash
curl -H "traceparent:  00-ff000000000000000000000000000041-ff00000000000041-01" ${cluster_ip}
```
