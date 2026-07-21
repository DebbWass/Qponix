# Changelog — Qponix (CuponsSMS)

All notable changes are documented here. Dates are absolute.

## [Unreleased]

### Added
- **GoGift gift-card support** — the parser now recognizes `gogift.co.il` links: `gogift` added as a
  keyword, a URL code-extraction pattern for `giftcard.gogift.co.il/.../CODE` (the code must contain a
  digit, so path words like `redeem` aren't mistaken for a code), and a confidence boost so these
  auto-import. Covered by 4 new tests (total 115).

## [1.3] — 2026-07-21 — Hardening, fixes & test suite

### Fixed — correctness & data integrity
- **SmsReceiver** now uses `goAsync()` + `pendingResult.finish()`, so the DB insert can no longer be
  killed with the process (previously coupons could be silently dropped).
- **Duplicate coupons** across real-time receipt vs inbox scan eliminated via content-based dedup in
  `insertCoupon` (`countByCodeAndSender`), on top of the existing unique `sms_id` index.
- **`updateBalance`** and **`markAsUsed`** now run inside `db.withTransaction {}`; `markAsUsed` also
  writes a `usage_log` row for the remaining balance (statistics no longer under-count), and is idempotent.
- **`SmsParser.parseDate`** rolls a year-less `DD.MM` date to the next year when it would otherwise land
  in the past (December→January case), validates day/month, and uses deterministic milliseconds.
- **Confidence** is clamped to `[0,1]` (matana4u path could previously reach 1.20).

### Fixed — features & consistency
- **Custom blacklist** is now actually consumed by the parser (was a dead setting); custom keywords and
  blacklist are threaded through all ingestion paths (inbox scan, real-time SMS, WhatsApp).
- **Expiry semantics** unified to end-of-day (23:59:59.999) via new `util/DateUtils.kt`; manually added
  coupons no longer expire ~24h early.
- **Keyword/blacklist edits** are atomic (single DataStore `edit {}`), fixing a lost-update race; commas
  are sanitized to protect the CSV encoding.
- **WhatsApp service** cancels its coroutine scope in `onDestroy` (leak fix) and no longer pollutes the
  merchant name with the package id.
- **Detail screen** shows a "coupon not found" state instead of an infinite spinner when a coupon is missing.
- **Statistics** guard against divide-by-zero in the merchant bar; load queries run in parallel and cancel
  a prior in-flight refresh. Empty-state typo corrected.
- **Settings** rescan and backup use independent busy flags (no longer disable each other).
- **Expiry worker** uses a stable per-coupon notification id and is scheduled with `KEEP` (was `REPLACE`,
  which reset the 24h timer on every launch).

### Changed — backup format
- **Backup v2**: writes via `MediaStore.Downloads` on API 29+ (works on Android 10+), legacy path with
  `WRITE_EXTERNAL_STORAGE (maxSdkVersion=28)` on API 26–28, and now includes the `usage_log`. The importer
  accepts both v1 and v2 and remaps usage-log coupon ids.

### Build & tests
- Migrated all dependencies to the Gradle **version catalog** (`libs.versions.toml`).
- Added **111 JVM tests** (Robolectric, MockK, Turbine) covering parser, DAOs, repository, migrations,
  backup round-trip, preferences, use case, all ViewModels, and the expiry worker.
- Lint errors reduced from 5 to **0** (POST_NOTIFICATIONS guard, WorkManager initializer removal,
  telephony `uses-feature`).

## [1.2] — 2026-04-16
- Swipe navigation between coupons (HorizontalPager + AssistedInject per-page ViewModels).

## [1.1] — 2026-04-09
- Settings, expiry notifications, custom keywords, pending coupons, statistics, WhatsApp service.

## [1.0] — 2026-04-09
- Initial release: SMS parsing, Room storage, coupon list/detail, manual add, Hebrew RTL.
