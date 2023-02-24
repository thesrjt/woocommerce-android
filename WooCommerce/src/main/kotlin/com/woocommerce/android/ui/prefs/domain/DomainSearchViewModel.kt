package com.woocommerce.android.ui.prefs.domain

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion.Paid
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion.Premium
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DomainSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    domainSuggestionsRepository: DomainSuggestionsRepository,
    currencyFormatter: CurrencyFormatter,
    private val domainChangeRepository: DomainChangeRepository
) : DomainSuggestionsViewModel(
    savedStateHandle = savedStateHandle,
    domainSuggestionsRepository = domainSuggestionsRepository,
    currencyFormatter = currencyFormatter,
    initialQuery = "",
    searchOnlyFreeDomains = false,
    isFreeCreditAvailable = savedStateHandle[KEY_IS_FREE_CREDIT_AVAILABLE]!!
) {
    override val helpOrigin = HelpOrigin.DOMAIN_CHANGE

    private val navArgs: DomainSearchFragmentArgs by savedStateHandle.navArgs()

    override fun navigateToNextStep(selectedDomain: DomainSuggestion) {
        when (selectedDomain) {
            is Premium -> {
                createShoppingCart(selectedDomain.name, selectedDomain.productId)
            }
            is Paid -> {
                if (navArgs.isFreeCreditAvailable) {
                    triggerEvent(NavigateToDomainRegistration(selectedDomain.name, selectedDomain.productId))
                } else {
                    createShoppingCart(selectedDomain.name, selectedDomain.productId)
                }
            }
            else -> throw UnsupportedOperationException("This domain search is only for paid domains")
        }
    }

    private fun createShoppingCart(domain: String, productId: Int) {
        launch {
            val result = domainChangeRepository.addDomainToCart(productId, domain, true)

            if (!result.isError) {
                triggerEvent(ShowCheckoutWebView(domain, navArgs.wpComDomain))
            } else {
                triggerEvent(
                    ShowUiStringSnackbar(UiStringText(result.error.message ?: "Unable to create a shopping cart"))
                )
                triggerEvent(Exit)
            }
        }
    }

    data class NavigateToDomainRegistration(val domain: String, val productId: Int) : MultiLiveEvent.Event()
    data class ShowCheckoutWebView(val domain: String, val wpComDomain: String) : MultiLiveEvent.Event()
}
