#!/usr/bin/env sh
set -e
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper jar not found. It will be downloaded."
  WRAPPER_URL=$(grep -E '^wrapperUrl=' "$PROPS" | sed 's/wrapperUrl=//')
  mkdir -p "$(dirname "$WRAPPER_JAR")"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "Neither curl nor wget found. Please install one or run mvn directly."
    exit 1
  fi
fi

exec java -jar "$WRAPPER_JAR" "$@"
