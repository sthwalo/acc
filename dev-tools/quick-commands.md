# FIN Quick Commands Cheat Sheet

## 🚀 Essential Commands

### Start Applications (Modular Architecture)
```bash
./gradlew runConsole                   # Console app (default)
./gradlew runApi                       # API server
curl http://localhost:8080/api/health  # Test API
```

### Database
```bash
psql -h localhost -p 5432 -U _user -d _db
SELECT * FROM accounts ORDER BY account_code;
SELECT COUNT(*) FROM journal_entries;
```

### File Search
```bash
find . -name "*.java" -type f                    # All Java files
grep -r "ApplicationContext" --include="*.java" .  # Find context usage
grep -r "TODO" --include="*.java" .             # Find TODOs
```

### Project Management
```bash
./gradlew clean build        # Clean build
./gradlew fatJar            # Build fat JAR
git status --porcelain       # Quick git status
git add -A && git commit -m "message"  # Quick commit
```

### Common File Operations
```bash
ls -la                       # List files with details
cat -n filename.java         # View file with line numbers
tail -f api_server.log       # Monitor API logs
tail -f console_app.log      # Monitor console logs
```

## 🎯 Most Used Commands (Updated for Modular Architecture)
1. `./gradlew runConsole` - Start console app
2. `./gradlew runApi` - Start API server
3. `psql -h localhost -U _user -d _db` - Connect to database
4. `find . -name "*.java"` - Find Java files
5. `grep -r "search_term" --include="*.java" .` - Search in code
6. `git status && git add -A` - Check and stage changes
7. `./gradlew test --tests "fin.context.*Test"` - Test dependency injection

See `commands.md` for complete reference.
