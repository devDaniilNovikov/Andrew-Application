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
- **Navigation** (`ui/screens/MainScreen.kt`, `ui/navigation/`): a bottom `NavigationBar` with exactly three tabs — Создание / Список / История (`Screen.Create`/`Active`/`History`); the PRD forbids more sections. Start destination is the Active list. Editing is a separate route, `EditRequestRoute.PATTERN` = `edit/{requestId}`, pushed on top of the tab it was opened from (that tab stays highlighted). Always switch tabs with `NavController.navigateToTab()` — it drops an unfinished edit without saving and keeps per-tab state.
- **Create and edit share `CreateRequestScreen`** (edit mode when `requestId != -1`). The Active bottom sheet ("Изменить") and a tap on a History card both call `navigate(EditRequestRoute.forRequest(id))`; `CreateRequestViewModel.loadRequestForEditing` prefills the form and, for closed requests, adds the result fields (price, final comment, cancel reason). The create tab is never cleared on entry, so a half-typed draft survives tab switches. After saving, edit → `popBackStack()`, create → `navigateToTab(Active)`; in edit mode the "Очистить" button becomes "Отмена". One-shot events use `Channel` + `receiveAsFlow()` collected under `repeatOnLifecycle(STARTED)`.
- **Swipes** (`ActiveRequestsScreen.kt`): right = complete (opens `CompleteRequestDialog` for price/comment), left = cancel (reason dialog). `confirmValueChange` always returns `false`, so the card snaps back and the dialog performs the action; confirming shows a Snackbar whose action calls `restoreRequest`.
- **Reminders** (`notifications/`): `NotificationSchedulerImpl` sets exact `AlarmManager` alarms when `ExactAlarmPermission.isGranted()` (always on Android 13+ via `USE_EXACT_ALARM`; `SCHEDULE_EXACT_ALARM` only covers Android 12–12L, `maxSdkVersion 32`), otherwise inexact ones; the `PendingIntent` requestCode is the request id. `reminderTriggerAtMillis()` returns null for a missing or past time, and then the pending alarm is cancelled rather than left to fire at the old time. `ReminderRescheduler.rescheduleAll()` idempotently re-syncs alarms with the active requests in the DB on every `MainActivity.onStart`, from `BootReceiver` (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) and from `ExactAlarmPermissionReceiver`. `NotificationReceiver` posts only if the request is still ACTIVE. `ReminderStatusBanner` on the Active screen warns when notifications or exact alarms are unavailable.
- **Notification tap → card:** the notification starts `MainActivity` with `NotificationHelper.EXTRA_REQUEST_ID` and `FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP`, so a running app gets it in `onNewIntent` instead of being recreated (which would wipe an unsaved form). The id is a one-shot `deepLinkRequestId`: `MainScreen` switches to the Active tab, and `ActiveRequestsScreen` opens the bottom sheet once the list has loaded, then calls `onDeepLinkConsumed()` so the card doesn't reopen on every list change.
- **Theme:** `AppTheme` (LIGHT/DARK/SYSTEM) persists in SharedPreferences (`ThemePreferences` → `ThemeRepositoryImpl` → `ThemeViewModel`, created in `MainActivity`, which also re-applies the edge-to-edge bar styles). Palette lives in `ui/theme/Color.kt`; the extra `ColorScheme.urgentOrange` detects dark mode by comparing `error` with `DarkAppError`.
- Urgency colouring (overdue / today / future) is computed separately in `ui/components/RequestCard.kt` and `ui/components/RequestDetailsBottomSheet.kt` — change both.

## Constraints and gotchas

- **Offline only, by design.** `OfflineManifestPolicyTest` fails if the source or merged manifest requests `INTERNET`, network/Wi-Fi state, `CALL_PHONE` or `READ_CONTACTS`. That is why calling uses `ACTION_DIAL` and contact import uses the system picker (`ACTION_PICK` on `Phone.CONTENT_URI`).
- `ExactAlarmManifestPolicyTest` pins the exact-alarm permissions and the receivers' intent filters: the `BootReceiver` filter in the manifest must stay in sync with `BootReceiver.RESCHEDULE_ACTIONS`.
- **Two string files, both Russian.** `res/values/strings.xml` and `res/values-ru/strings.xml` must hold identical content — Russian-locale devices (the real user) read `values-ru`, and `StringResourcesParityTest` fails on any drift. Make every string change in both. Enum display names come from `ui/extensions/EnumExtensions.kt`.
- In composables, read strings with `stringResource()`, not `LocalContext.current.getString()`: lint (`LocalContextGetResourceValueCall`) fails the CI build on the latter.
- `FakeRequestRepository` in `CreateRequestViewModelTest.kt` implements the whole `RequestRepository` interface: add every new repository method there too, or the unit-test source set won't compile.
- The code intentionally diverges from `PRD.md` in places: phone is optional (if entered, it must have 7–15 digits — `create_phone_invalid_error`), `nextActionDateTime` is nullable in the DB (the form still requires it), `EquipmentType` has 6 values instead of the PRD's 8, and History supports hard delete. `README.md` (Russian) describes the current behaviour; treat it and the code as the truth and ask before "fixing" anything toward the PRD.
- Release builds run R8 with resource shrinking; Room classes are kept by `proguard-rules.pro` and the domain enums are `@Keep`.

## Repo workflow

- `main` is the default branch and where all current work lands, through short-lived branches and PRs. Recent commit messages are mostly English Conventional Commits (`fix:`, `feat:`, `docs:`).
- CI (`.github/workflows/android-ci.yml`, JDK 21) runs `assembleDebug`, `testDebugUnitTest` and `lintDebug` on every PR and on pushes to `main`; on failure it prints the lint report to the job log and uploads `app/build/reports`. Instrumented tests are not run in CI.
