package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.math.BigDecimal

abstract class SyncStrategyTest : BaseUnitTest() {
    protected val orderCreationRepository = mock<OrderCreationRepository> {
        onBlocking { createOrUpdateDraft(any()) } doAnswer InlineClassesAnswer {
            val order = it.arguments.first() as Order
            Result.success(order.copy(total = order.total + BigDecimal.TEN))
        }
    }
    protected val order = Order.EMPTY.copy(items = OrderTestUtils.generateTestOrderItems())
    protected val orderDraftChanges = MutableStateFlow(order)
    protected val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    protected val createUpdateOrderUseCase = CreateUpdateOrder(
        dispatchers = coroutinesTestRule.testDispatchers,
        orderCreationRepository = orderCreationRepository
    )
}