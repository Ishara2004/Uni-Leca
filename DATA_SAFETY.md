# Google Play Data Safety Notes — Uni Leca 2.0

Use this file as the implementation-backed reference when completing the Play Console Data safety form for the `production-v2` build.

## Current production-v2 behavior

- Account required: **No**
- Advertising SDK: **No**
- Analytics SDK: **No**
- Crash-reporting SDK sending data off-device: **No**
- Cloud synchronization: **No**
- INTERNET permission: **No**
- Android automatic app-data cloud backup: **Disabled** (`android:allowBackup="false"`)
- Attendance/semester/module/timetable data: **Stored locally on device**
- Manual backup/report export: **User initiated through Android Storage Access Framework**
- Notification permission: **Optional and requested in context**
- Boot completed permission: **Used to restore local reminder scheduling after restart**

## Play Console answer baseline

Based on the code in this branch, Uni Leca itself does not collect or share user data off-device. The developer must still review the final signed release and every included SDK before submitting the Data safety form. If any SDK, cloud sync, telemetry, account system, ads, or AI/network feature is added later, reassess the form and privacy policy before release.

## Verification before every release

1. Inspect the merged AndroidManifest for unexpected permissions.
2. Inspect the release dependency graph for analytics, advertising, telemetry, authentication, cloud, or networking SDKs.
3. Confirm exported backups are initiated only by the user.
4. Confirm the privacy policy matches the exact shipped build.
5. Re-answer Play Console Data safety questions whenever data handling changes.
