package com.trinhquyetchien.markdowntray.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarOccurrence(
    val event: CalendarEvent,
    val occurrenceDate: String, // "YYYY-MM-DD"
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isRecurring: Boolean
) {
    val durationMinutes: Int
        get() {
            val totalStart = startHour * 60 + startMinute
            val totalEnd = endHour * 60 + endMinute
            return if (totalEnd > totalStart) totalEnd - totalStart else 30
        }
}

object CalendarUtils {
    private val ymdFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatYMD(date: Date): String = ymdFormat.format(date)

    fun parseTime(timeStr: String): Pair<Int, Int> {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Pair(h, m)
    }

    /**
     * Lấy các occurrences diễn ra trong ngày `targetDateStr` (định dạng "yyyy-MM-dd")
     */
    fun getOccurrencesForDate(events: List<CalendarEvent>, targetDateStr: String): List<CalendarOccurrence> {
        val cal = Calendar.getInstance()
        val targetDate = try {
            ymdFormat.parse(targetDateStr) ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        cal.time = targetDate
        val jsWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat

        val results = mutableListOf<CalendarOccurrence>()

        for (ev in events) {
            if (ev.start.length < 10) continue
            val evDate = ev.start.substring(0, 10)
            val startTimeStr = if (ev.start.length >= 16) ev.start.substring(11, 16) else "00:00"
            val endTimeStr = if (ev.end.length >= 16) ev.end.substring(11, 16) else "01:00"

            val (sH, sM) = parseTime(startTimeStr)
            var (eH, eM) = parseTime(endTimeStr)
            if (eH < sH || (eH == sH && eM <= sM)) {
                // Tối thiểu 30 phút hoặc cùng giờ
                eH = (sH + (sM + 30) / 60) % 24
                eM = (sM + 30) % 60
            }

            if (ev.repeatDays.isNotEmpty()) {
                // Sự kiện lặp lại
                val anchorDate = evDate
                if (anchorDate <= targetDateStr && ev.repeatDays.contains(jsWeekday)) {
                    val isException = ev.exceptionDates.contains(targetDateStr)
                    val isPastUntil = !ev.untilDate.isNullOrEmpty() && targetDateStr >= ev.untilDate
                    if (!isException && !isPastUntil) {
                        results.add(
                            CalendarOccurrence(
                                event = ev,
                                occurrenceDate = targetDateStr,
                                startHour = sH,
                                startMinute = sM,
                                endHour = eH,
                                endMinute = eM,
                                isRecurring = true
                            )
                        )
                    }
                }
            } else {
                // Sự kiện đơn
                if (evDate == targetDateStr) {
                    results.add(
                        CalendarOccurrence(
                            event = ev,
                            occurrenceDate = targetDateStr,
                            startHour = sH,
                            startMinute = sM,
                            endHour = eH,
                            endMinute = eM,
                            isRecurring = false
                        )
                    )
                }
            }
        }

        return results.sortedWith(compareBy({ it.startHour }, { it.startMinute }))
    }

    /**
     * Lấy danh sách 7 ngày trong tuần chứa targetDate
     */
    fun getWeekDates(anchorDate: Calendar): List<Date> {
        val cal = anchorDate.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        // Tìm ngày thứ hai trong tuần
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dates = mutableListOf<Date>()
        for (i in 0 until 7) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    /**
     * Lấy các ngày trong tháng (cả các ngày lấp đầy tuần đầu và tuần cuối của tháng)
     */
    fun getMonthGridDates(anchorDate: Calendar): List<Date> {
        val cal = anchorDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.firstDayOfWeek = Calendar.MONDAY

        // Lùi về thứ Hai đầu tiên của lưới
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        val dates = mutableListOf<Date>()
        for (i in 0 until 42) { // 6 tuần x 7 ngày
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }
}
