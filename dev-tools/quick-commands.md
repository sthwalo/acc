# FIN Quick Commands Cheat Sheet

## 🚀 Essential Commands

### Start Applications
```bash
./gradlew run                           # Console app
./gradlew run --args="api"             # API server
curl http://localhost:8080/api/health  # Test API
```

### Database
```bash
psql -h localhost -p 5432 -U drimacc_user -d drimacc_db
SELECT * FROM accounts ORDER BY account_code;
SELECT COUNT(*) FROM journal_entries;
```

### File Search
```bash
find . -name "*.java" -type f                    # All Java files
grep -r "DatabaseConfig" --include="*.java" .   # Find database config
grep -r "TODO" --include="*.java" .             # Find TODOs
```

### Project Management
```bash
./gradlew clean build        # Clean build
git status --porcelain       # Quick git status
git add -A && git commit -m "message"  # Quick commit
```

### Common File Operations
```bash
ls -la                       # List files with details
cat -n filename.java         # View file with line numbers
tail -f api_server.log       # Monitor logs
```

## 🎯 Most Used Commands
1. `./gradlew run` - Start console app
2. `./gradlew run --args="api"` - Start API server  
3. `psql -h localhost -U drimacc_user -d drimacc_db` - Connect to database
4. `find . -name "*.java"` - Find Java files
5. `grep -r "search_term" --include="*.java" .` - Search in code
6. `git status && git add -A` - Check and stage changes

See `commands.md` for complete reference.
