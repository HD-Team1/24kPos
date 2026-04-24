package Pos.order;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import Pos.common.OrderStatus;
import Pos.product.Product;

import java.time.LocalDateTime;

public class Order implements Serializable{
	public int orderId;
	private List<Product> products;
	private LocalDateTime orderedAt;

	private OrderStatus status;
	
	public Order(List<Product> products, OrderStatus status) {
		this.orderedAt = LocalDateTime.now();
		this.orderId = (int)(System.currentTimeMillis() % 1_000_000_000);
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
                " | 상품수=" + (products == null ? 0 : products.size()) +
                " | 주문시간=" + orderedAt;
    }
    
	public int getOrderId() {
		return this.orderId;
	}

	public List<Product> getProducts() {
	    return products;
	}
}