# Uni Leca 2.0.0 Beta 1 — Public Preview

This is a public preview build of the refreshed Uni Leca Android app. It is distributed through GitHub Releases for early device testing and feedback.

## Download

Open the [Uni Leca GitHub Releases](https://github.com/Ishara2004/Uni-Leca/releases) page and download the APK asset named:

- \`Uni-Leca-2.0.0-beta.1.apk\`

The adjacent \`.sha256\` file contains the SHA-256 checksum for the APK.

Download the APK from the official repository release page only. Do not install APK files re-uploaded by unrelated websites or accounts.

## What is included

- Refreshed light-blue and white visual identity
- Light, Dark and System appearance settings
- Updated Today, Timetable, Analytics, Reports and Settings screens
- Filled Material icons and more consistent screen headers
- Colour-coded Present, Absent, Medical and No Class states
- Updated UL adaptive launcher icon
- Timetable full-page scrolling
- Existing attendance, semester, backup and report features

## Important preview-build details

- **Release channel:** Public Beta / Preview — not a stable Play Store release
- **Application ID:** \`com.ishara.unileca.uipreview\`
- **Production application ID:** \`com.ishara.unileca\` (not included in this preview APK)
- **Minimum Android version:** Android 8.0 / API 26
- **Signing:** GitHub Actions debug signing is used for this preview. This is not the production signing identity.

The preview uses a separate application ID so it can be installed alongside the production-package app. Android treats these as separate apps with separate local data.

Because this preview APK is debug-signed using the GitHub Actions runner's debug key, a later preview build may have a different signing certificate. Android may therefore refuse an in-place update. Before replacing this preview with a later build:

1. Open Uni Leca Preview and export a JSON backup.
2. Confirm that the backup file is saved somewhere you can access.
3. Uninstall only the \`com.ishara.unileca.uipreview\` preview app if Android reports a signature conflict.
4. Install the newer preview APK and restore the JSON backup.

Do not uninstall your separate production-package Uni Leca app when replacing the preview.

## Installation on Android

1. Download \`Uni-Leca-2.0.0-beta.1.apk\` to your Android device.
2. Open the downloaded APK.
3. When Android asks, allow your browser or file manager to install apps from this source. On Android 8.0 and later this is a per-source permission under **Install unknown apps**.
4. Return to the installer and complete installation.
5. Open Uni Leca.

The permission is controlled by Android. Menu names may differ slightly between device manufacturers. You can turn the permission off again after installation.

## Data and privacy

Uni Leca stores academic tracking data locally on the device. It does not require an account or provide cloud synchronisation. Use the in-app Backup & Restore screen to move data between separate installations.

## Feedback and issue reports

Please report reproducible bugs through [GitHub Issues](https://github.com/Ishara2004/Uni-Leca/issues). Include:

- Device model and Android version
- Uni Leca version shown in Settings
- Steps to reproduce the issue
- Screenshot or screen recording, with personal information hidden

This beta is provided for early testing. Please keep a current JSON backup while testing.
