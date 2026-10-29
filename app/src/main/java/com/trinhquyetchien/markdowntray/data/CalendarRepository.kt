package com.trinhquyetchien.markdowntray.data

import java.io.File
import org.json.JSONArray

object CalendarRepository {

    fun loadEvents(file: File): List<CalendarEvent> {
        return try {
            if (!file.exists()) return emptyList()
            val content = file.readText(Charsets.UTF_8).trim()
            if (content.isEmpty()) return emptyList()
            val array = JSONArray(content)
            val list = mutableListOf<CalendarEvent>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(CalendarEvent.fromJsonObject(obj))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveEvents(file: File, events: List<CalendarEvent>): Boolean {
        return try {
            file.parentFile?.mkdirs()
            val array = JSONArray()
            events.forEach { array.put(it.toJsonObject()) }
            file.writeText(array.toString(2), Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
