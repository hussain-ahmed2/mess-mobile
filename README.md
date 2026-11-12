# MessMate BD

Native Android app for running a Bangladeshi Bachelor/Student **mess** on top of
**your own Google Sheet**.

There is no MessMate server. The mess manager creates a Google Spreadsheet from
the app, the app writes the sheet structure and the formulas, and every member
reads and writes the very same sheet. The sheet stays the single source of
truth – the app is only a fast, offline capable front end for it.

---

## 1. Tech stack

| Layer | Choice |
|-------|--------|
| Language / UI | Kotlin, Jetpack Compose, Material 3, Navigation Compose (UI layer pending) |
| Architecture | MVVM + Clean Architecture, `StateFlow`, Unidirectional data flow |
| Auth | Google **Credential Manager** (identity) + GMS `AuthorizationClient` (`drive.file` token) |
| Cloud | Google Drive REST v3 + Google Sheets REST v4 |
| Cache | Room (offline cache, instant start-up) |
| Background | WorkManager (outbox replay + periodic sync) |
| DI | Hilt |
| PDF | `android.graphics.pdf.PdfDocument` + Android share sheet (UI layer pending) |
| i18n | English / বাংলা via `values/` + `values-bn/` |

### Toolchain (pinned – see `AGENTS.md`)

AGP 9.5.1 · Gradle 9.9.1 · Kotlin 2.4.21 · JDK 17 · `compileSdk = 37` · `targetSdk = 36` ·
`minSdk = 24` (with `java.time` desugaring)

### OAuth scope constraint

The app only ever requests:

```
openid email profile
https://www.googleapis.com/auth/drive.file
```

`drive.file` grants access **only to files the user picked or the app created**.
The restricted `drive` and `spreadsheets` scopes are never requested – which is
exactly why the sheet has to be created *programmatically* by the app
(see `SpreadsheetProvisioner`): a file created by the app is fully readable and
writable with `drive.file`.

Sign-in happens in two steps:

1. **Credential Manager** – the user picks a Google account, the app gets the
   e-mail address (the identity of the member) and an id token.
2. **GMS `AuthorizationClient`** – the app requests the single `drive.file`
   scope and receives an OAuth2 access token for the Drive/Sheets REST calls.

---

## 2. Module map

```
app/src/main/java/com/messmate/bd/
├── domain/                     ← pure Kotlin, no Android, no network
│   ├── model/                  Enums, domain models, SheetSchema, MealGrid
│   ├── calculation/            CalculationEngine (mirror of the sheet formulas),
│   │                           CutoffPolicy (cut-off rules)
│   └── repository/             MessRepository, AccountRepository (interfaces)
├── data/
│   ├── remote/                 GoogleApiFactory, SheetsGateway, DriveGateway,
│   │                           SpreadsheetProvisioner, MonthRolloverService,
│   │                           SummaryFormulaBuilder
│   ├── local/                  Room database, entities, DAOs, converters
│   ├── mapper/                 Sheet ⇄ entity ⇄ domain mapping
│   ├── repository/             MessRepositoryImpl, SheetWriter, AccountRepositoryImpl
│   └── work/                   SyncWorker (outbox replay + pull)
├── auth/                       GoogleAuthManager (Credential Manager + GMS
│                               AuthorizationClient), SessionStore (AES-GCM +
│                               Android Keystore)
├── core/network/               NetworkMonitor
└── di/                         Hilt modules
```

---

## 3. The generated spreadsheet

### Tabs

| Tab | Shape | Written with |
|-----|-------|--------------|
| `Config` | `Key \| Value` (8 keys) | `batchUpdate` (RAW) |
| `Members` | `MemberID \| Name \| Gmail \| Role \| SeatRent \| ActiveStatus \| PrevBalance` | `batchUpdate` (RAW) |
| `Meals` | `Date \| MemberID \| Breakfast \| Lunch \| Dinner \| GuestMeals \| UpdatedBy \| UpdatedAt` | `batchUpdate` (RAW) |
| `Bazar` | `EntryID \| Date \| ShopperName \| Items \| Amount \| LoggedBy \| Timestamp` | `append` |
| `UtilityBills` | `BillID \| Date \| Category \| Amount \| SplitType \| TargetMemberID \| Notes` | `append` |
| `Deposits` | `DepositID \| Date \| MemberID \| Amount \| Method \| TrxID \| Status` | `append` |
| `AuditLog` | `LogID \| Timestamp \| ActorEmail \| ActionType \| Details` | `append` |
| `Summary` | 100 % formulas (see below) | `batchUpdate` (USER_ENTERED) |

### `Config` keys (row 1 = header)

| Row | Key | Example |
|-----|-----|---------|
| 2 | `MessName` | সায়েন্টস মেস |
| 3 | `MonthYear` | `2026-11` |
| 4 | `ManagerEmail` | `manager@gmail.com` |
| 5 | `BreakfastWeight` | `1` or `0.5` |
| 6 | `BreakfastCutoff` | `07:00` |
| 7 | `LunchCutoff` | `10:00` |
| 8 | `DinnerCutoff` | `17:00` |
| 9 | `LockStatus` | `OPEN` / `LOCKED` |

### The `Meals` grid

The tab is pre-populated with **one row per (member, day)** so that a meal
toggle is a *single cell write* – no append, no row shifting, no collisions
when two members toggle at the same time.

Rows are grouped **member first, then day**:

```
M001 → day 1 … day 31      rows  2 … 32
M002 → day 1 … day 31      rows 33 … 63
M003 → day 1 … day 31      rows 64 … 94
```

Member ids are strictly increasing (`M001`, `M002`, …) and members are never
deleted (only deactivated), therefore the row of a member never moves when
somebody joins later. Every device derives the same coordinates from the
`Members` tab + the month length → see `MealGrid`.

### `Summary` formulas

```
Row 1  | TotalBazar | =SUM(Bazar!$E$2:$E$5000)
       | TotalMeals | =SUM(Meals!$C$2:$C$4000)*Config!$B$5+SUM(Meals!$D$2:$D$4000)
       |            |  +SUM(Meals!$E$2:$E$4000)+SUM(Meals!$F$2:$F$4000)
       | LiveMealRate | =IFERROR($B$1/$D$1,0)
       | TotalEqualUtilities | =SUMIFS(UtilityBills!$D$2:$D$5000,
       |                             UtilityBills!$E$2:$E$5000,"EQUAL_ALL")
Row 2  | Mess | =Config!$B$2      | Month | =Config!$B$3
Row 3  | MemberID | Name | TotalMeals | MealCost | SeatRent | EqualUtility
       | CustomUtility | TotalCost | PrevBalance | TotalDeposit | NetBalance
Row 4+ | one formula block per member:
         TotalMeals    = SUMIFS(Meals!C…)*Config!$B$5 + SUMIFS(Meals!D…) + …
         MealCost      = ROUND($C4*IFERROR($F$1,0),2)
         SeatRent      = IFERROR(VLOOKUP($A4,Members!$A$2:$G$500,5,FALSE),0)
         EqualUtility  = IFERROR(IF(VLOOKUP($A4,Members!$A$2:$G$500,6,FALSE)="ACTIVE",
                          $H$1/COUNTIFS(Members!$F$2:$F$500,"ACTIVE"),0),0)
         CustomUtility = SUMIFS(UtilityBills!$D$2:$D$5000,
                                UtilityBills!$F$2:$F$5000,$A4)
         TotalCost     = ROUND($D4+$E4+$F4+$G4,2)
         PrevBalance   = IFERROR(VLOOKUP($A4,Members!$A$2:$G$500,7,FALSE),0)
         TotalDeposit  = SUMIFS(Deposits!$D$2:$D$5000,Deposits!$C$2:$C$5000,$A4,
                                Deposits!$G$2:$G$5000,"VERIFIED")
         NetBalance    = ROUND(($I4+$J4)-$H4,2)
```

`CalculationEngine` mirrors these formulas 1:1 so the app shows the same
numbers while offline.

### Domain rules that are encoded in the schema

* **Meal rate** = `TotalBazar / TotalMeals`, breakfast weighted (0.5 / 1.0),
  guest meals count as full meals and belong to the member that hosts them –
  so the bazar money is distributed without a leak.
* **Variable cost** = `Member Meals × Meal Rate`.
* **Fixed cost** = `SeatRent + EqualUtility + CustomUtility`.
  `UtilityBills` are either `EQUAL_ALL` (split between the active members) or
  `CUSTOM_MEMBER` (charged to one member, e.g. an AC sub-meter).
* **Net balance** = `(PrevBalance + verified deposits) − TotalCost`
  (positive = the manager owes the member).
* **Bazar vs. utility** – LPG can be logged either as a `Bazar` entry or as a
  `UtilityBills` entry with category `Gas`; the manager decides per entry.
* **Month rollover** – the manager locks the sheet
  (`Config!LockStatus = LOCKED`), a **new** spreadsheet is created for the next
  month with the same members, the same seat rents and the closing balance as
  `PrevBalance`. The old sheet is renamed to `… (Closed)`.

---

## 4. Data flow

```
ViewModel ──▶ MessRepository ──▶ Room ──▶ UI
                     │              ▲
                     │              │ optimistic writes + outbox
                     ▼              │
               Google Sheet ◀── SyncWorker (WorkManager)
```

* The UI **always** reads from Room → instant start-up, works offline.
* Every command is written to Room first (optimistic) and queued in the
  `pending_operations` outbox; the queue is pushed to the sheet immediately
  when online and replayed by `SyncWorker` otherwise.
* One `values.batchGet` reads **all** tabs in a single HTTP request.
* Writes use `values.batchUpdate` (targeted cells) and `values.append`
  (ledgers) – never a read-modify-write cycle.

---

## 5. Setup

1. Create a project in the [Google Cloud console](https://console.cloud.google.com).
2. Enable **Google Drive API** and **Google Sheets API**.
3. Create an **OAuth client ID → Web application** and copy the client id.
4. Put it into `local.properties` (not committed):

   ```properties
   MESSMATE_SERVER_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
   ```

   (or export `MESSMATE_SERVER_CLIENT_ID` before building)
5. Open the project in Android Studio and run the `app` configuration.

### Permissions the app asks for

`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` (sync status) and
`CAMERA` (QR scan for “join a mess”).

---

## 6. Project status

| Area | State |
|------|-------|
| Gradle / Hilt / Room / WorkManager setup | ✅ |
| Domain model, calculation engine, cut-off policy | ✅ |
| Google Sheet provisioning (8 tabs + formulas + sharing) | ✅ |
| Room cache, mappers, repository, outbox, sync worker | ✅ |
| Credential Manager sign-in, encrypted session storage | ✅ |
| Compose UI | ✅ onboarding, dashboard, bazar, utilities, members, statement, audit, settings |
| PDF export (`PdfDocument` + share sheet) | ✅ |
| QR invite + QR scanner | ✅ |

### Screens

| Screen | What it does |
|--------|--------------|
| Onboarding | Google sign-in, the scope explainer, "create a mess" / "join a mess" |
| Create mess | Mess name, month, cut-off times, breakfast weight, the first members |
| Join mess | Paste the sheet id, pick it from Drive, or scan the manager's QR code |
| Dashboard | Live meal rate, personal balance, today's and tomorrow's meals with the cut-off countdown |
| Bazar | The market trips and the total bazar |
| Utilities | Cook, gas, electricity, Wi-Fi, water, garbage – equal split or one member |
| Members | The roster, seat rents, deposits (with verify/reject) and the invite QR code |
| Statement | The full monthly breakdown of every member + PDF export |
| Audit log | Every change of the mess, newest first |
| Settings | Language (English / বাংলা), cut-offs, breakfast weight, close the month, sign out |
