package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.orders.creation.coupon.edit.CouponValidator.CouponValidationResult
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreateCouponEditViewModel @Inject constructor(
    private val validator: CouponValidator,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: OrderCreateCouponEditFragmentArgs by savedState.navArgs()
    private val couponCode = savedState.getNullableStateFlow(
        viewModelScope,
        navArgs.couponCode,
        String::class.java,
        "key_coupon_code"
    )

    private val validationState = savedState.getNullableStateFlow(
        viewModelScope,
        ValidationState.Idle,
        ValidationState::class.java,
        "key_validation_state"
    )

    val viewState = combine(couponCode, validationState) { code, validation ->
        val isDoneButtonEnabled = code?.isNotNullOrEmpty() == true && validation == ValidationState.Idle
        ViewState(
            isDoneButtonEnabled = isDoneButtonEnabled,
            couponCode = code ?: "",
            isRemoveButtonVisible = navArgs.couponCode.isNotNullOrEmpty(),
            validationState = validation
        )
    }.asLiveData()

    suspend fun onDoneClicked() {
        validationState.update {
            ValidationState.InProgress
        }
        val couponCode = couponCode.value
        when {
            couponCode != null && validator.isCouponValid(couponCode) == CouponValidationResult.VALID -> {
                validationState.update {
                    ValidationState.Idle
                }
                val initialCouponCode = navArgs.couponCode
                if (initialCouponCode.isNullOrEmpty()) {
                    triggerEvent(CouponEditResult.AddNewCouponCode(couponCode))
                } else {
                    triggerEvent(CouponEditResult.UpdateCouponCode(initialCouponCode, couponCode))
                }
            }

            couponCode != null && validator.isCouponValid(couponCode) == CouponValidationResult.NETWORK_ERROR -> {
                validationState.update {
                    ValidationState.Error(R.string.order_creation_coupon_network_error)
                }
            }

            else -> {
                validationState.update {
                    ValidationState.Error(R.string.order_creation_coupon_invalid_code)
                }
                viewState.value?.isDoneButtonEnabled = false
            }
        }
    }

    fun onCouponCodeChanged(newCode: String) {
        validationState.update {
            ValidationState.Idle
        }
        couponCode.update {
            newCode
        }
    }

    fun onCouponRemoved() {
        navArgs.couponCode?.let {
            triggerEvent(CouponEditResult.RemoveCoupon(it))
        }
    }

    data class ViewState(
        var isDoneButtonEnabled: Boolean = false,
        val couponCode: String = "",
        val isRemoveButtonVisible: Boolean = false,
        val validationState: ValidationState = ValidationState.Idle,
    )

    @Parcelize
    sealed class ValidationState : Parcelable {
        @Parcelize
        object Idle : ValidationState()

        @Parcelize
        object InProgress : ValidationState()

        @Parcelize
        data class Error(
            @StringRes val message: Int,
        ) : ValidationState()
    }

    @Parcelize
    sealed class CouponEditResult : MultiLiveEvent.Event(), Parcelable {
        @Parcelize
        data class UpdateCouponCode(val oldCode: String, val newCode: String) : CouponEditResult()

        @Parcelize
        data class AddNewCouponCode(val couponCode: String) : CouponEditResult()

        @Parcelize
        data class RemoveCoupon(val couponCode: String) : CouponEditResult()
    }
}