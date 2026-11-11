This is a **native Android** app written in Kotlin with Jetpack Compose. There is no React
Native, no Expo and no JS toolchain in this repository.

The app is a **local only** mess manager: the data lives in a Room database on the device,
there is no server and no account. The backup and restore feature writes the whole database
into a JSON file so the user gets their data back after an uninstall.

Prioritize mobile-first patterns, performance and cross-device compatibility.

## Versions are pinned, verify before upgrading

Gradle, AGP, Kotlin and the AndroidX libraries are pinned in `gradle/libs.versions.toml` and
`build.gradle.kts`. Do not bump them without checking the compatibility matrix:

- AGP 9.5.1 requires Gradle 9.6.0+ and JDK 17.
- AGP 9 has **built-in Kotlin support**: do **not** apply `org.jetbrains.kotlin.android`.
  The Kotlin Gradle plugin version is pinned through `buildscript { classpath(...) }` in the root
  `build.gradle.kts` and has to stay in sync with `libs.versions.toml`.
- Every AndroidX dependency declares a `minCompileSdk` in its AAR metadata. The app compiles
  against SDK 36, so a dependency that requires 37+ breaks the build. Check
  `META-INF/com/android/build/gradle/aar-metadata.properties` before bumping a version.
- Room is processed with KSP, the KSP version is independent of the Kotlin version but has to
  support the Kotlin compiler that is used.

## Commands

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (needs signing secrets)
./gradlew test              # unit tests
./gradlew lintDebug         # Android lint
```

Use the Gradle wrapper, never a system Gradle. JDK 17 is the reference JDK.

## Structure

- The screens live in `app/src/main/java/com/hussain/messmobile/ui/screens/`, one package per
  screen.
- Reusable UI lives in `app/src/main/java/com/hussain/messmobile/ui/components/`, the dialogs
  are grouped in `MessDialogs.kt`.
- The view models live in `app/src/main/java/com/hussain/messmobile/ui/mess/` and are created by
  `ViewModelFactory`.
- The database lives in `app/src/main/java/com/hussain/messmobile/data/local/`, the only place
  that talks to Room is `MessRepository`.
- Colours, spacing and typography live in `app/src/main/java/com/hussain/messmobile/ui/theme/`.
  The colour palette is provided with `LocalAppColors` / `appColors`, use `ThemedText` and the
  `appColors` accessor instead of hard coded colours so light and dark mode keep working.
- Navigation is set up in `ui/navigation/AppTabs.kt`, one enum entry per tab.

## Rules

- The database is the single source of truth. Screens read from the repository flows and write
  through the view models, never through a DAO.
- Never commit a keystore, a password or any other signing material. The release build reads
  them from environment variables (`ANDROID_KEYSTORE_*`) or from `~/.gradle/gradle.properties`
  (`MESS_UPLOAD_*`).
- The release APK is built by CI (`.github/workflows/release.yml`), not on a developer machine.
  Keep the workflow in sync with `compileSdk`/`buildToolsVersion` changes.
- The backup format is a public contract: it has to stay readable by every version of the app.
  Bump `BackupFile.CURRENT_FORMAT_VERSION` when the format changes in an incompatible way and
  keep `BackupFileTest` up to date.
- Run `./gradlew test lintDebug` before declaring a task done.
