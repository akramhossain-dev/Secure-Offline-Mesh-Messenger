# Developer Setup Guide

## Prerequisites

| Tool | Required Version | Download |
|---|---|---|
| JDK | 17 (LTS) | [Adoptium](https://adoptium.net/) |
| Android Studio | Ladybug 2024.2.1+ | [developer.android.com](https://developer.android.com/studio) |
| Android SDK | API 26–35 | Via Android Studio SDK Manager |
| Git | 2.x | [git-scm.com](https://git-scm.com) |
| Kotlin | 2.0.21 (managed by Gradle) | Bundled via Version Catalog |

---

## Initial Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-org/Secure-Offline-Mesh-Messenger.git
cd Secure-Offline-Mesh-Messenger
```

### 2. Configure the Android SDK path

```bash
# Create local.properties in android/ (not committed to Git)
cd android
echo "sdk.dir=/path/to/your/android/sdk" > local.properties
```

**Default SDK locations:**
- **Linux:** `$HOME/Android/Sdk`
- **macOS:** `$HOME/Library/Android/sdk`
- **Windows:** `C:\Users\<you>\AppData\Local\Android\Sdk`

### 3. Open in Android Studio

Open the `android/` directory (not the repo root) in Android Studio.

```
File → Open → Select: Secure-Offline-Mesh-Messenger/android/
```

Android Studio will automatically sync Gradle and download dependencies.

### 4. Install Inter font files

The app uses **Inter** by Google Fonts. Place the font files in:
```
android/app/src/main/res/font/
```

Required files:
- `inter_thin.ttf`
- `inter_extralight.ttf`
- `inter_light.ttf`
- `inter_regular.ttf`
- `inter_medium.ttf`
- `inter_semibold.ttf`
- `inter_bold.ttf`
- `inter_extrabold.ttf`
- `inter_black.ttf`

Download from: [fonts.google.com/specimen/Inter](https://fonts.google.com/specimen/Inter)

> **Phase A1 note:** Until fonts are added, Typography.kt will fall back to system fonts. The app will still compile and run.

---

## Building

### Debug build

```bash
cd android
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release build

```bash
cd android
./gradlew assembleRelease
```

> Release build requires signing configuration (Phase A2).

### Install to connected device

```bash
./gradlew installDebug
```

### Run all unit tests

```bash
./gradlew test
```

### Run instrumented tests (device/emulator required)

```bash
./gradlew connectedAndroidTest
```

---

## Code Quality

### Run all checks

```bash
# Detekt (static analysis)
./gradlew detekt

# ktlint (formatting check)
./gradlew ktlintCheck

# Spotless (format check)
./gradlew spotlessCheck
```

### Auto-fix formatting

```bash
# Auto-fix ktlint
./gradlew ktlintFormat

# Auto-fix Spotless
./gradlew spotlessApply
```

### Full quality gate (run before PR)

```bash
./gradlew detekt ktlintCheck test
```

---

## Project Structure

```
Secure-Offline-Mesh-Messenger/
├── .github/                    # GitHub templates
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/
├── android/                    # Android application
│   ├── app/
│   │   ├── src/main/java/com/mesh/emergency/
│   │   │   ├── app/            # Application class, MainActivity
│   │   │   ├── core/
│   │   │   │   ├── common/     # Config, constants, extensions, logging
│   │   │   │   ├── designsystem/ # Theme, colors, typography
│   │   │   │   ├── navigation/ # Route definitions
│   │   │   │   └── utils/      # Date, permission utilities
│   │   │   ├── di/             # Hilt modules
│   │   │   ├── feature/        # Feature modules (Phase A2+)
│   │   │   ├── domain/         # Use cases, domain models (Phase A2+)
│   │   │   └── data/           # Repository implementations (Phase A2+)
│   │   ├── src/main/res/       # Android resources
│   │   └── build.gradle.kts
│   ├── gradle/
│   │   ├── libs.versions.toml  # Version Catalog
│   │   └── wrapper/
│   ├── config/
│   │   └── detekt/detekt.yml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── docs/                       # Project documentation
    ├── app/
    ├── communication/
    ├── development/
    ├── hardware/
    ├── security/
    └── roadmap/
```

---

## IDE Configuration

### Recommended Android Studio plugins
- **Kotlin** (bundled)
- **Compose Multiplatform IDE Support**
- **Database Navigator** (for Room inspection in Phase A2)
- **GitLive** (optional — for real-time collaboration)

### Code style settings
The project uses `.editorconfig` for formatting. Android Studio should pick it up automatically.

To manually import: **File → Settings → Code Style → Kotlin → Import scheme → From .editorconfig**

### Run configurations
After opening the project, the following run configurations should appear:
- **app** — installs and runs debug build on connected device/emulator

---

## Troubleshooting

### Gradle sync fails
```bash
# Clean the build cache
./gradlew clean

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

### `local.properties not found`
```bash
echo "sdk.dir=/path/to/android/sdk" > android/local.properties
```

### Font compilation errors
If Inter fonts are missing, comment out the `InterFontFamily` in `Typography.kt`
and use `FontFamily.Default` temporarily. The app will still compile.

### KSP annotation processing errors
```bash
./gradlew clean assembleDebug --rerun-tasks
```
