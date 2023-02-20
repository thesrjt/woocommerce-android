package com.woocommerce.android.ui.orders.creation.variations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditProductSelectionBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.VariationListAdapter
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateEditVariationSelectionFragment : BaseFragment(R.layout.fragment_order_create_edit_product_selection) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val viewModel by viewModels<OrderCreateEditVariationSelectionViewModel>()

    private val skeletonView = SkeletonView()

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreateEditProductSelectionBinding.bind(view)
        with(binding) {
            productsList.layoutManager = LinearLayoutManager(requireContext())
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentOrderCreateEditProductSelectionBinding) {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            screenTitle = state.parentProduct?.name ?: getString(R.string.order_creation_variations_screen_title)

            state.variationsList?.let { variations ->
                binding.bindVariationsList(variations, state.parentProduct)
            }

            binding.showSkeleton(state.isSkeletonShown)
        }
    }

    private fun FragmentOrderCreateEditProductSelectionBinding.bindVariationsList(
        variationsList: List<ProductVariation>,
        parentProduct: Product?
    ) {
        if (productsList.adapter == null) {
            productsList.adapter = VariationListAdapter(
                requireContext(),
                GlideApp.with(requireContext()),
                parentProduct = parentProduct,
                loadMoreListener = object : OnLoadMoreListener {
                    override fun onRequestLoadMore() {
                        viewModel.onLoadMore()
                    }
                },
                onItemClick = {
//                    sharedViewModel.onProductSelected(it.remoteProductId, it.remoteVariationId)
//                    findNavController().popBackStack(R.id.orderCreationFragment, false)
                }
            )
        }
        (productsList.adapter as VariationListAdapter).submitList(variationsList)
    }

    private fun FragmentOrderCreateEditProductSelectionBinding.showSkeleton(
        show: Boolean
    ) {
        if (show) {
            skeletonView.show(productsList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun getFragmentTitle() = screenTitle
}
