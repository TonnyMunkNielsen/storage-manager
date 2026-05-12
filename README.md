# Storage Manager

Storage Manager is a self-hosted inventory and expiry tracking application for items stored in numbered storage boxes. It is built with Spring Boot, Vaadin, PostgreSQL, Flyway, and Docker Compose.

The application helps track item types, individual stored items, best-before dates, storage box status, desiccant change dates, item images, notifications, and encrypted database backups.

## Contents

- [Current Features](#current-features)
- [Tech Stack](#tech-stack)
- [Quick Start: Local Development](#quick-start-local-development)
- [Running the Full Docker Stack](#running-the-full-docker-stack)
- [Configuration](#configuration)
- [Database and Backups](#database-and-backups)
- [Project Structure](#project-structure)
- [Development Commands](#development-commands)
- [TODOs](#todos)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Current Features

- Vaadin web UI for managing:
  - Dashboard
  - Item types
  - Item instances
  - Storage boxes
  - Notifications
- PostgreSQL persistence with Flyway-managed schema migrations.
- Item type images stored in the database and served through `/api/images/item-type/{id}`.
- JSON import and export for item types, including image data.
- Expiry and expiry-warning notification records based on item best-before dates and each item type's notification day modifier.
- Optional console notification delivery.
- Placeholder support for Home Assistant and SMS notifications.
- Optional AES-encrypted PostgreSQL backups using `pg_dump`.
- Docker Compose setup for PostgreSQL, pgAdmin, and the application.

## Tech Stack

- Java 25
- Spring Boot 4.0.6
- Vaadin 25.1.5
- Gradle
- PostgreSQL 16
- Flyway
- Spring Data JPA
- Lombok
- Spotless with Palantir Java Format
- Docker and Docker Compose

## Quick Start: Local Development

This flow runs PostgreSQL in Docker, optionally starts pgAdmin, and runs the Spring Boot application directly through Gradle. It is the recommended setup while developing.

### Prerequisites

- JDK 25
- Docker Desktop or another Docker Compose compatible runtime
- Git
- Optional: PostgreSQL client tools on your host if you want local backup and restore commands to work outside Docker

### 1. Create local environment file

PowerShell:

```powershell
Copy-Item .env.example .env
```

Bash:

```bash
cp .env.example .env
```

Edit `.env` and replace all placeholder passwords before using it for anything beyond a throwaway local setup.

Note: Docker Compose reads `.env` automatically. Spring Boot does not load `.env` by itself when run from Gradle or an IDE, so application-specific values must be exported in your shell or configured in your IDE run configuration if you need to override the defaults.

### 2. Start the local database

```powershell
docker compose up -d postgres-storage-manager-db
```

This starts PostgreSQL on:

```text
localhost:5432
```

Optional: start pgAdmin after confirming `pgadmin-config/servers.json` and `pgadmin-config/.pgpass` exist as files:

```powershell
docker compose up -d pgadmin-storage-manager
```

pgAdmin will be available at:

```text
http://localhost:5052
```

### 3. Run the application with the dev profile

PowerShell:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Bash:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application will be available at:

```text
http://localhost:8080
```

The `dev` profile points the app at `localhost:5432` and uses the same default database name and username as `.env.example`.

### 4. Run tests

PowerShell:

```powershell
.\gradlew.bat test
```

Bash:

```bash
./gradlew test
```

## Running the Full Docker Stack

To build and run PostgreSQL, pgAdmin, and the application in Docker:

```powershell
docker compose up --build
```

If you do not need pgAdmin, or have not set up its mounted config files yet, run only the app and database:

```powershell
docker compose up --build app postgres-storage-manager-db
```

Open:

```text
http://localhost:8080
```

Stop the stack without deleting data:

```powershell
docker compose down
```

Stop the stack and delete the PostgreSQL Docker volume:

```powershell
docker compose down -v
```

Use `down -v` only when you intentionally want to remove the local database.

## Configuration

Configuration is split between Docker Compose variables and Spring Boot application variables.

### Profiles

| Profile | Purpose | Notes |
| --- | --- | --- |
| default | Production-style runtime | Requires explicit datasource environment variables. Used by the Docker image. |
| `dev` | Local development | Uses localhost PostgreSQL defaults and launches the browser through Vaadin. |

### Docker Compose Variables

These values are normally set in `.env`.

| Variable | Default in `.env.example` | Description |
| --- | --- | --- |
| `POSTGRES_DB` | `storagemanagerdb` | PostgreSQL database name. |
| `POSTGRES_USER` | `storage_manager` | PostgreSQL application user. |
| `POSTGRES_PASSWORD` | `change-this-password` | PostgreSQL application password. Replace this locally. |
| `PGADMIN_DEFAULT_EMAIL` | `admin@example.com` | pgAdmin login email. |
| `PGADMIN_DEFAULT_PASSWORD` | `change-this-password` | pgAdmin login password. Replace this locally. |

### Spring Boot Variables

| Variable | Default | Description |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Required in default profile | JDBC URL, for example `jdbc:postgresql://localhost:5432/storagemanagerdb`. |
| `SPRING_DATASOURCE_USERNAME` | Required in default profile | Database username. |
| `SPRING_DATASOURCE_PASSWORD` | Required in default profile | Database password. |
| `SPRING_FLYWAY_ENABLED` | `true` | Enables Flyway migrations. |
| `APP_NOTIFICATION_CONSOLE_ENABLED` | `false` | Logs pending notifications and marks them as sent when enabled. Useful during development. |
| `APP_NOTIFICATION_HOME_ASSISTANT_ENABLED` | `false` | Enables the Home Assistant notification branch. The actual sender is still a TODO. |
| `APP_NOTIFICATION_HOME_ASSISTANT_URL` | empty | Reserved for the future Home Assistant integration endpoint. |
| `APP_NOTIFICATION_HOME_ASSISTANT_TOKEN` | empty | Reserved for the future Home Assistant long-lived access token or webhook secret. |
| `APP_NOTIFICATION_SMS_ENABLED` | `false` | Enables the SMS notification branch. The actual sender is still a TODO. |
| `APP_BACKUP_DIRECTORY` | `database_backups` in default profile, `/data/backups` in dev and Compose | Directory where encrypted backups and the AES key are stored. |
| `APP_BACKUP_SCHEDULED_ENABLED` | `false` | Enables the scheduled backup service bean. |
| `APP_BACKUP_SCHEDULED_CRON` | `0 0 2 * * ?` | Cron expression for scheduled backups. Default is 02:00 every day. |
| `APP_BACKUP_RETENTION_DAYS` | `30` | Number of days to keep encrypted backup files. |
| `APP_BACKUP_INCLUDE_FLYWAY_HISTORY` | `true` | Includes `flyway_schema_history` in backups for migration-safe restore flows. |
| `SERVER_PORT` | `8080` | Optional Spring Boot override for the HTTP port. |

### Upload Limits

- Spring multipart uploads are configured up to `100MB`.
- Item type image validation allows JPEG, PNG, GIF, and WebP.
- Individual item type images are limited to `5MB` by `ImageValidator`.

## Database and Backups

### Database

The schema is managed by Flyway migrations in:

```text
src/main/resources/db/migration
```

Hibernate is configured with `ddl-auto: validate`, so database structure should be changed through migrations, not through automatic Hibernate schema updates.

### Backups

The backup service:

- Reads the configured Spring datasource URL, username, and password.
- Runs `pg_dump`.
- Encrypts the SQL dump with AES.
- Deletes the temporary unencrypted dump.
- Stores files named like `backup_yyyy-MM-dd_HH-mm-ss.sql.enc`.
- Stores or reuses an AES key file named `aes.key`.
- Deletes encrypted backup files older than `APP_BACKUP_RETENTION_DAYS`.

The Docker image installs `postgresql-client`, so `pg_dump` and `psql` are available inside the app container. If you run backup or restore logic from your host machine, install PostgreSQL client tools locally and make sure they are on `PATH`.

Important: keep `aes.key` with the backups. Encrypted backups cannot be restored without the matching key.

## Project Structure

```text
.
|-- compose.yaml                         # PostgreSQL, pgAdmin, and app services
|-- Dockerfile                           # Multi-stage Java 25 app image
|-- build.gradle                         # Gradle build, Spring Boot, Vaadin, tests, formatting
|-- src/main/java/net/tmn/storage_manager
|   |-- database                         # JPA entities, repositories, database config
|   |-- service                          # Business logic, validation, notifications
|   |-- service/backup                   # Database backup and restore services
|   |-- web/rest                         # REST endpoints
|   `-- web/vaadin                       # Vaadin UI views and forms
`-- src/main/resources
    |-- application.yaml                 # Default runtime configuration
    |-- application-dev.yaml             # Local development overrides
    `-- db/migration                     # Flyway migrations
```

## Development Commands

PowerShell:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
.\gradlew.bat test
.\gradlew.bat spotlessCheck
.\gradlew.bat spotlessApply
.\gradlew.bat bootJar
```

Bash:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
./gradlew test
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew bootJar
```

Docker:

```powershell
docker compose up -d postgres-storage-manager-db
docker compose up -d pgadmin-storage-manager
docker compose up --build
docker compose logs -f app
docker compose down
```

## TODOs

### Home Assistant Notifications

- Implement the Home Assistant sender in `NotificationService`.
- Decide whether the integration should call the Home Assistant REST API, a webhook, or a specific `notify.*` service.
- Add configuration for the target notification service, recipient/device, timeout, and retry behavior.
- Use `APP_NOTIFICATION_HOME_ASSISTANT_URL` and `APP_NOTIFICATION_HOME_ASSISTANT_TOKEN` or replace them with more specific property names.
- Mark notifications as `SENT` only after Home Assistant confirms delivery.
- Add integration-style tests with a mocked Home Assistant endpoint.
- Document the Home Assistant setup steps and required token permissions.

### Email Notifications

- Add email notification support, likely through Spring Mail.
- Add SMTP configuration for host, port, username, password, TLS, from address, and recipient list.
- Create separate email subjects/templates for expiry warnings and expired items.
- Add retry handling and failure logging.
- Add tests for rendered email content and sender failure behavior.
- Consider per-user notification preferences if authentication is re-enabled.

### Hosting on a NAS

- Create a production-oriented Compose file or deployment guide for NAS environments.
- Store PostgreSQL data, encrypted backups, and pgAdmin state on explicit NAS volumes.
- Add a reverse proxy example, such as Caddy, Traefik, Nginx Proxy Manager, or the NAS vendor's built-in proxy.
- Add TLS guidance for local-only and externally reachable deployments.
- Move secrets out of plain `.env` where the NAS supports secret stores or protected environment variables.
- Document backup retention, restore testing, and off-device backup copying.
- Add an update procedure for pulling/building a new image without losing the database volume.
- Re-enable authentication before exposing the app outside a trusted local network.

### Publishing to Maven Central

- Decide whether Maven Central should publish the runnable application artifact, a reusable library module, or both.
- Add proper Maven publication metadata: group ID, artifact ID, versioning strategy, project name, description, license, SCM URL, and developer information.
- Configure Gradle `maven-publish` for release artifacts.
- Configure artifact signing with GPG or another Maven Central compatible signing setup.
- Set up Sonatype Central Portal publishing credentials outside the repository.
- Add release tasks for building, testing, signing, and publishing from a clean checkout.
- Document the release process, including version bumps, tags, changelog updates, and rollback steps.
- Consider GitHub Actions or another CI workflow for repeatable Maven Central releases.

### Additional Hardening

- Re-enable and finish Spring Security. The dependency is currently commented out in `build.gradle`.
- Add `@EnableScheduling` and `@EnableAsync` if automatic scheduled jobs and async backup execution should run in production.
- Add a UI or admin endpoint for creating, listing, and restoring backups.
- Add health checks and readiness checks suitable for Docker and NAS deployment.
- Add sample data or seed tooling for local development.
- Add backup files and `aes.key` to ignore rules if backups are kept inside the repository directory.

## Troubleshooting

### The app fails with missing datasource variables

The default profile requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. For local development, run with:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

### Port already in use

Default ports:

- App: `8080`
- PostgreSQL: `5432`
- pgAdmin: `5052`

Change the port mapping in `compose.yaml` or set `SERVER_PORT` for the Spring Boot application.

### Backups fail with `pg_dump` or `psql` not found

Install PostgreSQL client tools on the machine running the application, or run the app in the provided Docker image where `postgresql-client` is already installed.

### pgAdmin does not start correctly

The Compose file mounts:

```text
./pgadmin-config/servers.json:/pgadmin4/servers.json:ro
./pgadmin-config/.pgpass:/.pgpass:ro
```

Make sure those paths exist as files and match the credentials in `.env`.

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).
