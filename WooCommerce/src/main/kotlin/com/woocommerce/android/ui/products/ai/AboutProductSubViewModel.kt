package com.woocommerce.android.ui.products.ai

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.getStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.parcelize.Parcelize

class AboutProductSubViewModel(
    savedStateHandle: SavedStateHandle,
    override val onDone: (String) -> Unit,
    private val appsPrefsWrapper: AppPrefsWrapper
) : AddProductWithAISubViewModel<String> {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val productFeatures = savedStateHandle.getStateFlow(
        viewModelScope,
        UiState(
            productFeatures = "",
            selectedAiTone = appsPrefsWrapper.aiContentGenerationTone
        )
    )

    val state = productFeatures.asLiveData()

    fun onDoneClick() {
        onDone(productFeatures.value.productFeatures)
    }

    fun onProductFeaturesUpdated(features: String) {
        productFeatures.value = productFeatures.value.copy(productFeatures = features)
    }

    fun onNewToneSelected(tone: AiTone) {
        productFeatures.value = productFeatures.value.copy(selectedAiTone = tone)
        appsPrefsWrapper.aiContentGenerationTone = productFeatures.value.selectedAiTone
    }

    override fun close() {
        viewModelScope.cancel()
    }

    @Parcelize
    data class UiState(
        val productFeatures: String,
        val selectedAiTone: AiTone
    ) : Parcelable

    enum class AiTone(@StringRes val displayName: Int, val slug: String) {
        Casual(R.string.product_creation_ai_tone_casual, "Casual"),
        Formal(R.string.product_creation_ai_tone_formal, "Formal"),
        Flowery(R.string.product_creation_ai_tone_flowery, "Flowery"),
        Convincing(R.string.product_creation_ai_tone_convincing, "Convincing");

        companion object {
            fun fromString(source: String): AiTone =
                AiTone.values().firstOrNull { it.slug == source } ?: Casual
        }
    }
}
