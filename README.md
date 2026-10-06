# Community Infrastructure Reporting API

A Spring Boot backend for reporting local infrastructure problems, tracking their progress, and viewing incident analytics. Residents submit incidents; workers and admins move them through verification, work, and resolution. Each status change records who made it and when.

## Requirements

- Java 21
- MySQL with a database named `community_reporting`
- Maven Wrapper included in this project

The project uses Spring Boot 4.1.1, Spring Security, JPA/Hibernate, Flyway, BCrypt, and JWT authentication. H2 is used only for automated tests.

## Local setup

Create the database in MySQL Workbench or the MySQL client:

```sql
CREATE DATABASE IF NOT EXISTS community_reporting;
```

Set the database password and a private JWT signing secret before starting the app. Replace all placeholder values below. The signing secret must be a random value of at least 32 UTF-8 bytes.

### Command Prompt (cmd.exe)

Run these commands from the project directory in the same terminal window:

```cmd
set "SPRING_DATASOURCE_PASSWORD=YOUR_MYSQL_PASSWORD"
set "APP_JWT_SECRET=YOUR_RANDOM_SECRET"
mvnw.cmd spring-boot:run
```

### PowerShell

This example generates a fresh signing secret for the current terminal session:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = 'YOUR_MYSQL_PASSWORD'
$jwtBytes = New-Object byte[] 64
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generator.GetBytes($jwtBytes)
$generator.Dispose()
$env:APP_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

Environment variables set this way last until the terminal closes. Keep the same secret across restarts if you want existing tokens to remain valid. Generating a new secret invalidates tokens signed with the previous one.

The default base URL is `http://localhost:8080`. Keep the terminal open while using the app; press Ctrl+C to stop it.

### Configuration

| Environment variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | MySQL connection URL | `jdbc:mysql://localhost:3306/community_reporting` |
| `SPRING_DATASOURCE_USERNAME` | MySQL username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | Required; no default |
| `APP_JWT_SECRET` | JWT signing secret | Required; no default |
| `APP_JWT_EXPIRATION` | Token lifetime in milliseconds | `86400000` (24 hours) |

Flyway applies migrations on startup. Hibernate validates the resulting schema rather than creating or altering tables itself:

- V1 creates users, incidents, and incident history.
- V2 adds the incident version column used for optimistic locking.

Keep real passwords, signing secrets, and exported Postman environments containing secrets out of version control. If the previously embedded credentials were used, rotate them in MySQL and in your environment. Changing the JWT secret invalidates existing tokens.

## Authentication and roles

Registration always creates a `RESIDENT`. Both registration and login return JSON containing a `token` field. Passwords are hashed with BCrypt; the API uses stateless JWT authentication.

Send a token on protected requests using:

```http
Authorization: Bearer <token>
```

Public access covers registration, login, incident listings, individual incidents, incidents by reporter, and status history. Creating incidents requires authentication. Updating status requires `WORKER` or `ADMIN`. Analytics requires an authenticated account.

The generated security password printed by Spring at startup is from its default user setup. Use the custom registration/login endpoints and JWT tokens for this API.

### Create the first admin

1. Register an account through `POST /api/auth/register` so its password is hashed correctly.
2. Promote that existing account using MySQL Workbench or the MySQL client:

```sql
UPDATE community_reporting.users
SET role = 'ADMIN'
WHERE email = 'admin@example.com';

SELECT id, name, email, role
FROM community_reporting.users
WHERE email = 'admin@example.com';
```

3. Log in with the email and password you registered, then use the returned token for admin requests.

Replace the example email with your registered email. Roles are `RESIDENT`, `WORKER`, and `ADMIN`. There is currently no HTTP endpoint for creating admins or changing roles. Do not insert plaintext passwords into the users table.

## API endpoints

All paths below are relative to `http://localhost:8080`.

| Method | Path | Purpose | Access |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Register a resident; returns a token | Public |
| POST | `/api/auth/login` | Log in; returns a token | Public |
| POST | `/api/incidents` | Report an incident | Authenticated |
| GET | `/api/incidents` | List/filter incidents with pagination | Public |
| GET | `/api/incidents/{id}` | Get one incident | Public |
| GET | `/api/incidents/user/{userId}` | List a reporter's incidents with pagination | Public |
| PATCH | `/api/incidents/{id}/status` | Advance incident status | Worker or admin |
| GET | `/api/incidents/{id}/history` | View status history | Public |
| GET | `/api/analytics/incidents-by-category` | Count incidents by category | Authenticated |
| GET | `/api/analytics/incidents-by-status` | Count incidents by status | Authenticated |
| GET | `/api/analytics/recent-incidents` | Get the 10 most recent incidents | Authenticated |
| GET | `/api/analytics/incidents-per-day` | Count incidents per reporting date | Authenticated |
| GET | `/api/analytics/average-resolution-time` | Get average resolution time in hours | Authenticated |
| GET | `/api/analytics/dashboard` | Get dashboard summary and counts | Authenticated |

Incident listings accept `status`, `category`, `page`, `size`, and `sort`. The default page size is 20, sorted by `createdAt` descending; pages start at 0.

Example:

```text
GET /api/incidents?status=REPORTED&category=POTHOLE&page=0&size=20&sort=createdAt,desc
```

## Postman examples

For JSON requests, select **Body > raw > JSON** and use `Content-Type: application/json`.

### Register

Send `POST http://localhost:8080/api/auth/register` with **Authorization > No Auth**:

```json
{
  "name": "Example Resident",
  "email": "resident@example.com",
  "password": "REPLACE_WITH_A_STRONG_PASSWORD"
}
```

Successful registration returns HTTP 201 and a token. Name is required with a maximum of 100 characters, email is required with a maximum of 150 characters, and password must contain at least 8 characters.

### Log in

Send `POST http://localhost:8080/api/auth/login` with **Authorization > No Auth**:

```json
{
  "email": "resident@example.com",
  "password": "REPLACE_WITH_A_STRONG_PASSWORD"
}
```

Use the exact credentials you registered with. Successful login returns HTTP 200:

```json
{
  "token": "<JWT_TOKEN>"
}
```

Login does not require an existing token. The path must include `/api`; `/auth/login` is not the login endpoint.

To store the returned token in a Postman environment, optionally add this post-response script to the login or registration request:

```javascript
if (pm.response.code === 200 || pm.response.code === 201) {
  pm.environment.set("token", pm.response.json().token);
}
```

For protected requests, choose **Authorization > Bearer Token** and enter `{{token}}`. Select the same environment in which the token was saved.

### Report an incident

Send `POST http://localhost:8080/api/incidents` with Bearer Token authorization:

```json
{
  "category": "POTHOLE",
  "description": "Large pothole near the intersection.",
  "latitude": -25.7461111,
  "longitude": 28.1880556
}
```

Successful creation returns HTTP 201. New incidents start as `REPORTED`, and the reporter is taken from the authenticated account.

Categories: `POTHOLE`, `WATER_LEAK`, `ELECTRICITY`, `STREETLIGHT`, `SEWER`, `ILLEGAL_DUMPING`, `OTHER`.

Description is required and limited to 1,000 characters. Latitude must be between -90 and 90, longitude between -180 and 180, and both allow up to 7 decimal places.

### Update incident status

Send `PATCH http://localhost:8080/api/incidents/1/status` with a worker/admin token. Replace `1` with the incident ID:

```json
{
  "status": "VERIFIED"
}
```

Allowed transitions follow this sequence, one step at a time:

```text
REPORTED -> VERIFIED -> IN_PROGRESS -> RESOLVED
```

Resolved incidents cannot advance further. Each successful update stores the old status, new status, updater, and timestamp. Conflicting concurrent updates roll back their history entries and return HTTP 409; refresh the incident before retrying.

Use the PATCH endpoint above. `/api/incidentsUpdate` is not an endpoint, and status updates do not use PUT.

## Error responses and troubleshooting

| Status | Meaning |
| --- | --- |
| 400 | Validation failure, malformed JSON, or invalid status transition |
| 401 | Login email/password is incorrect |
| 403 | A protected request has no valid authentication or lacks the required role |
| 404 | An incident or reporter requested by the incident service does not exist |
| 405 | Unsupported method on a public endpoint, such as PUT on `/api/auth/login` |
| 409 | Email is already registered or a concurrent incident update conflicts |

The security configuration allows internal error dispatches so malformed login JSON and incorrect login methods preserve their original 400/405 responses. Protected endpoints still require authentication.

If login returns 403, check that you are sending **POST** to **`/api/auth/login`**, use **No Auth**, and send the email/password as JSON. Restart the backend after code changes to ensure the latest version is running.

## Tests

From Command Prompt:

```cmd
mvnw.cmd test
```

From PowerShell:

```powershell
.\mvnw.cmd test
```

The latest verified suite contains 12 passing tests covering:

- Authentication errors and duplicate registration, including a duplicate insert race.
- Missing resources, status-transition validation, and expected HTTP error mapping.
- Input length, coordinate bounds, and coordinate precision.
- Optimistic locking and rollback of history from a conflicting update.
- Application startup and both Flyway migrations against isolated H2 databases.
- Registration/login over HTTP, tokens returned without existing authentication, malformed login requests, unsupported login methods, and protected access.

Tests use isolated databases and a test-only signing key; they do not need your local MySQL password or production JWT secret. The local MySQL startup log also confirmed a successful connection and application of V2. These checks do not replace a complete production deployment review.
