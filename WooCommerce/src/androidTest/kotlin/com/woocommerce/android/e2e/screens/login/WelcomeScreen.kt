package com.woocommerce.android.e2e.screens.login

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.TabNavComponent

class WelcomeScreen : Screen {
    companion object {
        const val LOGIN_BUTTON = R.id.button_free_trial_mode_login_store
        const val WPCOM_LOGIN_BUTTON = R.id.button_login_wpcom

        fun logoutIfNeeded(composeTestRule: ComposeContentTestRule): WelcomeScreen {
            if (isElementDisplayed(R.id.dashboard)) {
                TabNavComponent()
                    .gotoMoreMenuScreen()
                    .openSettings(composeTestRule)
                    .logOut()
            }

            Thread.sleep(1000)

            return skipCarouselIfNeeded()
        }

        fun skipCarouselIfNeeded(): WelcomeScreen {
            return if (Screen.isElementDisplayed(CarouselScreen.SKIP_BUTTON)) {
                CarouselScreen().skip()
            } else {
                WelcomeScreen()
            }
        }
    }

    constructor() : super(LOGIN_BUTTON)

    fun selectLogin(): SiteAddressScreen {
        clickOn(LOGIN_BUTTON)
        return SiteAddressScreen()
    }

    fun selectWPCOMLogin(): EmailAddressScreen {
        clickOn(WPCOM_LOGIN_BUTTON)
        return EmailAddressScreen()
    }
}
