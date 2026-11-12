# Z AI — Personal AI & Code Studio for Android

**Z AI** is a native Android workspace built with Kotlin and Jetpack Compose featuring multi-turn AI chat with KV-cache prompt reuse, an integrated multi-file Code Studio with live HTML preview, a local AI Document & File Analyzer, Task Manager, and Voice commands.

---

## Direct APK Download

The compiled and signed release APK is included directly in this repository:

- **[Download Signed Release APK (`releases/Z-AI-Assistant-v1.0-release.apk`)](releases/Z-AI-Assistant-v1.0-release.apk)**
  - **Application ID**: `com.aistudio.omniworkspace.qrvxnz`
  - **Version**: `1.0` (`versionCode = 1`)
  - **Minimum Android SDK**: Android 7.0 (`minSdk = 24`, `targetSdk = 36`)

---

## Core Features

- **Code Studio & Recent Files (`X/5`)**:
  - Edit multi-file web and code projects with an instant Live Preview.
  - Persistent **Recent Files (`X/5`)** bar saves your last 5 opened project files and snippets locally for one-tap switching.
  - **Export Project as `.zip` or Standalone `.html`** directly to device storage using Android's Storage Access Framework.
- **AI Document & File Analyzer**:
  - Open any local `.txt`, `.md`, `.json`, `.csv`, `.xml`, `.html`, or source code file.
  - Generates word, line, character, and estimated token counts, extracts section headings and actionable items, converts action items into tasks, and exports `.md` analysis reports.
- **KV-Cache Multi-Turn Prompt Reuse**:
  - Reuses cached system-prompt and conversation-history prefixes across multi-turn chat sessions with live cache hit rate and token-savings telemetry.
- **Task Manager & Voice Workspace**:
  - Organize priorities, set reminders, and execute hands-free voice actions.

---

## Building from Source

```bash
KEYSTORE_PATH=/path/to/upload-keystore.jks \
STORE_PASSWORD=android \
KEY_PASSWORD=android \
gradle assembleRelease
```

A GitHub Actions workflow (`.github/workflows/release-apk.yml`) is also included to automatically build and publish `Z-AI-Assistant-v1.0-release.apk` to **GitHub Releases** on every push.
