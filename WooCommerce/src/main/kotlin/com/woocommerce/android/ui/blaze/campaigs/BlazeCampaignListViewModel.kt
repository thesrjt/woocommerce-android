package com.woocommerce.android.ui.blaze.campaigs

import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_DETAIL_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Locale
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val selectedSite: SelectedSite,
    private val blazeUrlsHelper: BlazeUrlsHelper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val LOADING_TRANSITION_DELAY = 200L
        private const val CENTS_TO_UNITS = 100f
    }

    private val navArgs: BlazeCampaignListFragmentArgs by savedStateHandle.navArgs()

    private var totalPages = 1
    private var currentPage = 1
    private val isLoadingMore = MutableStateFlow(false)
    private val isCampaignCelebrationShown = MutableStateFlow(false)

    val state = combine(
        blazeCampaignsStore.observeBlazeCampaigns(selectedSite.get()),
        isLoadingMore.withIndex().debounce { (index, isLoading) ->
            if (index != 0 && !isLoading) {
                // When resetting to not loading, wait a bit to make sure the campaigns list has been fetched from DB
                LOADING_TRANSITION_DELAY
            } else 0L
        }.map { it.value },
        isCampaignCelebrationShown
    ) { campaigns, loadingMore, isBlazeCelebrationScreenShown ->
        BlazeCampaignListState(
            campaigns = campaigns.map { mapToUiState(it) },
            onAddNewCampaignClicked = { onAddNewCampaignClicked() },
            isLoading = loadingMore,
            isCampaignCelebrationShown = isBlazeCelebrationScreenShown
        )
    }.asLiveData()

    init {
        if (navArgs.isPostCampaignCreation) {
            showCampaignCelebrationIfNeeded()
        }
        launch {
            loadCampaignsFor(currentPage)
        }
    }

    fun onEndOfTheListReached() {
        if (!isLoadingMore.value && currentPage + 1 <= totalPages) {
            launch {
                isLoadingMore.value = true
                loadCampaignsFor(++currentPage)
                isLoadingMore.value = false
            }
        }
    }

    fun onCampaignCelebrationDismissed() {
        isCampaignCelebrationShown.value = false
    }

    private suspend fun loadCampaignsFor(page: Int) {
        val result = blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get(), page)
        if (result.isError || result.model == null) {
            triggerEvent(Event.ShowSnackbar(R.string.blaze_campaign_list_error_fetching_campaigns))
        } else {
            totalPages = result.model?.totalPages ?: 1
        }
    }

    private fun onCampaignClicked(campaignId: Int) {
        val url = blazeUrlsHelper.buildCampaignDetailsUrl(campaignId)
        analyticsTrackerWrapper.track(
            stat = BLAZE_CAMPAIGN_DETAIL_SELECTED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.CAMPAIGN_LIST.trackingName)
        )
        triggerEvent(
            ShowCampaignDetails(
                url = url,
                urlToTriggerExit = blazeUrlsHelper.buildCampaignsListUrl()
            )
        )
    }

    private fun mapToUiState(campaignEntity: BlazeCampaignEntity) =
        ClickableCampaign(
            campaignUi = BlazeCampaignUi(
                product = BlazeProductUi(
                    name = campaignEntity.title,
                    imgUrl = campaignEntity.imageUrl.orEmpty(),
                ),
                status = CampaignStatusUi.fromString(campaignEntity.uiStatus),
                stats = listOf(
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_impressions,
                        value = campaignEntity.impressions.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_clicks,
                        value = campaignEntity.clicks.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_budget,
                        value = DecimalFormat("#.##", DecimalFormatSymbols(Locale.getDefault()))
                            .format(campaignEntity.budgetCents / CENTS_TO_UNITS)
                    )
                )
            ),
            onCampaignClicked = { onCampaignClicked(campaignEntity.campaignId) }
        )

    private fun onAddNewCampaignClicked() {
        val url = blazeUrlsHelper.buildUrlForSite(BlazeFlowSource.CAMPAIGN_LIST)
        triggerEvent(LaunchBlazeCampaignCreation(url, BlazeFlowSource.CAMPAIGN_LIST))
    }

    private fun showCampaignCelebrationIfNeeded() {
        if (!appPrefsWrapper.isBlazeCelebrationScreenShown) {
            isCampaignCelebrationShown.value = true
            appPrefsWrapper.isBlazeCelebrationScreenShown = true
        }
    }

    data class BlazeCampaignListState(
        val campaigns: List<ClickableCampaign>,
        val onAddNewCampaignClicked: () -> Unit,
        val isLoading: Boolean,
        val isCampaignCelebrationShown: Boolean
    )

    data class ClickableCampaign(
        val campaignUi: BlazeCampaignUi,
        val onCampaignClicked: () -> Unit,
    )

    data class LaunchBlazeCampaignCreation(val url: String, val source: BlazeFlowSource) : Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : Event()
}
