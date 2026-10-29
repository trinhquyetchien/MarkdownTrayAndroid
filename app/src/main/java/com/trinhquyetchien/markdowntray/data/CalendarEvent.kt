package com.trinhquyetchien.markdowntray.data

import org.json.JSONArray
import org.json.JSONObject

data class CalendarEvent(
    val id: String,
    val title: String,
    val note: String = "",
    val color: String = "#0a84ff",
    val start: String, // format "YYYY-MM-DDTHH:MM:SS" or "YYYY-MM-DDTHH:MM"
    val end: String,   // format "YYYY-MM-DDTHH:MM:SS" or "YYYY-MM-DDTHH:MM"
    val repeatDays: List<Int> = emptyList(), // 0=Sun, 1=Mon, ..., 6=Sat
    val exceptionDates: List<String> = emptyList(), // ["YYYY-MM-DD"]
    val untilDate: String? = null // "YYYY-MM-DD"
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("note", note)
        obj.put("color", color)
        obj.put("start", start)
        obj.put("end", end)
        if (repeatDays.isNotEmpty()) {
            val rArray = JSONArray()
            repeatDays.forEach { rArray.put(it) }
            obj.put("repeatDays", rArray)
        }
        if (exceptionDates.isNotEmpty()) {
            val eArray = JSONArray()
            exceptionDates.forEach { eArray.put(it) }
            obj.put("exceptionDates", eArray)
        }
        if (!untilDate.isNullOrEmpty()) {
            obj.put("untilDate", untilDate)
        }
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): CalendarEvent {
            val rList = mutableListOf<Int>()
            val rArray = obj.optJSONArray("repeatDays")
            if (rArray != null) {
                for (i in 0 until rArray.length()) {
                    rList.add(rArray.getInt(i))
                }
            }

            val eList = mutableListOf<String>()
            val eArray = obj.optJSONArray("exceptionDates")
            if (eArray != null) {
                for (i in 0 until eArray.length()) {
                    eList.add(eArray.getString(i))
                }
            }

            val until = obj.optString("untilDate").takeIf { it.isNotBlank() }

            return CalendarEvent(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                title = obj.optString("title", "Untitled"),
                note = obj.optString("note", ""),
                color = obj.optString("color", "#0a84ff").ifEmpty { "#0a84ff" },
                start = obj.optString("start", ""),
                end = obj.optString("end", ""),
                repeatDays = rList,
                exceptionDates = eList,
                untilDate = until
            )
        }
    }
}
