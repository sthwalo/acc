**ApiResponse DTO Migration**

- **What changed:** The canonical `ApiResponse` implementation has been consolidated into the `fin.dto` module (`app/src/main/java/fin/dto/ApiResponse.java`). The duplicate `fin.controller.ApiResponse` implementation has been removed.

- **Why:** To avoid ambiguity and duplication across layers (controllers vs DTOs). Per SRP and SOC, the `ApiResponse` should live with DTOs so both backend controllers and frontend TypeScript integration use a single, consistent contract.

- **Action for engineers:**
  - Import `fin.dto.ApiResponse` in controllers: `import fin.dto.ApiResponse;`
  - Use the DTO factory signatures in controllers: `ApiResponse.success(String message, T data)` and `ApiResponse.error(String message, String errorCode)`.
  - If you see controller code using the old `ApiResponse.success(data)` or `ApiResponse.error(message)` single-argument forms, update to the new signatures (add a message or error code) to match the DTO API.

- **Frontend impact:** The TypeScript client should continue to use the same `ApiResponse<T>` shape defined in project docs (`docs/technical/TYPESCRIPT_INTEGRATION_STRATEGY.md`). No changes are required in the client if it already models the DTO shape.

- **Testing:** After migration, compile and run tests to verify all controllers use the DTO correctly.

- **Reference:** `app/src/main/java/fin/dto/ApiResponse.java` (canonical implementation)

If you want, I can add a short migration script or a linter rule to detect use of the old controller-level `ApiResponse` signature across the codebase.