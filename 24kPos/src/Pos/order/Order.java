package Pos.order;

import java.time.LocalDateTime;

import Pos.product.Product;

import java.time.LocalDateTime;

public class Order {
	public long orderId;
	private Product[] products;
	private LocalDateTime orderedAt;
	public enum orderStatus {
	    REQUESTED, PROCESSING, COMPLETED, CANCELED
	}
	private orderStatus status;
	
	public Order(Product[] products, orderStatus status) {
		this.orderedAt = LocalDateTime.now();
		this.orderId = Integer.parseInt(String.format("%02d%02d%03d", 
					this.orderedAt.getMonthValue(),
					this.orderedAt.getDayOfMonth(),
					Math.abs((Integer.toString(this.orderedAt.getHour())
					+ Integer.toString(this.orderedAt.getMinute())
					+ Integer.toString(this.orderedAt.getSecond())
					+ Integer.toString(this.orderedAt.getNano())).hashCode()%100000)
				));
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
