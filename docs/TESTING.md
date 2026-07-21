# Testing — Qponix (CuponsSMS)

## סקירה

הפרויקט מכוסה ב-**111 בדיקות JVM** (ללא צורך במכשיר/אמולטור) ובנוסף חבילת בדיקות מכשירניות אחת.
כל בדיקות ה-JVM רצות דרך Robolectric / JUnit4 / MockK / Turbine.

## הרצה

```bash
# כל בדיקות ה-JVM (מהיר, ללא מכשיר)
./gradlew testDebugUnitTest

# בדיקות מכשירניות (דורש מכשיר/אמולטור מחובר)
./gradlew connectedDebugAndroidTest

# ניתוח סטטי + בנייה
./gradlew lint
./gradlew assembleDebug
```

> **הערה על Robolectric:** בהרצה הראשונה מורדים קובצי `android-all` (דורש רשת פעם אחת).  
> ה-SDK מקובע ל-34 ב-`app/src/test/resources/robolectric.properties`, וה-Application מוגדר ל-`android.app.Application` כדי לא לאתחל את אפליקציית ה-Hilt בבדיקות.

## חבילות בדיקה (JVM — `app/src/test/`)

| חבילה | סוג | מכסה |
|-------|-----|------|
| `data/sms/SmsParserTest` (32) | JUnit | זיהוי קוד, blacklist, סכומים, confidence, גלגול שנה, מילות מפתח/רשימה שחורה מותאמות, הצמדת confidence, `extractExpiryOnly` |
| `util/DateUtilsTest` (7) | JUnit | פרסור לסוף היום, שני פורמטים, קלט לא תקין, פרמוט |
| `domain/model/CouponTest` (12) | JUnit | `isExpired` / `displayBalance` / `hasAmount` / `formatAmount` |
| `data/repository/CouponRepositoryImplTest` (11) | Robolectric + Room | dedup תוכן, טרנזקציית `updateBalance`, `markAsUsed` + אידמפוטנטיות, מיפוי, מחיקה רכה |
| `data/db/UsageLogDaoTest` (5) | Robolectric + Room | אגרגציות, FK CASCADE |
| `data/db/MigrationTest` (4) | Robolectric | סכימת v1 ידנית → מיגרציה 1→2→3 + שרידות נתונים |
| `data/prefs/AppPreferencesTest` (8) | Robolectric + DataStore | הוספה/הסרה אטומית, ניקוי פסיקים, IDs שנדחו, ברירות מחדל |
| `util/BackupRoundTripTest` (7) | Robolectric + Room | round-trip v2 + מיפוי יומן, תאימות v1, JSON פגום, דילוג כפילויות |
| `domain/usecase/ImportFromSmsUseCaseTest` (3) | MockK + Room | פיצול auto/pending, אידמפוטנטיות, שמירת pending |
| `ui/screens/home/HomeViewModelTest` (4) | MockK + Turbine | פיצול active/expired, dedup, מצב בחירה מרובה |
| `ui/screens/detail/DetailViewModelTest` (3) | MockK | טעינה / `notFound` / מחיקה |
| `ui/screens/add/AddCouponViewModelTest` (5) | MockK | ולידציה, תפוגה סוף-יום, שמירה, איפוס `isSaved` |
| `ui/screens/settings/SettingsViewModelTest` (2) | MockK | הפרדת `isBackingUp` / `isRescanning` |
| `ui/screens/settings/KeywordsViewModelTest` (3) | MockK + Turbine | שיקוף מצב, האצלה לעדכונים אטומיים |
| `ui/screens/statistics/StatisticsViewModelTest` (2) | MockK | אגרגציה, בטיחות אפס-נתונים |
| `data/workers/ExpiryNotificationWorkerTest` (3) | Robolectric + work-testing | gating של העדפות, ID התראה יציב |

עזרים: `testutil/MainDispatcherRule` (מחליף `Dispatchers.Main` ל-`StandardTestDispatcher`).

## בדיקות מכשירניות (`app/src/androidTest/`)

| חבילה | מכסה |
|-------|------|
| `data/db/CouponDaoTest` | CRUD + Flow queries על in-memory Room (במכשיר) |

## הנחיות כתיבה

- כל בדיקה דטרמיניסטית וניתנת להרצה חוזרת; מסד ה-Room הוא in-memory ונסגר ב-`@After`.
- בדיקות DataStore מאפסות מצב ב-`@Before` (Robolectric משתף קובץ בין בדיקות באותה מחלקה).
- אין mock-ים מיותרים — בדיקות ה-DB/Repository משתמשות ב-Room אמיתי; MockK רק לגבולות (SmsReader, prefs, DAOs ב-ViewModels).
