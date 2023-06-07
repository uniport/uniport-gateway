#!/bin/bash

set -Eeo pipefail

NODE_IMAGE=node:18-alpine

if [ -n "$MINIKUBE_ACTIVE_DOCKERD" ]
then
    echo "\$MINIKUBE_ACTIVE_DOCKERD is set, skipping validation…"
    exit
fi


docker run --rm \
  -e NPM_CONFIG_UPDATE_NOTIFIER=false \
  -e NPM_CONFIG_YES=true \
  -v "${PWD}":/tmp \
  $NODE_IMAGE \
  npx ajv-cli validate --allow-union-types -s /tmp/src/main/resources/schema/portalGatewayDynamicSchema.json -d "/tmp/target/helm/proxy-config.*/dynamic-config/**/*.json"

docker run --rm \
  -e NPM_CONFIG_UPDATE_NOTIFIER=false \
  -e NPM_CONFIG_YES=true \
  -v "${PWD}":/tmp \
  $NODE_IMAGE \
  npx ajv-cli validate --allow-union-types -s /tmp/src/main/resources/schema/portalGatewayStaticSchema.json -d "/tmp/target/helm/proxy-config.*/*.json"
