# OTLP Trace with Google Auth Example

Run this sample to connect to an endpoint that is protected by GCP authentication.

First, get GCP credentials on your machine:

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
 - Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
 - Windows: `%APPDATA%\gcloud\application_default_credentials.json`

Next, update the `build.gradle` file to modify required JVM arguments:

```groovy
def autoconf_config = [
        '-Dgoogle.cloud.project=your-gcp-project-id',
        '-Dotel.exporter.otlp.endpoint=https://your-api-endpoint:port',
        // other arguments
    ]
```

Finally, to run the sample from the project root:

```
cd examples/otlptrace && gradle run
```
