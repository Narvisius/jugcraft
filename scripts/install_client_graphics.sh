#!/usr/bin/env bash
# Ubuntu 24.04 client runners: signed graphics packages without unrelated vendor indexes.
set -euo pipefail

sources=$(mktemp --suffix=.list)
trap 'rm -f "$sources"' EXIT
chmod 644 "$sources"
apt_options=(
  -o "Dir::Etc::sourcelist=$sources" -o Dir::Etc::sourceparts=-
  -o APT::Update::Error-Mode=any
  -o Acquire::Languages=none
  -o Acquire::Retries=1 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30
)
packages=(mesa-vulkan-drivers libgl1-mesa-dri libglx-mesa0 libegl1 libegl-mesa0 mesa-utils xvfb)

# APT's socket timeout does not bound a download that continues to trickle bytes.
# Bound indexes to two minutes and downloads to five: a healthy but slow 17.5 MB
# Mesa transfer exceeded the former two-minute download limit on hosted runners.
# This source override applies only to these commands, never the runner's global configuration.
# Azure's HTTP endpoint matches actions/runner-images' configure-apt-sources.sh;
# its HTTPS endpoint timed out in CI. APT still verifies Ubuntu signatures/hashes.
for mirror in https://archive.ubuntu.com/ubuntu http://azure.archive.ubuntu.com/ubuntu; do
  : > "$sources"
  for suite in noble noble-updates noble-backports noble-security; do
    printf 'deb [signed-by=/usr/share/keyrings/ubuntu-archive-keyring.gpg] %s %s main universe restricted multiverse\n' \
      "$mirror" "$suite" >> "$sources"
  done
  echo "Preparing client graphics from $mirror"
  if sudo timeout --kill-after=10s 120s apt-get "${apt_options[@]}" update &&
      sudo timeout --kill-after=10s 300s apt-get "${apt_options[@]}" install --download-only -y "${packages[@]}"; then
    # Finish from verified local archives. Never retry or conceal a dpkg/install failure.
    sudo timeout --kill-after=10s 120s apt-get "${apt_options[@]}" install --no-download -y "${packages[@]}"
    exit 0
  fi
  echo "::warning::Graphics package fetch failed or timed out at $mirror"
done
echo '::error::Unable to fetch required signed graphics packages from either Ubuntu mirror.' >&2
exit 1
