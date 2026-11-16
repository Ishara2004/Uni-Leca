# Uni Leca 2.0 — Google Play Release Gate

This checklist is for the `production-v2` branch and reflects the production engineering work required before merging to `main` and publishing.

## Code gates

- [x] Final application ID set: `com.ishara.unileca`
- [x] targetSdk set to API 36 for the 2026 Google Play requirement
- [x] Destructive Room migration removed
- [x] v1 -> v2 data-preserving Room migration added
- [x] Attendance duplicate invariant added at database level
- [x] Historical attendance preserved when timetable slots are removed
- [x] One-off extra sessions supported
- [x] Lecture/LAB/Tutorial analytics separated
- [x] Calendar date picker added
- [x] Semester permanent delete flow added with confirmation
- [x] Exact-alarm permission removed
- [x] Reboot receiver background work made lifecycle-safe
- [x] Reminder recurrence and ghost-alarm cancellation logic added
- [x] Notification quick attendance actions added
- [x] Versioned checksummed backup format added with legacy v1 import
- [x] CSV formula-injection/quoting hardening added
- [x] Release R8/resource shrinking enabled
- [x] Unused Firebase/AI/network dependencies removed from the app module
- [x] Android automatic cloud backup disabled to match offline privacy wording
- [x] Privacy policy and Data safety implementation notes added
- [x] CI build/test/lint/bundle workflow added

## Required verification before merge

- [ ] CI `testDebugUnitTest` passes
- [ ] CI `lintDebug` passes
- [ ] CI `assembleDebug` passes
- [ ] CI `bundleRelease` passes
- [ ] Install a migration test build over the currently used v1 app and confirm the existing ~2 months of local data appears unchanged
- [ ] Verify attendance totals for at least 5 real modules manually against the pre-upgrade app/backup
- [ ] Verify Lecture/LAB/Tutorial percentages independently
- [ ] Verify extra session add/edit/delete and attendance behavior
- [ ] Verify calendar jump across past/current/future dates
- [ ] Verify notification permission grant and denial
- [ ] Verify reminders after reboot
- [ ] Verify weekly reminder repeats the following week
- [ ] Verify deleting a slot prevents future reminders
- [ ] Verify ending/deleting active semester stops its reminders
- [ ] Verify home widget before, during and after the final class of the day
- [ ] Verify backup v2 export -> restore round trip
- [ ] Verify an old v1 JSON backup restores successfully
- [ ] Test dark/light mode, font scale, TalkBack, small phone and tablet layouts
- [ ] Run Google Play pre-launch report on the uploaded AAB

## Play Console gates

- [ ] Create app entry with package `com.ishara.unileca`
- [ ] Enroll/use Play App Signing and protect the upload key outside Git
- [ ] Complete App content declarations
- [ ] Complete Data safety using `DATA_SAFETY.md` as the code baseline
- [ ] Publish a public privacy-policy URL based on `PRIVACY_POLICY.md`
- [ ] Upload icon, phone screenshots and feature graphic
- [ ] Apply the text in `STORE_LISTING.md`
- [ ] Set content rating and target audience accurately
- [ ] If the developer account is a personal account created after 13 Nov 2023, complete a closed test with at least 12 opted-in testers continuously for 14 days before applying for production access
- [ ] Review the Play pre-launch report and fix blocking crashes/ANRs/accessibility issues
- [ ] Roll out production gradually after test approval

## Never do before release

- Do not merge `production-v2` until the v1 -> v2 migration has been tested on a copy of real user data.
- Do not commit `.jks`, `.keystore`, passwords, API keys or Play credentials.
- Do not change `applicationId` after the Play app identity is established unless intentionally publishing a different app.
- Do not claim cloud sync, encryption, AI or other functionality that is not in the shipped build.
