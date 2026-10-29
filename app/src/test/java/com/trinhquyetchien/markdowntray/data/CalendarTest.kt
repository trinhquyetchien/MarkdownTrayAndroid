package com.trinhquyetchien.markdowntray.data

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarTest {

    @Test
    fun testEventSerialization() {
        val event = CalendarEvent(
            id = "test_1",
            title = "Họp nhóm dự án",
            note = "Thảo luận tính năng Compose",
            color = "#ff9f0a",
            start = "2026-10-29T10:00:00",
            end = "2026-10-29T11:00:00",
            repeatDays = listOf(1, 3, 5),
            exceptionDates = listOf("2026-11-02"),
            untilDate = "2026-12-31"
        )

        val json = event.toJsonObject()
        val parsed = CalendarEvent.fromJsonObject(json)

        assertEquals("test_1", parsed.id)
        assertEquals("Họp nhóm dự án", parsed.title)
        assertEquals("Thảo luận tính năng Compose", parsed.note)
        assertEquals("#ff9f0a", parsed.color)
        assertEquals(listOf(1, 3, 5), parsed.repeatDays)
        assertEquals(listOf("2026-11-02"), parsed.exceptionDates)
        assertEquals("2026-12-31", parsed.untilDate)
    }

    @Test
    fun testOccurrencesSingleEvent() {
        val event = CalendarEvent(
            id = "single_1",
            title = "Việc một lần",
            start = "2026-10-29T14:30:00",
            end = "2026-10-29T16:00:00"
        )

        val occToday = CalendarUtils.getOccurrencesForDate(listOf(event), "2026-10-29")
        assertEquals(1, occToday.size)
        assertEquals(14, occToday[0].startHour)
        assertEquals(30, occToday[0].startMinute)
        assertEquals(16, occToday[0].endHour)
        assertEquals(0, occToday[0].endMinute)
        assertEquals(90, occToday[0].durationMinutes)
        assertEquals(false, occToday[0].isRecurring)

        val occTomorrow = CalendarUtils.getOccurrencesForDate(listOf(event), "2026-10-30")
        assertEquals(0, occTomorrow.size)
    }

    @Test
    fun testOccurrencesRecurringEvent() {
        // Thứ 5 (2026-10-29). Calendar.THURSDAY - 1 = 4 (jsWeekday)
        val event = CalendarEvent(
            id = "rec_1",
            title = "Học tập định kỳ",
            start = "2026-10-01T08:00:00",
            end = "2026-10-01T09:30:00",
            repeatDays = listOf(4) // Thứ Năm
        )

        val occ = CalendarUtils.getOccurrencesForDate(listOf(event), "2026-10-29")
        assertEquals(1, occ.size)
        assertEquals(true, occ[0].isRecurring)
        assertEquals("2026-10-29", occ[0].occurrenceDate)

        // Nếu ngày đó là exceptionDate
        val eventWithException = event.copy(exceptionDates = listOf("2026-10-29"))
        val occExcept = CalendarUtils.getOccurrencesForDate(listOf(eventWithException), "2026-10-29")
        assertEquals(0, occExcept.size)
    }
}
