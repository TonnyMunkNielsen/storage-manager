# Storage Manager

## Runtime Configuration

The default Spring profile is production-oriented. It expects database credentials
and integration secrets from the environment:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_NOTIFICATION_HOME_ASSISTANT_ENABLED`
- `APP_NOTIFICATION_HOME_ASSISTANT_URL`
- `APP_NOTIFICATION_HOME_ASSISTANT_TOKEN`
- `APP_NOTIFICATION_SMS_ENABLED`
- `APP_NOTIFICATION_CONSOLE_ENABLED`
- `APP_BACKUP_SCHEDULED_ENABLED`
- `APP_BACKUP_SCHEDULED_CRON`
- `APP_BACKUP_DIRECTORY`
- `APP_BACKUP_RETENTION_DAYS`
- `APP_BACKUP_INCLUDE_FLYWAY_HISTORY`

For local development without environment setup, run with the `dev` profile:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

For Docker Compose, create a local `.env` from `.env.example` and replace the
placeholder passwords before starting the stack.
