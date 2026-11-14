package com.trinhquyetchien.markdowntray.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StandaloneAlarm(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(), // 0=Sun, 1=Mon, ..., 6=Sat (matching JS weekday)
    val isEnabled: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("hour", hour)
        obj.put("minute", minute)
        val arr = JSONArray()
        repeatDays.forEach { arr.put(it) }
        obj.put("repeatDays", arr)
        obj.put("isEnabled", isEnabled)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): StandaloneAlarm {
            val repeatDays = mutableListOf<Int>()
            val arr = obj.optJSONArray("repeatDays")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    repeatDays.add(arr.getInt(i))
                }
            }
            return StandaloneAlarm(
                id = obj.optString("id", UUID.randomUUID().toString()),
                title = obj.optString("title", ""),
                hour = obj.optInt("hour", 8),
                minute = obj.optInt("minute", 0),
                repeatDays = repeatDays,
                isEnabled = obj.optBoolean("isEnabled", true)
            )
        }
    }
}
