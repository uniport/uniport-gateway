#!/usr/bin/env bash
# This script checks if the json files used in the helm chart is valid according to their corresponding schemas.

set -Eeo pipefail

declare -r NODE_IMAGE=uniportcr.artifacts.inventage.com/node:20-alpine

if [ -n "$MINIKUBE_ACTIVE_DOCKERD" ]; then
    echo "\$MINIKUBE_ACTIVE_DOCKERD is set, skipping validation…"
    exit
fi

function validateWithDynamicSchema() {
    docker run --rm \
        -e NPM_CONFIG_UPDATE_NOTIFIER=false \
        -e NPM_CONFIG_YES=true \
        -v "${PWD}":/tmp \
        $NODE_IMAGE \
        npx ajv-cli validate --allow-union-types -s /tmp/target/schema/portalGatewayDynamicSchema.json -d "/tmp/target/uniport-gateway/proxy-config.*/dynamic-config/**/*.json"
}

function validateWithStaticSchema() {
    docker run --rm \
        -e NPM_CONFIG_UPDATE_NOTIFIER=false \
        -e NPM_CONFIG_YES=true \
        -v "${PWD}":/tmp \
        $NODE_IMAGE \
        npx ajv-cli validate --allow-union-types -s /tmp/target/schema/portalGatewayStaticSchema.json -d "/tmp/target/uniport-gateway/proxy-config.*/*.json"
}

validateWithDynamicSchema
validateWithStaticSchema
