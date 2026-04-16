# Qponix (CuponsSMS) — מדריך התקנה, הרצה והעלאה ל-GitHub

## דרישות מקדימות

| כלי | גרסה מינימלית |
|-----|--------------|
| Android Studio | Ladybug (2024.2+) |
| JDK | 17 |
| Android SDK | API 26+ (Android 8.0) |
| Gradle | 8.7+ |
| Git | כל גרסה |

---

## שלב 1: פתיחת הפרויקט

1. פתח **Android Studio**
2. בחר **Open** → נווט לתיקיית `CuponsSMS`
3. המתן ל-Gradle sync (כמה דקות בפעם הראשונה)

---

## שלב 2: וידוא dependencies

Android Studio יוריד אוטומטית את כל ה-dependencies מ-`app/build.gradle.kts`.

אם יש שגיאת sync — נסי:
- **File → Invalidate Caches → Invalidate and Restart**

---

## שלב 3: הרצה על אמולטור / מכשיר

### על אמולטור:
1. **Device Manager** → Create Virtual Device → Pixel 8 (API 34)
2. לחצי **Run ▶**

### על מכשיר פיזי:
1. הפעילי **Developer Options + USB Debugging** במכשיר
2. חברי USB
3. לחצי **Run ▶**

---

## שלב 4: בדיקת הרשאות

האפליקציה תבקש הרשאות SMS בפעם הראשונה שלוחצים על כפתור ה-SMS.

**בדיקה על אמולטור** — שליחת SMS סינטטי:
```
adb shell am broadcast -a android.provider.Telephony.SMS_RECEIVED \
  --es address "SuperStore" \
  --es pdu "07914151551512f2040B916105551234F500001CD5F25C0E5A96E7F3F0B90CA2BF41E539BD3CBEE3288FD769F41ECB7FB0E" \
  -n com.cupons.sms/.data.sms.SmsReceiver
```

---

## מבנה תיקיות

```
CuponsSMS/
├── CLAUDE.md                     ← הוראות לעוזר AI
├── README.md                     ← תיאור הפרויקט ל-GitHub
├── .gitignore                    ← קבצים שלא נעלה ל-Git
├── app/
│   ├── build.gradle.kts          ← dependencies
│   ├── proguard-rules.pro        ← כללי minification
│   └── src/main/
│       ├── AndroidManifest.xml   ← הרשאות + components
│       ├── java/com/cupons/sms/
│       │   ├── data/             ← DB, SMS, Repository
│       │   ├── domain/           ← Models, Repository Interface, Use Cases
│       │   ├── ui/               ← Screens, ViewModels, Theme
│       │   ├── di/               ← Hilt modules
│       │   └── CuponsApplication.kt
│       └── res/values/
├── docs/
│   ├── PRD.md                    ← דרישות מוצר
│   ├── TECH_SPEC.md              ← אפיון טכני
│   └── SETUP_GUIDE.md            ← המסמך הזה
├── build.gradle.kts              ← root gradle
└── settings.gradle.kts
```

---

## שאלות נפוצות

**ש: האפליקציה לא מזהה את ה-SMS שלי כקופון**  
ת: ה-Parser דורש confidence ≥ 0.4. SMS עם קוד ברור יזוהה אוטומטית. ניתן להוסיף מילות מפתח מותאמות בהגדרות, או להוסיף קופון ידנית.

**ש: הרשאת SMS נדחתה**  
ת: כנסי להגדרות Android → אפליקציות → CuponsSMS → הרשאות → SMS → אפשר.

**ש: אני רוצה לבדוק את ה-DB ישירות**  
ת: ב-Android Studio → App Inspection → Database Inspector.

**ש: ניווט ה-Swipe בין קופונים לא עובד**  
ת: הניווט עובד רק כשנכנסים לקופון מהרשימה הראשית (HomeScreen). הרשימה מועברת כ-query parameter לניווט.

---

## העלאה ל-GitHub — מדריך שלב אחר שלב

### 1. התקנת Git (אם עדיין לא מותקן)
הורדי מ: https://git-scm.com/download/win

בדקי שהתקנה הצליחה:
```bash
git --version
```

### 2. יצירת חשבון GitHub
אם אין לך — הרשמי ב: https://github.com

### 3. יצירת Repository חדש ב-GitHub
1. היכנסי ל-GitHub → לחצי **+** → **New repository**
2. שם: `CuponsSMS` (או `Qponix`)
3. תיאור: `Android app for managing SMS coupons and gift cards`
4. **Public** או **Private** — לבחירתך
5. **אל תסמני** "Add a README file" (כבר יש לנו README.md)
6. לחצי **Create repository**

### 4. אתחול Git בפרויקט

פתחי Terminal/PowerShell בתיקיית הפרויקט `D:\AI_Agents\CuponsSMS`:
```bash
git init
git add .
git commit -m "Initial commit — Qponix v1.2"
```

### 5. חיבור ל-GitHub ו-Push

```bash
git remote add origin https://github.com/<YOUR_USERNAME>/CuponsSMS.git
git branch -M main
git push -u origin main
```

החלפי `<YOUR_USERNAME>` בשם המשתמש שלך ב-GitHub.

### 6. אימות — GitHub יבקש אישור

**GitHub יבקש אימות.** האפשרויות:
- **Personal Access Token (מומלץ):**  
  GitHub → Settings → Developer settings → Personal access tokens → Generate new token  
  תני לו הרשאת `repo` → העתיקי את הטוקן  
  השתמשי בו במקום הסיסמה בעת ה-push
- **GitHub Desktop** — ממשק גרפי פשוט יותר: https://desktop.github.com

### 7. עדכונים עתידיים

לכל שינוי שתרצי להעלות:
```bash
git add .
git commit -m "תיאור השינוי"
git push
```

---

## קבצים שלא יועלו ל-GitHub (`.gitignore`)

הקבצים הבאים מוגדרים ב-`.gitignore` ולא יועלו:
- `local.properties` — נתיב ה-SDK המקומי שלך
- `*.jks`, `*.keystore` — קבצי Signing (חשוב! לא לשתף)
- `.gradle/`, `build/` — קבצים שנוצרים בבנייה
- `.idea/` — הגדרות IDE מקומיות

---

## שלבים הבאים (Phase 4)

- [ ] Widget על מסך הבית
- [ ] ייצוא CSV
- [ ] גיבוי אוטומטי ל-Google Drive (opt-in)
- [ ] הצגת ברקוד/QR של קוד הקופון
