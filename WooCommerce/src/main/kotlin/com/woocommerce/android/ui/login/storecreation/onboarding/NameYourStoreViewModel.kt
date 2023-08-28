package com.woocommerce.android.ui.login.storecreation.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NameYourStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    private val onboardingRepository: StoreOnboardingRepository
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData(
        NameYourStoreDialogState(
            currentSiteTitle = selectedSite.get().name,
            enteredSiteTitle = "",
            isLoading = false,
            isError = false
        )
    )
    val viewState = _viewState

    fun saveSiteTitle(siteTitle: String, fromOnboarding: Boolean = true) {
        launch {
            _viewState.value = _viewState.value?.copy(isLoading = true, isError = false)
            onboardingRepository.saveSiteTitle(siteTitle).fold(
                onSuccess = {
                    if (fromOnboarding) {
                        triggerEvent(ShowSnackbar(R.string.store_onboarding_name_your_store_dialog_success))
                    } else {
                        triggerEvent(ShowSnackbar(R.string.settings_name_your_store_dialog_success))
                    }

                    triggerEvent(OnSiteTitleSaved)
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.store_onboarding_name_your_store_dialog_failure))
                    _viewState.value = _viewState.value?.copy(isError = true)
                }
            )
            _viewState.value = _viewState.value?.copy(isLoading = false)
        }
    }

    fun onNameYourStoreDismissed() {
        triggerEvent(Exit)
    }

    fun onSiteTitleInputChanged(input: String) {
        _viewState.value = _viewState.value?.copy(enteredSiteTitle = input, isError = false)
    }

    data class NameYourStoreDialogState(
        val currentSiteTitle: String,
        val enteredSiteTitle: String,
        val isLoading: Boolean,
        val isError: Boolean
    )

    object OnSiteTitleSaved : MultiLiveEvent.Event()
}
