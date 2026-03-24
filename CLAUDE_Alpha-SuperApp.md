# CLAUDE.md — Alpha SuperApp Session Context

## Project Identity

- **App Name**: Alpha SuperApp
- **Developer**: Aaradhya Dev Tamrakar (aaradhyadevtmr@gmail.com)
- **Current Version**: 1.1.0 (released 2026-03-22)
- **Next Version**: 1.2.0 (in progress)
- **Platform**: Android
- **Min SDK**: API 21 (Android 5.0)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (declarative, MVVM)
- **Build System**: Gradle with Kotlin DSL

---

## Architecture

- **Pattern**: MVVM + Repository
- **State Management**: Kotlin Flow / StateFlow
- **Persistence**: Android DataStore (preferences), Room (planned for calorie tracker)
- **DI**: Manual constructor injection (no Hilt/Dagger)
- **Navigation**: Jetpack Compose NavHost with string routes

### Package Structure

```
com.alpha/
├── MainActivity.kt
├── core/
│   └── ai/GeminiClient.kt          ← Google Gemini API client (text + multimodal)
├── features/
│   ├── budget/                     ← Budget tracking module
│   ├── calculator/                 ← Calculator module
│   ├── sbrcontrol/                 ← Gesture/Bluetooth control module
│   ├── websearch/                  ← AI-powered web search module
│   └── settings/                  ← Theme & preferences module
└── ui/
    ├── home/HomeScreen.kt          ← App hub / feature grid
    ├── navigation/NavGraph.kt      ← Central route definitions
    └── theme/                      ← Color, Typography, Theme
```

---

## Existing Features

| Module | Route | Key Files |
|--------|-------|-----------|
| Budget Tracker | `budget` | BudgetViewModel, BudgetRepository, BudgetScreen, DriveSync, GmailParser, EsewaXlsParser |
| Calculator | `calculator` | CalculatorScreen |
| SBR Gesture Control | `sbrcontrol` | SbrControlViewModel, HandGestureProcessor, GestureLogic, GestureStability, BluetoothComm |
| AI Web Search | `web_search` | WebSearchViewModel, WebSearchScreen, GeminiClient |
| Settings | `settings` | SettingsViewModel, SettingsScreen, AppSettings |

### Notable Integrations
- **Google Gemini API**: Used in web search (text + image). `GeminiClient.kt` in `core/ai/`.
- **Google Drive API**: Sync in `DriveSync.kt` inside budget module.
- **Gmail Parsing**: `GmailParser.kt` extracts transactions from emails.
- **Esewa XLS Parser**: `EsewaXlsParser.kt` parses Esewa payment Excel exports.
- **MediaPipe**: `hand_landmarker.task` in `assets/models/` — 21-point hand skeleton, ~30fps.
- **CameraX**: Used in both SBR (gesture) and Bill Photo (budget).

### Permissions (current)
- `INTERNET` — Gemini, Google APIs
- `CAMERA` — gesture recognition + bill photo
- `BLUETOOTH`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` — SBR device control

---

## Planned Changes (v1.2.0)

### 1. HTML Viewer — New Module
- **Route**: `html_viewer`
- **Location**: `features/htmlviewer/`
- WebView via `AndroidView` in Compose; JS + CSS enabled
- File picker: `ActivityResultContracts.OpenDocument` (MIME: text/html, text/css, application/javascript)
- Folder browser: `ActivityResultContracts.OpenDocumentTree` + `DocumentFile`
- Recent files: persisted via DataStore
- **New permissions**: `READ_EXTERNAL_STORAGE` (API < 33) or `READ_MEDIA_IMAGES`

### 2. Calorie Tracker — New Module
- **Route**: `calorie_tracker`
- **Location**: `features/calorietracker/`
- Data model: `FoodEntry(id, name, calories, protein, carbs, fat, timestamp, mealType)`
- Food identification: Open Food Facts API (`world.openfoodfacts.org`) or Gemini multimodal photo prompt
- Reuse `BillPhotoManager` pattern for camera capture
- Persistence: Room database (preferred over DataStore for date-based queries)
- Daily log grouped by meal type; daily goal vs. actual calories display

### 3. Google Sign-In — New Core Service
- **Location**: `core/auth/GoogleAuthManager.kt`
- Dependency: `com.google.android.gms:play-services-auth`
- `GoogleSignInOptions` with email + Drive scope
- Centralizes auth for `DriveSync` and `GmailParser` (currently auth is scattered)
- Session stored in `AppSettings` via DataStore
- Requires `google-services.json` in app root and OAuth client ID in `strings.xml`

### 4. Home Screen Categorization — UI Refactor
- **Location**: `ui/home/HomeScreen.kt` + new `HomeViewModel.kt`
- Layout modes: Grid (`LazyVerticalGrid`), List (`LazyColumn`), Sectioned (sticky headers)
- Default categories: Finance, Tools, AI, Device, System
- Selected layout persisted in `AppSettings`
- Model: `AppCategory(name, icon, routes: List<AppTile>)`

### 5. Calculator Fix — % and MOD
- **Location**: `features/calculator/CalculatorScreen.kt` (+ ViewModel)
- `%` → standard percentage: divides operand by 100 (e.g. `200 + 10%` = `220`)
- New `MOD` button → performs modulo (`a % b`) between two operands
- Parser must distinguish `%` token (percentage) vs `MOD` token (modulo)

### 6. Budget Tracker — Crash Fix + Income Tracking
**Bug Fix:**
- Transaction screen crashes on open — investigate null-safety on `Transaction` fields during DataStore deserialization
- Wrap StateFlow collection in `safeCollect`; show snackbar on error instead of crash
- Add `?.let` guards in `BudgetScreen.kt` and `BudgetRepository.kt`

**Income Tracking:**
- Extend `Transaction.kt`: add `type: TransactionType` enum (`INCOME` / `EXPENSE`)
- Existing Esewa imports classified as `EXPENSE`
- New "Add Income" dialog: amount, source, date, note → creates `Transaction(type = INCOME)`
- Recurring monthly income: `WorkManager` scheduled task auto-creates monthly INCOME entry
- **Net Worth formula**: `Sum(INCOME) - Sum(EXPENSE)` computed as derived StateFlow in `BudgetViewModel`
- Esewa XLSX is now just one import source, not the sole net worth determinant

**Expanded Categories (add to `TransactionCategory` enum):**
`GROCERIES`, `UTILITIES`, `RENT`, `HEALTHCARE`, `EDUCATION`, `SAVINGS`, `INVESTMENT`, `DINING`, `SHOPPING`, `SUBSCRIPTIONS`

---

## Key Conventions

- All screens are Composable functions, not Fragments
- ViewModels expose `StateFlow<ScreenState>` consumed by screens via `collectAsState()`
- Repository layer handles all DataStore / Room / network access
- Errors surfaced via state (never thrown to UI layer)
- Navigation done via `navController.navigate(route)` — no argument bundles except where necessary
- File references follow feature-first packaging: `features/{module}/{Screen|ViewModel|Repository}.kt`

---

## Files Reviewed This Session

| File | Notes |
|------|-------|
| `CodeDescription.md` | Full architecture + module breakdown |
| `README.md` | Sparse, needs version/feature/dependency fill-in |
| `CHANGELOG.md` | v1.1.0 logged; v1.2.0 unreleased section to be populated |

---

## Pending Documentation Updates

- `README.md`: Fill in Kotlin version, target API, actual features list, real dependency names, permissions section
- `CHANGELOG.md`: Populate `[1.2.0] - Unreleased` with all 6 feature entries (see session notes)
- Consider wiring semantic versioning Gradle plugin to read from CHANGELOG to prevent version drift between README and `build.gradle.kts`
