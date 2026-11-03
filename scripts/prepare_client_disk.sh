#!/usr/bin/env bash
# The expanded checkout, restored Gradle cache and 8 GB client swap need more than
# the hosted image's default free space. Never remove SDKs on a contributor's PC.
set -euo pipefail

if [[ ${GITHUB_ACTIONS:-} != true || ${RUNNER_ENVIRONMENT:-} != github-hosted ]]; then
  echo '::error::Client disk preparation is restricted to disposable GitHub-hosted runners.' >&2
  exit 1
fi

# These fixed SDK directories are unused by Java, Gradle, Python, Node and Mesa.
# Keep every game resource, test, dependency cache and assertion.
sudo rm -rf -- /usr/local/lib/android /usr/share/dotnet /opt/ghc /usr/local/.ghcup
df -h / /mnt
