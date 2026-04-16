package com.cupons.sms.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.cupons.sms.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.*

/**
 * DateInputField — שדה תאריך משולב:
 * - הקלדה ידנית עם פרמוט אוטומטי DD/MM/YYYY
 * - כפתור תאריכון (📅) שפותח DatePickerDialog
 *
 * @param value          ערך נוכחי בפורמט "DD/MM/YYYY" (יכול להיות חלקי)
 * @param onValueChange  קולבק כשהערך משתנה
 * @param label          תווית לשדה
 * @param modifier       Modifier חיצוני
 * @param colors         צבעי OutlinedTextField (אופציונלי)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = PrimaryGreen,
        unfocusedBorderColor = PrimaryGreen.copy(alpha = 0.4f),
        focusedLabelColor    = PrimaryGreen,
        cursorColor          = PrimaryGreen
    )
) {
    var showPicker by remember { mutableStateOf(false) }

    // ─── פרסור ערך נוכחי לaמשמש DatePicker ───
    val initialMillis: Long? = remember(value) {
        if (value.length == 10) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.isLenient = false
                // DatePicker מצפה ל-UTC midnight
                val parsed = sdf.parse(value) ?: return@remember null
                val cal = Calendar.getInstance()
                cal.time = parsed
                // המר לUTC midnight
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCal.set(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                utcCal.set(Calendar.MILLISECOND, 0)
                utcCal.timeInMillis
            } catch (e: Exception) { null }
        } else null
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    // ─── DatePickerDialog ───
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // DatePicker מחזיר UTC midnight — ממיר לתאריך מקומי
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.timeInMillis = millis
                        val formatted = "%02d/%02d/%04d".format(
                            utcCal.get(Calendar.DAY_OF_MONTH),
                            utcCal.get(Calendar.MONTH) + 1,
                            utcCal.get(Calendar.YEAR)
                        )
                        onValueChange(formatted)
                    }
                    showPicker = false
                }) {
                    Text("אישור", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("ביטול")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ─── שדה טקסט עם הקלדה + אייקון תאריכון ───
    OutlinedTextField(
        value         = value,
        onValueChange = { raw ->
            // פרמוט אוטומטי: מוסיף "/" אחרי יום ואחרי חודש.
            // בטוח לקרוא כפול — פורמט על מחרוזת מפורמטת כבר מחזיר את אותה מחרוזת.
            val digits = raw.filter { it.isDigit() }.take(8)
            val formatted = buildString {
                digits.forEachIndexed { i, c ->
                    append(c)
                    if (i == 1 || i == 3) append("/")
                }
            }
            onValueChange(formatted)
        },
        label         = { Text(label) },
        placeholder   = { Text("DD/MM/YYYY") },
        trailingIcon  = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    imageVector        = Icons.Default.CalendarMonth,
                    contentDescription = "בחר תאריך מתאריכון",
                    tint               = PrimaryGreen
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine      = true,
        modifier        = modifier,
        colors          = colors
    )
}

/**
 * פרסור DD/MM/YYYY לUnix timestamp (ms) — לשמירה ב-Room.
 * מחזיר null אם הפורמט לא תקין.
 */
fun parseDateToMillis(dateStr: String): Long? {
    if (dateStr.length < 10) return null
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.isLenient = false
        val parsed = sdf.parse(dateStr.trim()) ?: return null
        // שמור את 23:59:59 — תוקף עד סוף היום
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.timeInMillis
    } catch (e: Exception) { null }
}

/**
 * המרת Unix timestamp לפורמט "DD/MM/YYYY".
 */
fun millisToDateString(millis: Long?): String {
    if (millis == null) return ""
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(millis))
    } catch (e: Exception) { "" }
}
