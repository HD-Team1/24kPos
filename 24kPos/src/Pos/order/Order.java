package Pos.order;

import java.time.LocalDateTime;

import Pos.product.Product;

import java.time.LocalDateTime;

public class Order {
	private int orderId;
	private Product[] products;
	private LocalDateTime orderedAt;
	public enum orderStatus {
	    REQUESTED, PROCESSING, COMPLETED, CANCELED
	}
	private orderStatus status;
	
	public Order(Product[] products, orderStatus status) {
		this.products = products;
		this.status = status;
	}
	public orderStatus getStatus() {
		return this.status;
	}
	public void setStatus(orderStatus status) {
		this.status = status;
	}
}
