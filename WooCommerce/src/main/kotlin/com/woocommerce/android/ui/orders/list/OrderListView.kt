package com.woocommerce.android.ui.orders.list

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.databinding.OrderListViewBinding
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.model.WCOrderStatusModel

private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

class OrderListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderListViewBinding.inflate(LayoutInflater.from(ctx), this)

    private lateinit var ordersAdapter: OrderListAdapter
    private lateinit var listener: OrderListListener

    val emptyView
        get() = binding.emptyView

    val ordersList
        get() = binding.ordersList

    fun init(
        currencyFormatter: CurrencyFormatter,
        orderListListener: OrderListListener
    ) {
        this.listener = orderListListener
        this.ordersAdapter = OrderListAdapter(orderListListener, currencyFormatter)

        binding.ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = ordersAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }
    }

    /**
     * order list adapter method
     * set order status options to the order list adapter
     */
    fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        ordersAdapter.setOrderStatusOptions(orderStatusOptions)
    }

    /**
     * Submit new paged list data to the adapter
     */
    fun submitPagedList(viewLifecycleOwner: LifecycleOwner, list: PagingData<OrderListItemUIType>) {
        val recyclerViewState = onFragmentSavedInstanceState()
        ordersAdapter.submitData(viewLifecycleOwner.lifecycle, list)

        post {
            (binding.ordersList.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                if (layoutManager.findFirstVisibleItemPosition() < MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION) {
                    layoutManager.onRestoreInstanceState(recyclerViewState)
                }
            }
        }
    }

    /**
     * clear order list adapter data
     */
    fun clearAdapterData(viewLifecycleOwner: LifecycleOwner) {
        ordersAdapter.submitData(viewLifecycleOwner.lifecycle, PagingData.empty())
    }

    /**
     * scroll to the top of the order list
     */
    fun scrollToTop() {
        binding.ordersList.smoothScrollToPosition(0)
    }

    /**
     * save the order list on configuration change
     */
    fun onFragmentSavedInstanceState() = binding.ordersList.layoutManager?.onSaveInstanceState()

    fun setLoadingMoreIndicator(active: Boolean) {
        binding.loadMoreProgressbar.isVisible = active
    }
}
