#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/release.sh [release-version] [next-snapshot-version] [--publish] [--push]

Example:
  scripts/release.sh
  scripts/release.sh 2.0.0

Creates:
  chore: release <release-version>
  tag v<release-version>
  chore: prepare next development iteration
USAGE
}

die() {
  echo "release.sh: $*" >&2
  exit 1
}

release_version=""
next_snapshot_version=""
publish=false
push=false

while (($#)); do
  case "$1" in
    --publish) publish=true ;;
    --push) push=true ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      die "unknown option: $1"
      ;;
    *)
      if [[ -z "$release_version" ]]; then
        release_version="$1"
      elif [[ -z "$next_snapshot_version" ]]; then
        next_snapshot_version="$1"
      else
        die "too many arguments"
      fi
      ;;
  esac
  shift
done

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

[[ -f gradle.properties ]] || die "gradle.properties not found"
[[ -z "$(git status --porcelain)" ]] || die "working tree must be clean before releasing"

current_version="$(sed -n 's/^buildVersion=//p' gradle.properties)"
[[ -n "$current_version" ]] || die "buildVersion not found in gradle.properties"
[[ "$current_version" == *-SNAPSHOT ]] || die "current buildVersion must be a snapshot, found: $current_version"

default_release_version="${current_version%-SNAPSHOT}"
default_next_snapshot_version="$(python3 - "$default_release_version" <<'PY'
import re
import sys

version = sys.argv[1]
match = re.fullmatch(r"(.+\.)(\d+)", version)
if not match:
    print(f"{version}-SNAPSHOT")
else:
    prefix, patch = match.groups()
    print(f"{prefix}{int(patch) + 1}-SNAPSHOT")
PY
)"

prompt_default() {
  local label="$1"
  local default_value="$2"
  local value

  read -r -p "$label [$default_value]: " value
  echo "${value:-$default_value}"
}

if [[ -z "$release_version" ]]; then
  release_version="$(prompt_default "Release version" "$default_release_version")"
fi

if [[ -z "$next_snapshot_version" ]]; then
  next_snapshot_version="$(prompt_default "Next snapshot version" "$default_next_snapshot_version")"
fi

[[ "$release_version" != *-SNAPSHOT ]] || die "release version must not end with -SNAPSHOT"
[[ "$next_snapshot_version" == *-SNAPSHOT ]] || die "next snapshot version must end with -SNAPSHOT"
git rev-parse --verify "v$release_version" >/dev/null 2>&1 && die "tag v$release_version already exists"

echo
echo "Release plan:"
echo "  current: $current_version"
echo "  release: $release_version"
echo "  tag: v$release_version"
echo "  next: $next_snapshot_version"
echo "  publish: $publish"
echo "  push: $push"
echo
read -r -p "Continue? [y/N]: " confirm
[[ "$confirm" == "y" || "$confirm" == "Y" ]] || die "release cancelled"

set_version() {
  local version="$1"
  python3 - "$version" <<'PY'
from pathlib import Path
import sys

version = sys.argv[1]
path = Path("gradle.properties")
lines = path.read_text(encoding="utf-8").splitlines()
for index, line in enumerate(lines):
    if line.startswith("buildVersion="):
        lines[index] = f"buildVersion={version}"
        break
else:
    raise SystemExit("buildVersion not found in gradle.properties")

path.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
}

run_gradle() {
  if [[ -x ./gradlew ]]; then
    ./gradlew "$@"
  elif [[ -f ./gradlew.bat ]]; then
    ./gradlew.bat "$@"
  else
    die "Gradle wrapper not found"
  fi
}

set_version "$release_version"
run_gradle check

git add gradle.properties
git commit -m "chore: release $release_version"
git tag "v$release_version"

if [[ "$publish" == true ]]; then
  run_gradle publish
fi

set_version "$next_snapshot_version"
git add gradle.properties
git commit -m "chore: prepare next development iteration"

if [[ "$push" == true ]]; then
  git push
  git push origin "v$release_version"
fi

echo "Released v$release_version and prepared $next_snapshot_version"
