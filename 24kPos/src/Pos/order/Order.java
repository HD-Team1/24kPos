package Pos.order;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import Pos.common.OrderStatus;
import Pos.product.Product;

import java.time.LocalDateTime;

public class Order {
	private static final AtomicInteger idGenerator = new AtomicInteger(1);
	private int orderId;
	private Product[] products;
	private LocalDateTime orderedAt;

	private OrderStatus status;
	
	public Order(Product[] products, OrderStatus status) {
		this.orderId = idGenerator.getAndIncrement();
		this.products = products;
		this.status = status;
		this.orderedAt = LocalDateTime.now(); // 시간도 여기서 초기화
	}
	
	public OrderStatus getStatus() {
		return this.status;
	}
	public void setStatus(OrderStatus status) {
		this.status = status;
	}

    @Override
    public String toString() {
        return "주문ID=" + orderId +
                " | 상태=" + status +
                " | 상품수=" + (products == null ? 0 : products.length) +
                " | 주문시간=" + orderedAt;
    }
    
	public int getOrderId() {
		return orderId;
	}
	
}