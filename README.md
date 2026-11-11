# mess-mobile

A **native Android** mess manager written in **Kotlin** with **Jetpack Compose**.

Everything lives on the phone. There is no server, no account and no tracking: the app stores
the mess in a local [Room](https://developer.android.com/training/data-storage/room) database and
lets you save that data into a backup file, so you can continue where you left off after an
uninstall or on another phone.

## Features

| Area | What it does |
|---|---|
| **Members** | everybody who lives in the mess, with a phone number and a role (admin, cook, member) |
| **Meals** | breakfast, lunch and dinner per day, who cooks, what it cost and who joined |
| **Attendance** | mark who signed up and who actually ate, share the cost of a meal equally |
| **Dues** | what every member owes per month, record the payments and see what is still open |
| **Expenses** | the shared costs of the mess (groceries, power, repairs) with a monthly total |
| **Shopping list** | what still has to be bought and what it costs |
| **Announcements** | messages for the whole mess, pinnable |
| **Backup** | export the whole database into a JSON file and restore it later |

## Stack

| | |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose (Material 3) |
| Navigation | Navigation Compose (bottom tabs) |
| Storage | Room 2.8 (KSP) |
| Backup format | JSON (kotlinx.serialization) |
| Build | Gradle 9.9.1, Android Gradle Plugin 9.5.1 |
| SDK | `compileSdk` 37, `targetSdk` 36, `minSdk` 24 |
| JDK | 17 |

> AGP 9 has **built-in Kotlin support**, the `org.jetbrains.kotlin.android` plugin is therefore
> not applied. The Kotlin Gradle plugin version is pinned in `build.gradle.kts` and the Compose
> compiler plugin in `gradle/libs.versions.toml`.

## Project layout

```
app/src/main/java/com/hussain/messmobile/
├── MessApplication.kt          # database, repository and backup manager
├── data/
│   ├── local/                  # Room entities, DAOs and the database
│   ├── model/                  # the enums of the app
│   ├── repository/             # the single place that talks to the database
│   └── backup/                 # the backup file and the backup manager
├── ui/
│   ├── components/             # dialogs, cards and the shared widgets
│   ├── mess/                   # the view models
│   ├── navigation/AppTabs.kt   # the bottom navigation bar and the routes
│   ├── screens/                # one package per screen
│   └── theme/                  # colours, spacing and typography
└── util/DateUtils.kt
```

## Backup and restore

This is the part that makes an uninstall harmless:

1. Open **More → Backup** and tap **Save a backup**. The system dialog lets you pick where the
   file is stored, for example on the SD card, in Google Drive or in a chat.
2. Uninstall the app whenever you want, the backup file stays where you put it.
3. Install the app again, open **More → Backup**, tap **Restore a backup** and pick the file.
   The app asks for a confirmation and then replaces everything with the content of the backup.

The backup is a plain JSON file. It is human readable, it can be moved between phones and it
keeps every relation between the records (the meals still point at their cook, the payments
still point at their member).

The Android system backup is enabled as well, so the app data is also copied by Android itself
when the device is backed up.

## Building

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK, needs signing (see below)
./gradlew test              # unit tests
./gradlew lintDebug         # Android lint
```

The release build is **not** meant to be run on a developer machine, it is built by CI.

## Continuous integration

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/ci.yml` | push to `main`, pull requests | unit tests, debug build, lint |
| `.github/workflows/release.yml` | tag `v*` or manual dispatch | signed release APK, GitHub release |

### Release

Push a tag and the release workflow builds a signed APK and attaches it to a GitHub release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The tag is used as the `versionName`, the run number is used as the `versionCode`. A manual run
(`workflow_dispatch`) allows overriding both.

### Repository secrets

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | the upload keystore, base64 encoded |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Create the keystore once:

```bash
keytool -genkey -v -keystore release.keystore -alias upload -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.keystore
```

Store the output of `base64` in `ANDROID_KEYSTORE_BASE64`. **Never commit the keystore**, the
`.gitignore` already ignores `*.jks` and `*.keystore`.

### How the release APK is signed

Gradle builds the release APK **unsigned**, the CI signs it with `apksigner`. The keystore
therefore never has to be part of the Gradle configuration and cannot leak into the build
script or into a stack trace.

### Signing outside of CI

The same two commands work on a developer machine:

```bash
./gradlew assembleRelease
"$ANDROID_HOME/build-tools/37.0.0/apksigner" sign \
  --ks /absolute/path/release.keystore \
  --ks-key-alias upload \
  --ks-pass pass:YOUR_STORE_PASSWORD \
  --key-pass pass:YOUR_KEY_PASSWORD \
  --out mess-mobile.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```
