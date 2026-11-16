# 🎓 Uni Leca

**An offline-first university attendance, timetable and attendance-risk companion for Android.**

Uni Leca helps students organise semester modules, manage weekly class schedules, record attendance and monitor attendance thresholds. Academic tracking stays on the device: the app does not require an account or send attendance records to a server.

## Product experience

Uni Leca uses a student-focused, energetic visual identity built around light blue, white and a cyan accent. The interface has matching light and dark colour schemes, rounded Material 3 surfaces and filled icons with clear meanings.

- **Light / Dark / System theme:** choose a preferred appearance in Settings. The selection is saved on the device.
- **Filled icons:** bottom navigation, screen headings and common actions use a consistent filled Material icon style.
- **Colour-coded attendance:** Present uses green, Absent red, Medical amber and No Class slate-blue. Status labels use text and colour rather than status icons.
- **Custom launcher identity:** the adaptive launcher icon uses a white UL monogram with a cyan accent on a blue background, with a monochrome version for supported themed launchers.
- **Scrollable screens:** long pages such as Timetable, Today, Analytics, Reports, Settings and history can scroll through their full content while the main bottom navigation stays available.

## Features

### Attendance tracking

- Present, Absent, Medical and No Class states
- One attendance record per timetable session/date, enforced by a Room database constraint
- Past attendance can be corrected or cleared
- Future attendance cannot be recorded before the session begins
- Zero recorded classes display as no data instead of a misleading 100%
- Attendance states remain visually distinct in both light and dark themes

### Lecture / LAB / Tutorial analytics

Each module has an overall attendance percentage plus independent percentages for its session types. For example:

| Session type | Attendance |
|---|---:|
| Overall | 84.6% |
| Lecture | 75.0% |
| LAB | 100.0% |
| Tutorial | — |

The Analytics screen also estimates the percentage after another missed session, how many additional misses remain before falling below the threshold, or how many consecutive attendances may be needed to recover.

### Timetable and extra sessions

- Recurring weekly Lecture, Tutorial and LAB slots
- Module names and attendance thresholds can be edited
- Timetable slots can be added, edited and removed
- Full-page scrolling for module and weekly timetable sections
- One-off extra sessions attached to a specific calendar date
- Time-range and overlap validation
- Removing a schedule with existing attendance archives the schedule instead of deleting its historical attendance

### Calendar navigation

The Today screen supports previous/next date navigation, a jump-to-today action and a Material calendar picker for direct access to a date. Sessions scheduled for the selected date are shown with their module, session type and time.

### Semester lifecycle

- Create semesters
- Duplicate modules and weekly timetable from an earlier semester
- Only one active semester is maintained by the repository workflow
- End semesters into read-only history
- Review semester history and open previous semesters
- Permanently delete a semester with an explicit destructive confirmation

### Reminders and widget

- Optional post-class reminders
- No exact-alarm permission
- Reminder recurrence for weekly timetable slots
- One-shot reminders for extra sessions
- Reboot-safe reminder restoration
- Notification actions for Present, Absent and Medical
- Home-screen widget that does not offer attendance actions before a session starts and does not fall back to an already-finished morning class after the day's schedule is over

### Backup and reports

- PDF report
- CSV export with safer quoting and formula handling
- v2 JSON backup envelope with format version, timestamp and SHA-256 integrity checksum
- Legacy v1 JSON backup import support
- Validation before replacing the local database
- Android automatic cloud backup disabled; exports go only to a location chosen through Android's Storage Access Framework

## Screens and navigation

The main navigation provides:

1. **Today** — date selection, scheduled sessions and attendance marking
2. **Timetable** — semester modules and recurring weekly sessions
3. **Analytics** — overall and Lecture / LAB / Tutorial attendance statistics
4. **Reports** — PDF and CSV export
5. **Settings** — appearance, reminders, attendance policy, backup and history

Settings also links to Attendance History and Semester History. The app uses a shared visual system so screen headers, cards, empty states, actions and icons remain consistent across these areas.

## Architecture

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

The UI is split into focused screens and uses Navigation Compose. Dependencies are created once in UniLecaApplication and supplied through AppContainer; the ViewModel does not construct its own repository, scheduler or settings dependencies.

The UI refresh changes presentation and user preferences. It does not change the Room schema, attendance-history data model or backup format.

## Technology

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Material Icons
- Room / SQLite
- Kotlin Coroutines + Flow
- Android Glance
- AlarmManager with inexact idle-safe reminders
- Moshi
- Robolectric / JUnit
- R8 + resource shrinking for release builds

## Android configuration

| Setting | Value |
|---|---|
| Production application ID | com.ishara.unileca |
| Minimum SDK | 26 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Version | 2.0.0-beta.1 (public preview) |
| Version code | 2 |

The debug UI-preview build adds the .uipreview application ID suffix so it can be installed alongside an existing Uni Leca installation for side-by-side testing. The release application ID remains com.ishara.unileca.

Release signing values are read only from environment variables. Keystores and secrets are excluded from Git.


## Public beta download

**Uni Leca 2.0.0 Beta 1** is available as a free Android preview through [GitHub Releases](https://github.com/Ishara2004/Uni-Leca/releases/tag/v2.0.0-beta.1).

Download the `Uni-Leca-2.0.0-beta.1.apk` asset from the release page. This preview uses the separate application ID `com.ishara.unileca.uipreview`, allowing it to sit alongside an installation using the production application ID.

This is a debug-signed public preview, not the production-signed Play Store build. A later preview may use a different debug signing certificate; export a JSON backup before replacing it. See [BETA_TESTING.md](BETA_TESTING.md) for installation, backup and upgrade instructions.

## Build and verification

The GitHub Actions workflow runs on pushes to main and production-v2 and verifies pull requests targeting main.

The verification job runs:

- **Unit tests:** testDebugUnitTest
- **Android lint:** lintDebug
- **Debug APK:** assembleDebug
- **Release app bundle:** bundleRelease

Build locally from the repository root with JDK 17 and an Android SDK installation:

- gradle testDebugUnitTest
- gradle lintDebug
- gradle assembleDebug
- gradle bundleRelease

The CI job uploads the debug APK and release AAB as workflow artifacts. The UI-preview debug APK is intended for device testing; it is not the Play Store release build.

Business tests cover independent session-type analytics, zero-data attendance behavior, one-off extra sessions, preservation of attendance history when schedule rules are removed and legacy backup restoration.

## Data and privacy

The release branch does not require an account and removes the original unused Firebase, AI and network dependencies. Academic tracking remains local to the device. Backup exports are initiated by the user and saved through Android's Storage Access Framework.

See:

- PRIVACY_POLICY.md
- DATA_SAFETY.md
- PLAY_STORE_RELEASE_CHECKLIST.md
- STORE_LISTING.md

## Release process

Before production publishing, complete every required verification step in PLAY_STORE_RELEASE_CHECKLIST.md. In particular, install the v2 migration over a copy of real v1 user data and verify that existing attendance records remain unchanged before merging to main.

Do not merge production-v2 to main or publish the release build until the migration and real-device release checks have passed.
