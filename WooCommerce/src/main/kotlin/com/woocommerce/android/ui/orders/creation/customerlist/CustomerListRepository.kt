package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCCustomerStore
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class CustomerListRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val customerStore: WCCustomerStore,
    private val dataStore: WCDataStore
) {
    private var countries: List<Location> = emptyList()

    /**
     * Ensure country/state data has been fetched
     */
    suspend fun loadCountries() {
        countries = dataStore.getCountries().map { it.toAppModel() }
        if (countries.isEmpty()) {
            dataStore.fetchCountriesAndStates(selectedSite.get()).model?.let {
                countries = it.map { it.toAppModel() }
            }
        }
    }

    fun getCountry(countryCode: String) =
        countries.find { it.code == countryCode }
            ?: Location.EMPTY

    fun getState(countryCode: String, stateCode: String) =
        dataStore.getStates(countryCode)
            .find { it.code == stateCode }?.toAppModel()
            ?: Location.EMPTY

    /**
     * Submits a fetch request to get the first page of customers matching the passed query
     */
    suspend fun searchCustomerList(
        searchQuery: String,
    ): List<WCCustomerModel>? =
        customerStore.fetchCustomersFromAnalytics(
            site = selectedSite.get(),
            searchQuery = searchQuery,
            pageSize = 50,
            page = 1,
        ).takeUnless { it.isError }?.model

    suspend fun fetchCustomerByRemoteId(remoteId: Long): WooResult<WCCustomerModel> {
        return customerStore.fetchSingleCustomer(selectedSite.get(), remoteId)
    }

    fun getCustomerByRemoteId(remoteId: Long): WCCustomerModel? {
        return customerStore.getCustomerByRemoteId(selectedSite.get(), remoteId)
    }
}
