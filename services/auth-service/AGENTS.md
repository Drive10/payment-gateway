# Auth Service - Agent Rules

> Specific guidelines for AI agents working on the auth-service

---

## Service Overview

- **Port**: 8081
- **Database**: PostgreSQL (users, roles)
- **Cache**: Redis (sessions, token blacklist)
- **Dependencies**: None (core service)

---

## Key Responsibilities

1. User registration and authentication
2. JWT token generation and validation
3. OAuth2 integration
4. Session management
5. RBAC (Admin, Merchant, User roles)

---

## Important Files

```
services/auth-service/
├── src/main/java/com/payflow/auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── security/JwtTokenProvider.java
│   ├── repository/UserRepository.java
│   └── dto/RegisterRequest.java, LoginRequest.java
└── src/main/resources/application.yml
```

---

## Critical Rules

1. **Passwords must be hashed** - Use BCrypt, never plain text
2. **JWT secrets minimum 32 chars** - Use secure random
3. **Token expiration** - Access: 1h, Refresh: 7 days
4. **Rate limit login attempts** - Prevent brute force

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/auth/register | Register new user |
| POST | /api/v1/auth/login | User login |
| POST | /api/v1/auth/refresh | Refresh token |
| POST | /api/v1/auth/logout | Logout (invalidate token) |
| GET | /api/v1/auth/me | Get current user |

---

## Security Implementation

### JWT Token Structure

```java
// Token contains:
{
  "sub": "user-id",
  "email": "user@example.com",
  "roles": ["MERCHANT"],
  "iat": 1712486400,
  "exp": 1712490000
}
```

### Password Hashing

```java
// NEVER do this
String hashed = md5(password);

// DO this
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashed = encoder.encode(password);
```

---

## Testing

```bash
# Run auth service tests
mvn test -pl services/auth-service
```

---

## Related Docs

- [.ai/context/services.md](../../.ai/context/services.md) - Service context
- [.ai/rules/backend.md](../../.ai/rules/backend.md) - Backend rules
- [SECURITY.md](../../SECURITY.md) - Security policy