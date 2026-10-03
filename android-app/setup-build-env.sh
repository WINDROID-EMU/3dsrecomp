#!/usr/bin/env bash
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"
CMDLINE_TOOLS="$SDK/cmdline-tools/latest"
mkdir -p "$SDK/cmdline-tools"
if [[ ! -x "$CMDLINE_TOOLS/bin/sdkmanager" ]]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl --retry 5 --retry-delay 2 -fL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o "$tmp/tools.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$CMDLINE_TOOLS"
  mv "$tmp/cmdline-tools" "$CMDLINE_TOOLS"
fi
command -v javac >/dev/null || { echo 'Instale o JDK antes: sudo apt install openjdk-21-jdk-headless' >&2; exit 1; }
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$CMDLINE_TOOLS/bin:$PATH"
yes | sdkmanager --licenses >/dev/null || true
for attempt in 1 2 3; do
  echo "Instalação Android, tentativa $attempt/3"
  rm -rf "$SDK/ndk/27.2.12479018" "$SDK/.temp"/*
  if sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;27.2.12479018"; then
    test -x "$SDK/build-tools/35.0.0/aapt2"
    test -x "$SDK/build-tools/35.0.0/d8"
    test -x "$SDK/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
    break
  fi
  [[ "$attempt" == 3 ]] && { echo 'Falha ao instalar o SDK/NDK após 3 tentativas.' >&2; exit 1; }
done
command -v cargo-ndk >/dev/null 2>&1 || cargo install cargo-ndk --locked
printf 'Ambiente pronto.\nANDROID_HOME=%s\nANDROID_NDK_HOME=%s/ndk/27.2.12479018\n' "$ANDROID_HOME" "$ANDROID_HOME"
