# NEXT_SESSION_PROMPT.md — paste this into a fresh Claude Code session

The project-wide review + fixes + test-suite effort on the Qponix Android app (`D:\AI_Agents\CuponsSMS`)
is **COMPLETE** (all 6 phases). Do not repeat completed work.

## Before doing anything

1. Read `PROJECT_HANDOFF.md` (repo root) — full status and per-fix history.
2. Run `git status` and `git log --oneline -10` to see the current state.
3. Verify the build/tests still pass: `.\gradlew.bat testDebugUnitTest lint assembleDebug`.
   Expected: `testDebugUnitTest` **111/111**, lint **0 errors** (47 warnings), `assembleDebug` SUCCESS.
   If they don't match, the repo wins — investigate, document in PROJECT_HANDOFF.md, then continue.

## What was done (summary)

Six phases: baseline → critical data-integrity fixes (goAsync, content dedup, transactions,
markAsUsed usage_log, parseDate year-rollover, confidence clamp, backup v2) → wiring/lifecycle
(custom blacklist, WhatsApp scope, DateUtils end-of-day, atomic prefs, worker KEEP, stable notif ids)
→ UI/ViewModel fixes → version-catalog migration + 111 JVM tests (Robolectric/MockK/Turbine) →
clean verification → docs (TECH_SPEC v1.3, README, CLAUDE.md, TESTING.md, CHANGELOG.md, LICENSE).

## Exact next action (all optional — the mandated work is done)

Pick from, in priority order:
1. Add `.github/workflows/android-ci.yml` (testDebugUnitTest + lint + assembleDebug on push).
2. Add release signing config (requires a keystore — user must supply).
3. Implement the automatic-backup Worker consuming `backupFrequency`/`driveBackupEnabled` (currently inert).
4. Deferred cosmetic items — TECH_SPEC §10 (Color.kt names, i18n string extraction). Only if the team wants them.

## Rules (still apply if you continue)

- Implement → add regression tests → `testDebugUnitTest` green → `assembleDebug` → lint 0 errors →
  update `PROJECT_HANDOFF.md` + this file → then proceed. Never skip/disable failing tests.
- Robolectric first run needs network once (android-all jars). SDK pinned to 34 in
  `app/src/test/resources/robolectric.properties`.
