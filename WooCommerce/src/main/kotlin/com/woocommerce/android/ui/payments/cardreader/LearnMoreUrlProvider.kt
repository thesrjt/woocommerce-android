package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import dagger.Reusable
import javax.inject.Inject

@Reusable
class LearnMoreUrlProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    fun provideLearnMoreUrlFor(learnMoreUrl: LearnMoreUrl): String {
        val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
            selectedSite.get().id,
            selectedSite.get().siteId,
            selectedSite.get().selfHostedSiteId
        )
        return when (learnMoreUrl) {
            LearnMoreUrl.IN_PERSON_PAYMENTS -> {
                when (preferredPlugin) {
                    STRIPE_EXTENSION_GATEWAY -> AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS
                    WOOCOMMERCE_PAYMENTS, null -> AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
                }
            }
            LearnMoreUrl.CASH_ON_DELIVERY -> {
                when (preferredPlugin) {
                    STRIPE_EXTENSION_GATEWAY -> AppUrls.STRIPE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
                    WOOCOMMERCE_PAYMENTS, null -> AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
                }
            }
        }
    }

    enum class LearnMoreUrl {
        IN_PERSON_PAYMENTS,
        CASH_ON_DELIVERY
    }
}
