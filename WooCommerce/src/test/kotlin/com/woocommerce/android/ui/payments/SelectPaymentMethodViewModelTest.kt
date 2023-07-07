package com.woocommerce.android.ui.payments

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.TRY_TAP_TO_PAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Refund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.methodselection.NavigateBackToHub
import com.woocommerce.android.ui.payments.methodselection.NavigateBackToOrderList
import com.woocommerce.android.ui.payments.methodselection.NavigateToCardReaderHubFlow
import com.woocommerce.android.ui.payments.methodselection.NavigateToCardReaderPaymentFlow
import com.woocommerce.android.ui.payments.methodselection.NavigateToCardReaderRefundFlow
import com.woocommerce.android.ui.payments.methodselection.OpenGenericWebView
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodFragmentArgs
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewModel
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Loading
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Success
import com.woocommerce.android.ui.payments.methodselection.SharePaymentUrlViaQr
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

private const val PAYMENT_URL = "paymentUrl"
private const val ORDER_TOTAL = "100$"

@OptIn(ExperimentalCoroutinesApi::class)
class SelectPaymentMethodViewModelTest : BaseUnitTest() {
    private val site: SiteModel = mock {
        on { name }.thenReturn("siteName")
    }
    private val order: Order = mock {
        on { paymentUrl }.thenReturn(PAYMENT_URL)
        on { total }.thenReturn(BigDecimal(1L))
        on { id }.thenReturn(1L)
    }
    private val orderEntity: OrderEntity = mock()

    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val orderStore: WCOrderStore = mock {
        onBlocking { getOrderByIdAndSite(any(), any()) }.thenReturn(orderEntity)
        on { getOrderStatusForSiteAndKey(any(), any()) }.thenReturn(mock())
    }
    private val networkStatus: NetworkStatus = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) }.thenReturn(ORDER_TOTAL)
    }
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(site) }.thenReturn(mock())
    }
    private val orderMapper: OrderMapper = mock {
        on { toAppModel(orderEntity) }.thenReturn(order)
    }
    private val cardPaymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock {
        onBlocking { isCollectable(order) }.thenReturn(false)
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val learnMoreUrlProvider: LearnMoreUrlProvider = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus = mock()
    private val appPrefs: AppPrefs = mock()

    @Test
    fun `given hub flow, when view model init, then navigate to hub flow emitted`() = testBlocking {
        // GIVEN & WHEN
        val viewModel = initViewModel(CardReadersHub())

        // THEN
        assertThat(viewModel.event.value).isEqualTo(NavigateToCardReaderHubFlow(CardReadersHub()))
    }

    @Test
    fun `given hub flow, when view model init, then loading state emitted`() = testBlocking {
        // GIVEN & WHEN
        val viewModel = initViewModel(CardReadersHub())

        // THEN
        assertThat(viewModel.viewStateData.value).isEqualTo(Loading)
    }

    @Test
    fun `given refund flow, when view model init, then loading state emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val refundAmount = BigDecimal(23)
        val viewModel = initViewModel(Refund(orderId, refundAmount))

        // THEN
        assertThat(viewModel.viewStateData.value).isEqualTo(Loading)
    }

    @Test
    fun `given payment flow, when view model init, then no events emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given payment flow, when view model init, then success state emitted`() = testBlocking {
        // GIVEN
        val orderId = 1L

        // WHEN
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(Success::class.java)
    }

    @Test
    fun `given payment flow and payment collectable, when view model init, then success emitted with collect true`() =
        testBlocking {
            // GIVEN & WHEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertTrue((viewModel.viewStateData.value as Success).isPaymentCollectableWithExternalCardReader)
        }

    @Test
    fun `given payment flow and ipp and ttp collectable, when view model init, then success emitted with ttp collectable true`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(tapToPayAvailabilityStatus()).thenReturn(TapToPayAvailabilityStatus.Result.Available)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertTrue((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given payment flow and ipp not collectable and ttp collectable, when view model init, then success emitted with ttp collectable false`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(false)
            whenever(tapToPayAvailabilityStatus()).thenReturn(TapToPayAvailabilityStatus.Result.Available)
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertFalse((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given payment flow and ipp collectable and ttp not collectable, when view model init, then success emitted with ttp collectable false`() =
        testBlocking {
            // GIVEN
            whenever(cardPaymentCollectibilityChecker.isCollectable(order)).thenReturn(true)
            whenever(tapToPayAvailabilityStatus()).thenReturn(
                TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
            )
            val orderId = 1L

            // WHEN
            val viewModel = initViewModel(Payment(orderId, ORDER))

            // THEN
            assertFalse((viewModel.viewStateData.value as Success).isPaymentCollectableWithTapToPay)
        }

    @Test
    fun `given refund flow, when view model init, then navigate to refund flow emitted`() = testBlocking {
        // GIVEN & WHEN
        val orderId = 1L
        val refundAmount = BigDecimal(23)
        val viewModel = initViewModel(Refund(orderId, refundAmount))

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            NavigateToCardReaderRefundFlow(
                Refund(orderId, refundAmount),
                CardReaderType.EXTERNAL
            )
        )
    }

    @Test
    fun `given order payment flow, when on cash payment clicked, then show dialog event emitted`() = testBlocking {
        // GIVEN
        val orderId = 1L
        val viewModel = initViewModel(Payment(orderId, ORDER))

        // WHEN
        viewModel.onCashPaymentClicked()

        // THEN
        val events = viewModel.event.captureValues()
        assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
        assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
        assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.existing_order_cash_dlg_message)
        assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(R.string.simple_payments_cash_dlg_button)
        assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
    }

    @Test
    fun `given simple payment flow, when on cash payment clicked, then show dialog event emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, SIMPLE))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            val events = viewModel.event.captureValues()
            assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
            assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
            assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.simple_payments_cash_dlg_message)
            assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(
                R.string.simple_payments_cash_dlg_button
            )
            assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given tap to pay test flow, when on cash payment clicked, then show dialog event emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val viewModel = initViewModel(Payment(orderId, TRY_TAP_TO_PAY))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            val events = viewModel.event.captureValues()
            assertThat(events.last()).isInstanceOf(ShowDialog::class.java)
            assertThat((events.last() as ShowDialog).titleId).isEqualTo(R.string.simple_payments_cash_dlg_title)
            assertThat((events.last() as ShowDialog).messageId).isEqualTo(R.string.simple_payments_cash_dlg_message)
            assertThat((events.last() as ShowDialog).positiveButtonId).isEqualTo(
                R.string.simple_payments_cash_dlg_button
            )
            assertThat((events.last() as ShowDialog).negativeButtonId).isEqualTo(R.string.cancel)
        }

    @Test
    fun `given order payment flow, when on cash payment clicked, then collect tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on cash payment clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCashPaymentClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on bt reader clicked, then collect tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                    "card_reader_type" to "external"
                )
            )
        }

    @Test
    fun `given simple payment flow, when on bt reader clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                    "card_reader_type" to "external"
                )
            )
        }

    @Test
    fun `when on bt reader clicked, then navigate to card reader payment flow`() =
        testBlocking {
            // GIVEN
            val cardReaderFlowParam = Payment(1L, SIMPLE)
            val viewModel = initViewModel(cardReaderFlowParam)

            // WHEN
            viewModel.onBtReaderClicked()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                NavigateToCardReaderPaymentFlow(
                    cardReaderFlowParam,
                    CardReaderType.EXTERNAL
                )
            )
        }

    @Test
    fun `when on tap too pay clicked, then navigate to card reader payment flow`() =
        testBlocking {
            // GIVEN
            val cardReaderFlowParam = Payment(1L, SIMPLE)
            val viewModel = initViewModel(cardReaderFlowParam)

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                NavigateToCardReaderPaymentFlow(
                    cardReaderFlowParam,
                    CardReaderType.BUILT_IN
                )
            )
        }

    @Test
    fun `given simple payments flow, when on tap to pay clicked, then collect tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    "payment_method" to "card",
                    "order_id" to 1L,
                    "flow" to "simple_payment",
                    "card_reader_type" to "built_in",
                )
            )
        }

    @Test
    fun `given order payments flow, when on tap to pay clicked, then collect tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    "payment_method" to "card",
                    "order_id" to 1L,
                    "flow" to "order_payment",
                    "card_reader_type" to "built_in",
                )
            )
        }

    @Test
    fun `when on tap too pay clicked, then app prefs stores ttp was used`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onTapToPayClicked()

            // THEN
            verify(appPrefs).setTTPWasUsedAtLeastOnce()
        }

    @Test
    fun `given simple payment flow, when on connect to reader result, then failed tracked with simple payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onConnectToReaderResultReceived(false)

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on connect to reader result, then failed tracked with order payment flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onConnectToReaderResultReceived(false)

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment compl success, then complected tracked with order flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_AMOUNT to "100$",
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on reader payment compl success, then completed tracked with simple flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_AMOUNT to "100$",
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on reader payment compl fail, then fail tracked with simple flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.FAILED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment compl fail, then fail tracked with order flow`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.FAILED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on reader payment complete, then exit to order list`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToOrderList)
        }

    @Test
    fun `given simple payment flow, when on reader payment complete, then exit to hub`() =
        testBlocking {
            // GIVEN
            whenever(orderEntity.status).thenReturn(CoreOrderStatus.COMPLETED.value)
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onCardReaderPaymentCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToHub(CardReadersHub()))
        }

    @Test
    fun `given order payment flow, when on share link clicked, then coll tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link clicked, then collect tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given tap to pay flow, when on share link clicked, then collect tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, TRY_TAP_TO_PAY))

            // WHEN
            viewModel.onSharePaymentUrlClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to "tap_to_pay_try_a_payment",
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link completed, then completed tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on share link completed, then completed tracked with order flow`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                mapOf(
                    AnalyticsTracker.KEY_PAYMENT_METHOD to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                    "order_id" to 1L,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given order payment flow, when on share link completed update fail, then fail tracked with order flow`() =
        testBlocking {
            // GIVEN
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(true)
            }
            val error = WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event)
            whenever(
                orderStore.updateOrderStatus(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(flowOf(error))
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on share link completed update fail, then fail tracked with simple flow`() =
        testBlocking {
            // GIVEN
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(true)
            }
            val error = WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event)
            whenever(
                orderStore.updateOrderStatus(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(flowOf(error))
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onSharePaymentUrlCompleted()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `given simple payment flow, when on back pressed, then nothing is tracked`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, SIMPLE))

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verify(analyticsTrackerWrapper, never()).track(eq(AnalyticsEvent.PAYMENTS_FLOW_CANCELED), any())
        }

    @Test
    fun `given order payment flow, when on back pressed, then flow cancelation is tracked`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel(Payment(1L, ORDER))

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
        }

    @Test
    fun `when learn more link clicked, then correct event is triggered`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))
        whenever(
            learnMoreUrlProvider.provideLearnMoreUrlFor(LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS)
        ).thenReturn(
            AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        )

        // WHEN
        (viewModel.viewStateData.value as Success).learMoreIpp.onClick.invoke()

        // THEN
        assertThat(viewModel.event.value).isInstanceOf(OpenGenericWebView::class.java)
    }

    @Test
    fun `when learn more clicked, then trigger proper event with correct url`() {
        // GIVEN
        val viewModel = initViewModel(Payment(1L, ORDER))
        whenever(
            learnMoreUrlProvider.provideLearnMoreUrlFor(LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS)
        ).thenReturn(
            AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS
        )

        // WHEN
        (viewModel.viewStateData.value as Success).learMoreIpp.onClick.invoke()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            OpenGenericWebView(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS)
        )
    }

    @Test
    fun `given payment flow and not connected and ttp system not supported, when vm init, then tracks ttp system`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPaySystemNotSupported = TapToPayAvailabilityStatus.Result.NotAvailable.SystemVersionNotSupported
            whenever(tapToPayAvailabilityStatus()).thenReturn(tapToPaySystemNotSupported)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPaySystemNotSupported, "payment_methods")
        }

    @Test
    fun `given payment flow and not connected and ttp country not supported, when vm init, then tracks ttp country`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPayCountryNotSupported = TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported
            whenever(tapToPayAvailabilityStatus()).thenReturn(tapToPayCountryNotSupported)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayCountryNotSupported, "payment_methods")
        }

    @Test
    fun `given payment flow ttp gps not available, when vm init, then tracks ttp gps`() =
        testBlocking {
            // GIVEN
            val tapToPayGpsNotAvailable = TapToPayAvailabilityStatus.Result.NotAvailable.GooglePlayServicesNotAvailable
            whenever(tapToPayAvailabilityStatus()).thenReturn(tapToPayGpsNotAvailable)
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayGpsNotAvailable, "payment_methods")
        }

    @Test
    fun `given payment flow and not connected and ttp nfc not available, when vm init, then tracks ttp nfc`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val tapToPayNfcNotAvailable = TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
            whenever(tapToPayAvailabilityStatus()).thenReturn(tapToPayNfcNotAvailable)

            // WHEN
            initViewModel(param)

            // THEN
            verify(cardReaderTracker).trackTapToPayNotAvailableReason(tapToPayNfcNotAvailable, "payment_methods")
        }

    @Test
    fun `given paymentUrl not empty, when vm init, then true returned`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            whenever(order.paymentUrl).thenReturn(PAYMENT_URL)

            // WHEN
            val viewModel = initViewModel(param)

            // THEN
            assertThat((viewModel.viewStateData.value as Success).isScanToPayAvailable).isTrue()
        }

    @Test
    fun `given paymentUrl empty, when vm init, then false returned`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            whenever(order.paymentUrl).thenReturn("")

            // WHEN
            val viewModel = initViewModel(param)

            // THEN
            assertThat((viewModel.viewStateData.value as Success).isScanToPayAvailable).isFalse()
        }

    @Test
    fun `given update order status success, when onScanToPayClicked, then SharePaymentUrlViaQr emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val viewModel = initViewModel(param)
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(false)
            }
            whenever(orderStore.updateOrderStatus(eq(orderId), any(), any())).thenReturn(
                flowOf(WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event))
            )

            // WHEN
            val stateValues = viewModel.viewStateData.captureValues()
            viewModel.onScanToPayClicked()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                SharePaymentUrlViaQr(PAYMENT_URL)
            )
            assertThat(stateValues[0]).isInstanceOf(Success::class.java)
            assertThat(stateValues[1]).isEqualTo(Loading)
            assertThat(stateValues[2]).isInstanceOf(Success::class.java)
        }

    @Test
    fun `given update order status error, when onScanToPayClicked, then error tracked and ShowSnackbar emitted`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val viewModel = initViewModel(param)
            val event = mock<OnOrderChanged> {
                on { isError }.thenReturn(true)
            }
            whenever(orderStore.updateOrderStatus(eq(orderId), any(), any())).thenReturn(
                flowOf(WCOrderStore.UpdateOrderResult.RemoteUpdateResult(event))
            )

            // WHEN
            viewModel.onScanToPayClicked()

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to
                        AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW,
                )
            )
            assertThat(viewModel.event.value).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(R.string.order_error_update_general)
            )
        }

    @Test
    fun `when onScanToPayClicked, then collect tracked`() {
        // GIVEN
        val orderId = 1L
        val param = Payment(orderId = orderId, paymentType = ORDER)
        val viewModel = initViewModel(param)

        // WHEN
        viewModel.onScanToPayClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
            mapOf(
                "payment_method" to "scan_to_pay",
                "order_id" to 1L,
                "flow" to "order_payment",
            )
        )
    }

    @Test
    fun `given order payment type, when onScanToPayCompleted, then NavigateBackToOrderList`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = ORDER)
            val viewModel = initViewModel(param)

            // WHEN
            viewModel.onScanToPayCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToOrderList)
        }

    @Test
    fun `when onScanToPayCompleted, then completed tracked`() {
        // GIVEN
        val orderId = 1L
        val param = Payment(orderId = orderId, paymentType = ORDER)
        val viewModel = initViewModel(param)

        // WHEN
        viewModel.onScanToPayCompleted()

        // THEN
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
            mapOf(
                "payment_method" to "scan_to_pay",
                "order_id" to 1L,
                "flow" to "order_payment",
            )
        )
    }

    @Test
    fun `given simple payment type, when onScanToPayCompleted, then NavigateBackToHub`() =
        testBlocking {
            // GIVEN
            val orderId = 1L
            val param = Payment(orderId = orderId, paymentType = SIMPLE)
            val viewModel = initViewModel(param)

            // WHEN
            viewModel.onScanToPayCompleted()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(NavigateBackToHub(CardReadersHub()))
        }

    private fun initViewModel(cardReaderFlowParam: CardReaderFlowParam): SelectPaymentMethodViewModel {
        return SelectPaymentMethodViewModel(
            SelectPaymentMethodFragmentArgs(cardReaderFlowParam = cardReaderFlowParam).initSavedStateHandle(),
            selectedSite,
            orderStore,
            coroutinesTestRule.testDispatchers,
            networkStatus,
            currencyFormatter,
            wooCommerceStore,
            orderMapper,
            analyticsTrackerWrapper,
            cardPaymentCollectibilityChecker,
            learnMoreUrlProvider,
            cardReaderTracker,
            tapToPayAvailabilityStatus,
            appPrefs,
        )
    }
}
