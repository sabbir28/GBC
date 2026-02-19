# BM College Routine (GBC)

BM College Routine is an Android application that helps students quickly see today's classes, full weekly routines, and reminders in one place.

## What the app does

- Downloads and syncs the latest routine JSON from GitHub during splash startup.
- Shows **today's classes** with current, upcoming, and previous slots.
- Provides a **full schedule** screen for all weekdays.
- Includes in-app **settings** for reminder behavior.
- Supports in-app update checks and APK download flow.
- Includes an **optional login/registration profile** where users can save personal details.

## Optional Login / Registration system

A new optional profile system is available from the sidebar:

1. Open drawer menu in Home.
2. Tap **Registration**.
3. Turn on **Enable optional login profile**.
4. Enter your details (name, email, phone, department/program).
5. Tap **Save details**.

Saved details are stored locally in `SharedPreferences` (`user_profile`) and shown in the drawer header (name + department) when enabled.

## Navigation

Sidebar menu now includes:

- Today
- Full Schedule
- Settings
- Registration

## Tech stack

- Java (Android)
- Material Components
- RecyclerView + NestedScrollView layouts
- SharedPreferences for local persistence

## Build and run

```bash
./gradlew assembleDebug
```

### Troubleshooting `:app:mergeDebugResources` (`material-1.6.1`)

If you hit a build error caused by an invalid string like `{str}` inside a
transformed Material dependency file in your local Gradle cache, run:

```bash
./gradlew repairMaterialTransformCache assembleDebug --refresh-dependencies
```

This project includes `repairMaterialTransformCache`, which removes only
corrupted Material transform-cache entries from `GRADLE_USER_HOME` so Gradle
can re-download and regenerate them cleanly.

In CI, the workflow now runs this repair task before build with `--refresh-dependencies` to avoid stale cached transforms.

## Package

- Root package: `sabbir.apk`

## Main screens

- Splash / Sync: `MainActivity`
- Home dashboard: `UI/HomeActivity`
- Schedule: `UI/Schedule`
- Settings: `UI/Setting`
- Registration: `UI/RegistrationActivity`
