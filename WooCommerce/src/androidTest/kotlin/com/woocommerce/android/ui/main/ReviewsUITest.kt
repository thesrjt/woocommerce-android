package com.woocommerce.android.ui.main

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.util.ReviewData
import com.woocommerce.android.screenshots.util.MocksReader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReviewsUITest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .logoutIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent().gotoReviewsScreen()
    }

    @Test
    fun reviewListShowsAllReviews() {
        val reviewsJSONArray = MocksReader().readAllReviewsToArray()

        for (i in 0 until reviewsJSONArray.length()) {
            val reviewContainer: JSONObject = reviewsJSONArray.getJSONObject(i)
            val currentReview = ReviewData(
                reviewContainer.getInt("product_id"),
                reviewContainer.getString("status"),
                reviewContainer.getString("reviewer"),
                reviewContainer.getString("review"),
                reviewContainer.getInt("rating")
            )

            ReviewsListScreen()
                .scrollToReview(currentReview.title)
                .assertReviewCard(currentReview)
                .selectReviewByTitle(currentReview.title)
                .assertSingleReviewScreen(currentReview)
                .goBackToReviewsScreen()
        }
    }
}
