# Changes: Set `local` as default Spring profile and remove `dev` usage

- Added `spring.profiles.default=local` to each service's `src/main/resources/application.yml` so running without explicit profile uses `local`.
- Removed `deploy/docker-compose.dev.yml` and updated docs to use `deploy/local/docker-compose.yml`.
- Searched and updated documentation references from `docker-compose.dev.yml` to `deploy/local/docker-compose.yml`.
- Did **not** modify package names that contain `dev` (e.g., `dev.payment.*`) as they are unrelated to Spring profiles.
- Future: you can add `application-prod.yml` later and set `SPRING_PROFILES_ACTIVE=prod` in your production environment.

