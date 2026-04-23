package Pos.product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product implements Serializable{
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
	
	public long getProductId() {
	    return productId;
	}
	
	public String getProductName() {
	    return productName;
	}

	public LocalDateTime getExpiredAt() {
	    return expiredAt;
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
	
	@Override
	public String toString() {
	    return "Product{" +
	            "id=" + productId +
	            ", name='" + productName + '\'' +
	            ", price=" + productPrice +
	            ", expiredAt=" + expiredAt +
	            ", quantity=" + quantity +
	            '}';
	}
}
