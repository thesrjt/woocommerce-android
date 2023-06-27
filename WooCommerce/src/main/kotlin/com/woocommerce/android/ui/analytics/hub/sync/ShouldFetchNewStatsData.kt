package com.woocommerce.android.ui.analytics.hub.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class ShouldFetchNewStatsData @Inject constructor(
    @DataStoreQualifier(DataStoreType.ANALYTICS) private val dataStore: DataStore<Preferences>
) {
    operator fun invoke(rangeSelection: StatsTimeRangeSelection): Boolean {
        dataStore.data.map { preferences ->
            val timestampKey = rangeSelection.selectionType.identifier
            preferences[longPreferencesKey(timestampKey)]
        }
        return true
    }

    suspend fun storeLastAnalyticsUpdate(rangeSelection: StatsTimeRangeSelection) {
        dataStore.edit { preferences ->
            val timestampKey = rangeSelection.selectionType.identifier
            preferences[longPreferencesKey(timestampKey)] = System.currentTimeMillis()
        }
    }

    companion object {
        const val maxOutdatedTime = 1000 * 30 // 30 seconds
    }
}
