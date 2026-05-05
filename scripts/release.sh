#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

usage() {
  cat <<'USAGE'
Usage: scripts/release.sh [release-version] [--check-command <command>] [--publish-command <command>] [--branch <branch>] [--push] [--publish] [--force-tag]

Example:
  scripts/release.sh
  scripts/release.sh 2.0.0
  scripts/release.sh 2.0.0 --publish --push

Creates an annotated v<release-version> tag from the current commit.
Project versions are derived from git describe: vX.Y.Z is a release, commits after it are X.Y.Z-dev.N.hash.
USAGE
}

die() {
  echo -e "${RED}release.sh: $*${NC}" >&2
  exit 1
}

info() {
  echo -e "${GREEN}$*${NC}"
}

warn() {
  echo -e "${YELLOW}$*${NC}"
}

ask_yes_no() {
  local prompt="$1"
  local default="${2:-n}"
  local suffix="[y/N]"
  local answer

  if [[ "$default" == "y" ]]; then
    suffix="[Y/n]"
  fi

  read -r -p "$prompt $suffix: " answer
  if [[ -z "$answer" ]]; then
    [[ "$default" == "y" ]]
    return
  fi

  [[ "$answer" =~ ^[yY]$ ]]
}

release_version=""
publish=false
push=false
force_tag=false
expected_branch=""
check_command=""
publish_command=""

while (($#)); do
  case "$1" in
    --publish) publish=true ;;
    --push) push=true ;;
    --force-tag) force_tag=true ;;
    --check-command)
      shift
      [[ $# -gt 0 ]] || die "--check-command requires a value"
      check_command="$1"
      ;;
    --publish-command)
      shift
      [[ $# -gt 0 ]] || die "--publish-command requires a value"
      publish_command="$1"
      ;;
    --branch)
      shift
      [[ $# -gt 0 ]] || die "--branch requires a value"
      expected_branch="$1"
      ;;
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
      else
        die "too many arguments"
      fi
      ;;
  esac
  shift
done

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

[[ -z "$(git status --porcelain)" ]] || die "working tree must be clean before releasing"

if [[ -z "$check_command" ]]; then
  if [[ -x ./gradlew ]]; then
    check_command="./gradlew check"
  elif [[ -f ./gradlew.bat ]]; then
    check_command="./gradlew.bat check"
  else
    die "Gradle wrapper not found"
  fi
fi

if [[ -z "$publish_command" ]]; then
  if [[ -x ./gradlew ]]; then
    publish_command="./gradlew publish"
  elif [[ -f ./gradlew.bat ]]; then
    publish_command="./gradlew.bat publish"
  else
    die "Gradle wrapper not found"
  fi
fi

current_branch="$(git rev-parse --abbrev-ref HEAD)"
default_branch="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
if [[ -z "$expected_branch" ]]; then
  expected_branch="${default_branch:-$current_branch}"
fi

warn "Current branch: $current_branch"
if [[ -n "$expected_branch" && "$current_branch" != "$expected_branch" ]]; then
  warn "Expected branch: $expected_branch"
  ask_yes_no "Continue from $current_branch?" "n" || die "release cancelled"
fi

current_version="unknown"
default_release_version=""
if describe="$(git describe --tags --long --match 'v*' 2>/dev/null)"; then
  base="$(echo "$describe" | sed 's/-[0-9]*-g[0-9a-f]*$//' | sed 's/^v//')"
  commits="$(echo "$describe" | sed 's/.*-\([0-9]*\)-g[0-9a-f]*$/\1/')"
  hash="$(echo "$describe" | sed 's/.*-g//')"
  if [[ "$commits" -eq 0 ]]; then
    current_version="$base"
    default_release_version="$base"
  else
    current_version="${base}-dev.${commits}.${hash}"
    default_release_version="$(python3 - "$base" <<'PY'
import re
import sys
version = sys.argv[1]
match = re.fullmatch(r"(.+\.)(\d+)", version)
if match:
    prefix, patch = match.groups()
    print(f"{prefix}{int(patch) + 1}")
else:
    print(version)
PY
)"
  fi
else
  warn "No existing v* tag found."
fi

prompt_default() {
  local label="$1"
  local default_value="$2"
  local value

  read -r -p "$label [$default_value]: " value
  echo "${value:-$default_value}"
}

if [[ -z "$release_version" ]]; then
  if [[ -n "$default_release_version" ]]; then
    release_version="$(prompt_default "Release version" "$default_release_version")"
  else
    read -r -p "Release version: " release_version
  fi
fi

[[ "$release_version" != *-SNAPSHOT ]] || die "release version must not end with -SNAPSHOT"
[[ "$release_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "release version must follow semver format (e.g. 2.1.0)"

tag_name="v$release_version"
tag_exists=false
if git rev-parse --verify "$tag_name" >/dev/null 2>&1; then
  tag_exists=true
  warn "Tag $tag_name already exists."
  if [[ "$force_tag" != true ]]; then
    ask_yes_no "Force recreate $tag_name?" "n" || die "release cancelled"
    force_tag=true
  fi

  read -r -p "Type 'force $tag_name' to confirm: " force_confirm
  [[ "$force_confirm" == "force $tag_name" ]] || die "force tag confirmation failed"
fi

ask_yes_no "Run checks with '$check_command'?" "y" && run_checks=true || run_checks=false
if [[ "$publish" != true ]]; then
  ask_yes_no "Publish artifacts with '$publish_command' after tagging?" "n" && publish=true || publish=false
fi
if [[ "$push" != true ]]; then
  ask_yes_no "Push tag to origin?" "n" && push=true || push=false
fi

echo
echo "Release plan:"
echo "  branch: $current_branch"
echo "  current: $current_version"
echo "  release: $release_version"
echo "  tag: $tag_name"
echo "  force tag: $force_tag"
echo "  checks: $run_checks"
echo "  publish: $publish"
echo "  push: $push"
echo
ask_yes_no "Continue?" "n" || die "release cancelled"

if [[ "$run_checks" == true ]]; then
  eval "$check_command"
fi

if [[ "$force_tag" == true ]]; then
  git tag -fa "$tag_name" -m "Release $release_version"
else
  git tag -a "$tag_name" -m "Release $release_version"
fi

if [[ "$publish" == true ]]; then
  eval "$publish_command"
fi

if [[ "$push" == true ]]; then
  if [[ "$force_tag" == true && "$tag_exists" == true ]]; then
    git push origin "$tag_name" --force
  else
    git push origin "$tag_name"
  fi
fi

info "Done! Created $tag_name."
