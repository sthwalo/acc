# FIN Application Development Guide

This document provides guidelines and information for developers working on the FIN application.

## Development Environment Setup

1. **Prerequisites**:
   - Java Development Kit (JDK) 17 or later
   - Git for version control
   - IDE with Java support (IntelliJ IDEA, Eclipse, VS Code with Java extensions, etc.)

2. **Getting the Source Code**:
   ```bash
   git clone <repository-url>
   cd FIN
   ```

3. **Building the Project**:
   ```bash
   ./gradlew build
   ```

## Project Structure

```
FIN/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/java/fin/  # Application source code
│   │   └── test/java/fin/  # Test source code
│   └── build.gradle.kts    # Module build configuration
├── docs/                   # Documentation
├── gradle/                 # Gradle wrapper files
├── gradlew                 # Gradle wrapper script (Unix)
├── gradlew.bat             # Gradle wrapper script (Windows)
└── settings.gradle.kts     # Project settings
```

## Key Files

- **App.java**: Main application class with the interactive menu and core functionality
- **AppTest.java**: JUnit tests for the App class
- **build.gradle.kts**: Gradle build configuration for the app module

## Build Commands

- `./gradlew build`: Builds the project and runs tests
- `./gradlew test`: Runs the tests
- `./gradlew run`: Runs the application
- `./gradlew distZip`: Creates a ZIP distribution
- `./gradlew distTar`: Creates a TAR distribution
- `./gradlew clean`: Cleans the build directory

## Testing

The project uses JUnit Jupiter for testing. Tests are located in the `app/src/test/java/fin` directory.

To run tests:
```bash
./gradlew test
```

## Adding Features

When adding new features to the application:

1. **Update App.java**:
   - Add new methods for the feature functionality
   - Update the menu in `showMenu()` method
   - Add a new case in the switch statement in `main()`

2. **Update Tests**:
   - Add tests for new functionality in AppTest.java
   - Ensure all tests pass with `./gradlew test`

3. **Update Documentation**:
   - Update relevant documentation files in the `docs` directory

## Code Style Guidelines

- Use standard Java naming conventions
- Add comments for complex logic
- Write unit tests for all new functionality
- Keep methods focused on a single responsibility

## Distribution

To create a distributable version of the application:

```bash
./gradlew distZip
```

The distribution will be created in `app/build/distributions/`.

## Continuous Integration

(Add information about CI setup if applicable)

## Version Control Guidelines

- Use descriptive commit messages
- Create feature branches for new features
- Submit pull requests for code review before merging to main branch
