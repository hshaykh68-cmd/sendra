#!/bin/bash

# Script to download Gradle Wrapper JAR for Codemagic CI/CD
# This is called by codemagic.yaml before building

set -e

WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_DIR="gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"

echo "Creating gradle wrapper directory..."
mkdir -p "$WRAPPER_DIR"

echo "Downloading gradle-wrapper.jar from GitHub..."
curl -L -o "$WRAPPER_JAR" "$WRAPPER_URL"

echo "Verifying download..."
if [ -f "$WRAPPER_JAR" ]; then
    echo "✓ gradle-wrapper.jar downloaded successfully"
    ls -lh "$WRAPPER_JAR"
else
    echo "✗ Failed to download gradle-wrapper.jar"
    exit 1
fi

echo "Making gradlew executable..."
chmod +x gradlew

echo "Gradle wrapper setup complete!"
