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
    echo "Auto-selecting: Company 2 (Xinghizana Group), Fiscal Period 2 (FY2024-2025)"
    # Setup sequence: Company Setup -> Select Company 2 -> Back to Main -> Fiscal Period -> Select Period 2 -> Back to Main
    # Then leave it interactive for user
    (echo -e "1\n2\n6\n2\n2\n4"; cat) | java $JAVA_OPTS -jar "$FAT_JAR" "$@"
else
    echo "Fat JAR not found, falling back to classpath execution..."
    echo "Auto-selecting: Company 2 (Xinghizana Group), Fiscal Period 2 (FY2024-2025)"
    JARS=$(find . -name "*.jar" | grep -v "gradle-wrapper" | tr '\n' ':')
    (echo -e "1\n2\n6\n2\n2\n4"; cat) | java $JAVA_OPTS -cp "$JARS" fin.ConsoleApplication "$@"
fi