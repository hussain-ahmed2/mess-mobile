# HANDOFF — MessMate BD

**Repo:** `git@github.com-personal:hussain-ahmed2/mess-mobile.git` · branch `main`
**Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room, WorkManager, Credential Manager, Google Drive/Sheets REST.

MessMate BD is a mess manager for Bangladeshi Bachelor/Student messes on a **"Bring Your Own Google Sheet" (BYOS)** architecture: the manager creates a Google Spreadsheet from the app, the app writes the sheet structure and the formulas, and every member reads and writes that very same sheet. **There is no MessMate server.** The sheet is the master; the app is a fast, offline-capable front end for it.

---

## 1. Setup on the new machine (15 min)

### 1.1 Toolchain (pinned – see `AGENTS.md`)

| Tool | Version |
|------|---------|
| Gradle | 9.9.1 (wrapper is committed – `./gradlew` just works) |
| AGP | 9.5.1 (built-in Kotlin support → **do not** apply `org.jetbrains.kotlin.android`) |
| Kotlin | 2.4.21 (pinned via the root `buildscript { classpath(...) }`) |
| JDK | 17 or 21 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 24 (`java.time` is desugared) |

```bash
sdkmanager "platforms;android-37.0" "build-tools;37.0.0"   # if not installed
```

### 1.2 The one thing you must add: the OAuth client id

The client id is **not** in the repo. Create it in the
[Google Cloud console](https://console.cloud.google.com):

1. Enable **Google Drive API** and **Google Sheets API**.
2. *Credentials → Create credentials → OAuth client ID → **Web application***
   (`client_type = 3`).
3. Copy the id into `local.properties` (git-ignored):

```properties
sdk.dir=/path/to/Android/Sdk
MESSMATE_SERVER_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
```

`local.properties.example` is the template. The value is embedded as
`BuildConfig.GOOGLE_SERVER_CLIENT_ID` and used in two places:
Credential Manager (`GetSignInWithGoogleOption`) and the GMS
`AuthorizationClient` that requests the `drive.file` scope.

### 1.3 Build & test

```bash
./gradlew assembleDebug      # debug APK
./gradlew test               # unit tests
./gradlew lint               # Android Lint
```

State at hand-off: **assembleDebug ✅ · unit tests ✅ · lint ✅ (0 errors)**.

---

## 2. Architecture map

```
app/src/main/java/com/messmate/bd/
├── domain/                 pure Kotlin – no Android, no network, no Room
│   ├── model/              Enums, domain models, SheetSchema, MealGrid
│   ├── calculation/        CalculationEngine (mirror of the sheet formulas),
│   │                       CutoffPolicy (cut-off rules per role)
│   └── repository/         MessRepository, AccountRepository (interfaces)
├── data/
│   ├── remote/             GoogleApiFactory, SheetsGateway, DriveGateway,
│   │                       SpreadsheetProvisioner, MonthRolloverService,
│   │                       SummaryFormulaBuilder
│   ├── local/              Room: entities, DAOs, converters
│   ├── mapper/             sheet ⇄ entity ⇄ domain
│   ├── repository/         MessRepositoryImpl, SheetWriter, AccountRepositoryImpl
│   └── work/               SyncWorker (outbox replay + pull)
├── auth/                   GoogleAuthManager (Credential Manager + GMS
│                           AuthorizationClient), SessionStore (AES-GCM +
│                           Android Keystore)
├── core/                   NetworkMonitor, LocaleController
└── ui/                     theme, navigation, one package per screen
```

### The sheet is the contract

`domain/model/SheetSchema.kt` is the **single source of truth** for the 8 tabs,
their headers, column order, the `Config` row numbers and the `Summary` layout.
Changing it changes the format of every mess sheet in the wild → bump
`SCHEMA_VERSION` and write a migration.

| Tab | Written with | Content |
|-----|--------------|---------|
| `Config` | batchUpdate (RAW) | 8 key/value rows (row 1 = header → `MessName`=B2 … `LockStatus`=B9) |
| `Members` | batchUpdate (RAW) | id, name, gmail, role, seat rent, status, prev balance |
| `Meals` | batchUpdate (RAW) | **pre-populated grid**, one row per (member, day) |
| `Bazar` / `UtilityBills` / `Deposits` / `AuditLog` | `values.append` | append-only ledgers |
| `Summary` | batchUpdate (USER_ENTERED) | 100 % formulas (metrics + one row per member) |

**Meal grid:** rows are *member-major* → `row = 2 + memberIndex * daysInMonth + (day - 1)`.
Member ids are strictly increasing (`M001`, `M002`, …) and members are never
deleted (only deactivated), so a row never moves when somebody joins later.
Every device derives the same coordinates from the `Members` tab.

**Formula mirror:** `CalculationEngine` reproduces the `Summary` formulas 1:1 so
the app shows the sheet's numbers while offline. Change one → change the other.

---

## 3. What is done

* **Sheet provisioning** – Drive file creation, the 8 tabs, the formulas,
  member sharing, the first audit row.
* **Month rollover** – lock the sheet, create the next month's sheet with the
  same members/seat rents and the closing balance as `PrevBalance`, rename the
  old one `… (Closed)`.
* **Sync** – `values.batchGet` reads all tabs in **one** request; writes are
  single-cell `batchUpdate` or `append` (never read-modify-write).
* **Offline** – Room is the source of truth for the UI; every write is queued
  in `pending_operations` and replayed by `SyncWorker` (network constraint +
  15 min periodic).
* **Auth** – Credential Manager for the identity, GMS `AuthorizationClient`
  for the single `drive.file` scope, session encrypted with AES-256-GCM
  (Android Keystore).
* **UI** – onboarding (sign in / create / join), dashboard (live meal rate,
  balance, meal switches with cut-off countdown, guest-meal stepper), bazar,
  utilities (equal vs. custom split), members (seat rents, deposits,
  invite QR), statement + PDF export, audit log, settings (language, cut-offs,
  breakfast weight, close month, sign out).
* **i18n** – runtime switch English ⇄ বাংলा (`LocaleManager` on API 33+,
  configuration context below); both languages are in the base APK
  (`bundle { language { enableSplit = false } }`).

---

## 4. NOT verified yet (needs a real device + a real client id)

Nothing has been executed against the live Google APIs. Check in this order:

1. **Sign in** – Credential Manager account picker → the `drive.file` consent
   screen → the app reaches the onboarding.
2. **Create a mess** – the sheet appears in the manager's Drive; open it in the
   browser and check:
   * all 8 tabs exist, `Sheet1` is gone, row 1 is frozen;
   * the `Summary` tab shows `TotalBazar`, `TotalMeals`, `LiveMealRate`,
     `TotalEqualUtilities` and one formula row per member;
   * the `Meals` tab has `members × days` rows.
3. **Second device** – share the sheet with a second Gmail, join it there and
   toggle a meal → the first device sees it after a sync.
4. **Cut-off** – set a cut-off 2 minutes ahead, wait → the switch locks for the
   member, the manager can still override.
5. **Offline** – airplane mode, log a bazar trip → the "pending sync" badge
   appears → back online → the row shows up in the sheet.
6. **Month end** – close the month → the old sheet is locked and renamed, a new
   sheet exists for the next month with the carried-over balances.
7. **PDF** – export the statement and share it to WhatsApp.

---

## 5. Known gaps / things to improve

Ordered by how likely they are to bite.

1. **`GoogleAuthManager.currentToken()` uses `runBlocking` inside the HTTP
   interceptor.** It runs on an IO thread, so it works, but it can deadlock if
   the main thread is ever blocked. Better: mint the token before the request
   (`GoogleAuthManager` exposes `refreshAccessToken()`), or cache a token that
   is refreshed by the sync worker.
2. **The scope consent can expire.** `refreshAccessToken()` returns `null` when
   Google wants a consent screen again; the request then fails with "no
   account". The UI should react by calling
   `accountRepository.requestAccessToken(activity)` again.
3. **The manager cannot override another member's meal from the dashboard.**
   The repository allows it (`setMeal` with any member id + MANAGER role), but
   the UI only toggles *my own* meals. A member picker is missing.
4. **`Summary` is never read back** – the app computes the numbers locally
   (deliberate: it works offline). If someone edits a formula in the sheet by
   hand, the app does not notice.
5. **Guest meals are attributed to the hosting member.** That keeps
   `Σ member meals == total meals` (no money leaks), but the sheet cannot
   distinguish "member was a guest" from "member hosted guests".
6. **The audit trail has no manual entries** – only mutations are logged.
7. **No push notifications.** Members do not get told that the meal rate moved.
8. **The QR scanner needs the camera permission** – the flow is implemented
   (`zxing-android-embedded`), the runtime permission dialog is not.
9. `applicationId` is `com.messmate.bd` while the old app used
   `com.hussain.messmobile` – the package changed, so it is a **new app** in
   the Play Store.

---

## 6. Third-party API gotchas (they cost me time – do not repeat them)

* **`google-api-services-sheets` moved to `com.google.api.services.sheets.v4`**
  and its values API changed:
  `values.batchGet(spreadsheetId).setRanges(...)`,
  `values.batchUpdate(spreadsheetId, BatchUpdateValuesRequest().setData(listOf(ValueRange)))`,
  `values.append(spreadsheetId, range, ValueRange).setInsertDataOption("INSERT_ROWS")`.
  `AddSheetRequest`/`UpdateSheetPropertiesRequest` take a **`SheetProperties`**
  (the sheet id lives inside it), not a `SpreadsheetProperties`.
* **Google Drive stayed at `com.google.api.services.drive`** (no `.v3`).
* **Credential Manager 1.6 lives in `androidx.credentials`** (not
  `androidx.credential`). The Google credential comes from
  `com.google.android.libraries.identity.googleid.GoogleIdTokenCredential`
  and it contains **no access token** – only the identity.
* **The access token for Drive/Sheets comes from the GMS
  `AuthorizationClient`** (`Identity.getAuthorizationClient(context)` →
  `authorize(AuthorizationRequest)` → `AuthorizationResult.accessToken`).
  When the user has to approve the scope, `result.hasResolution()` is `true`
  and the `PendingIntent` has to be started from an Activity – that is why
  `GoogleAuthManager.requestAccessToken(activity)` needs an Activity and
  `MainActivity.onActivityResult` forwards the result.
* `google-api-client` 2.9.2: the token interceptor is
  `request.interceptor = HttpExecuteInterceptor { ... }` (Kotlin hides the
  setter).

---

## 7. Git

```
main:  BYOS rewrite (data layer) → UI layer → polish
```

Working tree is committed and pushed. `local.properties` is git-ignored –
create it on the new machine (section 1.2).

```bash
git pull
./gradlew assembleDebug
```
