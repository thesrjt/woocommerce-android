package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneMonthAgo
import com.woocommerce.android.extensions.startOfCurrentMonth
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubMonthToDateRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = referenceDate
        )

        val oneMonthAgo = referenceDate.oneMonthAgo(calendar)
        calendar.time = oneMonthAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = oneMonthAgo
        )
    }
}