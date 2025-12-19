# Java Version Management Guide

## Overview
This document provides instructions for managing Java versions in the FIN financial management system development environment, specifically covering the downgrade from Java 21 to Java 17.

## Java 17 Migration (November 2024)

### Background
The FIN system was upgraded from Java 21 to Java 17.0.12 LTS to ensure compatibility with production requirements and maintain consistency with the project's build configuration.

### Prerequisites
- macOS development environment
- Homebrew package manager
- Oracle JDK 17 installed at `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`
- zsh shell (default on macOS)

## Step-by-Step Migration Process

### 1. Identify Available Java Installations
```bash
# List all Java installations on the system
/usr/libexec/java_home -V

# Expected output should include:
# 17.0.12 (x86_64) "Oracle Corporation" - "Java SE 17.0.12"
#     /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

### 2. Check Current PATH Configuration
```bash
# Analyze current PATH to identify Java conflicts
echo $PATH | tr ':' '\n' | grep -E 'java|jdk'

# Common issue: Homebrew Java taking precedence
# /opt/homebrew/opt/openjdk@21/bin (needs to be overridden)
```

### 3. Set Java 17 Environment Variables
```bash
# Set JAVA_HOME to Oracle JDK 17
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Prepend Java 17 bin directory to PATH
export PATH=$JAVA_HOME/bin:$PATH
```

### 4. Verify Java Version Switch
```bash
# Confirm Java 17 is active
java --version

# Expected output:
# java 17.0.12 2024-07-16 LTS
# Java(TM) SE Runtime Environment (build 17.0.12+8-LTS-286)
# Java HotSpot(TM) 64-Bit Server VM (build 17.0.12+8-LTS-286, mixed mode, sharing)
```

### 5. Test Build Compatibility
```bash
# Verify project builds successfully with Java 17
./gradlew clean build

# Expected result: BUILD SUCCESSFUL
# Note: Checkstyle warnings are expected and do not indicate errors
```

### 6. Make Configuration Persistent
```bash
# Add Java 17 configuration to zsh profile
echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc

# Verify configuration was added
tail -3 ~/.zshrc
```

### 7. Verify Gradle Integration
```bash
# Confirm Gradle is using Java 17
./gradlew --version

# Expected JVM line:
# JVM:          17.0.12 (Oracle Corporation 17.0.12+8-LTS-286)
```

## Verification Results

### ✅ Environment Status
- **Java Runtime**: Java 17.0.12 LTS (Oracle Corporation)
- **JAVA_HOME**: `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home`
- **PATH Configuration**: Java 17 bin directory prioritized over Homebrew Java
- **Gradle Integration**: Gradle 8.4 confirmed using Java 17

### ✅ Build Verification
- **Clean Build**: `./gradlew clean build` completed successfully
- **Runtime Active**: `java --version` shows correct version
- **Gradle Compatible**: No compatibility issues detected
- **Persistent Configuration**: Settings preserved across terminal sessions

### ✅ Project Compatibility
- Matches requirements in `build.gradle.kts`
- Compatible with development workflow patterns documented in `.github/copilot-instructions.md`
- No breaking changes to existing functionality

## Development Workflow (Post-Migration)

The development environment is fully configured for all operational modes:

### Console Application
```bash
java -jar app/build/libs/fin-spring.jar
# → ConsoleApplication.main() → interactive menu system
```

### API Server
```bash
java -jar app/build/libs/fin-spring.jar api
# → ApiApplication.main() → REST API on port 8080
```

### Batch Processing
```bash
java -jar app/build/libs/fin-spring.jar --batch [command]
# → automated processing mode
```

## Troubleshooting

### Common Issues

#### Issue: Wrong Java Version Active
```bash
# Problem: java --version shows Java 21 instead of 17
# Solution: Check PATH order and JAVA_HOME setting
echo $JAVA_HOME
echo $PATH | tr ':' '\n' | head -5

# Fix: Ensure Java 17 JAVA_HOME is set and PATH is prepended
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

#### Issue: Gradle Using Wrong Java Version
```bash
# Problem: ./gradlew --version shows wrong JVM
# Solution: Gradle inherits from JAVA_HOME
./gradlew --stop  # Stop Gradle daemon
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew --version  # Restart with correct Java
```

#### Issue: Homebrew Java Interference
```bash
# Problem: Homebrew Java taking precedence in PATH
# Solution: Ensure Oracle JDK comes first in PATH
# Check: /opt/homebrew/opt/openjdk@21/bin should NOT appear before Oracle JDK

# Temporary fix:
export PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH

# Permanent fix: Update ~/.zshrc with correct order
```

### Verification Commands
```bash
# Quick environment check
java --version && echo "JAVA_HOME: $JAVA_HOME"

# Full environment verification
./gradlew --version | grep JVM
echo $PATH | tr ':' '\n' | grep -E 'java|jdk' | head -3
```

## Related Documentation
- [Project Architecture](.github/copilot-instructions.md) - Development patterns and requirements
- [Build Configuration](../app/build.gradle.kts) - Gradle Java version settings
- [Development Tasks](./tasks/README.md) - Task documentation standards

## Maintenance Notes
- **Java 17 LTS**: Long-term support until September 2029
- **Oracle JDK**: Commercial license for production use
- **Compatibility**: Verified with Gradle 8.4 and project dependencies
- **Performance**: No degradation observed compared to Java 21

---
*Last Updated: November 2024*  
*Migration Completed: November 4, 2024*  
*Status: ✅ Production Ready*