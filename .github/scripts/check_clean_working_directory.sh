#!/usr/bin/env bash
#
# check_clean_working_directory.sh
#
# This script checks for uncommitted changes in a git repository,
# specifically looking for uncommitted generated code. It allows
# for certain file patterns (like pom.xml) to be excluded from the check.
#
# Usage:
#   ./check_clean_working_directory.sh
#
# Environment Variables:
#   EXCLUDED_PATTERNS - Optional. Colon-separated list of regex patterns
#                      for files to exclude from checks.
#                      Example: ".*pom\.xml$:.*\.properties$"
#
# Exit Codes:
#   0 - No uncommitted changes found (or only in excluded files)
#   1 - Uncommitted changes found in non-excluded files
#
set -EeuCo pipefail

# Ensures the working directory is clean

# Default patterns to exclude (if not set via environment variable)
DEFAULT_EXCLUDED_PATTERNS=(
  ".*pom\.xml$"
  ".*\.npmrc$"
  # ".*\.properties$"
  # ".*\.lock$"
  # ".*package-lock\.json$"
)

# Check if EXCLUDED_PATTERNS is set as an environment variable
if [[ -n "${EXCLUDED_PATTERNS:-}" ]]; then
  # Convert environment variable string to array (expecting colon-separated values)
  IFS=':' read -ra EXCLUDED_PATTERNS_ARRAY <<< "$EXCLUDED_PATTERNS"
  echo "Using excluded patterns from environment: ${EXCLUDED_PATTERNS_ARRAY[*]}"
else
  # Use default patterns if environment variable is not set
  EXCLUDED_PATTERNS_ARRAY=("${DEFAULT_EXCLUDED_PATTERNS[@]}")
  echo "Using default excluded patterns: ${EXCLUDED_PATTERNS_ARRAY[*]}"
fi

# Join patterns with | to create a single regex pattern
EXCLUDED_REGEX=$(IFS="|"; echo "(${EXCLUDED_PATTERNS_ARRAY[*]})")

git_status_output=$(git status --porcelain --untracked-files=no)

# Check if git status returned anything
if [[ -z "$git_status_output" ]]; then
  echo "No changes detected by git status."
  exit 0
fi

while IFS= read -r line; do
  # Skip empty lines
  if [[ -z "$line" ]]; then
    continue
  fi

  echo "$line"
  # Extract the file path - handle different status output formats
  file=$(echo "$line" | awk '{print $NF}')

  # Ensure file path is not empty
  if [[ -z "$file" ]]; then
    echo "Warning: Could not extract file path from status line: '$line'"
    continue
  fi

  if [[ "$file" =~ $EXCLUDED_REGEX ]]; then
    echo "Skipping diff check for excluded file: $file"
    continue # Skip processing and diff for excluded files
  fi

  # Check if the file exists before trying to get diff
  if [[ ! -f "$file" ]]; then
    echo "Warning: File '$file' does not exist or is not a regular file. Skipping diff."
    continue
  fi

  git_diff_output=$(git diff --ignore-space-at-eol --ignore-space-change -- "$file")
  if [[ -n "$git_diff_output" ]]; then
    echo "$git_diff_output"
    echo "ERROR: You have uncommitted generated code in $file!" >&2
    exit 1
  fi
done <<< "$git_status_output"

exit 0
