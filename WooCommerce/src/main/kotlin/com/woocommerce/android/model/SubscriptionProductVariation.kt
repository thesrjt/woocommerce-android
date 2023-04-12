package com.woocommerce.android.model

import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.parseFromIso8601DateFormat
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Suppress("LongParameterList")
@Parcelize
class SubscriptionProductVariation(
    val subscriptionDetails: SubscriptionDetails?,
    override val remoteProductId: Long,
    override val remoteVariationId: Long,
    override val sku: String,
    override val image: Product.Image?,
    override val price: BigDecimal?,
    override val regularPrice: BigDecimal?,
    override val salePrice: BigDecimal?,
    override val saleEndDateGmt: Date?,
    override val saleStartDateGmt: Date?,
    override val isSaleScheduled: Boolean,
    override val stockStatus: ProductStockStatus,
    override val backorderStatus: ProductBackorderStatus,
    override val stockQuantity: Double,
    override var priceWithCurrency: String? = null,
    override val isPurchasable: Boolean,
    override val isVirtual: Boolean,
    override val isDownloadable: Boolean,
    override val isStockManaged: Boolean,
    override val description: String,
    override val isVisible: Boolean,
    override val shippingClass: String,
    override val shippingClassId: Long,
    override val menuOrder: Int,
    override val attributes: Array<VariantOption>,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : ProductVariation(
    remoteProductId = remoteProductId,
    remoteVariationId = remoteVariationId,
    sku = sku,
    image = image,
    price = price,
    regularPrice = regularPrice,
    salePrice = salePrice,
    saleEndDateGmt = saleEndDateGmt,
    saleStartDateGmt = saleStartDateGmt,
    isSaleScheduled = isSaleScheduled,
    stockStatus = stockStatus,
    backorderStatus = backorderStatus,
    stockQuantity = stockQuantity,
    priceWithCurrency = priceWithCurrency,
    isPurchasable = isPurchasable,
    isVirtual = isVirtual,
    isDownloadable = isDownloadable,
    isStockManaged = isStockManaged,
    description = description,
    isVisible = isVisible,
    shippingClass = shippingClass,
    shippingClassId = shippingClassId,
    menuOrder = menuOrder,
    attributes = attributes,
    length = length,
    width = width,
    height = height,
    weight = weight
) {
    constructor(model: WCProductVariationModel) :
        this(
            subscriptionDetails = model.metadata?.let { SubscriptionDetailsMapper.toAppModel(it) },
            remoteProductId = model.remoteProductId,
            remoteVariationId = model.remoteVariationId,
            sku = model.sku,
            image = model.getImageModel()?.let {
                Product.Image(
                    it.id,
                    it.name,
                    it.src,
                    DateTimeUtils.dateFromIso8601(model.dateCreated) ?: Date()
                )
            },
            price = model.price.toBigDecimalOrNull(),
            regularPrice = model.regularPrice.toBigDecimalOrNull(),
            salePrice = model.salePrice.toBigDecimalOrNull(),
            saleEndDateGmt = model.dateOnSaleToGmt.parseFromIso8601DateFormat(),
            saleStartDateGmt = model.dateOnSaleFromGmt.parseFromIso8601DateFormat(),
            isSaleScheduled = model.dateOnSaleFromGmt.isNotEmpty() || model.dateOnSaleToGmt.isNotEmpty(),
            stockStatus = ProductStockStatus.fromString(model.stockStatus),
            backorderStatus = ProductBackorderStatus.fromString(model.backorders),
            stockQuantity = model.stockQuantity,
            isPurchasable = model.purchasable,
            isVirtual = model.virtual,
            isDownloadable = model.downloadable,
            isStockManaged = model.manageStock,
            description = model.description.fastStripHtml(),
            isVisible = ProductStatus.fromString(model.status) == ProductStatus.PUBLISH,
            shippingClass = model.shippingClass,
            shippingClassId = model.shippingClassId.toLong(),
            menuOrder = model.menuOrder,
            attributes = model.attributeList
                ?.map { VariantOption(it) }
                ?.toTypedArray()
                ?: emptyArray(),
            length = model.length.toFloatOrNull() ?: 0f,
            width = model.width.toFloatOrNull() ?: 0f,
            height = model.height.toFloatOrNull() ?: 0f,
            weight = model.weight.toFloatOrNull() ?: 0f
        )

    override fun copy(
        remoteProductId: Long,
        remoteVariationId: Long,
        sku: String,
        image: Product.Image?,
        price: BigDecimal?,
        regularPrice: BigDecimal?,
        salePrice: BigDecimal?,
        saleEndDateGmt: Date?,
        saleStartDateGmt: Date?,
        isSaleScheduled: Boolean,
        stockStatus: ProductStockStatus,
        backorderStatus: ProductBackorderStatus,
        stockQuantity: Double,
        priceWithCurrency: String?,
        isPurchasable: Boolean,
        isVirtual: Boolean,
        isDownloadable: Boolean,
        isStockManaged: Boolean,
        description: String,
        isVisible: Boolean,
        shippingClass: String,
        shippingClassId: Long,
        menuOrder: Int,
        attributes: Array<VariantOption>,
        length: Float,
        width: Float,
        height: Float,
        weight: Float
    ): ProductVariation {
        return SubscriptionProductVariation(
            remoteProductId = remoteProductId,
            remoteVariationId = remoteVariationId,
            sku = sku,
            image = image,
            price = price,
            regularPrice = regularPrice,
            salePrice = salePrice,
            saleEndDateGmt = saleEndDateGmt,
            saleStartDateGmt = saleStartDateGmt,
            isSaleScheduled = isSaleScheduled,
            stockStatus = stockStatus,
            backorderStatus = backorderStatus,
            stockQuantity = stockQuantity,
            priceWithCurrency = priceWithCurrency,
            isPurchasable = isPurchasable,
            isVirtual = isVirtual,
            isDownloadable = isDownloadable,
            isStockManaged = isStockManaged,
            description = description,
            isVisible = isVisible,
            shippingClass = shippingClass,
            shippingClassId = shippingClassId,
            menuOrder = menuOrder,
            attributes = attributes,
            length = length,
            width = width,
            height = height,
            weight = weight,
            subscriptionDetails = subscriptionDetails
        )
    }
}