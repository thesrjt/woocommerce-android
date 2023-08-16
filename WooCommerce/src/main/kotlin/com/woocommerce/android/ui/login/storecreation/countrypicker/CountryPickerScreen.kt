package com.woocommerce.android.ui.login.storecreation.countrypicker

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.countrypicker.CountryPickerViewModel.StoreCreationCountry

@Composable
fun CountryPickerScreen(viewModel: CountryPickerViewModel) {
    viewModel.countryPickerState.observeAsState().value?.let { countries ->
        Scaffold(topBar = {
            ToolbarWithHelpButton(
                onNavigationButtonClick = viewModel::onArrowBackPressed,
                onHelpButtonClick = viewModel::onHelpPressed
            )
        }) { padding ->
            CountryPickerForm(
                countries = countries,
                onContinueClicked = viewModel::onContinueClicked,
                onCurrentCountryClicked = viewModel::onCurrentCountryClicked,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun CountryPickerForm(
    countries: List<StoreCreationCountry>,
    onContinueClicked: () -> Unit,
    onCurrentCountryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100)
                )
        ) {
            CountryPickerHeaderContent(state.countries, state.storeName, onCurrentCountryClicked)
        }
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@Composable
private fun CountryPickerHeaderContent(
    availableCountries: List<StoreCreationCountry>,
    onCurrentCountryClicked: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(id = R.string.store_creation_store_profiler_industries_header).uppercase(),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = R.string.store_creation_country_picker_title),
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = stringResource(id = R.string.store_creation_country_picker_description),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_200))
        )

        CurrentCountryItem(
            country = availableCountries.first { it.isSelected },
            onCurrentCountryClicked = onCurrentCountryClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(id = R.dimen.major_200))
        )
    }
}

@Composable
private fun CurrentCountryItem(
    country: StoreCreationCountry,
    onCurrentCountryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onCurrentCountryClicked() }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.major_75),
                    bottom = dimensionResource(id = R.dimen.major_75),
                    end = dimensionResource(id = R.dimen.major_100),
                )
            ) {
                Text(
                    text = country.emojiFlag,
                    modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100))
                )
                Column {
                    Text(
                        fontWeight = FontWeight.Bold,
                        text = country.name,
                        color = colorResource(
                            id = if (isSystemInDarkTheme()) R.color.color_primary
                            else R.color.color_on_surface
                        ),
                        modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.minor_50))
                    )

                    Text(
                        text = stringResource(id = R.string.store_creation_country_picker_current_location_description),
                        color = colorResource(
                            id = if (isSystemInDarkTheme()) R.color.color_primary
                            else R.color.color_on_surface
                        )
                    )
                }
            }

            Divider(
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun CountryPickerPreview() {
    WooThemeWithBackground {
        CountryPickerForm(
            countries = listOf(
                StoreCreationCountry(
                    name = "Canada",
                    code = "CA",
                    emojiFlag = "\uD83C\uDDE8\uD83C\uDDE6",
                    isSelected = false
                ),
                StoreCreationCountry(
                    name = "Spain",
                    code = "ES",
                    emojiFlag = "\uD83C\uDDEA\uD83C\uDDF8",
                    isSelected = false
                ),
                StoreCreationCountry(
                    name = "United States",
                    code = "US",
                    emojiFlag = "\uD83C\uDDFA\uD83C\uDDF8",
                    isSelected = false
                ),
                StoreCreationCountry(
                    name = "Italy",
                    code = "IT",
                    emojiFlag = "\uD83C\uDDEE\uD83C\uDDF9",
                    isSelected = false
                )
            ),
            onContinueClicked = {},
            onCurrentCountryClicked = {},
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
        )
    }
}
