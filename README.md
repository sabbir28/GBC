# GBC Routine Manager (Android)

GBC is an Android routine/schedule app focused on showing today's classes, upcoming/past sessions, and update delivery from GitHub releases.

## About this refactor

This refactor introduces a **file-size and class-size discipline** so core logic stays easier to maintain:

- No Java source file should exceed ~500 lines.
- Large responsibilities are split into focused classes.
- `HomeActivity` now acts as a coordinator instead of a monolithic controller.

### What was changed

- `HomeActivity` was split into smaller components:
  - `HomeScheduleController`: schedule parsing, state transitions (current/upcoming/past), and ticker-driven UI refresh.
  - `HomeUpdateController`: app update checks, update dialog behavior, download binding, and APK install handoff.
- Existing behaviors were preserved while separating concerns.

## Project structure (key files)

- `app/src/main/java/sabbir/apk/UI/HomeActivity.java`
- `app/src/main/java/sabbir/apk/UI/home/HomeScheduleController.java`
- `app/src/main/java/sabbir/apk/UI/home/HomeUpdateController.java`

## Coding standards used

- Keep classes single-purpose where practical.
- Prefer composition/controllers over very large activities.
- Keep each source file below 500 lines whenever possible.
- Keep UI thread and background work clearly separated.

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
```

