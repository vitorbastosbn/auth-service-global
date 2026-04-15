# Auth Service Global

A production-ready authentication microservice built with Spring Boot 3, Spring Security 6, and JWT.

## Stack

- **Java 17** + Spring Boot 3.2.x
- **Spring Security 6** (stateless JWT-based)
- **Spring Data JPA** + PostgreSQL 15
- **Flyway** for database migrations
- **JJWT 0.12.x** for JWT generation/validation
- **Lombok** + **MapStruct**
- **Docker** + **Docker Compose**

---

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15+ (or Docker)
- Docker & Docker Compose (optional)

---

## Local Development Setup

### 1. Clone and configure environment

```bash
cp .env.example .env
# Edit .env with your values
```

### 2. Start PostgreSQL

```bash
docker run -d \
  --name auth_postgres \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

### 3. Set required environment variables

```bash
export DB_URL=jdbc:postgresql://localhost:5432/auth_db
export DB_USER=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-very-secure-secret-key-at-least-32-characters-long
```

### 4. Build and run

```bash
mvn clean package -DskipTests
java -jar target/auth-service-1.0.0.jar
```

The service starts on **http://localhost:8080**.

---

## Docker Setup

```bash
# Copy and configure environment
cp .env.example .env
# Edit JWT_SECRET in .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service
```

---

## Running Tests

### Unit tests only

```bash
mvn test -Dtest="AuthServiceTest,JwtServiceTest,UserServiceTest"
```

### All tests (requires Docker for Testcontainers)

```bash
mvn test
```

---

## API Endpoints

### Authentication (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and get tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Revoke refresh token |
| POST | `/api/v1/auth/validate` | Validate an access token |

### User (authenticated)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users/me` | Get current user profile |
| PUT | `/api/v1/users/me` | Update current user profile |
| PATCH | `/api/v1/users/me/password` | Change password |
| DELETE | `/api/v1/users/me` | Delete current user account |

### Admin (ADMIN role required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/users` | List all users (paginated) |
| PATCH | `/api/v1/admin/users/{id}/role` | Update user role |
| PATCH | `/api/v1/admin/users/{id}/status` | Activate/deactivate user |

### System

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |

---

## Example Requests

See [`requests.http`](requests.http) for a complete collection of HTTP requests.

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Test@1234"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Test@1234"
}
```

**Response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "uuid-string",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": "...", "name": "John Doe", "email": "john@example.com", "role": "USER" }
}
```

---

## Default Admin User

Seeded by Flyway migration `V3`:

| Field | Value |
|-------|-------|
| Email | `admin@auth.com` |
| Password | `Admin@123` |
| Role | `ADMIN` |

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/auth_db` | PostgreSQL JDBC URL |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | *(required)* | HMAC-SHA signing secret (≥32 chars) |
| `JWT_ACCESS_EXPIRATION` | `900` | Access token TTL in seconds (15 min) |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh token TTL in seconds (7 days) |

---

## Security Features

- **BCrypt** password hashing (cost factor 12)
- **Stateless JWT** authentication
- **Refresh token rotation** on each refresh
- **Brute-force protection**: accounts blocked for 10 minutes after 5 failed login attempts
- **Role-based access control** (`USER`, `ADMIN`)
- **CORS** configured for cross-origin requests
- **Correlation ID** propagated via `X-Correlation-Id` header
