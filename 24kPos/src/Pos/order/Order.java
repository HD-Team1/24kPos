package Pos.order;

import java.time.LocalDateTime;

import temp.Product;
import java.time.LocalDateTime;

public class Order {
	private int orderId;
	private Product[] products;
	private LocalDateTime orderedAt;
	private enum status {
		"발주 신청", "발주 중", "발주 완료", "발주 취소"
	}
	
	public Order(Product[] products, enum status) {
		this.products = products;
		this.status = status;
	}
	public enum getStatus() {
		return this.status;
	}
	public void setStatus(enum status) {
		this.status = status;
	}
}
