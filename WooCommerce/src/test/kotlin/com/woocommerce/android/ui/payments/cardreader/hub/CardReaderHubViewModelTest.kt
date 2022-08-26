package com.woocommerce.android.ui.payments.cardreader.hub

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettings
import com.woocommerce.android.ui.payments.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderHubViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderHubViewModel
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on(it.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(WOOCOMMERCE_PAYMENTS)
    }
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wooStore: WooCommerceStore = mock()
    private val cardReaderChecker: CardReaderOnboardingChecker = mock {
        onBlocking { getOnboardingState() } doReturn mock<CardReaderOnboardingState.OnboardingCompleted>()
    }
    private val cashOnDeliverySettings: CashOnDeliverySettings = mock()
    private val cardReaderTracker: CardReaderTracker = mock()

    private val savedState = CardReaderHubFragmentArgs(
        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub,
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `when screen shown, then collect payments row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_manage_card_reader
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }
    }

    @Test
    fun `when screen shown, then collect payment row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_gridicons_money_on_surface
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_shopping_cart
            }
    }

    @Test
    fun `when screen shown, then manual card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual
            }
    }

    @Test
    fun `when user clicks on collect payment, then app navigates to payment collection screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPaymentCollectionScreen
            )
    }

    @Test
    fun `when user clicks on collect payment, then collect payment event tracked`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_COLLECT_PAYMENT_TAPPED)
    }

    @Test
    fun `when user clicks on manage card reader, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderDetail(
                    CardReaderFlowParam.CardReadersHub
                )
            )
    }

    @Test
    fun `when user clicks on manage card reader, then manage card readers event tracked`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_MANAGE_CARD_READERS_TAPPED)
    }

    @Test
    fun `given ipp canada disabled, when user clicks on purchase card reader, then app opens external webview`() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                    AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER
                )
            )
    }

    @Test
    fun `given ipp canada enabled, when user clicks on purchase card reader, then app opens external webview`() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                    "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
                )
            )
    }

    @Test
    fun `when user clicks on purchase card reader, then orders card reader event tracked`() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_ORDER_CARD_READER_TAPPED)
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview with in-person-payments link`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun `given wcpay active, when user clicks on purchase card reader, then woo purchase link shown`() {
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(WOOCOMMERCE_PAYMENTS)

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun `given stripe active, when user clicks on purchase card reader, then stripe purchase link shown`() {
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(STRIPE_EXTENSION_GATEWAY)

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.STRIPE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun `given onboarding check error, when user clicks on text, then onboarding shown`() = testBlocking {
        val genericError = mock<CardReaderOnboardingState.GenericError>()
        whenever(cardReaderChecker.getOnboardingState()).thenReturn(
            genericError
        )

        initViewModel()

        viewModel.viewStateData.value?.onboardingErrorAction!!.onClick.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(
                    genericError
                )
            )
    }

    @Test
    fun `given onboarding check error, when user clicks on text, then payments hub tapped tracked`() = testBlocking {
        whenever(cardReaderChecker.getOnboardingState()).thenReturn(
            mock<CardReaderOnboardingState.GenericError>()
        )

        initViewModel()

        viewModel.viewStateData.value?.onboardingErrorAction!!.onClick.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_ONBOARDING_ERROR_TAPPED)
    }

    @Test
    fun ` when screen shown, then manuals row is displayed`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual &&
                    it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
            }
    }

    @Test
    fun `when user clicks on manuals row, then app navigates to manuals screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen
            )
    }

    @Test
    fun `when user clicks on manuals row, then click on manuals tracked`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED)
    }

    @Test
    fun `when multiple plugins installed, then payment provider row is shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
            }
    }

    @Test
    fun `when multiple plugins installed, then payment provider icon is shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_payment_provider
            }
    }

    @Test
    fun `given multiple plugins installed, when change payment provider clicked, then trigger onboarding event`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(
                CardReaderOnboardingState.ChoosePaymentGatewayProvider
            )
        )
    }

    @Test
    fun `given multiple plugins installed, when payment provider clicked, then clear plugin selected flag`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

        verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(false)
        )
    }

    @Test
    fun `given multiple plugins installed, when change payment provider clicked, then track event`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED)
    }

    @Test
    fun `when single plugin installed, then payment provider row is not shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(false)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .noneMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
            }
    }

    @Test
    fun `given multiple plugins installed but not selected, when view model init, then error`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.ChoosePaymentGatewayProvider>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction?.text).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding error, when view model init, then show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction?.text).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding complete, when view model init, then do not show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction).isNull()
        }

    @Test
    fun `given onboarding error, when screen shown, then manage card reader row disabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isFalse
        }

    @Test
    fun `given onboarding complete, when screen shown, then manage card reader row enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding error, when screen shown, then collect payment row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding error, when screen shown, then card reader manual is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding status changed to competed, when screen shown again, then onboarding error hidden`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction?.text).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )

            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            viewModel.onViewVisible()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction?.text).isNull()
        }

    @Test
    fun `given pending requirements status, when screen shown, then onboarding error visible`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.onboardingErrorAction?.text).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_with_pending_requirements, containsHtml = true)
            )
        }

    @Test
    fun `given pending requirements status, when screen shown, then collect payment row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then order card reader row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then card reader manuals is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then manage card reader is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    // region cash on delivery
    @Test
    fun `when screen shown, then cash on delivery row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
            }
    }

    @Test
    fun `when screen shown, then cod row present with correct icon`() =
        testBlocking {
            assertThat((viewModel.viewStateData.value)?.rows)
                .anyMatch {
                    it.icon == R.drawable.ic_manage_card_reader
                }
        }

    @Test
    fun `when screen shown, then cash on delivery row present with correct description`() {
        assertThat(
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).description
        ).isEqualTo(UiString.UiStringRes(R.string.card_reader_enable_pay_in_person_description))
    }

    @Test
    fun `when screen shown, then cash on delivery is disabled`() {
        assertThat(
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).isChecked
        ).isFalse
    }

    @Test
    fun `when screen shown, then cash on delivery is allowed to toggle`() {
        assertThat(
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).isEnabled
        ).isTrue
    }

    @Test
    fun `given cash on delivery enabled, when screen shown, then cash on delivery state is enabled`() =
        testBlocking {
            whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()).thenReturn(true)

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isTrue()
        }

    @Test
    fun `given cash on delivery disabled, when screen shown, then cash on delivery state is disabled`() =
        testBlocking {
            whenever(cashOnDeliverySettings.isCashOnDeliveryEnabled()).thenReturn(false)

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isFalse
        }

    @Test
    fun `given cash on delivery api in progress, when cod toggled, then cash on delivery state not allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettings.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[1].rows.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isFalse
        }

    @Test
    fun `given cash on delivery api success, when cod toggled, then cash on delivery state is allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettings.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isTrue
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then cash on delivery state is allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettings.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isTrue
        }

    @Test
    fun `given cash on delivery api success, when cod toggled, then track cod success event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettings.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryEnabledSuccess()
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then track cod failure event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettings.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryEnabledFailure(
                "Enabling COD failed. Please try again later"
            )
        }
    // endregion

    private fun getSuccessWooResult() = WooResult(
        model = WCGatewayModel(
            id = "",
            title = "",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "",
            methodDescription = "",
            features = emptyList()
        )
    )

    private fun getFailureWooResult() = WooResult<WCGatewayModel>(
        error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = "Enabling COD failed. Please try again later"
        )
    )

    private fun initViewModel() {
        viewModel = CardReaderHubViewModel(
            savedState,
            inPersonPaymentsCanadaFeatureFlag,
            appPrefsWrapper,
            selectedSite,
            analyticsTrackerWrapper,
            wooStore,
            cardReaderChecker,
            cashOnDeliverySettings,
            cardReaderTracker
        )
        viewModel.onViewVisible()
    }
}
