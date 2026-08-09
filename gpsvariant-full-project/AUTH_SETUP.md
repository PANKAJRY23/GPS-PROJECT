# GPSVariant Authentication

This production package includes the GPSVariant login, registration and password-reset flow.

## Included

- Spring Security form login using the `users` table.
- BCrypt password hashing (strength 12).
- Registration with username, email and password confirmation.
- Forgot-password page with account-enumeration protection.
- Cryptographically random, single-use, expiring reset tokens.
- SMTP password-reset email with configurable public URL.
- CSRF protection for browser forms and authenticated API requests.
- Responsive login/register/forgot/reset UI.
- Password show/hide controls and client-side confirmation checks.
- Per-user ownership checks for image generation and final GPS save.

## Required environment variables

Set these outside source control. See `.env.example`.

At minimum configure:

```text
DB_URL=jdbc:postgresql://db:5432/gpsdb
DB_USERNAME=<production database user>
DB_PASSWORD=<long random database password>
GOOGLE_MAPS_API_KEY=<production Google Maps key>
APP_BASE_URL=https://your-real-domain.example
MAIL_ENABLED=true
MAIL_HOST=<smtp host>
MAIL_PORT=587
MAIL_USERNAME=<smtp username>
MAIL_PASSWORD=<smtp password or app password>
MAIL_FROM=<verified sender address>
SESSION_COOKIE_SECURE=true
JPA_DDL_AUTO=validate
```

## Authentication URLs

- `/login`
- `/register`
- `/forgot-password`
- `/reset-password?token=...`

## Password reset behavior

1. The user submits an email address.
2. The application returns the same user-facing message whether or not the account exists.
3. Existing reset tokens for that account are invalidated.
4. A random reset token is stored with an expiry time and single-use flag.
5. The raw token is sent only through the reset URL in the email.
6. The token expires after the configured number of minutes (15 by default).
7. After a successful reset, the token is marked as used.
8. Expired reset tokens are periodically cleaned up.

## Existing database users

Existing users need a valid unique email address to use forgot password. The `enabled` field must be `true` for login.

The production package also adds ownership to final GPS records. If an existing `gps_final_data` table already contains rows, the new `user_id` column may initially be null. Backfill those rows from their main image's `user_id` before changing the database to a strict `NOT NULL` constraint through a migration process.

## Production notes

- Never commit `.env` or real credentials.
- Rotate any credentials that were present in an older copy of this project.
- Use HTTPS and set `APP_BASE_URL` to the real HTTPS URL.
- Use a verified SMTP sender and an app password where your provider requires it.
- Keep PostgreSQL and uploaded images on persistent storage with backups.
- Use `JPA_DDL_AUTO=update` only for the first deployment if schema creation/update is required; move to controlled migrations and `validate` afterward.
- Put the application behind an HTTPS reverse proxy and add network-level rate limiting/WAF controls for public authentication endpoints.
