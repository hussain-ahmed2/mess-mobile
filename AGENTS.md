This is a **native Android** app written in Kotlin with Jetpack Compose. There is no React
Native, no Expo and no JS toolchain in this repository.

MessMate BD is a mess manager for Bangladeshi Bachelor/Student messes that runs on a
**"Bring Your Own Google Sheet" (BYOS)** architecture: the mess manager creates a Google
Spreadsheet from the app, the app writes the sheet structure and the formulas, and every
member reads and writes that very same sheet. There is **no MessMate server**.

## Architecture rules (do not break them)

* **The sheet is the master.** The app is a front end for it. Every number that the app
  shows must exist as a formula in the `Summary` tab – the local calculation engine
  (`CalculationEngine`) is only a mirror of it so the app works offline.
* **Room is the source of truth for the UI.** The ViewModels never read from the network
  directly. Writes go to Room first (optimistic) and are queued in the `pending_operations`
  outbox; `SyncWorker` replays the queue when the device is online.
* **Never read-modify-write a sheet cell.** Either target a single cell with
  `values.batchUpdate` or append a row with `values.append`. Two members must never be able
  to overwrite each other.
* **Read everything in one request.** `values.batchGet` with all tab ranges – one HTTP
  request per sync, not one per tab.
* **Only `drive.file`.** The app never asks for the restricted `drive` or `spreadsheets`
  scopes. That is why the sheet has to be created programmatically (a file the app creates
  is fully accessible with `drive.file`).
* **The `Meals` grid is member-major.** Row = `2 + memberIndex * daysInMonth + (day - 1)`.
  Member ids are strictly increasing (`M001`, `M002`, …) and members are never deleted
  (only deactivated), so a row never moves when somebody joins later. Every device derives
  the same coordinates from the `Members` tab – see `MealGrid`.

## The sheet schema is a public contract

`domain/model/SheetSchema.kt` is the single source of truth for the 8 tabs, their headers,
column order, the `Config` row numbers and the `Summary` layout. Changing it changes the
format of every mess sheet in the wild – bump `SCHEMA_VERSION` and write a migration when
you do.

## Versions are pinned, verify before upgrading

Gradle, AGP, Kotlin and the AndroidX libraries are pinned in `gradle/libs.versions.toml` and
`build.gradle.kts`. Do not bump them without checking the compatibility matrix:

- AGP 9.5.1 requires Gradle 9.6.0+ and JDK 17.
- AGP 9 has **built-in Kotlin support**: do **not** apply `org.jetbrains.kotlin.android`.
  The Kotlin Gradle plugin version is pinned through `buildscript { classpath(...) }` in the root
  `build.gradle.kts` and has to stay in sync with `libs.versions.toml`.
- Every AndroidX dependency declares a `minCompileSdk` in its AAR metadata. The app compiles
  against SDK 37 (`compileSdk = 37`, `compileSdkMinor = 0`), so a dependency that requires 38+
  breaks the build. Check `META-INF/com/android/build/gradle/aar-metadata.properties` before
  bumping a version. `targetSdk` stays at 36, that is what the Play Store asks for today.
- Room is processed with KSP, the KSP version is independent of the Kotlin version but has to
  support the Kotlin compiler that is used.
- `minSdk = 24` – `java.time` is used everywhere and is desugared
  (`isCoreLibraryDesugaringEnabled = true`). Do not use APIs that are not part of the
  desugared library (for example `java.util.Base64` – use `android.util.Base64`).

## Commands

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (needs signing secrets)
./gradlew test              # unit tests
./gradlew lint              # Android Lint
```

The Google OAuth client id is **not** in the repository. Put it into `local.properties`:

```properties
MESSMATE_SERVER_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

or export `MESSMATE_SERVER_CLIENT_ID` before building. It is the OAuth client id of a **Web
application** (client_type = 3) from the Google Cloud console and is used by Credential
Manager ("Sign in with Google") and by the GMS `AuthorizationClient` that requests the
`drive.file` scope.

## Coding conventions

- Comments explain **why**, not what. Keep them short and in English.
- Domain code (`domain/`) is pure Kotlin: no Android imports, no network, no Room.
  That is what makes the calculation engine and the cut-off policy unit testable.
- Public APIs get KDoc. Enums keep their `sheetValue` – that string is what ends up in the
  spreadsheet, so it must stay stable.
- Money is rounded with `CalculationEngine.money()` (two decimals) – the same way the
  spreadsheet rounds.
