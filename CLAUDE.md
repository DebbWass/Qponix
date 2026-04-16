# CLAUDE.md — Qponix (CuponsSMS)

## Project Overview
Android app for managing digital coupons and gift cards received via SMS.
**App name:** Qponix | **Package:** `com.cupons.sms` | **Min SDK:** 26 (Android 8.0)

## Architecture
Clean Architecture + MVVM:
- **Data layer:** `data/` — Room DB, SMS parsing, repository impl
- **Domain layer:** `domain/` — models, repository interface, use cases
- **UI layer:** `ui/` — Jetpack Compose screens + ViewModels
- **DI:** Hilt (`di/AppModule.kt`)

## Key Tech Stack
- Kotlin + Jetpack Compose (Material 3)
- Room 2.6.1 (SQLite, KSP)
- Hilt 2.52 (DI, with AssistedInject for per-page ViewModels)
- Navigation Compose 2.8.3
- Coroutines + StateFlow
- DataStore (settings/preferences)
- WorkManager (background tasks)
- Hebrew RTL support throughout

## Screen Map
| Route | Screen | ViewModel |
|-------|---------|-----------|
| `splash` | SplashScreen | — |
| `home` | HomeScreen (5 tabs) | HomeViewModel |
| `detail/{id}?couponIds={ids}` | DetailScreen (HorizontalPager) | DetailViewModel (per page, AssistedInject) |
| `add` | AddCouponScreen | AddCouponViewModel |
| `settings` | SettingsScreen | SettingsViewModel |
| `settings/keywords` | KeywordsSettingsScreen | KeywordsViewModel |
| `statistics` | StatisticsScreen | StatisticsViewModel |

## Important Patterns

### Swipe Navigation (Detail Screen)
`DetailScreen` uses `HorizontalPager` — each page gets its own `DetailViewModel` via
`@HiltViewModel(assistedFactory = ...)` + `@AssistedInject`. The `couponIds` list is
passed as a comma-separated query parameter in the nav route and parsed in `AppNavigation`.

### HomeScreen → Detail Navigation
```kotlin
onCouponClick = { couponId: Long, couponIds: List<Long> -> ... }
```
`HomeScreen` passes **both** the clicked coupon ID and the full current-tab ID list.

### Pending Coupons
Low-confidence SMS parses (< 0.6) are stored with `is_pending = 1` and shown at the top
of the Active tab for user confirmation/rejection.

### SMS Parser
6-stage pipeline in `SmsParser.kt`:
0. Blacklist check → 1. Keyword detection → 2. Code extraction →
3. Confidence scoring → 4. Amount → 5. Expiry/URL/Merchant

## Database
Two tables: `coupons` (21 columns) and `usage_log` (5 columns).
Soft delete — coupons are never physically deleted; `is_deleted = 1` moves them to "Deleted" tab.

## Language & Locale
All UI text is in **Hebrew**. RTL layout is enabled in `AndroidManifest.xml`.
Date format: `dd/MM/yyyy`. Currency default: `₪`.

## Coding Conventions
- All UI in Jetpack Compose (no XML layouts)
- Theme colors defined in `ui/theme/Color.kt` — use named constants (e.g., `PrimaryGreen`, `BgDeep`, `CardBackground`)
- ViewModels expose `StateFlow<UiState>` — collected with `collectAsStateWithLifecycle()`
- Repository is injected via interface (`CouponRepository`), implemented in `CouponRepositoryImpl`
- Use `viewModelScope.launch` for all coroutines in ViewModels

## Do NOT
- Add network calls — this is fully offline/local-only
- Use XML layouts — Compose only
- Use `LiveData` — StateFlow only
- Commit `local.properties` or keystore files
