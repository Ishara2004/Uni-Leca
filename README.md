# 🎓 Uni Leca - Ultimate University Attendance & Timetable Companion

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-green.svg?style=flat)]()
[![Database](https://img.shields.io/badge/Database-Room%20%2F%20SQLite-orange.svg?style=flat)]()

**Uni Leca** is a feature-rich, standalone Android application meticulously designed for university undergraduates to painlessly track academic schedules, manage custom module requirements, and maintain crucial attendance thresholds. Built with cutting-edge Android development practices, it features an intuitive dark-themed user interface, robust offline storage, automated alerts, and professional analytical reporting systems.

---

## 🚀 Key Features

### 📅 Smart Timetable & Semester Lifecycle Management
* **Seamless Semester Rollovers:** Create distinct semesters with the unique ability to duplicate existing modules and schedules to avoid repetitive setups.
* **Flexible Slot Configurations:** Customize lecture types (Lecture, Tutorial, Lab) with precise multi-hour window allocations down to the minute.
* **Dynamic Module Thresholds:** Independently configure unique eligibility requirements per module (e.g., 80% mandatory attendance) using custom interactive sliders.

### ⏱️ One-Tap Attendance Logging (Today View)
* **Status Varieties:** Log attendance variables perfectly suited for academia: `Present`, `Absent`, `Medical Leave`, or `No Class`.
* **Chronological Daily Dashboard:** Automatically structures the current day's sessions in a clean visual queue showing exact times and direct interactive state switchers.

### 📊 Deep Analytics Dashboard
* **Visual Ratios:** Real-time generation of overall attendance performance breakups via crisp, embedded data visualization pie charts.
* **Smart Status Tracking:** Real-time calculations categorizing module security states into automated high-visibility visual badges: `SAFE`, `WARNING`, or `BELOW THRESHOLD`.
* **Proactive Protection:** Visually alerts users immediately when a single missed session risks dropping eligibility below the predefined barrier.

### 📄 Professional Exporting & Backups
* **Document Generation:** Native high-quality PDF summary sheets and structural CSV/Excel sheet generation for individual semesters. Includes deep log histories tracking medical validation states.
* **Data Insurance:** Integrated local backup and structured restoration helper framework utilizing optimized data extraction rules to prevent accidental tracking loss.

### 📱 Launcher Widget Integration
* Powered by **Android Glance**, bringing interactive minimal dashboard controls right to the user's home screen for instantaneous checks without app launch requirements.

---

## 🛠️ Architecture & Tech Stack

Uni Leca is built with stability, testability, and clean architecture separation principles at its core:

* **Language:** 100% Type-Safe [Kotlin](https://kotlinlang.org/)
* **UI Layer:** [Jetpack Compose](https://developer.android.com/jetpack/compose) for a fully declarative, reactive UI system alongside modern Material 3 layout primitives.
* **Architecture Pattern:** Strict **MVVM (Model-View-ViewModel)** orchestration pattern maximizing unidirectional data flows.
* **Local Storage:** [Room Persistent Library](https://developer.android.com/training/data-storage/room) mapping structural database interactions using customized DAOs and transactional repository patterns.
* **Asynchronous Operations:** Kotlin Coroutines and StateFlow elements handling fluid UI state updates asynchronously.
* **Background Scheduling:** [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager) paired with system BroadcastReceivers to schedule smart background status syncs and system boot survival routines.
* **Widget Framework:** Jetpack Glance rendering custom, light-overhead app widgets mirroring Room state parameters natively.

---

## 📂 Project Structure Walkthrough

```bash
Uni-Leca/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── backup/       # Data encryption & Backup/Restore engines
│   │   │   │   │   ├── db/           # Room Database configurations & Daos
│   │   │   │   │   ├── model/        # Unified immutable domain data models
│   │   │   │   │   └── repository/   # Single-source-of-truth abstractions
│   │   │   │   ├── notification/     # Alarm Schedulers, Boot and Notification receivers
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/        # Material 3 typographic & dark system custom palettes
│   │   │   │   │   ├── reports/      # PDF & CSV reporting engines
│   │   │   │   │   ├── settings/     # Global preference state controllers
│   │   │   │   │   └── viewmodel/    # Main business logic handlers
│   │   │   │   └── widget/           # Android Glance home screen interactive widgets
│   │   │   └── res/                  # Visual assets, Vector layouts, and structural XML templates
│   │   └── test/                     # UI Screenshot, Integration & Robolectric verification suites
```

---

## ⚙️ Setup & Installation

1. **Clone the repository:**
   ```bash
    git clone https://github.com/Ishara2004/Uni-Leca.git
    cd Uni-Leca
   ```

2. **Environment Configuration:**
   * Duplicate `.env.example` to create your own configuration:
     ```bash
     cp .env.example .env
     ```

3. **Build & Run:**
   * Open the project root inside **Android Studio (Ladybug or newer)**.
   * Sync the Gradle configurations (`build.gradle.kts`).
   * Connect an Android device or virtual emulator (API level 26 or above recommended) and press **Run**.

---

## 🧪 Testing Focus
The codebase features a modern verification setup:
* **Robolectric Integration:** Validating core background workflows without emulator overhead.
* **Screenshot Verification:** Automated visual testing suites ensuring layout component structures do not warp or distort across multi-density display dimensions.

---

## 📄 License
This project is open-source and structured under the standard MIT License parameters.
