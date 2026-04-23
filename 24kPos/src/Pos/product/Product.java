package Pos.product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product implements Serializable{
	private int productId;
	public String productName;
	public BigDecimal productPrice;
	public LocalDateTime expiredAt;
	private int quantity;
	
	
	public Product(String productName, BigDecimal productPrice, LocalDateTime expiredAt) {
		this.productId = Integer.parseInt(String.format("%02d%02d%03d", 
				 LocalDateTime.now().getMonthValue(),
				 LocalDateTime.now().getDayOfMonth(),
				Math.abs((Integer.toString(LocalDateTime.now().getHour())
				+ Integer.toString(LocalDateTime.now().getMinute())
				+ Integer.toString(LocalDateTime.now().getSecond())
				+ Integer.toString(LocalDateTime.now().getNano())).hashCode()%100000)
			));
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
