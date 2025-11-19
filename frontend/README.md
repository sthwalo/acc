# FIN Frontend - Container-First Development

## Overview
The FIN frontend is a React/TypeScript application built with Vite, following container-first development principles as outlined in the main project [copilot-instructions.md](../.github/copilot-instructions.md).

## Architecture Principles

### Container-First Development (MANDATORY)
All frontend development **MUST** use the containerized backend. No direct localhost connections allowed.

### Backend-Aligned Patterns
- **Service Registry**: Services registered in central registry (similar to backend ApplicationContext)
- **Fail-Fast Error Handling**: No fallback data - exceptions thrown when data unavailable
- **Repository Pattern**: Data access through service layer

## Development Setup

### Prerequisites
- Node.js 18+
- Docker & Docker Compose
- Backend running in container (see main README)

### Quick Start - Container-First Workflow

```bash
# 1. Start containerized backend
npm run workflow:backend

# 2. Start frontend development server (proxies to container)
npm run dev

# 3. Open browser to http://localhost:3000
```

### Alternative: Full Containerized Environment

```bash
# Start everything in containers (backend + frontend)
npm run workflow:full

# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
```

### Manual Container Setup

```bash
# Start backend in container
docker-compose up -d fin-app postgres

# Wait for backend health check
curl http://localhost:8080/health

# Start frontend
npm run dev
```

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server with container proxy |
| `npm run build` | Build for production |
| `npm run build:verify` | Full build verification (type-check + lint + build) |
| `npm run type-check` | TypeScript type checking |
| `npm run lint` | ESLint code quality check |
| `npm run lint:fix` | Auto-fix ESLint issues |
| `npm run workflow` | Show development workflow options |
| `npm run workflow:backend` | Start containerized backend |
| `npm run workflow:full` | Start full containerized environment |

## Environment Configuration

### Development (.env.development)
- `VITE_API_URL=http://localhost:8080/api/v1` - Containerized backend
- `CONTAINER_MODE=true` - Enables container-specific features
- `VITE_CORS_ENABLED=true` - CORS for development

### Production (.env.production)
- `VITE_API_URL=/api/v1` - Reverse proxy to backend
- `CONTAINER_MODE=true` - Always containerized in production

## Code Quality Standards

### Pre-commit Requirements
Before committing any changes:

```bash
# Build verification (MANDATORY)
npm run build:verify

# Test against containerized backend
npm run workflow:backend
npm run dev

# Verify functionality works
# Get user confirmation: "Changes tested and working"
```

### Error Handling Standards
- **NO fallback data allowed** - throw exceptions when data unavailable
- **Clear error messages** with specific guidance
- **Fail-fast policy** - stop execution on data errors

### Component Patterns
```typescript
// ✅ CORRECT: Use service registry
import { useApi } from '../hooks/useApi';

function MyComponent() {
  const api = useApi();

  const loadData = async () => {
    const data = await api.getCompanies(); // Throws on empty data
    // Use real data - NO FALLBACK
  };
}

// ❌ WRONG: Direct service import
import { apiService } from '../services/api'; // VIOLATION
```

## Testing Against Containers

### API Testing
```bash
# Test health endpoint
curl http://localhost:8080/health

# Test API endpoints
curl http://localhost:8080/api/v1/companies

# Frontend integration test
curl http://localhost:3000/api/v1/health # Should proxy to backend
```

### CORS Verification
```bash
# Test CORS headers
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS http://localhost:8080/api/v1/companies
```

## Docker Integration

### Frontend Container
```bash
# Build frontend container
npm run docker:build

# Run frontend container
npm run docker:run
```

### Full Stack Containers
```bash
# Start all services
docker-compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# View logs
docker-compose logs -f frontend
```

## Troubleshooting

### Backend Connection Issues
```bash
# Check backend health
curl http://localhost:8080/health

# Check container logs
docker-compose logs fin-app

# Restart backend
docker-compose restart fin-app
```

### Frontend Proxy Issues
```bash
# Check Vite dev server logs
npm run dev

# Verify proxy configuration in vite.config.ts
# Should proxy /api/* to http://localhost:8080
```

### Port Conflicts
```bash
# Check what's using ports
lsof -i :3000
lsof -i :8080

# Kill process using port
kill -9 <PID>
```

## Compliance Checklist

### Pre-Commit Verification
- [ ] `npm run build:verify` passes
- [ ] Tested against containerized backend
- [ ] No hardcoded fallback data
- [ ] Error handling follows fail-fast policy
- [ ] User confirmed changes work

### Code Review Standards
- [ ] Uses service registry pattern
- [ ] Implements proper error boundaries
- [ ] No console.log in production code
- [ ] Component follows established patterns
- [ ] API calls have proper error handling

## Architecture Alignment

This frontend follows the same architectural principles as the Java backend:

| Backend | Frontend |
|---------|----------|
| `ApplicationContext` | `ServiceRegistry` |
| `@Autowired` services | `useApi()` hook |
| SQLException on empty data | `throw new Error()` on empty data |
| Repository pattern | Service layer pattern |
| JAR-first deployment | Container-first development |

## Contributing

1. Follow container-first development workflow
2. Implement backend-aligned patterns
3. Test against containerized backend
4. Get user confirmation before committing
5. Update documentation for API changes

---

**See also**: [Main Project Instructions](../.github/copilot-instructions.md)
