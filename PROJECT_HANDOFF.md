# PROJECT_HANDOFF.md — Qponix (CuponsSMS)

> Single source of truth for the current project status. Updated after every completed fix-group.
> If this file contradicts the actual repository state, **the repository wins** — resolve, document, regenerate.

## Project overview

Android app (Hebrew, RTL, fully offline) for managing digital coupons / gift cards received via SMS and WhatsApp.
Kotlin + Jetpack Compose (Material 3), Clean Architecture + MVVM, Room 2.6.1, Hilt 2.52, DataStore, WorkManager.
Package `com.cupons.sms`, minSdk 26, targetSdk 35, AGP 8.13.2, Gradle wrapper 9.0.0, JDK 17.
Layers: `data/` (Room, SMS parsing, repo impl), `domain/` (models, repo interface, use case), `ui/` (Compose + ViewModels), `di/` (Hilt).

## Current effort

A project-wide expert review + improvement + automated-testing effort is underway, following the approved plan at
`C:\Users\Dorit\.claude\plans\project-wide-expert-review-improvement-drifting-stroustrup.md` (a copy of the audit + phases is summarized below).

### Phases
- **Phase 0 — Baseline** ✅ COMPLETE
- **Phase 1 — Critical fixes 1.1–1.7** ✅ COMPLETE
- **Phase 2 — Wiring/lifecycle 2.1–2.6 + lint errors** ✅ COMPLETE (lint now 0 errors)
- **Phase 3 — UI/ViewModel fixes 3.1–3.6** ✅ COMPLETE
- **Phase 4 — Version catalog + full JVM test suite (111 tests)** ✅ COMPLETE
- **Phase 5 — Full verification (clean)** ✅ COMPLETE (clean run: 111/111 tests, 0 lint errors, APK builds)
- **Phase 6 — Docs + final report** ✅ COMPLETE

## ALL PHASES COMPLETE ✅

### Follow-up (2026-07-21): GoGift gift-card support
User reported gogift.co.il coupons not detected. Root cause: custom keywords only pass the keyword gate
(stage 1), not code extraction (stage 2); `giftcard.gogift.co.il` was an unknown domain so no code pattern
matched. Fix in `SmsParser.kt`: added `gogift`/`gogift.co.il` keywords, a `gogift.co.il/.../CODE` URL pattern
(Pattern 8d, requires a digit in the code so path words like "redeem" aren't captured), and a certain-domain
confidence boost (`CERTAIN_COUPON_DOMAINS`). 4 new tests → **115 total, all green**; `assembleDebug` SUCCESS.
NOTE: the exact gogift URL structure was assumed (code = last path segment); awaiting a real example from the
user to confirm/adjust. Also flagged the rejectedIds caveat (rejected pending coupons are skipped forever).

Final state (clean run 2026-07-21): `assembleDebug` SUCCESS · `testDebugUnitTest` 115/115 · lint 0 errors, 47 warnings.
Docs updated: TECH_SPEC v1.3, README, CLAUDE.md, SETUP_GUIDE; created TESTING.md, CHANGELOG.md, LICENSE.
Nothing is left in a broken state. Remaining optional work (not started, low priority): CI workflow,
release signing config, the deferred cosmetic items (Color.kt names, i18n, nav-arg redesign) — all documented
in TECH_SPEC §10.
- **Phase 3 — UI/ViewModel fixes 3.1–3.6**
- **Phase 4 — Version catalog + full JVM test suite (~110–140 tests, Robolectric/MockK/Turbine)**
- **Phase 5 — Full verification (test/lint/build)**
- **Phase 6 — Docs (TECH_SPEC, README, TESTING.md, CHANGELOG.md) + final report**

**Gating rule:** a phase is complete only when `testDebugUnitTest` is fully green, build passes, and no new lint errors vs baseline. Never skip/disable failing tests.

## Completed so far

### Phase 4 — Version catalog + test suite (2026-07-21) ✅
`assembleDebug` SUCCESS; `testDebugUnitTest` **111/111 pass** across 16 suites; **lint 0 errors, 47 warnings** (down from 72 — catalog migration cleared UseTomlInstead).
- **4.1** `gradle/libs.versions.toml` `[libraries]` fully populated; `app/build.gradle.kts` migrated to `libs.*` refs. Added test deps: Robolectric 4.14.1, MockK 1.13.13, Turbine (now used), room-testing, work-testing, androidx.test core-ktx/junit(-ktx). `testOptions.unitTests.isIncludeAndroidResources = true`. `src/test/resources/robolectric.properties` pins `sdk=34`, `application=android.app.Application` (avoids booting Hilt app in tests).
- **4.2** New JVM test suites (all `src/test/`): SmsParserTest (32, extended), DateUtilsTest (7), CouponTest (12), CouponDao... covered via CouponRepositoryImplTest (11, Robolectric+Room: dedup matrix, updateBalance txn, markAsUsed idempotency, mapping, soft-delete), UsageLogDaoTest (5, aggregates + FK CASCADE), MigrationTest (4, hand-rolled v1 DDL → 1→2→3, data survival), AppPreferencesTest (8, atomic + comma sanitize + rejected ids), BackupRoundTripTest (7, v2 round-trip + usage-log remap + v1 compat + malformed + skip), ImportFromSmsUseCaseTest (3, auto/pending split + idempotency), HomeViewModelTest (4, tab split + dedup + multiselect), DetailViewModelTest (3, loaded/notFound), AddCouponViewModelTest (5, validation + EOD expiry + save), SettingsViewModelTest (2, flag independence), KeywordsViewModelTest (3), StatisticsViewModelTest (2, aggregation + zero-safety), ExpiryNotificationWorkerTest (3, prefs gating + stable id). Shared `testutil/MainDispatcherRule`.
- Instrumented `androidTest/CouponDaoTest` left untouched (device-only).

### Phase 3 — UI/ViewModel fixes (2026-07-21) ✅
`assembleDebug` SUCCESS; `testDebugUnitTest` 32/32; lint 0 errors.
- **3.1** `DetailUiState.notFound` flag; `DetailScreen` shows "הקופון לא נמצא (ייתכן שנמחק)" instead of eternal spinner.
- **3.2** `remember(coupon)` key on UpdateBalanceDialog balance/expiry initial values.
- **3.3** `BackupUiState.isRescanning` separated from `isBackingUp`; rescan button in SettingsScreen binds to `isRescanning`.
- **3.4** `StatisticsScreen` topMerchants `maxCount.coerceAtLeast(1f)` (no NaN); typo "מש/מים"→"ממשו".
- **3.5** `StatisticsViewModel` — 6 DAO calls run in parallel via `async`/`awaitAll`; `loadJob` cancelled before each refresh.
- **3.6** `HomeViewModel.observeDeleted` now `distinctBy` code (consistent); doc comments corrected to 5 tabs; `CouponDao` header updated.

### Phase 2 — Wiring/lifecycle + lint (2026-07-21) ✅
`assembleDebug` SUCCESS; `testDebugUnitTest` 32/32; **lint 0 errors** (was 5), 72 warnings.
- **2.1** customBlacklist threaded through `SmsReader.readAll` + `ImportFromSmsUseCase` (parser param from 1.1).
- **2.2** `WhatsAppAccessibilityService`: `onDestroy` cancels scope; passes `"WhatsApp"` as sender (readable merchant fallback, not package id); reads customKeywords/blacklist; stable notif id (`200000 + id%100000`).
- **2.3** New `util/DateUtils.kt` — `parseDateToEndOfDayMillis` (23:59:59.999 strict), `millisToDateString`, `formatDateInput`. `AddCouponViewModel` + `DateInputField` now both use it (fixes midnight-vs-EOD split). DetailScreen keeps its import path via thin delegating wrappers.
- **2.4** `AppPreferences` atomic `addCustomKeyword/removeCustomKeyword/addBlacklistWord/removeBlacklistWord` (single `edit{}`, comma+blank sanitize); `KeywordsViewModel` switched. Also added `onSavedHandled()` reset in AddCouponViewModel + AddCouponScreen.
- **2.5** `CuponsApplication` periodic worker `REPLACE`→`KEEP`.
- **2.6** `ExpiryNotificationWorker` stable notif id `100000 + coupon.id%100000` (was `1000+index`).
- **Lint errors fixed:** `NotificationHelper.safeNotify()` (POST_NOTIFICATIONS check + SecurityException catch); manifest `tools:node="remove"` on WorkManagerInitializer; `<uses-feature telephony required=false>`.

### Phase 1 — Critical fixes (2026-07-21) ✅
All 7 fixes implemented; `assembleDebug` SUCCESS; `testDebugUnitTest` **32/32 pass** (23 original + 9 new parser tests).
- **1.1** `SmsReceiver.kt` — `goAsync()` + `pendingResult.finish()` in finally; parse moved into coroutine; injected `AppPreferences`, passes customKeywords + customBlacklist; stable notif id (`NEW_COUPON_NOTIF_BASE=200000 + id%100000`).
- **1.2** `CouponDao.countByCodeAndSender()` + `CouponRepositoryImpl.insertCoupon` returns -1L for content-duplicate when `smsId != null` (manual adds exempt).
- **1.3** `CouponRepositoryImpl` injects `AppDatabase`, `updateBalance` wrapped in `db.withTransaction {}`.
- **1.4** `markAsUsed` now writes a usage_log row (amountUsed = remaining balance, balanceAfter = 0) inside a transaction; idempotent (balance 0 → no row).
- **1.5** `SmsParser.parseDate` — 2-part dates roll to next year when result < receivedAt − 24h grace; strict day 1..31 / month 1..12 validation; MILLISECOND=999 (deterministic).
- **1.6** `SmsParser.parse` — `confidence = confidence.coerceIn(0f,1f)`.
- **1.7** Backup v2: `ExportManager` split into pure `buildBackupJson(coupons, usageLogs)` + `writeToDownloads` (MediaStore API 29+, legacy + `WRITE_EXTERNAL_STORAGE maxSdkVersion=28` for 26–28); `BACKUP_VERSION=2` adds usageLogs; `exportToJson` now returns `String?` (filename). `ImportManager` accepts v1 AND v2, splits into `importFromJson(content)` (testable), remaps usage-log couponIds via oldId→newId map. `UsageLogDao.getAll()` added. SettingsViewModel updated to `$fileName`.
- New parser tests: year rollover ×4, invalid-date, confidence clamp, custom keyword, custom blacklist, extractExpiryOnly rollover.
- **DEFERRED to Phase 4 (need Robolectric):** repository dedup/transaction/markAsUsed tests, backup round-trip test, migration test. Code is written; JVM Room/JSON tests require Robolectric which is added in Phase 4.

### Phase 0 — Baseline (2026-07-21)
- `gradlew assembleDebug` → **BUILD SUCCESSFUL** (AGP 8.13.2 + Gradle 9.0.0 pairing works; no wrapper change needed).
- `gradlew testDebugUnitTest` → **23/23 pass** (SmsParserTest only test suite; note: audit docs said 25, actual count is 23).
- `gradlew lint` → **FAILS with 5 errors, 72 warnings** (pre-existing — this is the baseline):
  1. `NotificationHelper.kt:73` MissingPermission (notify without POST_NOTIFICATIONS check)
  2. `NotificationHelper.kt:98` MissingPermission
  3. `AndroidManifest.xml:68` RemoveWorkManagerInitializer (on-demand init + default initializer conflict)
  4. `AndroidManifest.xml:7` PermissionImpliesUnsupportedChromeOsHardware (RECEIVE_SMS, missing telephony uses-feature)
  5. `AndroidManifest.xml:9` same (READ_SMS)
  These 5 errors will be fixed in Phase 2 (they match audit findings). Lint task exits 1 until then — acceptance for later phases is "no NEW errors"; final target is 0 errors.
- Deleted `java_pid13868.hprof` (746 MB gitignored heap dump).
- `.gitignore`: now ignores `.claude/` (keeps `.claude/settings.json`).
- Created this file + `NEXT_SESSION_PROMPT.md`.

## Audit findings (to be fixed)

**CRITICAL:** C1 SmsReceiver no goAsync (drops coupons); C2 smsId scheme mismatch → duplicate coupons; C3 updateBalance not transactional; C4 parseDate year-naive (Dec→Jan bug) + lenient dates; C5 backup broken on Android 10+ (no MediaStore) + omits usage_log; C6 markAsUsed writes no usage_log.
**HIGH:** H1 customBlacklist never consumed by parser (dead setting); H2 customKeywords not passed in SmsReceiver/WhatsApp paths; H3 expiry midnight-vs-23:59:59 inconsistency; H4 KeywordsViewModel lost-update race; H5 Detail permanent spinner on missing coupon; H6 confidence unclamped (>1.0); H7 WhatsApp service scope leak + package-name sender; H8 periodic worker REPLACE-on-every-launch.
**MEDIUM:** M1 stats NaN divide + Hebrew typo; M2 shared isBackingUp flag; M3 sequential stats loads; M4 unstable notification IDs; M5 remember-without-key dialog; M6 CSV comma corruption in prefs; M7 inconsistent distinctBy; M8 empty version-catalog [libraries]; M9 doc drift (DB v2 vs v3 etc.); M10 (done in Phase 0).
**DEFERRED (do not fix, document only):** Color.kt misleading names; full i18n string extraction; couponIds nav-arg redesign; AddCouponViewModel StateFlow refactor; release signing.

## Key technical decisions

- **Dedup (C2):** keep synthetic smsId in receiver; add content-based dedup `countByCodeAndSender` in `insertCoupon` for SMS-sourced coupons (`smsId != null`); manual adds exempt. Rationale: querying SMS provider from onReceive is racy for a non-default SMS app.
- **Transactions (C3/C6):** inject `AppDatabase` into `CouponRepositoryImpl`, use `db.withTransaction {}` (room-ktx).
- **Backup (C5):** BACKUP_VERSION 2 — MediaStore.Downloads for API 29+, legacy path + WRITE_EXTERNAL_STORAGE (maxSdk 28) for API 26–28; adds usageLogs array; importer accepts v1 AND v2 with oldId→newId remap.
- **Tests:** JVM-only (no emulator): Robolectric 4.14.1, MockK 1.13.13, Turbine (already declared), work-testing. Instrumented CouponDaoTest left untouched.
- **Test count baseline:** 23 (not 25 as older docs claim).

## Known issues / blockers
- None blocking. Lint exits 1 (5 pre-existing errors) until Phase 2.

## Exact next step

All planned phases are complete. If continuing, the remaining **optional** items (in priority order) are:
1. Add `.github/workflows/android-ci.yml` running `testDebugUnitTest` + `lint` + `assembleDebug`.
2. Add a release signing config (needs a keystore — user action).
3. Optionally implement the automatic-backup Worker that consumes `backupFrequency`/`driveBackupEnabled`
   (currently inert prefs).
4. Deferred cosmetic items in TECH_SPEC §10 (Color.kt names, i18n) — only if the team decides to.

Verify current state first with `.\gradlew.bat testDebugUnitTest lint assembleDebug` before any new work.
