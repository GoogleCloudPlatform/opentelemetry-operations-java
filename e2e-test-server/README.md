# End to End tests

A set of end to end integration tests.  These are run inside various environments to ensure
GCP automagiks work and data flows into google cloud.


## Building a Docker image.

From the *root* directory of the projet, run:

```
docker build . --file=e2e.Dockerfile
```
