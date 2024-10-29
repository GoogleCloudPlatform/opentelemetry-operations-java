# Using Spring Sleuth to configure OpenTelemetry for Google Cloud Trace

This example shows how to export traces to GCP generated from a Spring Boot application.
Spring Cloud Sleuth is used to provide auto-configuration for distributed tracing. 

#### Prerequisites

##### Get Google Cloud Credentials on your machine

```shell
gcloud auth application-default login
```
Executing this command will save your application credentials to default path which will depend on the type of machine -
- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

##### Export the Google Cloud Project ID to `GOOGLE_CLOUD_PROJECT` environment variable:

```shell
export GOOGLE_CLOUD_PROJECT="my-awesome-gcp-project-id"
```

#### Run the application

You can run the example application via gradle. From the project root:

##### Build the executable JAR

```shell
cd examples/spring-sleuth/ && gradle bootJar
```

##### Run the executable JAR

The JAR built from the previous command typically ends up in `build/libs` -

```shell
java -jar build/libs/examples-spring-sleuth-1.0.0.jar
```

The application is now running. To generate traces, head to `http://localhost:8080` in your browser.
*You can also try hitting other supported endpoints - `http://localhost:8080/greeting` to generate additional traces.*

You should now see the exported traces in your Google Cloud project.
