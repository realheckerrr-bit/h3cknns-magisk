#!/usr/bin/env bash

# Get latest sccache version
get_sccache_ver() {
  curl -sL 'https://api.github.com/repos/mozilla/sccache/releases/latest' | jq -r .name
}

# $1=variant
# $2=install_dir
# $3=exe
install_from_gh() {
  local ver=$(curl -sL 'https://api.github.com/repos/mozilla/sccache/releases/latest' | jq -r .name)
  local url="https://github.com/mozilla/sccache/releases/download/${ver}/sccache-${ver}-$1.tar.gz"
  local dest="$2/$3"
  curl -L "$url" | tar xz -O --wildcards "*/$3" > $dest
  chmod +x $dest
}

if [ $RUNNER_OS = "macOS" ]; then
  brew install sccache
elif [ $RUNNER_OS = "Linux" ]; then
  install_from_gh x86_64-unknown-linux-musl /usr/local/bin sccache
elif [ $RUNNER_OS = "Windows" ]; then
  # Extract the Windows binary from a file archive. Streaming the executable
  # through tar can produce an invalid Win32 image under Git Bash.
  ver=$(get_sccache_ver)
  tmp_dir=$(mktemp -d)
  archive="$tmp_dir/sccache.tar.gz"
  dest="$USERPROFILE/.cargo/bin/sccache.exe"
  url="https://github.com/mozilla/sccache/releases/download/${ver}/sccache-${ver}-x86_64-pc-windows-msvc.tar.gz"
  curl -L "$url" -o "$archive"
  tar xzf "$archive" -C "$tmp_dir"
  mkdir -p "$(dirname "$dest")"
  cp "$tmp_dir"/sccache-*/sccache.exe "$dest"
  chmod +x "$dest"
  rm -rf "$tmp_dir"
fi
