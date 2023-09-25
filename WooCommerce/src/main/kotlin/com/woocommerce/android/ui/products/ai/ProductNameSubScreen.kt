package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductNameSubScreen(viewModel: ProductNameSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        Column(
            modifier = modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
        ) {
            ProductNameForm(
                enteredName = state.name,
                onProductNameChanged = viewModel::onProductNameChanged,
                onSuggestNameClicked = {},
                onContinueClicked = viewModel::onDoneClick
            )
        }
    }
}

@Composable
fun ProductNameForm(
    enteredName: String,
    onProductNameChanged: (String) -> Unit,
    onSuggestNameClicked: () -> Unit,
    onContinueClicked: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation

    @Composable
    fun ContinueButton() {
        WCColoredButton(
            enabled = enteredName.isNotEmpty(),
            onClick = onContinueClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }

    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.ai_product_creation_add_name_title),
                style = MaterialTheme.typography.h5
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            Text(
                text = stringResource(id = R.string.ai_product_creation_add_name_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_250)))

            ProductKeywordsTextFieldWithEmbeddedButton(
                textFieldContent = enteredName,
                onTextFieldContentChanged = onProductNameChanged,
                onButtonClicked = onSuggestNameClicked
            )

            Spacer(modifier = Modifier.weight(1f))

            // Button will scroll with the rest of UI on landscape mode, or... (see below)
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ContinueButton()
            }
        }

        // Button will stick to the bottom on portrait mode
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ContinueButton()
        }
    }
}

@Composable
private fun ProductKeywordsTextFieldWithEmbeddedButton(
    textFieldContent: String,
    onTextFieldContentChanged: (String) -> Unit,
    onButtonClicked: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(id = R.string.ai_product_creation_add_name_keywords_label),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isFocused) colorResource(id = R.color.color_primary)
                    else colorResource(id = R.color.divider_color),
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box {
                if (textFieldContent.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.ai_product_creation_add_name_keywords_placeholder),
                        style = MaterialTheme.typography.body1,
                        color = Color.Gray,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(id = R.dimen.major_100),
                            vertical = dimensionResource(id = R.dimen.major_150)
                        )
                    )
                }

                WCOutlinedTextField(
                    value = textFieldContent,
                    onValueChange = onTextFieldContentChanged,
                    label = "", // Can't use label here as it breaks the visual design.
                    placeholderText = "", // Uses Text() above instead.
                    textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.multiline_textfield_height)),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent, // Remove outline and use Column's border instead.
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                )
            }

            Divider()

            WCTextButton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_50)),
                onClick = onButtonClicked,
                icon = ImageVector.vectorResource(id = R.drawable.ic_ai_share_button),
                allCaps = false,
                text = stringResource(id = R.string.ai_product_creation_add_name_suggest_name_button),
            )
        }
    }
}

@Preview
@Composable
fun ProductNamePreview() {
    WooThemeWithBackground {
        ProductNameForm(
            enteredName = "Everyday Elegance with Our Soft Black Tee",
            onProductNameChanged = {},
            onSuggestNameClicked = {},
            onContinueClicked = {}
        )
    }
}
