package com.prayerquest.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.data.repository.PrayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository,
    gratitudeRepository: GratitudeRepository
) : ViewModel() {

    // Collections tab data
    val collections = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Famous Prayers tab data
    val famousPrayers = prayerRepository.observeAllFamousPrayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Answered Prayers tab data
    val answeredPrayers = prayerRepository.observeAnsweredItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gratitude tab data
    val gratitudeEntries = gratitudeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(
        private val collectionRepository: CollectionRepository,
        private val prayerRepository: PrayerRepository,
        private val gratitudeRepository: GratitudeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(collectionRepository, prayerRepository, gratitudeRepository) as T
        }
    }
}
