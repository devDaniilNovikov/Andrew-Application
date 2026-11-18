# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

«Контроль заявок» — a single-user, fully offline Android app for a refrigeration-repair technician to track call-backs and visits. Product spec: `PRD.md`; stage breakdown with checkboxes: `TASKS.md` (both in Russian). All UI text, KDoc and most comments are Russian.

One Gradle module `:app`, package `ru.andrew.application`: Kotlin, Jetpack Compose + Material 3, Navigation Compose, Room via KSP, coroutines/StateFlow. AGP 9.2, Gradle 9.4 (wrapper), Kotlin 2.4, minSdk 26, compile/targetSdk 35, Java 17. AGP 9 compiles Kotlin itself, so the module deliberately does not apply `org.jetbrains.kotlin.android` — don't add it.

## Commands

Needs JDK 17+ and an Android SDK (`ANDROID_HOME`, or `sdk.dir` in the git-ignored `local.properties`); without one Gradle fails during configuration.

```bash
./gradlew assembleDebug                # debug APK
./gradlew installDebug                 # install on a connected device/emulator
./gradlew compileDebugKotlin           # quickest compile check
./gradlew testDebugUnitTest            # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "ru.andrew.application.CreateRequestViewModelTest"                                        # one class
./gradlew testDebugUnitTest --tests "ru.andrew.application.OfflineManifestPolicyTest.mainActivityIsExportedLauncherActivity"  # one method
./gradlew connectedDebugAndroidTest    # instrumented tests (RequestDaoTest), needs a device/emulator
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ru.andrew.application.data.db.RequestDaoTest
./gradlew lintDebug                    # Android Lint; no ktlint/detekt is configured
```

## Architecture

UI (Compose screens) → ViewModel (StateFlow) → `RequestRepository` → `RequestDao` (Room). What spans several files:

- **No DI framework.** `di/DependencyProvider.kt` is a hand-written service locator of singletons (database, repositories, `NotificationScheduler`, `TimeProvider`). Every ViewModel has a `companion object Factory` that pulls from it via `CreationExtras[APPLICATION_KEY]`; screens call `viewModel(factory = XViewModel.Factory)`. New ViewModels follow the same pattern.
- **Every write goes through `RequestRepositoryImpl`, never the DAO directly.** It stamps `createdAt`/`updatedAt`/`closedAt` from `TimeProvider` and schedules or cancels the reminder alarm on create/update/complete/cancel/restore/delete. Status changes use targeted `UPDATE` queries (`updateRequestStatusAndResults`, `updateRequestResultsOnly`) rather than read-modify-write.
- **Request lifecycle:** `ACTIVE` → `COMPLETED` | `CANCELLED` (sets `closedAt`); `restoreToActive` backs the Snackbar undo and clears all result fields. Active list: `nextActionDateTime ASC`. History: `closedAt DESC`, or COMPLETED before CANCELLED when sorted by status. The SQL in `RequestDao` is the source of truth — `RequestRepositoryTest` only sorts in-memory lists and asserts a different status order. The only hard delete is swipe-left in History.
- **Room schema:** one table `requests` (`data/entity/Request.kt`), DB version 2, `exportSchema = false`, hand-written `MIGRATION_1_2` in `data/db/AppDatabase.kt`. Any entity change needs a version bump and a manual `Migration` (no exported schemas, so no auto-migrations). Enums are stored by `name`: renaming an `EquipmentType`/`ActionType`/`RequestStatus` constant breaks existing rows, and `Converters` falls back to `OTHER`/`ACTIVE` with only a log line.
- **Date/time conventions — keep all of them in sync:** `Converters` stores `LocalDateTime` as epoch millis via `ZoneOffset.UTC` (wall-clock encoding, not a real instant); `NotificationSchedulerImpl` converts with `ZoneId.systemDefault()` to get alarm times; the M3 `DatePicker` yields UTC-midnight millis, which the screens turn into a `LocalDate` with `ZoneOffset.UTC` and combine with the `TimePicker` hour/minute. Changing one side shifts stored dates or alarm times.
- **Navigation** (`ui/screens/MainScreen.kt`): a bottom `NavigationBar` with exactly three tabs — Создание / Список / История; the PRD forbids more sections. Start destination is the Active list. Routes carry optional query args, `create?requestId={id}` (edit mode when ≠ -1) and `active?deepLinkRequestId={id}`, so tab selection compares `route.substringBefore('?')`.
- **Create and edit share `CreateRequestScreen`.** The Active bottom sheet ("Изменить") and a tap on a History card both navigate to `create?requestId=<id>`; `CreateRequestViewModel.loadRequestForEditing` prefills the form and, for closed requests, adds the result fields (price, final comment, cancel reason). After saving, edit → `popBackStack()`, create → navigate to Active. One-shot events use `Channel` + `receiveAsFlow()` collected under `repeatOnLifecycle(STARTED)`.
- **Swipes** (`ActiveRequestsScreen.kt`): right = complete (opens `CompleteRequestDialog` for price/comment), left = cancel (reason dialog). `confirmValueChange` always returns `false`, so the card snaps back and the dialog performs the action; confirming shows a Snackbar whose action calls `restoreRequest`.
- **Reminders** (`notifications/`): exact `AlarmManager` alarms, inexact fallback when `SCHEDULE_EXACT_ALARM` is not granted; the `PendingIntent` requestCode is the request id. `NotificationReceiver` posts only if the request is still ACTIVE; `BootReceiver` reschedules every active request after reboot. A notification tap starts `MainActivity` with `requestId` plus a one-time `secure_token` from `SecureTokenManager`; a valid token becomes `deepLinkRequestId`, and the Active screen opens that request's bottom sheet. Tokens live only in memory, so a tap after the process has died opens the app without the card.
- **Theme:** `AppTheme` (LIGHT/DARK/SYSTEM) persists in SharedPreferences (`ThemePreferences` → `ThemeRepositoryImpl` → `ThemeViewModel`, created in `MainActivity`, which also re-applies the edge-to-edge bar styles). Palette lives in `ui/theme/Color.kt`; the extra `ColorScheme.urgentOrange` detects dark mode by comparing `error` with `DarkAppError`.
- Urgency colouring (overdue / today / future) is computed separately in `ui/components/RequestCard.kt` and `ui/components/RequestDetailsBottomSheet.kt` — change both.

## Constraints and gotchas

- **Offline only, by design.** `OfflineManifestPolicyTest` fails if the source or merged manifest requests `INTERNET`, network/Wi-Fi state, `CALL_PHONE` or `READ_CONTACTS`. That is why calling uses `ACTION_DIAL` and contact import uses the system picker (`ACTION_PICK` on `Phone.CONTENT_URI`).
- **Two string files, both Russian.** `res/values/strings.xml` and `res/values-ru/strings.xml` share the same keys, and Russian-locale devices (the real user) read `values-ru`. Make every string change in both. Enum display names come from `ui/extensions/EnumExtensions.kt`.
- `FakeRequestRepository` in `CreateRequestViewModelTest.kt` implements the whole `RequestRepository` interface: add every new repository method there too, or the unit-test source set won't compile.
- The code intentionally diverges from `PRD.md` in places: phone is optional, `nextActionDateTime` is nullable in the DB (the form still requires it), `EquipmentType` has 6 values instead of the PRD's 8, and History supports hard delete. Treat the code and recent commits as current behaviour; ask before "fixing" it toward the PRD.
- Release builds run R8 with resource shrinking; Room classes are kept by `proguard-rules.pro` and the domain enums are `@Keep`.

## Repo workflow and automation

- `main` is the default branch and where all current work lands, through short-lived branches (`fix-*`) and PRs. Recent commit messages are mostly English Conventional Commits (`fix:`, `feat:`, `style:`). The `dev` and `fix` branches are stale leftovers of the old scripted flow.
- `TASKS.md` is parsed by the scripts below — keep the `## Этап N. …` headings and the `- [ ] N.M. …` line format.
- `scripts/` is legacy Gemini (`google-antigravity`) automation: `orchestrator.py` runs a developer → reviewer → debugger agent loop over `TASKS.md`, auto-commits and pushes to `fix`, and opens PRs into `dev` via `create_pr.py`; `pr_reviewer.py` LLM-reviews the diff, writes `scripts/latest_review.json`, `scripts/data.js` and `evaluations.json`, and messages Telegram; `dashboard.html` is a static viewer for `scripts/data.js`. They read `GEMINI_API_KEY` (plus optional `TELEGRAM_*`, `GITHUB_TOKEN`) from the git-ignored `.env`, call paid APIs and push to the remote — don't run them unless asked.
- CI (`.github/workflows/pr_review.yml`, on PRs to `main`/`dev`) runs only `scripts/pr_reviewer.py`. Nothing in CI builds the app or runs tests, and on any API failure the reviewer falls back to the cached `latest_review.json` or a hard-coded 100/100 review, so a green check says nothing about correctness — verify with Gradle.
