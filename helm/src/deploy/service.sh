#!/usr/bin/env bash
set -euo pipefail

# Installs this microservice in a k8s environment with helm.

declare -r SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
declare -r PROJECT_ROOT_DIR="${SCRIPT_DIR}/../../.."
declare -r SCRIPT_TMP_DIR="${SCRIPT_DIR}/.tmp"
declare PATH="$SCRIPT_TMP_DIR:$PATH"
declare HELM_VERSION="v3.7.1"

function printUsage() {
    echo "Script to run helm for a microservice."
    echo "Usage: $ ./service.sh <COMMAND>"
    echo "Commands:"
    echo "  install <NAMESPACE> <ENVIRONMENTS>"
    echo "  uninstall <NAMESPACE>"
    echo "  dry-run <NAMESPACE> <ENVIRONMENTS>"
}

declare -r SERVICE=uniport-gateway
declare -r COMMAND=${1:-}
declare -r NAMESPACE=${2:-}
declare -r ENVIRONMENTS=${@:3}

if [[ -z "$SERVICE" || -z "$COMMAND" ]]; then
    printUsage
    exit 1
fi

declare -r HELM_CHART_DIRECTORY=${PROJECT_ROOT_DIR}/helm/target/${SERVICE}
command -v helm >/dev/null 2>&1 || installHelm

function installChart() {
    VALUES=""
    for ENV in $ENVIRONMENTS; do
        echo $ENV
        VALUES="$VALUES --values ${HELM_CHART_DIRECTORY}/values.${ENV}.yaml"
    done

    helm upgrade \
        --install \
        "$SERVICE" \
        "$HELM_CHART_DIRECTORY" \
        --namespace "$NAMESPACE" \
        --create-namespace \
        --debug \
        --wait \
        --timeout 10m \
        $VALUES
}

function dryRunChart() {
    VALUES=""
    for ENV in $ENVIRONMENTS; do
        echo $ENV
        VALUES="$VALUES --values ${HELM_CHART_DIRECTORY}/values.${ENV}.yaml"
    done

    helm template \
        "$SERVICE" \
        "$HELM_CHART_DIRECTORY" \
        $VALUES
}

case "$COMMAND" in
install)
    echo "--> Installing chart for service: ${SERVICE} (namespace: ${NAMESPACE}) (environment: ${ENVIRONMENTS})"
    installChart
    ;;
uninstall)
    echo "--> Uninstalling helm release: ${SERVICE} (namespace: ${NAMESPACE})"
    helm uninstall "$SERVICE" --namespace "$NAMESPACE" --debug
    ;;
dry-run)
    echo "--> Dry-run chart for service: ${SERVICE} (namespace: ${NAMESPACE}) (environment: ${ENVIRONMENTS})"
    dryRunChart
    ;;
*)
    echo " "
    echo "Unknow command $1"
    echo " "
    printUsage
    ;;
esac

exit
