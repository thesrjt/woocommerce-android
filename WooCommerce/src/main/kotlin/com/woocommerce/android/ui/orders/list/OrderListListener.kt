package com.woocommerce.android.ui.orders.list

import android.view.View

interface OrderListListener {
    fun openOrderDetail(
        orderId: Long,
        allOrderIds: List<Long>,
        orderStatus: String,
        sharedView: View? = null
    )
}
