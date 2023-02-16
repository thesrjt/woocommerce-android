package com.woocommerce.android.ui.login.storecreation.profiler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.NavigateToEcommercePlatformsStep
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.NavigateToNextStep
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoreProfilerCommerceJourneyFragment : BaseFragment() {
    private val viewModel: StoreProfilerCommerceJourneyViewModel by activityViewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    StoreProfilerScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is NavigateToNextStep -> navigateToCountryPickerStep()
                is NavigateToEcommercePlatformsStep -> navigateToProfilerEcommercePlatformsStep()
                is MultiLiveEvent.Event.NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
            }
        }
    }

    private fun navigateToCountryPickerStep() {
        findNavController().navigateSafely(
            StoreProfilerCommerceJourneyFragmentDirections
                .actionStoreProfilerCommerceJourneyFragmentToCountryPickerFragment()
        )
    }

    private fun navigateToProfilerEcommercePlatformsStep() {
        findNavController().navigateSafely(
            StoreProfilerCommerceJourneyFragmentDirections
                .actionStoreProfilerCommerceJourneyFragmentToStoreProfilerEcommercePlatformsFragment()
        )
    }
}