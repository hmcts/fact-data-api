#!/usr/bin/env bash
set -euo pipefail

MIGRATION_DIR="src/main/resources/migration"
DUP_FILE="duplicate_versions.txt"

# Clean up previous results
rm -f "$DUP_FILE"

# Get all version prefixes in master
MASTER_VERSIONS=$(git ls-tree -r --name-only origin/master -- "$MIGRATION_DIR" \
  | xargs -n1 basename \
  | grep -E '^V[0-9]+\.[0-9]+' \
  | sed -E 's/^(V[0-9]+\.[0-9]+).*/\1/' \
  | sort -u || true)

# Get files added/modified in this PR
PR_VERSIONS=()
while IFS= read -r filepath; do
  [[ -z "$filepath" ]] && continue
  filename=$(basename "$filepath")

  if [[ "$filename" =~ ^(V[0-9]+\.[0-9]+) ]]; then
    PR_VERSIONS+=("${BASH_REMATCH[1]}")
  fi
done < <(git diff --name-only origin/master...HEAD --diff-filter=AM -- "$MIGRATION_DIR" || true)

# Early exit if no migrations in PR
if [[ ${#PR_VERSIONS[@]} -eq 0 ]]; then
  echo "No Flyway migrations found in PR"
  exit 0
fi

# Check for duplicates within the PR itself
readarray -t SORTED_PR_VERSIONS < <(printf '%s\n' "${PR_VERSIONS[@]}" | sort)
INTERNAL_DUPS=$(printf '%s\n' "${SORTED_PR_VERSIONS[@]}" | uniq -d)

DUPLICATES=()

# Check against master
for version in "${PR_VERSIONS[@]}"; do
  if echo "$MASTER_VERSIONS" | grep -Fxq "$version"; then
    DUPLICATES+=("$version (exists in master)")
  fi
done

# Add internal duplicates
if [[ -n "$INTERNAL_DUPS" ]]; then
  while IFS= read -r dup; do
    [[ -z "$dup" ]] && continue
    DUPLICATES+=("$dup (duplicate within PR)")
  done <<< "$INTERNAL_DUPS"
fi

# Report results
if [[ ${#DUPLICATES[@]} -gt 0 ]]; then
  printf '%s\n' "${DUPLICATES[@]}" | tee "$DUP_FILE"
  exit 1
fi

echo "No duplicate Flyway versions detected"
exit 0
