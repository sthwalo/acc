#!/bin/bash
# Generate Journal Entries Script

echo "🔄 Generating Journal Entries..."

# Get the classpath from Gradle
CLASSPATH=$(./gradlew -q dependencies --configuration runtimeClasspath | grep -E '\.jar$' | tr '\n' ':')
CLASSPATH="$CLASSPATH:app/build/classes/java/main"

# Run the journal entry generator
java -cp "$CLASSPATH" -Xmx4g JournalEntryGenerator

echo "✅ Journal entry generation completed!"