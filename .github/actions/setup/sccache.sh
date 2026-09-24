#!/usr/bin/env bash
set -euo pipefail

# Keep the toolchain deterministic and avoid GitHub API rate limits on hosted runners.
SCCACHE_VERSION="${SCCACHE_VERSION:-v0.18.0}"

get_sccache_asset_url() {
  local variant="$1"
  local archive="$2"
  echo "https://github.com/mozilla/sccache/releases/download/${SCCACHE_VERSION}/sccache-${SCCACHE_VERSION}-${variant}.${archive}"
}

# $1=variant
# $2=install_dir
# $3=exe
install_from_gh() {
  local url
  url=$(get_sccache_asset_url "$1" tar.gz)
  local dest="$2/$3"
  curl -fsSL --retry 3 --retry-delay 2 "$url" |
    tar xz -O --wildcards "*/$3" > "$dest"
  chmod +x $dest
}

if [ $RUNNER_OS = "macOS" ]; then
  brew install sccache
elif [ $RUNNER_OS = "Linux" ]; then
  install_from_gh x86_64-unknown-linux-musl /usr/local/bin sccache
elif [ $RUNNER_OS = "Windows" ]; then
  # Use the zip archive because Git Bash tar can fail to extract the Windows
  # release archive on hosted runners.
  tmp_dir=$(mktemp -d)
  archive="$tmp_dir/sccache.zip"
  dest="$(cygpath -u "$USERPROFILE")/.cargo/bin/sccache.exe"
  url=$(get_sccache_asset_url x86_64-pc-windows-msvc zip)
  curl -fL --retry 3 --retry-delay 2 "$url" -o "$archive"
  powershell.exe -NoProfile -NonInteractive -Command \
    "Expand-Archive -LiteralPath '$(cygpath -w "$archive")' -DestinationPath '$(cygpath -w "$tmp_dir")' -Force"
  mkdir -p "$(dirname "$dest")"
  cp "$tmp_dir"/sccache-*/sccache.exe "$dest"
  chmod +x "$dest"
  export PATH="$(dirname "$dest"):$PATH"
  echo "$(cygpath -w "$(dirname "$dest")")" >> "$GITHUB_PATH"
  rm -rf "$tmp_dir"
fi
