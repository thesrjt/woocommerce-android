package com.woocommerce.android.ui.orders.creation.customerlistnew

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

data class CustomerListViewState(
    val searchQuery: String = "",
    val searchModes: List<SearchMode>,
    val body: CustomerList = CustomerList.Loading,
) {
    sealed class CustomerList {
        object Loading : CustomerList()
        object Empty : CustomerList()
        object Error : CustomerList()
        data class Loaded(val customers: List<Item>) : CustomerList()

        sealed class Item {
            data class Customer(
                val remoteId: Long,
                val firstName: String,
                val lastName: String,
                val email: String,
            ) : Item()

            object Loading : Item()
        }
    }
}

@Parcelize
data class SearchMode(
    @StringRes val labelResId: Int,
    val searchParam: String,
    val isSelected: Boolean,
) : Parcelable

data class PaginationState(
    val currentPage: Int,
    val hasNextPage: Boolean,
)
