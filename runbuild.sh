#!/bin/bash

# FIN Application Runner Script
# This script runs the FIN application with proper memory settings

echo "🚀 Starting FIN Financial Management System"
echo "📊 Console Application"
echo "==============================================="

# Clean and build the project first
echo "🔨 Building project..."
./gradlew clean build -x test -x checkstyleMain -x checkstyleTest
echo "✅ Build successful."

# Set memory options
JAVA_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m -Dfin.license.autoconfirm=true"

# For beta testing, always use direct JAR execution
echo "Using direct JAR execution for beta testing..."

# Use the fat JAR which includes all dependencies (with signature files excluded)
FAT_JAR="./app/build/libs/app-fat.jar"

if [ -f "$FAT_JAR" ]; then
    echo "Running with fat JAR: $FAT_JAR"
    java $JAVA_OPTS -jar "$FAT_JAR" "$@"
else
    echo "Fat JAR not found, falling back to classpath execution..."
    JARS=$(find . -name "*.jar" | grep -v "gradle-wrapper" | tr '\n' ':')
    java $JAVA_OPTS -cp "$JARS" fin.ConsoleApplication "$@"
fi
