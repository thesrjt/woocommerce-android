package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubCustomRangeData(
    startDate: Date,
    endDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = startDate
        val currentStart = calendar.startOfCurrentDay()

        calendar.time = endDate
        val currentEnd = calendar.endOfCurrentDay()

        currentRange = AnalyticsHubTimeRange(
            start = currentStart,
            end = currentEnd
        )

        val dayDifference = Date(endDate.time - startDate.time)

        calendar.time = startDate.oneDayAgo(calendar)
        val previousEnd = calendar.endOfCurrentDay()

        calendar.time = Date(previousEnd.time - dayDifference.time)
        val previousStart = calendar.startOfCurrentDay()

        previousRange = AnalyticsHubTimeRange(
            start = previousStart,
            end = previousEnd
        )
    }
}
