package com.example.unimove.ui.viewmodel

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val malaysiaTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = malaysiaTimeZone }
private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).apply { timeZone = malaysiaTimeZone }
private val dayOfMonthFormat = SimpleDateFormat("d", Locale.US).apply { timeZone = malaysiaTimeZone }
private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US).apply { timeZone = malaysiaTimeZone }

fun malaysiaTodayDateKey(): String {
    return dateKeyFormat.format(Calendar.getInstance(malaysiaTimeZone).time)
}

fun malaysiaDateKeyFor(calendar: Calendar): String {
    return dateKeyFormat.format(calendar.time)
}

fun parseMalaysiaDateKey(dateKey: String): Calendar {
    val date = dateKeyFormat.parse(dateKey) ?: Date()
    return Calendar.getInstance(malaysiaTimeZone).apply { time = date }
}

fun formatMealDateLabel(dateKey: String): String {
    val cal = parseMalaysiaDateKey(dateKey)
    val today = Calendar.getInstance(malaysiaTimeZone)
    
    val isSameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            
    if (isSameDay) return "Today"
    
    val yesterday = Calendar.getInstance(malaysiaTimeZone).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
            
    if (isYesterday) return "Yesterday"
    
    return displayDateFormat.format(cal.time)
}

fun canNavigateToNextMealDate(dateKey: String): Boolean {
    val cal = parseMalaysiaDateKey(dateKey)
    val today = Calendar.getInstance(malaysiaTimeZone)
    return cal.before(today) && !isSameDay(cal, today)
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

data class DateItem(
    val dateKey: String,
    val dayOfMonth: String,
    val dayOfWeek: String,
    val isToday: Boolean
)

fun getCalendarStrip(centerDateKey: String, range: Int = 14): List<DateItem> {
    val centerCal = parseMalaysiaDateKey(centerDateKey)
    val today = Calendar.getInstance(malaysiaTimeZone)
    
    val items = mutableListOf<DateItem>()
    for (i in -range..range) {
        val cal = centerCal.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, i)
        items.add(
            DateItem(
                dateKey = dateKeyFormat.format(cal.time),
                dayOfMonth = dayOfMonthFormat.format(cal.time),
                dayOfWeek = dayOfWeekFormat.format(cal.time),
                isToday = isSameDay(cal, today)
            )
        )
    }
    return items
}
