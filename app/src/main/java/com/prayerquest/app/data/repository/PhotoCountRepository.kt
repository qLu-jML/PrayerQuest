package com.prayerquest.app.data.repository

import android.content.Context
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Thin aggregator over [PhotoStorage] that answers two questions:
 *   1. "How many photos has this user stored on-device?" (across Photo
 *      Prayers, Gratitude, and Answered-Prayer Testimonies.)
 *   2. "Can they add another one?" (free tier caps at
 *      [PremiumFeatures.FREE_TOTAL_PHOTO_CAP]; premium is unlimited.)
 *
 * Kept as a repository rather than inlining into each ViewModel so we have a
 * single place to add caching / invalidation later, and so unit tests can
 * swap it for a fake. Count is exposed as a StateFlow so photo-picker CTAs
 * can react the moment a write lands — call [refresh] after every successful
 * save or delete.
 */
class PhotoCountRepository(
    private val context: Context,
) {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    /**
     * Recount photos on disk. Called on init and after every photo
     * add/delete so the UI cap check stays in sync with filesystem truth.
     */
    suspend fun refresh(): Int = withContext(Dispatchers.IO) {
        val n = PhotoStorage.countAllPhotos(context)
        _count.value = n
        n
    }

    /**
     * Whether this user can add another photo right now.
     *
     * Uses the cached count — if you just finished a write and need an
     * up-to-the-moment answer, call [refresh] first.
     */
    fun canAddPhoto(isPremium: Boolean): Boolean =
        PremiumFeatures.canAddPhoto(isPremium, _count.value)
}
