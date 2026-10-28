#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION=7.1.1
BASE="$ROOT/.local/ffmpeg"
PREFIX="$BASE/android-arm64"
NDK="${ANDROID_NDK_HOME:-${ANDROID_HOME:-$ROOT/.local/android/sdk}/ndk/28.2.13676358}"
HOST=linux-x86_64
[[ "$(uname -s)" != Darwin ]] || HOST=darwin-x86_64
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST/bin"
STAMP="$PREFIX/.krplay-build-v1"
[[ -f "$STAMP" && -f "$PREFIX/lib/libavcodec.so" && -f "$PREFIX/lib/libavutil.so" && -f "$PREFIX/lib/libswscale.so" ]] && exit 0
mkdir -p "$BASE"
ARCHIVE="$BASE/ffmpeg-n$VERSION.tar.gz"
if [[ ! -f "$ARCHIVE" ]]; then
  curl -fL --retry 3 "https://codeload.github.com/FFmpeg/FFmpeg/tar.gz/refs/tags/n$VERSION" -o "$ARCHIVE.part"
  mv "$ARCHIVE.part" "$ARCHIVE"
fi
python3 - "$ARCHIVE" <<'VERIFY'
import hashlib,sys
assert hashlib.sha256(open(sys.argv[1],'rb').read()).hexdigest() == 'f117507dc501f2a6c11f9241d8d0c3213846cfad91764361af37befd6b6c523d', 'FFmpeg source checksum mismatch'
VERIFY
tar -xf "$ARCHIVE" -C "$BASE"
mkdir -p "$BASE/build-arm64"
cd "$BASE/build-arm64"
"$BASE/FFmpeg-n$VERSION/configure" --prefix="$PREFIX" --target-os=android --arch=aarch64 \
  --enable-cross-compile --cc="$TOOLCHAIN/aarch64-linux-android29-clang" \
  --cxx="$TOOLCHAIN/aarch64-linux-android29-clang++" --ar="$TOOLCHAIN/llvm-ar" \
  --ranlib="$TOOLCHAIN/llvm-ranlib" --strip="$TOOLCHAIN/llvm-strip" \
  --disable-everything --disable-programs --disable-doc --disable-avdevice --disable-avformat \
  --disable-avfilter --disable-swresample --disable-network --disable-autodetect \
  --enable-avcodec --enable-avutil --enable-swscale --enable-decoder=h264,hevc \
  --enable-pthreads --enable-neon --enable-shared --disable-static \
  --extra-ldflags='-Wl,-z,max-page-size=16384'
make -j "${KRPLAY_BUILD_JOBS:-4}"
make install
touch "$STAMP"
