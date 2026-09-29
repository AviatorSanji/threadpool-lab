#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [[ -n "${JAVA_HOME:-}" ]]; then
    lab_jdk="$JAVA_HOME"
else
    lab_jdk="$(/usr/libexec/java_home -v 21)"
fi

mkdir -p target/classes
"$lab_jdk/bin/javac" --release 21 -encoding UTF-8 -d target/classes src/main/java/lab/*.java
"$lab_jdk/bin/java" -cp target/classes lab.Lesson01 "$@"
