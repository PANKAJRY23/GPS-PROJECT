# Verification

The production package was statically inspected for:

- YAML syntax and tab indentation.
- XML validity of `pom.xml`.
- Removal of the credentials that were present in the supplied source archive.
- Removal of the duplicate registration DTO that caused the earlier type mismatch.
- No hard-coded `localhost` password-reset URL in application code.
- No hard-coded `uploads` filesystem path in the upload/map services.
- Production Docker files and environment templates.

The execution environment used for this package does not contain Maven and has no network access to download Maven dependencies, so a full `mvn package`/integration-test run could not be executed here. The project should therefore be compiled and smoke-tested once on the target build machine/CI environment before the first client release.
