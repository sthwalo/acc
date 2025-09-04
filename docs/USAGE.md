# FIN Application Usage Guide

This document provides detailed instructions on how to run and use the FIN application.

## Prerequisites

- Java 17 or later installed on your system
- Access to a terminal or command prompt

## Running the Application

### From the Distribution Package

If you have the distribution package (`app` directory):

1. **Basic Run**:
   ```bash
   # On Unix-like systems (macOS, Linux)
   ./app/bin/app
   
   # On Windows
   app\bin\app.bat
   ```

2. **Run with Name Parameter**:
   ```bash
   # On Unix-like systems (macOS, Linux)
   ./app/bin/app "Your Name"
   
   # On Windows
   app\bin\app.bat "Your Name"
   ```

### From Source Code

If you have the source code:

1. **Using Gradle**:
   ```bash
   # Navigate to the project directory
   cd /path/to/FIN
   
   # Run the application
   ./gradlew run
   
   # Or with arguments (note the -- separator)
   ./gradlew run --args="Your Name"
   ```

## Using the Application

Once the application is running, you'll see a welcome message followed by a menu:

```
===== FIN Application Menu =====
1. Say hello
2. Show current time
3. Exit
Enter your choice (1-3):
```

### Menu Options

1. **Say hello**
   - Prompts you to enter your name
   - Displays a personalized greeting with your name

2. **Show current time**
   - Displays the current date and time in your system's default format

3. **Exit**
   - Closes the application with a goodbye message

### Interacting with the Menu

1. Type the number of your choice (1, 2, or 3) and press Enter
2. Follow any additional prompts that appear
3. After completing an action, the menu will be displayed again until you choose to exit

## Command-line Arguments

The application accepts an optional command-line argument:

- If provided, it's treated as your name and used for a personalized greeting
- Example: `./app/bin/app "John"` will display "Hello, John! Welcome to the FIN application."

## Creating a Distribution Package

To create your own distribution package:

1. Navigate to the project directory
2. Run: `./gradlew distZip` or `./gradlew distTar`
3. Extract the resulting ZIP/TAR file from `app/build/distributions/`

## Troubleshooting

- **Java Version Error**: If you see an error about "UnsupportedClassVersionError", make sure you have Java 17 or later installed
- **Permission Denied**: On Unix-like systems, you may need to make the script executable with `chmod +x app/bin/app`
- **Cannot Find Java**: Ensure that Java is installed and in your system's PATH
