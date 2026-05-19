#!/bin/sh
# Gradle wrapper starter script
# If wrapper jar is missing, download it first

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://services.gradle.org/distributions/gradle-8.5-bin.zip"
PROPERTIES_FILE="gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "distributionUrl=$WRAPPER_URL" > "$PROPERTIES_FILE"
    echo "distributionBase=GRADLE_USER_HOME" >> "$PROPERTIES_FILE"
    echo "distributionPath=wrapper/dists" >> "$PROPERTIES_FILE"
    echo "zipStoreBase=GRADLE_USER_HOME" >> "$PROPERTIES_FILE"
    echo "zipStorePath=wrapper/dists" >> "$PROPERTIES_FILE"
fi

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper JAR not found. Please run: gradle wrapper --gradle-version 8.5"
    echo "Or open this project in IntelliJ IDEA which will handle this automatically."
    exit 1
fi

exec java -jar "$WRAPPER_JAR" "$@"
