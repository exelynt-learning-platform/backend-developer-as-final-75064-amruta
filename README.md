# Resource Booking System

A secure, production-ready RESTful Resource Booking System backend developed with **Spring Boot 2.7.5**, **Java 17**, **Spring Security**, **JWT Authentication**, **Role-Based Access Control (RBAC)**, and **PostgreSQL** (using standard `javax.*` validation and persistence annotations).

---

## 🛠️ Features

- **Authentication & Authorization**: Stateless JWT token authentication with RBAC (`ADMIN` and `USER` roles) using BCrypt password hashing.
- **Resource Management**: Complete CRUD operations for bookable assets (e.g. Conference Rooms, Equipment, Vehicles) with optional availability filtering (`?available=true`).
- **Reservation Lifecycle & Conflict Prevention**:
  - Automatically validates against overlapping / double bookings for the same resource.
  - Supports reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`.
  - Enforces ownership: `USER` can access only their own reservations, while `ADMIN` has full access across all reservations.
  - Status preservation: Non-schedule reservation updates retain existing status without unwanted reset to `PENDING`.
- **Advanced Querying**:
  - Filter reservations by `status`, `minPrice`, and `maxPrice`.
  - Pagination with `page` and `size` parameters.
  - Dynamic sorting by field (`price`, `startTime`, `endTime`, `createdAt`, `status`) and direction (`asc`, `desc`).
- **Data Integrity & Production Security**:
  - Decimal precision for monetary prices (`BigDecimal`).
  - Production-ready safe schema validation (`ddl-auto=validate`).
  - Comprehensive global exception handling (`GlobalExceptionHandler`) and structured error responses.
  - SLF4J structured logging.
- **Interactive Documentation**: Swagger / OpenAPI 3.0 UI with Bearer JWT authorization support.

---

## 👥 Seed Users & Pre-configured Data

Upon startup, the system seeds initial administrative and standard users (if not present) and default resources. The credentials and usernames for seed users are configurable via environment variables:

| Username | Default Email | Role | Permissions |
|---|---|---|---|
| `admin` (or `${SEED_ADMIN_USERNAME}`) | `admin@example.com` | `ADMIN` | Full CRUD on resources and all reservations |
| `user` (or `${SEED_USER_USERNAME}`) | `user@example.com` | `USER` | Read resources; create and manage own reservations |

---

## ⚙️ Configuration & Environment Variables

The application relies exclusively on environment variables for sensitive credentials:

| Variable | Description | Requirement / Fallback |
|---|---|---|
| `DB_URL` | PostgreSQL JDBC URL | Default: `jdbc:postgresql://localhost:5432/resource_booking_db` |
| `DB_USERNAME` | PostgreSQL username | Default: `postgres` |
| `DB_PASSWORD` | PostgreSQL password | **Required** via environment variable |
| `JWT_SECRET` | 256-bit Base64-encoded secret key | **Required** via environment variable (fail-fast startup validation) |
| `SEED_ADMIN_USERNAME`| Seeded admin username | Default: `admin` |
| `SEED_ADMIN_EMAIL`   | Seeded admin email address | Default: `admin@example.com` |
| `SEED_ADMIN_PASSWORD`| Initial seeded `admin` user password | **Required** via environment variable (min 8 characters) |
| `SEED_USER_USERNAME` | Seeded user username | Default: `user` |
| `SEED_USER_EMAIL`    | Seeded user email address | Default: `user@example.com` |
| `SEED_USER_PASSWORD` | Initial seeded `user` password | **Required** via environment variable (min 8 characters) |
| `JPA_DDL_AUTO` | Hibernate DDL mode (`validate`, `update`) | Default: `validate` (Schema auto-initialized by `schema.sql`) |

### Generating a Secure JWT Secret
To generate a secure 256-bit (32-byte) Base64-encoded signing key, run:
```bash
# Using OpenSSL:
openssl rand -base64 32
```
Set the generated key in your environment before running the application:
```bash
export JWT_SECRET="<your-generated-base64-secret>"
```

> **Security Note**: Public sample secrets and weak default passwords (< 8 characters) are systematically rejected on application startup. In all environments, supply unique, strong values via environment variables.

---

## 📡 API Endpoints & RBAC Matrix

### 1. Authentication Endpoints (`/api/auth` or `/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` (or `/auth/login`) | Public | Authenticates credentials and returns JWT bearer token + role |

### 2. Resource Endpoints (`/api/resources`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/resources` | `USER`, `ADMIN` | List all resources (optional `?available=true` filter) |
| `GET` | `/api/resources/{id}` | `USER`, `ADMIN` | Retrieve resource details by ID |
| `POST` | `/api/resources` | `ADMIN` | Create a new resource |
| `PUT` | `/api/resources/{id}` | `ADMIN` | Update an existing resource |
| `DELETE` | `/api/resources/{id}` | `ADMIN` | Delete a resource |

### 3. Reservation Endpoints (`/api/reservations` or `/reservations`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/reservations` | `USER`, `ADMIN` | Create a reservation (User identity resolved from JWT; prevents overlapping bookings) |
| `GET` | `/api/reservations` | `USER`, `ADMIN` | Paginated, filtered, and sorted search (`ADMIN` sees all; `USER` sees only own) |
| `GET` | `/api/reservations/{id}` | `USER`, `ADMIN` | Retrieve reservation by ID (enforces user ownership) |
| `PUT` | `/api/reservations/{id}` | `USER`, `ADMIN` | Update reservation time/resource (enforces ownership and conflict checks) |
| `DELETE` | `/api/reservations/{id}` | `USER`, `ADMIN` | Delete/cancel reservation (enforces user ownership) |

> Both `/api/reservations` and `/reservations` prefixes are supported interchangeably for maximum API client compatibility.

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
GET /api/reservations?status=PENDING&minPrice=100.00&maxPrice=1000.00&page=0&size=10&sortBy=startTime&direction=asc
Authorization: Bearer <jwt-token>
```

---

## 🚀 Running the Application

### Prerequisites
- Java 17
- Spring Boot 2.7.5 (standard `javax.*` annotations for validation and persistence)
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
