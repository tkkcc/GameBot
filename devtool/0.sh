#!/usr/bin/env bash

run() {
  set -e
  cargo ndk -t x86 build --release
  adb push ../target/i686-linux-android/release/libdevtool.so /data/local/tmp/libgamebot.so
  # cargo ndk -t x86_64 build --release
  # adb push ../target/x86_64-linux-android/release/libgamebot.so /data/local/tmp/

}
dev() {
  cargo watch -w src -s './0.sh run'
}

"$@"
