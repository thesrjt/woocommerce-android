package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class CustomerListViewModelTest : BaseUnitTest() {
    private val customerListRepository: CustomerListRepository = mock {
        onBlocking {
            searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any()
            )
        }.thenReturn(Result.success(listOf(mock())))
    }
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val mockCustomer: CustomerListViewState.CustomerList.Item.Customer = mock()
    private val customerListViewModelMapper: CustomerListViewModelMapper = mock {
        on { mapFromWCCustomerToItem(any()) }.thenReturn(mockCustomer)
    }
    private val getSupportedSearchModes: CustomerListGetSupportedSearchModes = mock {
        onBlocking { invoke(false) }.thenReturn(
            listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = false,
                ),
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_email,
                    searchParam = "email",
                    isSelected = false,
                )
            )
        )
    }
    private val isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported = mock {
        onBlocking { invoke() }.thenReturn(false)
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    @Test
    fun `when viewmodel init, then viewstate is updated with customers`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
    }

    @Test
    fun `when viewmodel init, then viewstate is updated with search modes`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()

        // THEN
        assertThat(states.last().searchModes[0]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = true,
            )
        )
        assertThat(states.last().searchModes[1]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
                isSelected = false,
            )
        )
    }

    @Test
    fun `given success returned from repo, when viewmodel init, then viewstate is updated with customers and first page true`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success((1..30).map { mock() }))
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()

            // THEN
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertThat(
                (states.last().body as CustomerListViewState.CustomerList.Loaded).shouldResetScrollPosition
            ).isTrue()
        }

    @Test
    fun `given page number 1, when viewmodel init, then viewstate is updated to Loading state`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()

        // THEN
        assertThat(states.first().body).isInstanceOf(CustomerListViewState.CustomerList.Loading::class.java)
    }

    @Test
    fun `given error from repo, when viewmodel init, then viewstate is updated with error state`() = testBlocking {
        // GIVEN
        whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
            .thenReturn(Result.failure(Throwable()))
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Error::class.java)
    }

    @Test
    fun `given empty list from repo, when viewmodel init, then viewstate is updated with empty state`() = testBlocking {
        // GIVEN
        whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
            .thenReturn(Result.success(emptyList()))
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Empty::class.java)
    }

    @Test
    fun `given search query, when onSearchQueryChanged is called, then update search query is updated and tracked`() =
        testBlocking {
            // GIVEN
            val searchQuery = "customer"
            val viewModel = initViewModel()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)

            // THEN
            assertThat(viewModel.viewState.value?.searchQuery).isEqualTo(searchQuery)
        }

    @Test
    fun `when onSearchQueryChanged is called, then tracked with all`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchQueryChanged("customer")

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "all"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with email, then tracked with email`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_email)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "email"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with name, then tracked with name`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_name)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "name"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with username, then tracked with username`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_username)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "username"
            )
        )
    }

    @Test
    fun `given search query, when onSearchQueryChanged is called, then search is invoked and viewstate updated with customers`() =
        testBlocking {
            // GIVEN
            val searchQuery = "customer"
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)
            advanceUntilIdle()

            // THEN
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers)
                .isEqualTo(listOf(mockCustomer))
        }

    @Test
    fun `given search type, when onSearchTypeChanged is called, then selected mode is changed`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            // WHEN
            viewModel.onSearchTypeChanged(searchTypeId)

            // THEN
            assertThat(viewModel.viewState.value?.searchModes?.find { it.isSelected }?.labelResId).isEqualTo(
                searchTypeId
            )
            assertThat(viewModel.viewState.value?.searchModes?.find { it.isSelected }?.labelResId).isNotEqualTo(
                R.string.order_creation_customer_search_name
            )
        }

    @Test
    fun `given search type and empty search, when onSearchTypeChanged is called, then new customers are not loaded`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchTypeChanged(searchTypeId)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any()
            )
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
        }

    @Test
    fun `given search type and search is not empty, when onSearchTypeChanged is called, then job canceled and new customers are loaded`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged("customer")
            viewModel.onSearchTypeChanged(searchTypeId)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any()
            )
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
        }

    @Test
    fun `given less than page returned from repo, when onEndOfListReached, then next call doesnt load more customers`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any()
            )
        }

    @Test
    fun `given that one page size returned from repo, when onEndOfListReached, then next call load more customers`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success((1..30).map { mock() }))
            val viewModel = initViewModel()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any()
            )
        }

    @Test
    fun `given that one page size returned from repo, when onEndOfListReached, then loading item appended`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success((1..30).map { mock() }))
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()

            // WHEN
            viewModel.onEndOfListReached()

            // THEN
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers.last())
                .isInstanceOf(CustomerListViewState.CustomerList.Item.Loading::class.java)
            assertThat(
                (states.last().body as CustomerListViewState.CustomerList.Loaded).shouldResetScrollPosition
            ).isFalse()
        }

    @Test
    fun `given customer with remote id and fetching success, when onCustomerSelected, then customer fetched by id and passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(2L)
            }
            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(returnedWcCustomer))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(any()))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(any()))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    customerId = 2L,
                    billingAddress = address,
                    shippingAddress = address,
                )
            )
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id, when onCustomerSelected, then partialLoading state emitted`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(2L)
            }
            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(returnedWcCustomer))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(any()))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(any()))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            assertThat(states[0].partialLoading).isFalse()
            assertThat(states[1].partialLoading).isTrue()
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id and fetching null, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
            }

            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(null))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    customerId = 1L,
                    billingAddress = address,
                    shippingAddress = address,
                )
            )
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id and fetching error, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
            }

            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(
                    WooResult(
                        WooError(
                            WooErrorType.GENERIC_ERROR,
                            BaseRequest.GenericErrorType.NETWORK_ERROR
                        )
                    )
                )

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    customerId = 1L,
                    billingAddress = address,
                    shippingAddress = address,
                )
            )
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer without remote id, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(0L)
            }

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, never()).fetchCustomerByRemoteId(any())
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    customerId = 0L,
                    billingAddress = address,
                    shippingAddress = address,
                )
            )
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `when onAddCustomerClicked, then AddCustomer event triggered`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onAddCustomerClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(AddCustomer)
    }

    @Test
    fun `when onNavigateBack, then exit emitted`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onNavigateBack()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    private fun initViewModel() = CustomerListViewModel(
        savedState,
        customerListRepository,
        customerListViewModelMapper,
        isAdvancedSearchSupported,
        getSupportedSearchModes,
        analyticsTrackerWrapper,
    )
}
