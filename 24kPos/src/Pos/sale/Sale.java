package Pos.sale;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import Pos.product.Product;

public class Sale implements Serializable{
	private int saleId;
	public Product[] products;
	public LocalDateTime soldAt;
	private BigDecimal totalPrice;
	public enum saleStatus {
		COMPLETED, CANCELED
	}
	private saleStatus status;
	
	public Sale(Product[] products, saleStatus status) {
		this.products = products;
		this.status = status;
	}
	public saleStatus getStatus() {
		return this.status;
	}
	public void setStatus(saleStatus status) {
		this.status = status;
	}
	public int getSaleId() {
		return this.saleId;
	}
}
