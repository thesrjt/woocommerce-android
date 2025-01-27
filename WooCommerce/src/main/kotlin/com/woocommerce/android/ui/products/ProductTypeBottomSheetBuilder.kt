package com.woocommerce.android.ui.products

import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.SUBSCRIPTION
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE_SUBSCRIPTION
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import javax.inject.Inject

@Suppress("ForbiddenComment")
class ProductTypeBottomSheetBuilder @Inject constructor(
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions
) {
    suspend fun buildBottomSheetList(): List<ProductTypesBottomSheetUiItem> {
        val areSubscriptionsSupported = isEligibleForSubscriptions()
        return listOf(
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = string.product_type_simple_title,
                descResource = string.product_type_simple_desc,
                iconResource = drawable.ic_gridicons_product
            ),
            ProductTypesBottomSheetUiItem(
                type = SIMPLE,
                titleResource = string.product_type_virtual_title,
                descResource = string.product_type_virtual_desc,
                iconResource = drawable.ic_gridicons_cloud_outline,
                isVirtual = true
            ),
            ProductTypesBottomSheetUiItem(
                type = SUBSCRIPTION,
                titleResource = string.product_type_simple_subscription_title,
                descResource = string.product_type_simple_subscription_desc,
                iconResource = drawable.ic_event_repeat,
                isVisible = areSubscriptionsSupported
            ),
            ProductTypesBottomSheetUiItem(
                type = VARIABLE,
                titleResource = string.product_type_variable_title,
                descResource = string.product_type_variable_desc,
                iconResource = drawable.ic_gridicons_types,
            ),

            ProductTypesBottomSheetUiItem(
                type = VARIABLE_SUBSCRIPTION,
                titleResource = string.product_type_variable_subscription_title,
                descResource = string.product_type_variable_subscription_desc,
                iconResource = drawable.ic_event_repeat,
                isVisible = areSubscriptionsSupported
            ),
            ProductTypesBottomSheetUiItem(
                type = GROUPED,
                titleResource = string.product_type_grouped_title,
                descResource = string.product_type_grouped_desc,
                iconResource = drawable.ic_widgets
            ),
            ProductTypesBottomSheetUiItem(
                type = EXTERNAL,
                titleResource = string.product_type_external_title,
                descResource = string.product_type_external_desc,
                iconResource = drawable.ic_gridicons_up_right
            )
        )
    }
}
