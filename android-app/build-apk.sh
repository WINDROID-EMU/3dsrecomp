#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/android-app"

if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Android/Sdk" ]; then
    SDK="$HOME/Android/Sdk"
  else
    SDK="$HOME/android-sdk"
  fi
else
  SDK="$ANDROID_HOME"
fi

NDK="${ANDROID_NDK_HOME:-$SDK/ndk/27.2.12479018}"
API="${ANDROID_API:-26}"
BUILD_TOOLS="${ANDROID_BUILD_TOOLS:-$SDK/build-tools/35.0.0}"
PLATFORM="$SDK/platforms/android-35/android.jar"

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export ANDROID_NDK_HOME="$NDK"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$HOME/.cargo/bin:$PATH"

: "${ANDROID_HOME:?ANDROID_HOME is required}"
test -x "$BUILD_TOOLS/aapt2"
test -x "$BUILD_TOOLS/d8"
test -x "$BUILD_TOOLS/zipalign"
test -f "$PLATFORM"
test -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
command -v javac >/dev/null
command -v cargo-ndk >/dev/null

rm -rf "$APP/build/classes" "$APP/build/dex" "$APP/build/generated" "$APP/build/lib" "$APP/build/apk-unsigned.apk" "$APP/build/apk-aligned.apk"
mkdir -p "$APP/build/classes" "$APP/build/dex" "$APP/build/generated" "$APP/build/lib/arm64-v8a"

cargo ndk -t arm64-v8a -P "$API" build -p zakuro \
  --no-default-features --features android --lib
cp "$ROOT/target/aarch64-linux-android/debug/libzakuro.so" "$APP/build/lib/arm64-v8a/libmain.so"

"$BUILD_TOOLS/aapt2" compile --dir "$APP/src/main/res" -o "$APP/build/res.zip"
"$BUILD_TOOLS/aapt2" link \
  -o "$APP/build/apk-unsigned.apk" \
  -I "$PLATFORM" \
  --manifest "$APP/src/main/AndroidManifest.xml" \
  --java "$APP/build/generated" \
  "$APP/build/res.zip"

# Compile both generated R.java and the application's source Java files
find "$APP/build/generated" "$APP/src/main/java" -name '*.java' -print0 | xargs -0 javac -source 8 -target 8 \
  -classpath "$PLATFORM" -d "$APP/build/classes"
jar cf "$APP/build/classes.jar" -C "$APP/build/classes" .
"$BUILD_TOOLS/d8" --lib "$PLATFORM" --min-api "$API" \
  --output "$APP/build/dex" "$APP/build/classes.jar"
cp "$APP/build/dex/classes.dex" "$APP/build/classes.dex"

cd "$APP/build"
zip -q -r apk-unsigned.apk lib classes.dex
"$BUILD_TOOLS/zipalign" -f 4 apk-unsigned.apk apk-aligned.apk

if [ -x "$BUILD_TOOLS/apksigner" ]; then
  KEYSTORE="$APP/build/debug.keystore"
  if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v -keystore "$KEYSTORE" \
      -storepass android -keypass android -alias androiddebugkey \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -dname 'CN=Android Debug,O=Android,C=US'
  fi
  "$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android \
    --ks-key-alias androiddebugkey --key-pass pass:android \
    --out "$APP/build/zakuro-arm64-debug.apk" "$APP/build/apk-aligned.apk"
  printf 'APK assinado gerado em %s\n' "$APP/build/zakuro-arm64-debug.apk"
fi
