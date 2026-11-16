# 🎓 Uni Leca

**Offline university attendance, timetable and attendance-risk companion for Android.**

Uni Leca helps students maintain a weekly timetable, record class attendance, monitor module thresholds and understand attendance risk without creating an account or sending academic tracking data to a server.

> `production-v2` is the release-hardening branch. It includes a data-preserving migration from the original v1 database. Do not merge it to `main` until the migration has been installed over a copy of real v1 user data and the release checklist has passed.

## Features

### Attendance tracking

- Present, Absent, Medical and No Class states
- One attendance record per session/date enforced by Room
- Past attendance can be corrected or cleared
- Future attendance cannot be recorded before the session begins
- Zero recorded classes display as no data instead of a misleading 100%

### Lecture / LAB / Tutorial analytics

Each module has an overall attendance percentage plus independent percentages for its session types. Example:

```text
Operating Systems
Overall   84.6%
Lecture   75.0%
LAB      100.0%
Tutorial      —
```

The analytics screen also estimates the percentage after another missed session, how many additional misses remain before falling below the threshold, or how many consecutive attendances may be needed to recover.

### Timetable and extra sessions

- Recurring weekly Lecture, Tutorial and LAB slots
- Editable module names, thresholds and timetable slots
- One-off extra sessions attached to a specific calendar date
- Time-range and overlap validation
- Removing a schedule with existing attendance archives the schedule instead of deleting its historical attendance

### Calendar navigation

The Today screen supports previous/next navigation, a jump-to-today action and a Material calendar picker for direct access to any date.

### Semester lifecycle

- Create semesters
- Duplicate modules and weekly timetable from an earlier semester
- Only one active semester is maintained by the repository workflow
- End semesters into read-only history
- Review semester history
- Permanently delete a semester with an explicit destructive confirmation

### Reminders and widget

- Optional post-class reminders
- No exact-alarm permission
- Reminder recurrence for weekly timetable slots
- One-shot reminders for extra sessions
- Reboot-safe reminder restoration
- Notification actions for Present, Absent and Medical
- Home-screen widget that does not offer attendance actions before a session starts and no longer falls back to an already-finished morning class after the day's schedule is over

### Backup and reports

- PDF report
- CSV export with safer quoting/formula handling
- v2 JSON backup envelope with format version, timestamp and SHA-256 integrity checksum
- Legacy v1 JSON backup import support
- Validation before replacing the local database
- Android automatic cloud backup disabled; exports go only to a location chosen through Android's Storage Access Framework

## Architecture

```text
Compose UI screens
      ↓
MainViewModel
      ↓
AppContainer / injected dependencies
      ↓
AttendanceRepository
      ↓
Room database

AlarmScheduler ↔ BroadcastReceivers
BackupRestoreHelper ↔ versioned JSON
ExportHelper ↔ PDF / CSV
Glance Widget ↔ Repository
```

The UI is split into focused screens and uses Navigation Compose. Dependencies are created once in `UniLecaApplication` and supplied through `AppContainer`; the ViewModel no longer constructs its own repository/scheduler/settings dependencies.

## Technology

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room / SQLite
- Kotlin Coroutines + Flow
- Android Glance
- AlarmManager with inexact idle-safe reminders
- Moshi
- Robolectric / JUnit
- R8 + resource shrinking for release builds

## Android / Play configuration

- `applicationId`: `com.ishara.unileca`
- `minSdk`: 26
- `targetSdk`: 36
- `compileSdk`: 36
- Version: 2.0 / versionCode 2

Release signing values are read only from environment variables. Keystores and secrets are excluded from Git.

## Build verification

GitHub Actions runs:

```text
testDebugUnitTest
lintDebug
assembleDebug
bundleRelease
```

Business tests cover independent session-type analytics, zero-data attendance behavior, one-off extra sessions and preservation of attendance history when schedule rules are removed.

## Privacy

The release branch does not require an account and removes the original unused Firebase/AI/network dependencies. See:

- `PRIVACY_POLICY.md`
- `DATA_SAFETY.md`
- `PLAY_STORE_RELEASE_CHECKLIST.md`
- `STORE_LISTING.md`

## Release process

Before production publishing, follow every unchecked verification step in `PLAY_STORE_RELEASE_CHECKLIST.md`, especially installing the v2 migration over a copy of the existing v1 app data before merging to `main`.
