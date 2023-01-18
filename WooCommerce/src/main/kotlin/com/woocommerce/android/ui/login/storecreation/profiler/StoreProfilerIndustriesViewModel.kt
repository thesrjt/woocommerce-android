package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.NewStore.ProfilerData
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerIndustriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    private var industries: List<Industry> = emptyList()
    override val hasSearchableContent: Boolean
        get() = true

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_INDUSTRIES
            )
        )
        launch {
            isLoading.value = true
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            industries = fetchedOptions.industries
            profilerOptions.update {
                industries.map { it.toStoreProfilerOptionUi() }
            }
            isLoading.value = false
        }
    }

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_description)

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_title)

    override fun onContinueClicked() {
        val selectedOptionName = profilerOptions.value.firstOrNull { it.isSelected }?.name
        val selectedIndustry = industries.first { selectedOptionName == it.label }
        newStore.update(
            profilerData = (newStore.data.profilerData ?: ProfilerData())
                .copy(
                    industryLabel = selectedIndustry.label,
                    industryKey = selectedIndustry.key,
                    industryGroupKey = selectedIndustry.tracks
                )
        )
        triggerEvent(NavigateToCommerceJourneyStep)
    }

    override fun onSearchQueryChanged(query: String) {
        profilerOptions.update {
            if (query.isBlank()) industries.map { it.toStoreProfilerOptionUi() }
            industries
                .filter { industry ->
                    industry.label.contains(query, ignoreCase = true)
                }
                .map { it.toStoreProfilerOptionUi() }
        }
    }

    private fun Industry.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        name = label,
        key = key,
        isSelected = newStore.data.profilerData?.industryKey == key
    )
}
