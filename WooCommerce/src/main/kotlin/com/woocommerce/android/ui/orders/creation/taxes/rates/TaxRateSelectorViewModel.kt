package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
    repository: TaxRateRepository,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val _viewState: MutableStateFlow<ViewState> =
        savedState.getStateFlow(
            scope = this,
            initialValue = ViewState(),
            key = "view_state"
        )
    val viewState: StateFlow<ViewState> = _viewState

    init {
        launch {
            repository.fetchTaxRates().let { taxRates ->
                _viewState.value = _viewState.value.copy(
                    taxRates = taxRates.map { taxRate ->
                        TaxRateUiModel(
                            label = calculateTaxRateLabel(taxRate),
                            rate = calculateTaxRatePercentageText(taxRate),
                            taxRate = taxRate,
                        )
                    }
                )
            }
        }
    }

    private fun calculateTaxRatePercentageText(taxRate: TaxRate) =
        if (taxRate.rate.isNotNullOrEmpty()) {
            "${taxRate.rate}%"
        } else {
            ""
        }

    private fun calculateTaxRateLabel(taxRate: TaxRate) =
        StringBuilder().apply {
            if (taxRate.name.isNotNullOrEmpty()) {
                append(taxRate.name)
                append(" · ")
            }
            if (taxRate.countryCode.isNotNullOrEmpty()) {
                append(taxRate.countryCode)
                append(SPACE_CHAR)
            }
            if (taxRate.stateCode.isNotNullOrEmpty()) {
                append(taxRate.stateCode)
                append(SPACE_CHAR)
            }
            if (taxRate.postcode.isNotNullOrEmpty()) {
                append(taxRate.postcode)
                append(SPACE_CHAR)
            }
            if (taxRate.city.isNotNullOrEmpty()) {
                append(taxRate.city)
            }
        }.toString()

    fun onEditTaxRatesInAdminClicked() {
        triggerEvent(EditTaxRatesInAdmin)
        tracker.track(AnalyticsEvent.TAX_RATE_SELECTOR_EDIT_IN_ADMIN_TAPPED)
    }
    fun onInfoIconClicked() {
        triggerEvent(ShowTaxesInfoDialog)
    }

    fun onTaxRateSelected(taxRate: TaxRateUiModel) {
        triggerEvent(TaxRateSelected(taxRate.taxRate))
        tracker.track(AnalyticsEvent.TAX_RATE_SELECTOR_TAX_RATE_TAPPED)
    }

    fun onDismissed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    @Parcelize
    data class ViewState(
        val taxRates: List<TaxRateUiModel> = emptyList(),
    ) : Parcelable

    @Parcelize
    data class TaxRateUiModel(
        val label: String,
        val rate: String,
        val taxRate: TaxRate,
    ) : Parcelable

    data class TaxRateSelected(val taxRate: TaxRate) : MultiLiveEvent.Event()
    object EditTaxRatesInAdmin : MultiLiveEvent.Event()
    object ShowTaxesInfoDialog : MultiLiveEvent.Event()

    private companion object {
        private const val SPACE_CHAR = " "
    }
}
