# Resource Booking System

A secure, production-ready RESTful Resource Booking System backend developed with **Spring Boot 2.7.5** (using standard `javax.*` packages), **Java 17+**, **Spring Security**, **JWT Authentication**, **Role-Based Access Control (RBAC)**, and **PostgreSQL**.

---

## 🛠️ Features

- **Authentication & Authorization**: Stateless JWT token authentication with RBAC (`ADMIN` and `USER` roles) using BCrypt password hashing.
- **Resource Management**: Complete CRUD operations for bookable assets (e.g. Conference Rooms, Equipment, Vehicles).
- **Reservation Lifecycle & Conflict Prevention**:
  - Automatically validates against overlapping / double bookings for the same resource.
  - Supports reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`.
  - Enforces ownership: `USER` can access only their own reservations, while `ADMIN` has full access across all reservations.
- **Advanced Querying**:
  - Filter reservations by `status`, `minPrice`, and `maxPrice`.
  - Pagination with `page` and `size` parameters.
  - Dynamic sorting by field (`price`, `startTime`, `endTime`, `createdAt`, `status`) and direction (`asc`, `desc`).
- **Data Integrity & Production Security**:
  - Decimal precision for monetary prices (`BigDecimal`).
  - Production-ready safe schema validation (`ddl-auto=validate` with accompanying `schema.sql`).
  - Comprehensive global exception handling (`GlobalExceptionHandler`) and structured error responses.
  - SLF4J structured logging.
- **Interactive Documentation**: Swagger / OpenAPI 3.0 UI with Bearer JWT authorization support.

---

## 👥 Seed Users & Pre-configured Data

Upon startup, the system seeds initial users (if not present) and default resources:

| Username | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | Full CRUD on resources and all reservations |
| `user` | `user123` | `USER` | Read-only resources; create and manage own reservations |

---

## ⚙️ Configuration & Environment Variables

The application can be configured via environment variables:

| Variable | Description | Default / Development Fallback |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/resource_booking_db` |
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `JWT_SECRET` | 256-bit Base64-encoded secret key | Required via environment variable |
| `SEED_ADMIN_PASSWORD`| Initial seeded `admin` user password | `admin123` |
| `SEED_USER_PASSWORD` | Initial seeded `user` password | `user123` |
| `JPA_DDL_AUTO` | Hibernate DDL mode (`validate`, `update`, `create-drop`) | `update` (dev) / `validate` (prod) |
| `JPA_SHOW_SQL` | Hibernate SQL logging (`true`, `false`) | `false` |

> **Security Note**: In production, always supply a strong, unique 256-bit Base64-encoded secret via `JWT_SECRET`, set custom `SEED_ADMIN_PASSWORD` / `SEED_USER_PASSWORD`, and maintain `JPA_DDL_AUTO=validate`.

---

## 📡 API Endpoints & RBAC Matrix

### 1. Authentication Endpoints (`/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/login` | Public | Authenticates credentials and returns JWT bearer token + role |

### 2. Resource Endpoints (`/api/resources`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/resources` | `USER`, `ADMIN` | List all available resources |
| `GET` | `/api/resources/{id}` | `USER`, `ADMIN` | Retrieve resource details by ID |
| `POST` | `/api/resources` | `ADMIN` | Create a new resource |
| `PUT` | `/api/resources/{id}` | `ADMIN` | Update an existing resource |
| `DELETE` | `/api/resources/{id}` | `ADMIN` | Delete a resource |

### 3. Reservation Endpoints (`/reservations`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/reservations` | `USER`, `ADMIN` | Create a reservation (User identity resolved from JWT; prevents overlapping bookings) |
| `GET` | `/reservations` | `USER`, `ADMIN` | Paginated, filtered, and sorted search (`ADMIN` sees all; `USER` sees only own) |
| `GET` | `/reservations/{id}` | `USER`, `ADMIN` | Retrieve reservation by ID (enforces user ownership) |
| `PUT` | `/reservations/{id}` | `USER`, `ADMIN` | Update reservation time/resource (enforces ownership and conflict checks) |
| `DELETE` | `/reservations/{id}` | `USER`, `ADMIN` | Delete/cancel reservation (enforces user ownership) |

---

## 🔍 Reservation Search & Filtering Examples

### Query Parameters:
- `status`: `PENDING`, `CONFIRMED`, `CANCELLED` (optional)
- `minPrice`: Minimum reservation price filter (optional)
- `maxPrice`: Maximum reservation price filter (optional)
- `page`: Page index, zero-based (default: `0`)
- `size`: Page size, 1–100 (default: `10`)
- `sortBy`: `price`, `startTime`, `endTime`, `createdAt`, `status` (default: `createdAt`)
- `direction`: `asc` or `desc` (default: `desc`)

### Example Request:
```http
GET /reservations?status=PENDING&minPrice=100.00&maxPrice=1000.00&page=0&size=10&sortBy=startTime&direction=asc
Authorization: Bearer <jwt-token>
```

---

## 🚀 Running the Application

### Prerequisites
- Java 17 or higher
- PostgreSQL (or in-memory H2 for tests)
- Maven 3.8+ (or included `./mvnw`)

### Build
```bash
./mvnw clean package
```

### Run Tests
```bash
./mvnw test
```

### Run Application
```bash
./mvnw spring-boot:run
```

---

## 📖 Interactive API Documentation

Interactive Swagger / OpenAPI UI is accessible when running:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI v3 JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
