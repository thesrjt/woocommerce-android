package com.woocommerce.android.util

import android.content.Context

/**
 * "Feature flags" are used to hide in-progress features from release versions
 */
enum class FeatureFlag {
    DB_DOWNGRADE,
    JETPACK_CP,
    MORE_MENU_INBOX,
    COUPONS_M2,
    WC_SHIPPING_BANNER,
    UNIFIED_ORDER_EDITING,
    NATIVE_STORE_CREATION_FLOW,
    IAP_FOR_STORE_CREATION,
    IPP_UK,
    STORE_CREATION_ONBOARDING,
    FREE_TRIAL_M2,
    REST_API_I2,
    ANALYTICS_HUB_FEEDBACK_BANNER,
    GIFT_CARD_READ_ONLY_SUPPORT,
    QUANTITY_RULES_READ_ONLY_SUPPORT,
    BUNDLED_PRODUCTS_READ_ONLY_SUPPORT,
    COMPOSITE_PRODUCTS_READ_ONLY_SUPPORT,
    STORE_CREATION_PROFILER,
    EU_SHIPPING_NOTIFICATION,
    PRIVACY_CHOICES,
    SHARING_PRODUCT_AI,
    BLAZE,
    PRODUCT_DESCRIPTION_AI_GENERATOR,
    ORDER_CREATION_PRODUCT_DISCOUNTS,
    SHIPPING_ZONES;

    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }

            COUPONS_M2,
            JETPACK_CP,
            UNIFIED_ORDER_EDITING,
            NATIVE_STORE_CREATION_FLOW,
            FREE_TRIAL_M2,
            STORE_CREATION_ONBOARDING,
            REST_API_I2,
            GIFT_CARD_READ_ONLY_SUPPORT,
            QUANTITY_RULES_READ_ONLY_SUPPORT,
            BUNDLED_PRODUCTS_READ_ONLY_SUPPORT,
            IPP_UK,
            ANALYTICS_HUB_FEEDBACK_BANNER,
            STORE_CREATION_PROFILER,
            COMPOSITE_PRODUCTS_READ_ONLY_SUPPORT,
            EU_SHIPPING_NOTIFICATION,
            PRIVACY_CHOICES,
            BLAZE,
            SHARING_PRODUCT_AI,
            PRODUCT_DESCRIPTION_AI_GENERATOR,
            ORDER_CREATION_PRODUCT_DISCOUNTS -> true

            MORE_MENU_INBOX,
            WC_SHIPPING_BANNER,
            SHIPPING_ZONES -> PackageUtils.isDebugBuild()

            IAP_FOR_STORE_CREATION -> false
        }
    }
}
