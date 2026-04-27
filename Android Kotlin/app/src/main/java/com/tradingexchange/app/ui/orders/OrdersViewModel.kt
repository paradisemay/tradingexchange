package com.tradingexchange.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.OrderSide
import com.tradingexchange.app.domain.model.OrderType
import com.tradingexchange.app.domain.model.toMoney
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val ticker: String = "SBER",
    val quantity: String = "1",
    val limitPrice: String = "",
    val side: OrderSide = OrderSide.BUY,
    val orderType: OrderType = OrderType.MARKET,
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val nextCursor: String? = null,
    val message: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val brokerRepository: BrokerRepository,
    private val createOrderUseCase: CreateOrderUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(OrdersUiState())
    private val loading = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val nextCursor = MutableStateFlow<String?>(null)

    val state: StateFlow<OrdersUiState> = combine(
        form,
        brokerRepository.observeOrders(),
        loading,
        nextCursor,
        message,
    ) { formValue, orders, loadingValue, cursorValue, messageValue ->
        formValue.copy(orders = orders, isLoading = loadingValue, nextCursor = cursorValue, message = messageValue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    init {
        load()
    }

    fun updateTicker(value: String) = update { copy(ticker = value.uppercase(), message = null) }
    fun updateQuantity(value: String) = update { copy(quantity = value, message = null) }
    fun updateLimitPrice(value: String) = update { copy(limitPrice = value, message = null) }
    fun updateSide(value: OrderSide) = update { copy(side = value, message = null) }
    fun updateOrderType(value: OrderType) = update { copy(orderType = value, message = null) }

    fun load(cursor: String? = null) {
        viewModelScope.launch {
            loading.value = true
            runCatching { brokerRepository.refreshOrders(cursor) }
                .onSuccess { nextCursor.value = it.nextCursor }
                .onFailure { message.value = (it as? RepositoryException)?.appError?.userMessage() ?: "Не удалось загрузить заявки" }
            loading.value = false
        }
    }

    fun create() {
        val current = state.value
        viewModelScope.launch {
            loading.value = true
            val command = CreateOrderCommand(
                ticker = current.ticker,
                side = current.side,
                orderType = current.orderType,
                quantity = current.quantity.toMoney(),
                limitPrice = current.limitPrice.takeIf { it.isNotBlank() }?.toMoney(),
            )
            if (command.quantity <= BigDecimal.ZERO) {
                message.value = "Количество должно быть больше нуля"
            } else {
                runCatching { createOrderUseCase(command) }
                    .onSuccess {
                        message.value = "Заявка исполнена"
                        load()
                    }
                    .onFailure { message.value = (it as? RepositoryException)?.appError?.userMessage() ?: "Заявка отклонена" }
            }
            loading.value = false
        }
    }

    private fun update(reducer: OrdersUiState.() -> OrdersUiState) {
        form.value = form.value.reducer()
    }
}
