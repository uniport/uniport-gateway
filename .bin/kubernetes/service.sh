#!/bin/bash

# Safer bash scripts
# @see https://explainshell.com/explain?cmd=set+-euCo+pipefail
# @see https://vaneyckt.io/posts/safer_bash_scripts_with_set_euxo_pipefail/
set -euCo pipefail

# Parameters
SERVICE=${1:-}
COMMAND=${2:-}
NAMESPACE=${3:-}
ENVIRONMENT=${4:-}

# Configuration variables
SCRIPT_DIR=$(dirname "$0")
SCRIPT_TMP_DIR="${SCRIPT_DIR}/.tmp"
PROJECT_ROOT_DIR="${SCRIPT_DIR}/../.."
PATH="$PATH:$SCRIPT_TMP_DIR"
HELM_VERSION="v3.7.1"

trap cleanup EXIT

function printUsage() {
  echo "Usage: $ ./service.sh <SERVICE> <COMMAND>"
  echo "Commands:"
  echo "  install <NAMESPACE> <ENVIRONMENT> "
  echo "  uninstall <NAMESPACE>"
  exit 1
}

# Check if helm is installed and install it to a local tmp dir if it is not…
function installHelm() {
  local helmVersion="$HELM_VERSION"
  if [[ ! -d "$SCRIPT_TMP_DIR" ]]; then
    mkdir -p "$SCRIPT_TMP_DIR"
  fi

  curl -fsSL -o "${SCRIPT_TMP_DIR}/get_helm.sh" https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
  chmod 700 "${SCRIPT_TMP_DIR}/get_helm.sh"
  HELM_INSTALL_DIR="$SCRIPT_TMP_DIR" "./${SCRIPT_TMP_DIR}/get_helm.sh" --no-sudo --version $helmVersion
}

function cleanup() {
  if [[ -d "$SCRIPT_TMP_DIR" ]]; then
    rm -r "$SCRIPT_TMP_DIR"
  fi
}

if [[ -z "$SERVICE" || -z "$COMMAND" ]]; then
  printUsage
fi

HELM_CHART="${PROJECT_ROOT_DIR}/helm/target/${SERVICE}"
command -v helm >/dev/null 2>&1 || installHelm

if [[ ! -d "$HELM_CHART" ]]; then
  echo "Wrong service? Helm chart directory not found in: $HELM_CHART"
  exit 1
fi

case "$COMMAND" in
install)
  if [[ -z "$ENVIRONMENT" || -z "$NAMESPACE" ]]; then
    printUsage
  fi

  echo "--> Installing chart for service: ${SERVICE} (namespace: ${NAMESPACE})"
  helm upgrade --install \
    "$SERVICE" "$HELM_CHART" \
    --namespace "$NAMESPACE" \
    --create-namespace \
    --values "${HELM_CHART}/values.${ENVIRONMENT}.yaml" \
    --debug #--dry-run
  echo
  ;;
uninstall)
  if [[ -z "$NAMESPACE" ]]; then
    printUsage
  fi

  echo "--> Uninstalling helm release: ${SERVICE} (namespace: ${NAMESPACE})"
  helm uninstall "$SERVICE" --namespace "$NAMESPACE"
  echo
  ;;
esac

exit
