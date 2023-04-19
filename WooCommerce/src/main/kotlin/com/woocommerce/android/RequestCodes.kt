package com.woocommerce.android

/**
 * Global intent identifiers
 */
object RequestCodes {
    private const val BASE_REQUEST_CODE = 100

    const val ADD_ACCOUNT = BASE_REQUEST_CODE + 0
    const val SETTINGS = BASE_REQUEST_CODE + 1
    const val IN_APP_UPDATE = BASE_REQUEST_CODE + 3

    const val PRODUCT_INVENTORY_BACKORDERS = BASE_REQUEST_CODE + 301
    const val PRODUCT_INVENTORY_STOCK_STATUS = BASE_REQUEST_CODE + 302
    const val PRODUCT_TAX_STATUS = BASE_REQUEST_CODE + 304
    const val PRODUCT_TAX_CLASS = BASE_REQUEST_CODE + 305

    const val AZTEC_EDITOR_PRODUCT_DESCRIPTION = BASE_REQUEST_CODE + 400
    const val AZTEC_EDITOR_PRODUCT_SHORT_DESCRIPTION = BASE_REQUEST_CODE + 401

    const val PRODUCT_DETAIL_PRICING = BASE_REQUEST_CODE + 800
    const val VARIATION_DETAIL_PRICING = BASE_REQUEST_CODE + 900

    const val PRODUCT_DETAIL_INVENTORY = BASE_REQUEST_CODE + 1000
    const val VARIATION_DETAIL_INVENTORY = BASE_REQUEST_CODE + 1100

    const val PRODUCT_DETAIL_SHIPPING = BASE_REQUEST_CODE + 1200
    const val VARIATION_DETAIL_SHIPPING = BASE_REQUEST_CODE + 1300

    const val PRODUCT_DETAIL_IMAGES = BASE_REQUEST_CODE + 1400
    const val VARIATION_DETAIL_IMAGE = BASE_REQUEST_CODE + 1500
}
