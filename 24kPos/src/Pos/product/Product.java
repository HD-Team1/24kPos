package Pos.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
	private long  productId;
	public String productName;
	public BigDecimal productPrice;
	public LocalDateTime expiredAt;
	private int quantity;
	
	
	public Product(long productId, String productName, BigDecimal productPrice, LocalDateTime expiredAt) {
	    this.productId = productId;
	    this.productName = productName;
	    this.productPrice = productPrice;
	    this.expiredAt = expiredAt;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	public boolean isExpired() {
		return this.expiredAt.isBefore(LocalDateTime.now());
	}
	

}
