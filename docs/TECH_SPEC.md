# Qponix (CuponsSMS) — Technical Specification & Architecture
**Version:** 1.3  
**Updated:** 2026-07-21  
**Stack:** Kotlin · Jetpack Compose · Room · Hilt · MVVM · Coroutines/Flow

---

## Changelog

| גרסה | תאריך | שינויים |
|------|-------|---------|
| 1.0 | 2026-04-09 | גרסה ראשונה |
| 1.1 | 2026-04-09 | הוספת: settings, notifications, keywords, pending coupons, statistics, WhatsApp service |
| 1.2 | 2026-04-16 | ניווט Swipe — HorizontalPager + AssistedInject per-page ViewModels |
| 1.3 | 2026-07-21 | סבב תיקונים ובדיקות: goAsync ב-SmsReceiver, dedup תוכן, טרנזקציות, גלגול שנה ב-parseDate, גיבוי v2 (MediaStore + usage_log), wiring של רשימה שחורה מותאמת, איחוד תאריכי תפוגה, KEEP ל-Worker, מיגרציה ל-version catalog, ו-111 בדיקות JVM |

---

## 1. Architecture Overview

האפליקציה בנויה על **Clean Architecture** עם ארכיטקטורת **MVVM**:

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│       (Jetpack Compose Screens + ViewModels)         │
└──────────────────────┬──────────────────────────────┘
                       │ StateFlow / collectAsStateWithLifecycle
┌──────────────────────▼──────────────────────────────┐
│                   Domain Layer                       │
│            (Use Cases + Domain Models)               │
└──────────────────────┬──────────────────────────────┘
                       │ Repository Interface
┌──────────────────────▼──────────────────────────────┐
│                   Data Layer                         │
│   ┌─────────────────┐    ┌──────────────────────┐   │
│   │  Room Database  │    │   SMS ContentProvider │   │
│   │  (local SQLite) │    │   + BroadcastReceiver │   │
│   └─────────────────┘    └──────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## 2. Project Structure (Actual)

```
app/src/main/java/com/cupons/sms/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt              # Room DB instance (v3 — migrations 1→2→3)
│   │   ├── dao/
│   │   │   ├── CouponDao.kt            # CRUD + Flow queries
│   │   │   └── UsageLogDao.kt
│   │   └── entity/
│   │       ├── CouponEntity.kt         # 21-column table
│   │       └── UsageLogEntity.kt
│   ├── prefs/
│   │   └── AppPreferences.kt           # DataStore settings
│   ├── repository/
│   │   └── CouponRepositoryImpl.kt
│   ├── sms/
│   │   ├── SmsParser.kt                # 6-stage parsing pipeline
│   │   ├── SmsReader.kt                # One-time inbox scan
│   │   └── SmsReceiver.kt              # Real-time BroadcastReceiver
│   └── whatsapp/
│       └── WhatsAppAccessibilityService.kt
├── domain/
│   ├── model/
│   │   ├── Coupon.kt                   # Domain model + computed properties
│   │   └── UsageLog.kt
│   ├── repository/
│   │   └── CouponRepository.kt         # Interface
│   └── usecase/
│       └── ImportFromSmsUseCase.kt
├── ui/
│   ├── MainActivity.kt
│   ├── CuponsApplication.kt            # @HiltAndroidApp
│   ├── navigation/
│   │   └── AppNavigation.kt            # NavHost — all routes
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt           # 5-tab coupon list
│   │   │   └── HomeViewModel.kt
│   │   ├── detail/
│   │   │   ├── DetailScreen.kt         # HorizontalPager swipe container
│   │   │   └── DetailViewModel.kt      # @AssistedInject per-page VM
│   │   ├── add/
│   │   │   ├── AddCouponScreen.kt
│   │   │   └── AddCouponViewModel.kt
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt
│   │   │   ├── KeywordsSettingsScreen.kt
│   │   │   └── *ViewModel.kt
│   │   ├── statistics/
│   │   │   ├── StatisticsScreen.kt
│   │   │   └── StatisticsViewModel.kt
│   │   └── splash/
│   │       └── SplashScreen.kt
│   ├── components/
│   │   ├── CouponCard.kt               # Reusable list card
│   │   └── DateInputField.kt
│   └── theme/
│       ├── Color.kt                    # Named color constants
│       └── Theme.kt                    # Material3 theme
├── di/
│   └── AppModule.kt                    # Hilt DI module
└── util/
    ├── ExportManager.kt                # JSON backup
    ├── ImportManager.kt
    └── NotificationHelper.kt
```

---

## 3. Database Schema

### Table: `coupons` (21 columns)

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK AUTOINCREMENT | |
| `sms_id` | TEXT UNIQUE NULLABLE | ID מ-ContentProvider, null אם נוסף ידנית |
| `sender` | TEXT NOT NULL | שם/מספר השולח |
| `raw_sms_body` | TEXT | גוף ה-SMS המלא |
| `coupon_code` | TEXT NOT NULL | קוד הקופון |
| `original_amount` | REAL NULLABLE | סכום מקורי |
| `remaining_balance` | REAL NULLABLE | יתרה נוכחית |
| `currency` | TEXT DEFAULT '₪' | מטבע |
| `merchant_name` | TEXT NULLABLE | שם בית העסק |
| `website_url` | TEXT NULLABLE | קישור לאתר מימוש |
| `received_at` | INTEGER NOT NULL | Unix timestamp ms |
| `expires_at` | INTEGER NULLABLE | Unix timestamp ms |
| `is_used` | INTEGER DEFAULT 0 | Boolean |
| `is_archived` | INTEGER DEFAULT 0 | Boolean |
| `is_deleted` | INTEGER DEFAULT 0 | Soft delete |
| `notes` | TEXT NULLABLE | הערה חופשית |
| `confidence` | REAL DEFAULT 0.5 | Parser confidence 0.0–1.0 (מוצמד לטווח) |
| `is_pending` | INTEGER DEFAULT 0 | ממתין לאישור משתמש |
| `created_at` | INTEGER NOT NULL | Unix timestamp ms |
| `updated_at` | INTEGER NOT NULL | Unix timestamp ms |

> **גרסת DB:** 3. מיגרציה 1→2 הוסיפה `is_deleted`; מיגרציה 2→3 הוסיפה `confidence` (DEFAULT 0.5) ו-`is_pending`. שתי המיגרציות מכוסות בבדיקה (`MigrationTest`).

> **מניעת כפילויות תוכן:** בנוסף לאינדקס הייחודי על `sms_id`, `insertCoupon` דוחה קופון ממקור SMS/WhatsApp (`sms_id != null`) אם כבר קיים קופון לא-מחוק עם אותו `coupon_code` + `sender` (`countByCodeAndSender`). זאת מכיוון שאותה הודעה מקבלת `sms_id` שונה בזמן-אמת (סינתטי) לעומת סריקת ה-Inbox (מזהה ה-Provider). קופונים ידניים (`sms_id == null`) פטורים.

### Table: `usage_log` (6 columns)

| Column | Type | Notes |
|--------|------|-------|
| `id` | INTEGER PK AUTOINCREMENT | |
| `coupon_id` | INTEGER FK → coupons.id | CASCADE DELETE |
| `amount_used` | REAL NOT NULL | סכום שנוצל |
| `balance_after` | REAL NOT NULL | יתרה אחרי השימוש |
| `used_at` | INTEGER NOT NULL | Unix timestamp ms |
| `note` | TEXT NULLABLE | הערה אופציונלית |

---

## 4. SMS Parser Strategy

### 4.1 Parsing Pipeline (6 Stages)

```
Raw SMS Text
     │
     ▼
[0] Blacklist Check — OTP/2FA codes, unsubscribe, spam → REJECT
     │
     ▼
[1] Strong Keyword Detection — Hebrew/English coupon keywords → REQUIRED
     │
     ▼
[2] Coupon Code Extraction — 8 regex patterns (order matters)
     │
     ▼
[3] Confidence Scoring (0.0–1.0, מוצמד ב-coerceIn)
     │  בפועל: כשנמצא קוד, הבסיס הוא 0.50, ולכן ה-confidence לעולם אינו < 0.5.
     │  ≥ 0.6  → auto-import
     │  0.5–0.59 → is_pending=1 (user confirmation)
     │  (הסף CONFIDENCE_SUGGEST=0.4 ותחום ה-"<0.4 discard" אינם מגיעים בפועל —
     │   הקבוע נשמר לתאימות בדיקות אך אינו משפיע על ההתנהגות)
     ▼
[4] Amount Extraction — ₪/$/€ patterns
     │
     ▼
[5] Expiry, URL, Merchant extraction
     │
     ▼
ParsedSmsData object
```

### 4.2 Confidence Score Breakdown

| גורם | תוספת |
|------|-------|
| קוד נמצא (בסיס) | +0.50 |
| סכום נמצא | +0.25 |
| תאריך תפוגה | +0.15 |
| URL נמצא | +0.10 |
| URL של matana4u | +0.20 (boost) |

> הסכום הכולל מוצמד ל-`[0,1]` ב-`coerceIn` (נתיב matana4u עלול להגיע ל-1.20 לפני הצמדה).

> **גלגול שנה בתאריכי DD.MM:** כאשר תאריך התפוגה חסר שנה, מחושבת שנת הקבלה; אם התוצאה יוצאת לפני הקבלה (חלון חסד של 24ש') — מתווספת שנה. כך "עד 5.1" בהודעה מדצמבר יתפרש כ-5 בינואר בשנה העוקבת. תאריכים לא תקינים (יום/חודש מחוץ לטווח) נדחים.

### 4.3 מילות מפתח / רשימה שחורה מותאמות אישית

`SmsParser.parse` מקבל `customKeywords` **וגם** `customBlacklist`. הרשימה השחורה המותאמת נבדקת בשלב 0 יחד עם `PROMO_BLACKLIST`. שני הערכים מוזרמים מ-DataStore בכל הנתיבים: סריקת Inbox (`ImportFromSmsUseCase` → `SmsReader`), קבלה בזמן-אמת (`SmsReceiver`), ו-WhatsApp (`WhatsAppAccessibilityService`).

---

## 4b. Backup / Restore (v2)

- **פורמט:** JSON, `version = 2`. מכיל מערך `coupons` **וגם** `usageLogs` (יומן השימוש).
- **כתיבה:** `ExportManager.buildBackupJson` (טהור, נבדק ביחידה) + `writeToDownloads` — `MediaStore.Downloads` ב-API 29+, ונתיב legacy עם `WRITE_EXTERNAL_STORAGE (maxSdkVersion=28)` ב-API 26–28.
- **שחזור:** `ImportManager` מקבל **v1 וגם v2**. כפילויות מדולגות לפי `sms_id`. ב-v2 רשומות יומן השימוש ממופות מחדש ל-`id` הקופון החדש (`oldId → newRowId`); יומן של קופון שדולג — מושמט.

---

## 5. Swipe Navigation Architecture (v1.2)

### 5.1 Flow

```
HomeScreen (LazyColumn)
    │  onClick(couponId, couponIds: List<Long>)
    ▼
AppNavigation.kt
    │  navigate("detail/{couponId}?couponIds=1,2,3,4,5")
    ▼
DetailScreen.kt
    │  HorizontalPager(pageCount = couponIds.size, initialPage = initialIndex)
    ▼
CouponDetailPage (per page)
    │  hiltViewModel<DetailViewModel, Factory>(key="detail_$couponId") { factory -> factory.create(couponId) }
    ▼
DetailViewModel (@AssistedInject, couponId: Long)
```

### 5.2 Key Implementation Details

- **`DetailViewModel`** uses `@HiltViewModel(assistedFactory = ...)` + `@AssistedInject` (Hilt 2.49+)
- **`hiltViewModel(key, creationCallback)`** creates isolated VM per page, scoped to NavBackStackEntry
- **`beyondViewportPageCount = 1`** preloads adjacent pages for smooth swiping
- Each page's state (dialogs, errors, loading) is fully independent
- Delete/MarkUsed still navigates back to list (consistent UX)
- `couponIds` passed as comma-separated query param (e.g., `?couponIds=3,7,12,5`)

---

## 6. Key Dependencies

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Navigation + Pager
implementation("androidx.navigation:navigation-compose:2.8.3")
// HorizontalPager is in foundation (Compose 1.4+ stable, included via BOM)

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Hilt (with AssistedInject support, 2.49+)
implementation("com.google.dagger:hilt-android:2.52")
ksp("com.google.dagger:hilt-android-compiler:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.1")
```

---

## 7. Navigation Routes

| Route | Arguments | Screen |
|-------|-----------|--------|
| `splash` | — | SplashScreen |
| `home` | — | HomeScreen |
| `add` | — | AddCouponScreen |
| `settings` | — | SettingsScreen |
| `settings/keywords` | — | KeywordsSettingsScreen |
| `statistics` | — | StatisticsScreen |
| `detail/{couponId}?couponIds={ids}` | `couponId: Long`, `couponIds: String` | DetailScreen |

---

## 8. Security Considerations

1. **Local-only storage** — אין שרת, אין סנכרון ענן. כל הנתונים ב-SQLite מקומי.
2. **SMS Permission Justification** — טקסט מובנה ל-Google Play לתיאור השימוש.
3. **No sensitive logging** — קודי קופון לא יכתבו ל-Logcat ב-release build.
4. **ProGuard** — כיסוי מלא ב-release build עם `isMinifyEnabled = true`.
5. **Keystore never committed** — `.jks` ו-`keystore.properties` ב-.gitignore.

---

## 9. Development Phases

### Phase 1 — MVP (v1.0) ✅ Complete
- [x] DB schema + Room setup
- [x] SMS reading (one-time scan + real-time receiver)
- [x] Parser with confidence scoring
- [x] Home screen (list with tabs)
- [x] Detail screen (edit balance, mark used)
- [x] Manual add screen
- [x] Multi-select & delete
- [x] Hebrew localization (RTL)

### Phase 2 — Enhanced (v1.1) ✅ Complete
- [x] Settings screen
- [x] Expiry notifications (WorkManager)
- [x] Custom SMS keywords
- [x] Pending coupons (confidence < 0.6)
- [x] UI animations
- [x] JSON backup/export
- [x] Statistics screen
- [x] Unit tests (SmsParser, CouponDao)
- [x] WhatsApp Accessibility Service

### Phase 3 — UX Polish (v1.2) ✅ Complete
- [x] Swipe navigation between coupons (HorizontalPager)
- [x] Per-page ViewModel isolation (AssistedInject)
- [x] Page position indicator "X מתוך Y"

### Phase 4 — Hardening & Tests (v1.3) ✅ Complete
- [x] תיקוני אמינות: `goAsync` ב-SmsReceiver, טרנזקציות ב-updateBalance/markAsUsed
- [x] מניעת כפילויות תוכן ב-insert
- [x] גלגול שנה + ולידציה ב-parseDate; הצמדת confidence
- [x] גיבוי v2 (MediaStore + usage_log)
- [x] מיגרציה ל-Gradle version catalog
- [x] מערך בדיקות JVM מקיף (111 בדיקות; Robolectric + MockK + Turbine)

### Phase 5 — Future (v1.4+)
- [ ] Home screen widget (balance reminder)
- [ ] CSV export
- [ ] Backup to Google Drive (opt-in) — הפרפרנס `driveBackupEnabled`/`backupFrequency` קיימים אך עדיין ללא Worker
- [ ] Barcode / QR code display

---

## 10. פריטים שנדחו במכוון (Deferred)

הפריטים הבאים זוהו בסקירה אך **לא** שונו, מסיבות מוסברות:

| פריט | סיבה |
|------|------|
| שמות ב-`Color.kt` (למשל `PrimaryGreen` שהוא סגול) | שינוי נוגע בכל המסכים; קוסמטי בלבד, סיכון/רעש גבוה |
| חילוץ מחרוזות עברית ל-`strings.xml` (i18n מלא) | האפליקציה עברית-בלבד בכוונה; רפקטור ענק ללא תועלת מיידית |
| ארגומנט הניווט `couponIds` (רשימה מופרדת בפסיקים) | עובד לגדלים ריאליים; שקילת מעבר ל-SavedStateHandle/VM משותף בעתיד |
| `AddCouponViewModel` על `mutableStateOf` במקום StateFlow | עובד נכון; רק תוקנו ה-util של התאריך ואיפוס `isSaved` |
| תצורת חתימה ל-release (signing config) | אין keystore; להוסיף לפני שחרור ל-Play |
