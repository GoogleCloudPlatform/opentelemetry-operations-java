#!/bin/bash
#
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
if [[ -z "$1" ]]; then
  echo "Usage: $0 <artifact-registry-repo-name>"
  exit 1
fi

CONTAINER_REGISTRY=$1
REGISTRY_LOCATION=us-central1

if [[ -z "${GOOGLE_CLOUD_PROJECT}" ]]; then
  echo "GOOGLE_CLOUD_PROJECT environment variable not set"
  exit 1
fi

if [[ -z "${GOOGLE_CLOUD_RUN_REGION}" ]]; then
  echo "GOOGLE_CLOUD_RUN_REGION environment variable not set"
  exit 1
fi

IMAGE_NAME="${REGISTRY_LOCATION}-docker.pkg.dev/${GOOGLE_CLOUD_PROJECT}/${CONTAINER_REGISTRY}/autoinstrument-auth-extension"

echo "ENVIRONMENT VARIABLES VERIFIED"

echo "BUILDING APPLICATION DISTRIBUTION"
# Navigate to root to run gradlew
cd ../..
./gradlew -p examples/autoinstrument-auth-extension installDist
cd examples/autoinstrument-auth-extension

echo "BUILDING DOCKER IMAGE"
# Run docker build from the example directory so its context is correct, or from root with context path.
# The Dockerfile expects build/install/autoinstrument-auth-extension to be in the context.
# If we run from example dir, context is example dir, and build/install/... exists there if we ran gradlew from there.
# Let's run gradlew from root, then docker build from example dir.
cd ../..
docker build -t "${IMAGE_NAME}" -f examples/autoinstrument-auth-extension/Dockerfile examples/autoinstrument-auth-extension
cd examples/autoinstrument-auth-extension

echo "VERIFYING/CREATING ARTIFACT REPOSITORY"
gcloud artifacts repositories create "${CONTAINER_REGISTRY}" --repository-format=docker --location="${REGISTRY_LOCATION}" --description="OpenTelemetry auth extension sample" || true

echo "PUSHING IMAGE TO ARTIFACT REGISTRY"
docker push "${IMAGE_NAME}"

echo "DEPLOYING TO CLOUD RUN"
# We use --no-cpu-throttling for the sample to ensure that traces can be exported in the background.
# See https://cloud.google.com/sdk/gcloud/reference/run/deploy#--[no-]cpu-throttling for details.
gcloud run deploy autoinstrument-auth-extension \
        --set-env-vars="GOOGLE_CLOUD_PROJECT=${GOOGLE_CLOUD_PROJECT}" \
        --image="${IMAGE_NAME}" \
        --region="${GOOGLE_CLOUD_RUN_REGION}" \
        --no-cpu-throttling \
        --allow-unauthenticated
