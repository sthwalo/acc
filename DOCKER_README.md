# FIN Application Docker Setup

This setup provides containerized deployment for the FIN application with PostgreSQL databases.

## Prerequisites

- Docker and Docker Compose installed
- `.env` file with database credentials (see `.env.example`)

## Quick Start

1. **Configure Environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your actual database credentials
   ```

2. **Build and Run:**
   ```bash
   # Build the application
   docker-compose build

   # Start all services (production)
   docker-compose up -d

   # View logs
   docker-compose logs -f fin-app
   ```

3. **Access the Application:**
   - Console UI: `docker-compose exec fin-app java -jar app/build/libs/app.jar`
   - API Server: `docker-compose exec fin-app java -jar app/build/libs/app.jar api`
   - Batch Processing: `docker-compose exec fin-app java -jar app/build/libs/app.jar --batch [command]`

## Services

- **postgres**: Production PostgreSQL database
- **postgres-test**: Test PostgreSQL database
- **fin-app**: Production FIN application
- **fin-app-test**: Test FIN application

## Database Initialization

The databases are automatically initialized with:
- `test_schema.sql` (test database)
- Migration files from `app/src/main/resources/db/migration/`
- `budget_schema.sql`
- `limelight_budget_seed.sql`

## Environment Variables

All sensitive configuration is handled via environment variables:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` (production)
- `TEST_DATABASE_URL`, `TEST_DATABASE_USER`, `TEST_DATABASE_PASSWORD` (test)

## Testing

```bash
# Run tests in container
docker-compose exec fin-app-test ./gradlew test

# Run specific test
docker-compose exec fin-app-test ./gradlew test --tests "*TestClass*"
```

## Development

```bash
# Rebuild after code changes
docker-compose build --no-cache fin-app

# View database
docker-compose exec postgres psql -U $DATABASE_USER -d drimacc_db

# Stop all services
docker-compose down

# Clean up volumes (WARNING: deletes data)
docker-compose down -v
```

## Troubleshooting

- **Database connection issues**: Check `.env` file and ensure credentials are correct
- **Application won't start**: Check logs with `docker-compose logs fin-app`
- **Port conflicts**: Modify ports in `docker-compose.yml` if needed
- **Memory issues**: Adjust `JAVA_OPTS` in `.env` file

## Security Notes

- Credentials are never hardcoded in Docker files
- `.env` file is excluded from Docker context via `.dockerignore`
- Database volumes persist data between container restarts