# Flash Idea

Flash Idea is an Android app for lightweight idea capture, AI-assisted organization, local knowledge graph exploration, and agent-style note processing.

This repository is the clean release package for the latest app version. It contains only the Android source project and packaged release files.

## Download

Debug APK:

```text
release/FlashIdea-debug.apk
```

Source package:

```text
release/FlashIdea-source.zip
```

## Build

Requirements:

- JDK 17
- Android SDK with compile SDK 34

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

Or:

```powershell
.\gradlew.bat assembleDebug
```

## AI Configuration

Copy `local.properties.example` to `local.properties`, then fill in your local SDK path and optional cloud model credentials.

`local.properties` is ignored by Git. Keep App IDs, API keys, and model credentials out of commits.
