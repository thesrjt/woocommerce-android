package com.woocommerce.android.ui.analytics.ranges

import java.io.Serializable
import java.util.Date

data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
): Serializable {
    val description: String
        get() = "$start - $end"
}
