# Qponix — ניהול קופונים חכם

<p align="center">
  <img src="docs/assets/logo_placeholder.png" width="120" alt="Qponix Logo"/>
</p>

אפליקציית Android לניהול קופונים וגיפטקארדים דיגיטליים שמתקבלים ב-SMS.  
הכל במקום אחד, ללא עלויות, ללא ענן — הכל מקומי ופרטי.

---

## תכונות עיקריות

| תכונה | תיאור |
|--------|--------|
| **סריקת SMS אוטומטית** | זיהוי קופונים חדשים ב-SMS בזמן אמת |
| **ניווט Swipe בין קופונים** | החלקה ימינה/שמאלה במסך הפרטים למעבר מהיר |
| **5 טאבים** | פעיל / עומדים לפוג / פגי תוקף / ארכיון / נמחקו |
| **עדכון יתרה** | מעקב אחרי יתרה חלקית עם היסטוריית שימושים |
| **התראות תפוגה** | הודעה X ימים לפני שהקופון פג |
| **חיפוש ומיון** | לפי תאריך, סכום, שם בית עסק |
| **הוספה ידנית** | הוספת קופון שלא הגיע ב-SMS |
| **ייצוא JSON** | גיבוי ושחזור הקופונים |
| **סטטיסטיקות** | סיכום שימושים וחיסכון |
| **מצב בחירה מרובה** | מחיקה/ניהול של מספר קופונים בבת אחת |

---

## צילומי מסך

> *צילומי מסך יתווספו עם הגרסה הראשונה*

---

## התקנה ופיתוח

### דרישות מקדימות

| כלי | גרסה |
|-----|------|
| Android Studio | Ladybug (2024.2+) |
| JDK | 17 |
| Android SDK | API 26+ |
| Gradle | 8.7+ |

### פתיחת הפרויקט

```bash
git clone https://github.com/<your-username>/CuponsSMS.git
cd CuponsSMS
```

1. פתח Android Studio → **Open** → בחר את תיקיית `CuponsSMS`
2. המתן ל-Gradle sync
3. חבר מכשיר Android או הפעל אמולטור → לחץ **Run ▶**

### בדיקת SMS על אמולטור

```bash
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED \
  --es address "SuperStore" \
  --es pdu "07914151551512f2040B916105551234F500001CD5F25C0E5A96E7F3F0B90CA2BF41E539BD3CBEE3288FD769F41ECB7FB0E" \
  -n com.cupons.sms/.data.sms.SmsReceiver
```

---

## ארכיטקטורה

```
┌─────────────────────────────────┐
│           UI Layer              │
│   Jetpack Compose + ViewModels  │
└──────────────┬──────────────────┘
               │ StateFlow
┌──────────────▼──────────────────┐
│         Domain Layer            │
│    Use Cases + Domain Models    │
└──────────────┬──────────────────┘
               │ Repository Interface
┌──────────────▼──────────────────┐
│           Data Layer            │
│  Room DB  │  SMS Parser  │ DI  │
└─────────────────────────────────┘
```

**Stack:** Kotlin · Jetpack Compose · Room · Hilt · Coroutines/Flow · Navigation Compose

---

## מבנה הפרויקט

```
app/src/main/java/com/cupons/sms/
├── data/
│   ├── db/          # Room database, DAOs, entities
│   ├── prefs/       # DataStore settings
│   ├── repository/  # Repository implementation
│   ├── sms/         # SMS parser, reader, receiver
│   └── whatsapp/    # WhatsApp Accessibility Service
├── domain/
│   ├── model/       # Coupon, UsageLog
│   ├── repository/  # Repository interface
│   └── usecase/     # ImportFromSmsUseCase
├── ui/
│   ├── screens/     # home, detail, add, settings, statistics, splash
│   ├── components/  # CouponCard, DateInputField
│   ├── navigation/  # AppNavigation (NavHost)
│   └── theme/       # Colors, Theme
├── di/              # Hilt AppModule
└── util/            # ExportManager, NotificationHelper
```

---

## הרשאות נדרשות

| הרשאה | סיבה |
|--------|------|
| `READ_SMS` | קריאת הודעות SMS קיימות |
| `RECEIVE_SMS` | קבלת SMS חדשים בזמן אמת |
| `POST_NOTIFICATIONS` | התראות לפני תפוגת קופון |
| `INTERNET` | פתיחת קישורי מימוש בדפדפן |

> כל הנתונים מאוחסנים **מקומית בלבד** — אין שרת, אין ענן, אין שיתוף עם צד שלישי.

---

## תרומה לפרויקט

1. Fork את הפרויקט
2. צור branch חדש: `git checkout -b feature/my-feature`
3. Commit השינויים: `git commit -m 'Add my feature'`
4. Push: `git push origin feature/my-feature`
5. פתח Pull Request

---

## רישיון

MIT License — ראה [LICENSE](LICENSE) לפרטים.

---

<p align="center">נבנה עם ❤️ עבור חיסכון חכם יותר</p>
