# 403 Forbidden on /api/v1/companies/user Endpoint

## Issue Summary
After successful login, the frontend receives 403 Forbidden when calling GET /api/v1/companies/user, despite the user having valid company associations in the database.

**Date Reported**: 18 November 2025  
**Environment**: Spring Boot backend + React frontend, PostgreSQL database  
**Status**: Active - Debugging in progress  

## Symptoms
- User can log in successfully (login API returns token)
- Health check endpoint (`/api/v1/health`) works without issues
- Companies endpoint (`/api/v1/companies/user`) returns 403 Forbidden
- Error message: "Access denied. You do not have permission to perform this action."
- Frontend proxy correctly forwards requests to `localhost:8080`
- Database shows user exists and has active company relationships

## Root Cause Analysis
- **Database Integrity Confirmed**:
  - User exists in `users` table with correct email and active status
  - `user_companies` table has active associations (user_id -> company_id)
  - Referenced companies exist in `companies` table
- **Security Configuration**:
  - Spring Security requires authentication for all endpoints except `/api/v1/auth/**` and `/api/v1/health`
  - JWT authentication filter validates Bearer tokens
  - Companies endpoint requires valid JWT token
- **Likely Causes**:
  - JWT token invalid, expired, or malformed
  - Token not sent in Authorization header
  - Frontend not storing/retrieving token correctly
  - Backend JWT validation failure

## Debugging Steps
Follow these steps in order to isolate the issue:

### 1. Test Authentication Directly
- Call the `/api/v1/auth/me` endpoint to verify JWT authentication works:
  ```javascript
  // In browser console or frontend code
  const apiService = new ApiService();
  apiService.auth.getCurrentUser()
    .then(user => console.log('Auth works:', user))
    .catch(err => console.log('Auth failed:', err));
  ```
- **Expected**: Success with user data
- **If fails**: JWT/token issue - proceed to step 2

### 2. Check Browser Network Tab
- Open Developer Tools → Network tab
- Trigger the companies request
- Inspect the request headers for `/api/v1/companies/user`
- **Verify**:
  - `Authorization: Bearer <token>` header is present
  - Token is a long JWT string (not empty/null)
- **If missing**: Frontend not sending token - check ApiService interceptors

### 3. Verify Token Storage
- In browser console:
  ```javascript
  console.log(localStorage.getItem('auth_token'));
  ```
- **Expected**: Long JWT string starting with "eyJ"
- **If null/empty**: Login failed to store token - re-login

### 4. Check Backend Logs
- Monitor Spring Boot console output during request
- Look for:
  - JWT validation errors
  - "Cannot set user authentication" messages
  - Authentication success/failure logs
- **If errors**: Token invalid - check JWT secret, expiration, format

### 5. Re-login and Test
- Clear localStorage: `localStorage.clear()`
- Log in again through frontend
- Verify token stored after login
- Retry companies endpoint

### 6. Database Verification (Already Confirmed)
- User exists: `SELECT * FROM users WHERE email = 'user@example.com';`
- User companies: `SELECT * FROM user_companies WHERE user_id = ?;`
- Companies exist: `SELECT * FROM companies WHERE id IN (...);`

## Resolution Steps
Based on debugging results:

### If `/api/v1/auth/me` Works
- Authentication is functioning
- Issue is specific to companies endpoint
- Check `SpringCompanyController.getCompaniesForUser()` method
- Verify `SpringCompanyService.getCompaniesForUser()` logic
- Check for exceptions in service layer

### If `/api/v1/auth/me` Fails
- JWT authentication broken
- Check `JwtAuthenticationFilter` processing
- Verify JWT token validity
- Check `JwtService.validateToken()` method
- Re-login to refresh token

### Temporary Workaround (For Testing Only)
- Temporarily add `/api/v1/companies/user` to `permitAll()` in `SecurityConfig.java`
- **Warning**: This bypasses security - revert immediately after testing

## Prevention Measures
- Always test `/api/v1/auth/me` when encountering 403 errors
- Include token validation in frontend login flow
- Monitor JWT expiration (check token payload for `exp` field)
- Add comprehensive auth error handling in frontend
- Log authentication events in backend for debugging

## Related Files
- `frontend/src/services/ApiService.ts` - JWT interceptors
- `spring-app/src/main/java/fin/config/SecurityConfig.java` - Security rules
- `spring-app/src/main/java/fin/config/JwtAuthenticationFilter.java` - JWT processing
- `spring-app/src/main/java/fin/controller/spring/SpringCompanyController.java` - Companies endpoint
- `spring-app/src/main/java/fin/service/spring/SpringCompanyService.java` - Business logic

## Next Steps
- Execute debugging steps above
- Determine if issue is frontend token handling or backend JWT validation
- Implement fix based on findings
- Update this document with resolution