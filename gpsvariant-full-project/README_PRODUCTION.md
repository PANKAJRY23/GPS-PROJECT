# GPSVariant Production Package

This package keeps the existing GPSVariant workflow while hardening the application for a real deployment.

## Important security change

The original source archive contained live database, SMTP and Google Maps credentials in `application.yml`. Those secrets have been removed from source code. **Rotate those credentials before deploying**, especially the exposed Google Maps key and SMTP password.

Production values are supplied through environment variables.

## Main production changes

- Secrets removed from `application.yml`.
- Configurable database connection pool and upload directory.
- HTTPS-aware session cookie configuration.
- CSRF protection enabled for the API and browser forms; dashboard fetch requests send the CSRF token.
- Upload validation for JPG/PNG/WebP and configurable maximum size.
- Random server-side filenames; original filenames are not trusted.
- Uploads and generated GPS images use a configurable persistent directory.
- Main and second image ownership is checked against the logged-in user.
- Final GPS record is directly associated with the logged-in user and both image records.
- Final GPS record now persists map path, latitude, longitude, address lines and creation time.
- Google Maps HTTP calls have connection/read timeouts.
- Password reset URL uses `APP_BASE_URL` instead of localhost.
- Password-reset tokens are single-use and expired tokens are periodically cleaned up.
- Password-reset email account enumeration is still prevented at the controller level.
- BCrypt strength is 12.
- Session timeout and cookie attributes are configurable.
- Spring Boot Actuator health endpoint is exposed for deployment health checks.
- DevTools has been removed from the production build.
- Dockerfile and production Docker Compose are included.

## Required production environment

Copy `.env.example` to `.env` and set every `CHANGE_ME` value. Do not commit `.env`.

At minimum configure:

- `DB_PASSWORD`
- `APP_BASE_URL` (the public HTTPS URL)
- `GOOGLE_MAPS_API_KEY`
- `MAIL_HOST`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

Set `SESSION_COOKIE_SECURE=true` when the application is served over HTTPS.

## Database

For the first deployment, the existing application can use `JPA_DDL_AUTO=update` to update the schema. After verifying the schema, switch to `validate` and use a proper database migration process for future schema changes.

The production package adds a `user_id` column to `gps_final_data` for ownership. Existing rows may require a one-time data backfill before `JPA_DDL_AUTO=validate` is used. If the database is empty/new, `update` will create the column automatically.

## Docker deployment

1. Copy `.env.example` to `.env`.
2. Put real production values into `.env`.
3. Build and start:

   `docker compose -f docker-compose.prod.yml up -d --build`

4. Check application health:

   `GET /actuator/health`

5. Put the application behind an HTTPS reverse proxy. `Caddyfile.example` shows a simple Caddy setup.

## Reverse proxy / HTTPS

The Spring Boot application listens on port 8080. In production, do not expose it directly to the public internet if a reverse proxy is available. Terminate HTTPS at the reverse proxy and proxy traffic to `app:8080`.

Set `APP_BASE_URL` to the public HTTPS URL. This is particularly important for password-reset emails.

## Existing data / uploads

The `uploads` directory is persistent in Docker through `gpsvariant_uploads`. Do not delete that volume during an upgrade.

If migrating the existing local deployment, copy the existing `uploads/` contents into the persistent production volume before going live.

## Google Maps

The application requires a valid Google Static Maps API key for GPS image generation. Restrict the key in the Google Cloud console to the APIs and usage appropriate for this application.

## Email

Password reset requires working SMTP settings. Do not put an SMTP password into source control. For Gmail, use an app password where applicable rather than a normal account password.

## Production checklist

- [ ] Rotate all credentials that appeared in the old source archive.
- [ ] Configure a real HTTPS domain in `APP_BASE_URL`.
- [ ] Configure DNS and TLS at the reverse proxy.
- [ ] Configure PostgreSQL with a strong password.
- [ ] Configure Google Static Maps API and billing/quotas as required by Google.
- [ ] Configure SMTP and verify password reset end-to-end.
- [ ] Start with `JPA_DDL_AUTO=update` only if the database needs schema creation/update.
- [ ] Back up PostgreSQL before schema changes.
- [ ] Back up the uploads volume.
- [ ] Change `JPA_DDL_AUTO` to `validate` after the schema is confirmed.
- [ ] Keep `.env` out of source control.
- [ ] Monitor `/actuator/health`.
