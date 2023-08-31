package com.woocommerce.android.ui.login.storecreation.onboarding.woopayments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WooPaymentsSetupScreen() {
    Scaffold(
        topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationButtonClick = { },
            )
        },
        bottomBar = { WooPaymentsSetupFooter() },
        modifier = Modifier.background(color = colorResource(id = R.color.color_surface))
    ) { paddingValues ->
        WooPaymentsSetupContent(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        )
    }
}

@Composable
fun WooPaymentsSetupContent(
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Image(
            painter = painterResource(R.drawable.img_woo_payments_store_onboarding_setup_dialog),
            contentDescription = "WooPayments setup dialog image",
            alignment = Alignment.TopStart,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )

        val words = stringResource(id = R.string.store_onboarding_wcpay_setup_title).split("\n")
        val firstWord = words[0]
        val secondWord = words[1]

        val text = buildAnnotatedString {
            append(firstWord)
            append(" ") // Space between words
            withStyle(style = SpanStyle(color = colorResource(id = R.color.woo_purple_50))) {
                append(secondWord)
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )

        val bodyText = annotatedStringRes(stringResId = R.string.store_onboarding_wcpay_setup_description)
        ClickableText(
            text = bodyText,
            style = MaterialTheme.typography.body1,
        ) {
            text.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                .firstOrNull()
                ?.let { /* click actions */ }
        }
    }
}

@Composable
fun WooPaymentsSetupFooter() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            onClick = { },
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }

        WCOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            onClick = { },
        ) {
            Text(text = stringResource(id = R.string.learn_more))
        }
    }
}

@Preview
@Composable
fun WooPaymentsSetupScreenPreview() {
    WooThemeWithBackground {
        WooPaymentsSetupScreen()
    }
}
