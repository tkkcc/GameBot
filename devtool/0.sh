#!/usr/bin/env bash

run() {
  set -e
  arch=$(adb shell getprop ro.product.cpu.abi)
  case $arch in
  x86)
    target=i686
    ;;
  x86_64)
    target=x86_64
    ;;
  arm64-v8a)
    target=aarch64
    ;;
  armeabi-v7a)
    target=armv7
    ;;
  esac
  cargo ndk -t $target-linux-android build --release
  adb push ../target/$target-linux-android/release/libdevtool.so /data/local/tmp/libgamebot.so
}
dev() {
  cargo watch -w . -w ../gamebot -s './0.sh run'
}

"$@"
