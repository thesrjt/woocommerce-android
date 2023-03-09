package com.woocommerce.android.support

import android.content.Context
import android.net.ConnectivityManager
import android.os.Parcelable
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.extensions.stateLogInformation
import com.woocommerce.android.support.RequestConstants.requestCreationIdentityNotSetErrorMessage
import com.woocommerce.android.support.RequestConstants.requestCreationTimeoutErrorMessage
import com.woocommerce.android.support.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.ZendeskException.RequestCreationTimeoutException
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.Support
import java.util.Locale

class ZendeskManager(
    private val zendeskSettings: ZendeskSettings,
    private val siteStore: SiteStore,
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * This function creates a new customer Support Request through the Zendesk API Providers.
     */
    @Suppress("LongParameterList")
    suspend fun createRequest(
        context: Context,
        origin: HelpOrigin,
        ticketType: TicketType,
        selectedSite: SiteModel?,
        subject: String,
        description: String,
        extraTags: List<String>
    ) = callbackFlow {
        if (zendeskSettings.isIdentitySet.not()) {
            trySend(Result.failure(IdentityNotSetException))
            close()
            return@callbackFlow
        }

        val requestCallback = object : ZendeskCallback<Request>() {
            override fun onSuccess(result: Request?) {
                trySend(Result.success(result))
                close()
            }

            override fun onError(error: ErrorResponse) {
                trySend(Result.failure(Throwable(error.reason)))
                close()
            }
        }

        CreateRequest().apply {
            this.ticketFormId = ticketType.form
            this.subject = subject
            this.description = description
            this.tags = buildZendeskTags(siteStore.sites, origin, ticketType.tags + extraTags)
                .filter { ticketType.excludedTags.contains(it).not() }
            this.customFields = buildZendeskCustomFields(context, ticketType, siteStore.sites, selectedSite)
        }.let { request -> zendeskSettings.requestProvider?.createRequest(request, requestCallback) }

        // Sets a timeout since the callback might not be called from Zendesk API
        launch {
            delay(RequestConstants.requestCreationTimeout)
            trySend(Result.failure(RequestCreationTimeoutException))
            close()
        }

        awaitClose()
    }.flowOn(dispatchers.io)

    /**
     * This function refreshes the Zendesk's request activity if it's currently being displayed. It'll return true if
     * it's successful. We'll use the return value to decide whether to show a push notification or not.
     */
    fun refreshRequest(context: Context, requestId: String?): Boolean =
        Support.INSTANCE.refreshRequest(requestId, context)

    private fun getHomeURLOrHostName(site: SiteModel): String {
        var homeURL = UrlUtils.removeScheme(site.url)
        homeURL = StringUtils.removeTrailingSlash(homeURL)
        return if (TextUtils.isEmpty(homeURL)) {
            UrlUtils.getHost(site.xmlRpcUrl)
        } else homeURL
    }

    /**
     * This is a helper function which builds a list of `CustomField`s which will be used during ticket creation. They
     * will be used to fill the custom fields we have setup in Zendesk UI for Happiness Engineers.
     */
    private fun buildZendeskCustomFields(
        context: Context,
        ticketType: TicketType,
        allSites: List<SiteModel>?,
        selectedSite: SiteModel?,
        ssr: String? = null
    ): List<CustomField> {
        val currentSiteInformation = if (selectedSite != null) {
            "${getHomeURLOrHostName(selectedSite)} (${selectedSite.stateLogInformation})"
        } else {
            "not_selected"
        }
        return listOf(
            CustomField(TicketFieldIds.appVersion, PackageUtils.getVersionName(context)),
            CustomField(TicketFieldIds.deviceFreeSpace, DeviceUtils.getTotalAvailableMemorySize()),
            CustomField(TicketFieldIds.networkInformation, getNetworkInformation(context)),
            CustomField(TicketFieldIds.logs, WooLog.toString().takeLast(ZendeskConstants.maxLogfileLength)),
            CustomField(TicketFieldIds.ssr, ssr),
            CustomField(TicketFieldIds.currentSite, currentSiteInformation),
            CustomField(TicketFieldIds.sourcePlatform, ZendeskConstants.sourcePlatform),
            CustomField(TicketFieldIds.appLanguage, Locale.getDefault().language),
            CustomField(TicketFieldIds.categoryId, ticketType.categoryName),
            CustomField(TicketFieldIds.subcategoryId, ticketType.subcategoryName),
            CustomField(TicketFieldIds.blogList, getCombinedLogInformationOfSites(allSites))
        )
    }

    /**
     * This is a small helper function which just joins the `logInformation` of all the sites passed in with a separator.
     */
    private fun getCombinedLogInformationOfSites(allSites: List<SiteModel>?): String {
        allSites?.let { it ->
            return it.joinToString(separator = ZendeskConstants.blogSeparator) { it.logInformation }
        }
        return ZendeskConstants.noneValue
    }

    /**
     * This is a helper function which returns a set of pre-defined tags depending on some conditions. It accepts a list of
     * custom tags to be added for special cases.
     */
    private fun buildZendeskTags(
        allSites: List<SiteModel>?,
        origin: HelpOrigin,
        extraTags: List<String>
    ): List<String> {
        val tags = ArrayList<String>()
        allSites?.let { it ->
            // Add wpcom tag if at least one site is WordPress.com site
            if (it.any { it.isWPCom }) {
                tags.add(ZendeskConstants.wpComTag)
            }

            // Add Jetpack tag if at least one site is Jetpack connected. Even if a site is Jetpack connected,
            // it does not necessarily mean that user is connected with the REST API, but we don't care about that here
            if (it.any { it.isJetpackConnected }) {
                tags.add(ZendeskConstants.jetpackTag)
            }

            // Find distinct plans and add them
            val plans = it.asSequence().mapNotNull { it.planShortName }.distinct().toList()
            tags.addAll(plans)
        }
        tags.add(origin.toString())
        // Add Android tag to make it easier to filter tickets by platform
        tags.add(ZendeskConstants.platformTag)
        tags.addAll(extraTags)
        return tags
    }

    /**
     * This is a helper function which returns information about the network state of the app to be sent to Zendesk, which
     * could prove useful for the Happiness Engineers while debugging the users' issues.
     */
    @Suppress("DEPRECATION")
    private fun getNetworkInformation(context: Context): String {
        val networkType = when (NetworkUtils.getActiveNetworkInfo(context)?.type) {
            ConnectivityManager.TYPE_WIFI -> ZendeskConstants.networkWifi
            ConnectivityManager.TYPE_MOBILE -> ZendeskConstants.networkWWAN
            else -> ZendeskConstants.unknownValue
        }
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val carrierName = telephonyManager?.networkOperatorName ?: ZendeskConstants.unknownValue
        val countryCodeLabel = telephonyManager?.networkCountryIso ?: ZendeskConstants.unknownValue
        return listOf(
            "${ZendeskConstants.networkTypeLabel} $networkType",
            "${ZendeskConstants.networkCarrierLabel} $carrierName",
            "${ZendeskConstants.networkCountryCodeLabel} ${countryCodeLabel.uppercase(Locale.getDefault())}"
        ).joinToString(separator = "\n")
    }
}

sealed class TicketType(
    val form: Long,
    val categoryName: String,
    val subcategoryName: String,
    val tags: List<String> = emptyList(),
    val excludedTags: List<String> = emptyList()
) : Parcelable {
    @Parcelize object MobileApp : TicketType(
        form = TicketFieldIds.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        tags = listOf(ZendeskTags.mobileApp)
    )
    @Parcelize object InPersonPayments : TicketType(
        form = TicketFieldIds.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        tags = listOf(
            ZendeskTags.woocommerceMobileApps,
            ZendeskTags.productAreaAppsInPersonPayments
        )
    )
    @Parcelize object Payments : TicketType(
        form = TicketFieldIds.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.paymentsSubcategoryValue,
        tags = listOf(
            ZendeskTags.paymentsProduct,
            ZendeskTags.paymentsProductArea,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.paymentSubcategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )
    @Parcelize object WooPlugin : TicketType(
        form = TicketFieldIds.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = "",
        tags = listOf(
            ZendeskTags.woocommerceCore,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )
    @Parcelize object OtherPlugins : TicketType(
        form = TicketFieldIds.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.storeSubcategoryValue,
        tags = listOf(
            ZendeskTags.productAreaWooExtensions,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.storeSubcategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )
}

private object ZendeskConstants {
    const val blogSeparator = "\n----------\n"
    const val jetpackTag = "jetpack"
    const val supportCategory = "Support"
    const val mobileAppCategory = "Mobile App"
    const val mobileSubcategoryValue = "WooCommerce Mobile Apps"
    const val paymentsSubcategoryValue = "Payment"
    const val storeSubcategoryValue = "Store"
    const val networkWifi = "WiFi"
    const val networkWWAN = "Mobile"
    const val networkTypeLabel = "Network Type:"
    const val networkCarrierLabel = "Carrier:"
    const val networkCountryCodeLabel = "Country Code:"
    const val noneValue = "none"

    // We rely on this platform tag to filter tickets in Zendesk, so should be kept separate from the `articleLabel`
    const val platformTag = "Android"
    const val sourcePlatform = "Mobile_-_Woo_Android"
    const val wpComTag = "wpcom"
    const val unknownValue = "unknown"

    const val maxLogfileLength: Int = 63000 // Max characters allowed in the system status report field
}

private object TicketFieldIds {
    const val appVersion = 360000086866L
    const val blogList = 360000087183L
    const val deviceFreeSpace = 360000089123L
    const val wooMobileFormID = 360000010286L
    const val wooFormID = 189946L
    const val categoryId = 25176003L
    const val subcategoryId = 25176023L
    const val logs = 10901699622036L
    // SSR refers to WooCommerce System Status Report
    const val ssr = 22871957L
    const val networkInformation = 360000086966L
    const val currentSite = 360000103103L
    const val appLanguage = 360008583691L
    const val sourcePlatform = 360009311651L
}

object ZendeskTags {
    const val mobileApp = "mobile_app"
    const val woocommerceCore = "woocommerce_core"
    const val paymentsProduct = "woocommerce_payments"
    const val paymentsProductArea = "product_area_woo_payment_gateway"
    const val mobileAppWooTransfer = "mobile_app_woo_transfer"
    const val woocommerceMobileApps = "woocommerce_mobile_apps"
    const val productAreaWooExtensions = "product_area_woo_extensions"
    const val productAreaAppsInPersonPayments = "product_area_apps_in_person_payments"
    const val storeSubcategoryTag = "store"
    const val supportCategoryTag = "support"
    const val paymentSubcategoryTag = "payment"
    const val jetpackTag = "jetpack"
}

sealed class ZendeskException(message: String) : Exception(message) {
    object IdentityNotSetException : ZendeskException(requestCreationTimeoutErrorMessage)
    object RequestCreationTimeoutException : ZendeskException(requestCreationIdentityNotSetErrorMessage)
}

private object RequestConstants {
    const val requestCreationTimeout = 10000L
    const val requestCreationTimeoutErrorMessage = "Request creation timed out"
    const val requestCreationIdentityNotSetErrorMessage = "Request creation failed: identity not set"
}