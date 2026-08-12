# GPSVariant production hardening changelog

## Application configuration
- Removed all real DB, SMTP and Google API credentials from source.
- Moved production configuration to environment variables.
- Added configurable connection pool, session timeout, upload size and upload directory.
- Added Actuator health endpoint.

## Security
- Enabled CSRF protection for browser forms and authenticated API calls.
- Added CSRF header support to the dashboard fetch requests.
- Added secure/session cookie settings controlled by environment variables.
- Kept form login and logout behavior compatible with the existing UI.
- Enforced per-user ownership when generating maps and saving final GPS data.
- Added direct user ownership to final GPS records.
- Added upload content-type and size validation.
- Server-generated filenames prevent client-controlled path traversal/file names.
- Removed HTTP Basic authentication from the browser application.

## GPS/image workflow
- Kept the existing main-image upload, second-image upload, map generation and final-save workflow.
- Fixed the generated map URL returned to the browser so it matches the `/uploads/**` resource mapping.
- Final save now stores both image relationships plus GPS/address/map data.
- Google Static Maps calls now have connect/read timeouts.
- Upload and generated-image paths are configurable and persistent in Docker.

## Authentication
- Standardized registration on `com.gpsvariant.DTO.auth.RegistrationRequest`.
- Password reset URL uses `APP_BASE_URL` instead of localhost.
- Password reset tokens remain single-use and time-limited.
- Added scheduled cleanup for expired password-reset tokens.
- Mail can be enabled/disabled using `MAIL_ENABLED`.

## Packaging
- Removed Spring Boot DevTools from the production dependency set.
- Added Dockerfile and production Docker Compose.
- Existing uploaded images are intentionally not bundled into the production source package; deploy them separately if they are real client data.

## Final validation + high-resolution Google Maps update (August 2026)

- Added browser-side validation for username, email, password and confirmation fields while retaining server-side Jakarta validation.
- Added final-save GPS validation on the backend for latitude and longitude ranges.
- Added required-image/map checks and protection against using the same image as both main and second image.
- Final Save refreshes the dashboard only after the backend successfully persists `gps_final_data`.
- Increased Google Static Maps generation from `600x400` to `640x640` with `scale=2`, returning a 1280x1280 high-resolution map image.
- Increased map overlay size and enabled high-quality bicubic rendering when compositing the map onto the uploaded image.
- Final GPS image remains PNG so map labels and text are not additionally degraded by JPEG compression.
- The Google Maps API key remains environment-driven through `GOOGLE_MAPS_API_KEY`; no credential is added to source.
