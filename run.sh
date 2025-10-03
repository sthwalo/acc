#!/bin/bash

# FIN Application Runner Script (Fast Mode)
# This script runs the FIN application without building, assuming JAR is already built

echo "🚀 Starting FIN Financial Management System (Fast Mode)"
echo "📊 Console Application"
echo "==============================================="

# Set memory options
JAVA_OPTS="-Xmx1g -XX:MaxMetaspaceSize=256m -Dfin.license.autoconfirm=true"

# For fast execution, always use direct JAR execution
echo "Using direct JAR execution..."

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