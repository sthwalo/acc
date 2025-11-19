## TASK: Integrate Java controllers with TypeScript dashboard

Date: 2025-11-10
Author: Copilot (task draft)

Summary
-------
This task document defines a precise integration flow that connects the existing Java REST controllers in this repository to the TypeScript (React/Vite) dashboard. It includes example client code (ServiceRegistry, ApiService), React context (AuthContext), ProtectedRoute, the Companies view, an App router layout, and the required Java endpoint mappings and CORS notes.

Purpose
-------
- Provide a copy-pasteable integration plan that frontend developers can use to call the backend APIs.
- Provide backend developers with the required endpoint signatures, CORS and header expectations so they can wire controllers accordingly.
- Provide verification steps and a Quick Start to run the services locally.

- Assumptions
- -----------
- The dashboard runs on http://localhost:5173 (Vite default) and the Java services run on ports 8080+.
- The backend uses versioned routes under `/api/v1/*` and wraps all responses in the project's `ApiResponse<T>` envelope: { success, data, message, timestamp }.
- Authentication is token-based (JWT or similar) passed in the Authorization header as `Bearer <token>`.
- Frontend will store token in localStorage (AuthContext example).
- Each microservice exposes a REST API under `/api/*`.

1) Service Registry (frontend)
--------------------------------
Create `services/ServiceRegistry.ts` in the frontend to centralize service base URLs:

```ts
// services/ServiceRegistry.ts
const ServiceRegistry = {
    authService: 'http://localhost:8080/api/v1',
    companyService: 'http://localhost:8080/api/v1',
  payrollService: 'http://localhost:8082/api',
  budgetService: 'http://localhost:8083/api',
  reportService: 'http://localhost:8084/api',
  transactionService: 'http://localhost:8085/api'
};

export default ServiceRegistry;
```

Notes: adapt base URLs to development environment or use an env-driven approach (Vite `.env` -> import.meta.env.VITE_API_BASE). By default the examples below use `/api/v1` because the server registers routes under that prefix.

API envelope note
-----------------
The backend wraps all responses in `ApiResponse<T>` (fields: `success`, `data`, `message`, `timestamp`). Frontend code must unwrap this envelope. Example unwrap helper behavior:

```ts
// pseudo
const res = await axios.post('/api/v1/auth/login', body);
// actual auth payload is at res.data.data (if success)
if (!res.data.success) throw new Error(res.data.message);
const payload = res.data.data; // { token, user }
```

The provided frontend helpers will include a small `unwrapApiResponse` utility to centralize this logic.

2) Domain-specific API services
--------------------------------
Create `services/ApiService.ts` that uses axios and `ServiceRegistry`.

Key methods (examples): login, register, getCurrentUser, getCompanies, registerCompany, getPayrollPeriods, processPayroll, getBudgets, createBudget.

The frontend will always forward `Authorization: Bearer <token>` header with protected calls.

3) Enhanced AuthContext (token management)
-----------------------------------------
Add `contexts/AuthContext.tsx` (React + TypeScript) that:
- Reads token from localStorage on start
- Validates token by calling `GET /api/auth/me`
- Exposes login(email, password) and logout()

Important: handle failed validation by clearing localStorage and redirecting to login.

4) ProtectedRoute component
----------------------------
Implement a `ProtectedRoute` component that uses `useAuth()` and redirects unauthenticated users to `/login` while showing a simple loading state during token validation.

5) Companies view (frontend)
----------------------------
Implement `components/CompaniesView.tsx` that calls `getCompanies(token)` on mount. If the user has no companies, navigate to `/register-company`.

6) Java controller endpoint mapping
-----------------------------------
Below are the minimum REST endpoints that the frontend expects. Adjust method signatures to match existing DTOs in the Java codebase.

AuthController.java
-------------------

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // validate and return { token: '...', user: {...} }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // create user
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        // validate token and return user
    }
}

CompanyController.java
----------------------

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "http://localhost:5173")
public class CompanyController {
    @GetMapping
    public ResponseEntity<List<Company>> getUserCompanies(@RequestHeader("Authorization") String token) {
        // extract user from token
    }

    @PostMapping("/register")
    public ResponseEntity<Company> registerCompany(@RequestBody CompanyRegistrationRequest request, 
                                                  @RequestHeader("Authorization") String token) {
        // create company for authenticated user
    }
}

Notes for Java implementers
---------------------------
- Follow the project's 'NO FALLBACK DATA' policy: endpoints must fail-fast with clear error messages if required DB data is missing.
- Throw meaningful exceptions if token missing/invalid: respond 401 for auth failures.
- Include CORS `@CrossOrigin(origins = "http://localhost:5173")` or use a global CORS config.
- Ensure controllers register DTOs that match frontend expectations (field names, date formatting). Prefer ISO-8601 for dates.

7) App Router (frontend)
------------------------
Example `App.tsx` wiring `AuthProvider`, `ApiProvider` and `Router` with `ProtectedRoute` around dashboard routes.

8) Quick Start (local)
----------------------
Run the Java microservices and the frontend locally. Example (adjust JAR names and ports):

```bash
# Start your Java microservices
java -jar auth-service.jar --port=8080 &
java -jar company-service.jar --port=8081 &
java -jar payroll-service.jar --port=8082 &
java -jar budget-service.jar --port=8083 &

# Start the TypeScript dashboard (from frontend/)
cd frontend
npm install
npm run dev
```

9) Verification checklist
-------------------------
- [ ] Frontend can POST /api/auth/login and receive { token, user }
- [ ] Frontend can GET /api/auth/me with Authorization header and get the user
- [ ] Frontend can GET /api/companies with Authorization header and receive a list
- [ ] Companies listing renders and navigation to register-company works when empty
- [ ] CORS issues absent when dashboard served from http://localhost:5173

10) Security & production notes
------------------------------
- Do not store sensitive secrets in localStorage in production unless using secure, short-lived tokens; prefer HttpOnly cookie flows for very sensitive setups.
- Ensure backend validates token signature and expiry on every request.

11) Next steps (recommended)
---------------------------
1. Backend: implement/stub the controller methods above and add unit/integration tests.
2. Backend: document DTOs used by endpoints so frontend types can be generated or written safely.
3. Frontend: implement the `ServiceRegistry` and `ApiService` as typed TypeScript modules and add `contexts/AuthContext` and `components/ProtectedRoute`.
4. Add an example integration test that spins up the auth service and calls `/api/auth/login` from the frontend test harness (optional).

Files to add (suggested)
------------------------
- `frontend/src/services/ServiceRegistry.ts` - service base URLs
- `frontend/src/services/ApiService.ts` - axios wrappers
- `frontend/src/contexts/AuthContext.tsx` - token management
- `frontend/src/components/ProtectedRoute.tsx` - route guard
- `frontend/src/components/CompaniesView.tsx` - companies listing

Contact
-------
If you want, I can:
- create the actual frontend files in the `frontend/src/...` tree with typed examples,
- or create Java controller skeletons under `app/src/main/java/...` for you to fill in.

If you want me to proceed and add code files, reply with: "Yes, create the frontend examples" or "Yes, create Java controller stubs" and I'll continue.

---
End of task document.
