package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderCreationCustomAmountItemBinding
import com.woocommerce.android.ui.orders.creation.OrderCreateEditCustomAmountAdapter.CustomAmountViewHolder

class OrderCreateEditCustomAmountAdapter(
    private val onCustomAmountClick: (CustomAmountUIModel)-> Unit
) : ListAdapter<CustomAmountUIModel, CustomAmountViewHolder>(CustomAmountUIModelDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomAmountViewHolder {
        return CustomAmountViewHolder(
            OrderCreationCustomAmountItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CustomAmountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomAmountViewHolder(private val binding: OrderCreationCustomAmountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val safePosition: Int?
            get() = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }

        init {
            binding.root.setOnClickListener {
                safePosition?.let {
                    onCustomAmountClick(getItem(it))
                }
            }
        }

        fun bind(customAmountUIModel: CustomAmountUIModel) {
            binding.customAmountLayout.customAmountName.text = customAmountUIModel.name
            binding.customAmountLayout.customAmountAmount.text = customAmountUIModel.amount.toString()
        }
    }

    object CustomAmountUIModelDiffCallback : DiffUtil.ItemCallback<CustomAmountUIModel>() {
        override fun areItemsTheSame(
            oldItem: CustomAmountUIModel,
            newItem: CustomAmountUIModel
        ): Boolean = oldItem.name == newItem.name

        override fun areContentsTheSame(
            oldItem: CustomAmountUIModel,
            newItem: CustomAmountUIModel
        ): Boolean = oldItem.name == newItem.name
    }
}
