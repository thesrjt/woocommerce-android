package com.woocommerce.android.ui.orders.creation.products

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditProductSelectionBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigator
import com.woocommerce.android.ui.orders.creation.products.OrderCreateEditProductSelectionViewModel.AddProduct
import com.woocommerce.android.ui.orders.creation.products.OrderCreateEditProductSelectionViewModel.ProductNotFound
import com.woocommerce.android.ui.orders.creation.products.OrderCreateEditProductSelectionViewModel.ViewState
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductListAdapter
import com.woocommerce.android.ui.products.ProductSelectionItemKeyProvider
import com.woocommerce.android.ui.products.SelectableProductListItemLookup
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditProductSelectionFragment :
    BaseFragment(R.layout.fragment_order_create_edit_product_selection),
    OnLoadMoreListener,
    SearchView.OnQueryTextListener,
    OnActionExpandListener,
    MenuProvider {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val productListViewModel by viewModels<OrderCreateEditProductSelectionViewModel>()

    private val skeletonView = SkeletonView()
    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null
    private var doneMenuItem: MenuItem? = null

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    private var tracker: SelectionTracker<Long>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentOrderCreateEditProductSelectionBinding.bind(view)
        with(binding) {
            productsList.layoutManager = LinearLayoutManager(requireActivity())
            setupObserversWith(this)
        }
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        removeSearchListener()
        searchView = null
        searchMenuItem = null
        super.onDestroyView()
    }

    private fun setupObserversWith(binding: FragmentOrderCreateEditProductSelectionBinding) {
        productListViewModel.productListData.observe(viewLifecycleOwner) {
            binding.loadProductsAdapterWith(it)
        }
        productListViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            onViewStateChanged(binding, old, new)
        }
        productListViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                AddProduct -> {
                    sharedViewModel.onProductSelected(
                        sharedViewModel.selectedProducts
                    )
                    findNavController().navigateUp()
                }
                is ShowProductVariations -> OrderCreateEditNavigator.navigate(this, event)
                is ProductNotFound -> ToastUtils.showToast(
                    requireActivity(),
                    R.string.product_detail_fetch_product_invalid_id_error
                )
            }
        }
    }

    private fun FragmentOrderCreateEditProductSelectionBinding.loadProductsAdapterWith(
        products: List<Product>
    ) {
        val adapter = productsList.adapter
            .let { it as? ProductListAdapter }
            ?: ProductListAdapter(
                clickListener = { productId, _, product->
//                    productListViewModel.onProductSelected()
                    if (
                        !sharedViewModel.selectedProducts.contains(Pair(productId, null)) &&
                            product.numVariations == 0
                    ) {
                        sharedViewModel.selectedProducts.add(Pair(productId, null))
                    } else {
                        sharedViewModel.selectedProducts.remove(Pair(productId, null))
                    }
                    productListViewModel.onProductSelected(productId)
//                    (productsList.adapter as? ProductListAdapter)?.submitList(products)
                },
                loadMoreListener = this@OrderCreateEditProductSelectionFragment,
                currencyFormatter = currencyFormatter
            ).also {
                productsList.adapter = it
//                tracker = SelectionTracker.Builder(
//                    "myProductSelection", // a string to identity our selection in the context of this fragment
//                    productsList, // the RecyclerView where we will apply the tracker
//                    ProductSelectionItemKeyProvider(productsList), // the source of selection keys
//                    SelectableProductListItemLookup(productsList), // the source of information about recycler items
//                    StorageStrategy.createLongStorage() // strategy for type-safe storage of the selection state
//                ).withSelectionPredicate(
//                    SelectionPredicates.createSelectAnything() // allows multiple items to be selected without any restriction
//                ).build()
//                (productsList.adapter as ProductListAdapter).tracker = tracker
//                productListViewModel.tracker = tracker
            }
        val newProducts = products.apply {
            this.map {
                if (sharedViewModel.selectedProducts.isNullOrEmpty()) {
                    it.isSelected = false
                } else {
                    sharedViewModel.selectedProducts.forEach { selectedProductId ->
                        if (it.remoteId == selectedProductId.first) {
                            it.isSelected = true
                        }
                    }
                }
            }
        }
        adapter.submitList(newProducts)
    }

    override fun onRequestLoadMore() {
        productListViewModel.onLoadMoreRequest()
    }

    private fun onViewStateChanged(
        binding: FragmentOrderCreateEditProductSelectionBinding,
        old: ViewState?,
        new: ViewState
    ) {
        new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) {
            showSkeleton(binding, it)
        }
        new.isEmptyViewShowing?.takeIfNotEqualTo(old?.isEmptyViewShowing) {
            showEmptyView(binding, it)
        }
    }

    private fun showSkeleton(
        binding: FragmentOrderCreateEditProductSelectionBinding,
        show: Boolean
    ) {
        if (show) {
            skeletonView.show(binding.productsList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showEmptyView(
        binding: FragmentOrderCreateEditProductSelectionBinding,
        show: Boolean
    ) {
        if (show) {
            binding.emptyView.show(
                WCEmptyView.EmptyViewType.SEARCH_RESULTS,
                searchQueryOrFilter = productListViewModel.currentQuery
            )
        } else {
            binding.emptyView.hide()
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_products)

    // region Search configuration and events
    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_product_selection_fragment, menu)

        searchMenuItem = menu.findItem(R.id.menu_search)
        doneMenuItem = menu.findItem(R.id.menu_done)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.product_search_hint)
        searchView?.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
            ?.setOnClickListener { onClearSearchButtonClicked() }
    }

    override fun onPrepareMenu(menu: Menu) {
        searchMenuItem
            ?.takeIf { it.isActionViewExpanded != productListViewModel.isSearchActive }
            ?.restoreSearchMenuItemState()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                registerSearchListeners()
                true
            }
            R.id.menu_done -> {
                productListViewModel.onProductSelected(0L)
                true
            }
            else -> false
        }
    }

    private fun MenuItem.restoreSearchMenuItemState() {
        removeSearchListener()
        if (productListViewModel.isSearchActive) {
            expandActionView()
            searchView?.setQuery(productListViewModel.currentQuery, false)
        } else {
            collapseActionView()
        }
        registerSearchListeners()
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        productListViewModel.onSearchOpened()
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        productListViewModel.onSearchClosed()
        removeSearchListener()
        updateActivityTitle()
        searchMenuItem?.collapseActionView()
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let { productListViewModel.searchProductList(it) }
        ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let { productListViewModel.searchProductList(it, delayed = true) }
        return true
    }

    private fun registerSearchListeners() {
        searchMenuItem?.setOnActionExpandListener(this)
        searchView?.setOnQueryTextListener(this)
    }

    private fun removeSearchListener() {
        searchMenuItem?.setOnActionExpandListener(null)
        searchView?.setOnQueryTextListener(null)
    }

    private fun onClearSearchButtonClicked() {
        searchView?.setQuery("", false)
        productListViewModel.onSearchQueryCleared()
    }
    // endregion
}
