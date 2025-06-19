#!/usr/bin/env bash

set -eEuCo pipefail

function printUsage {
    cat <<EOF
    Provide single parameter for the new version, e.g. 1.2.3

    Usage:
            $0 1.2.3
            or
            $0.1.2.3-SNAPSHOT
EOF
}

if [[ "$#" -eq 0 ]]; then
    printUsage
    exit
fi

readonly newVersion=$1

mvn versions:set \
    -DgenerateBackupPoms=false \
    -DnewVersion="${newVersion}"
