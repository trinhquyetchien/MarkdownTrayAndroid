package com.trinhquyetchien.markdowntray.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trinhquyetchien.markdowntray.data.StorageManager

class AppViewModelFactory(
    private val context: Context,
    private val storageManager: StorageManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TodayViewModel::class.java) -> {
                TodayViewModel(storageManager) as T
            }
            modelClass.isAssignableFrom(DeadlineViewModel::class.java) -> {
                DeadlineViewModel(storageManager) as T
            }
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> {
                CalendarViewModel(context.applicationContext, storageManager) as T
            }
            modelClass.isAssignableFrom(NotesListViewModel::class.java) -> {
                NotesListViewModel(storageManager) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(storageManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
