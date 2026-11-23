#!/usr/bin/env bash
# Prints the CHANGELOG.md section of one version (without its heading); fails if it is missing or empty.
# Usage: changelog-section.sh 0.2.0 [CHANGELOG.md]
set -euo pipefail

version="${1:?usage: $0 <version> [changelog]}"
changelog="${2:-CHANGELOG.md}"

section=$(awk -v heading="## [$version]" '
    /^## \[/ { if (found) exit; if (index($0, heading) == 1) { found = 1; next } }
    /^\[[^]]+\]: / { if (found) exit }   # link references at the end of the file
    found { print }
' "$changelog" | sed -e '/./,$!d' | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}')

if [ -z "$section" ]; then
    echo "No '## [$version]' section with entries in $changelog. Move the Unreleased entries there first." >&2
    exit 1
fi
printf '%s\n' "$section"
