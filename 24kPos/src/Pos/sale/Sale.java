package Pos.sale;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import Pos.product.Product;

public class Sale implements Comparable<Sale>, Serializable {
	private int saleId;
	public List<Product> products;
	public LocalDateTime soldAt;
	private BigDecimal totalPrice;
	public enum saleStatus {
		COMPLETED, CANCELED
	}
	private saleStatus status;
	
    public Sale(List<Product> products, saleStatus status) {
    	LocalDateTime now = LocalDateTime.now();

        this.products = products;
        this.soldAt = now;
        this.totalPrice = calculateTotalPrice(products);
        this.status = status;

        // saleId 생성
        this.saleId = (int)(System.currentTimeMillis() % 1_000_000_000);
    }
	
	public saleStatus getStatus() {
		return this.status;
	}
	public void setStatus(saleStatus status) {
		this.status = status;
	}
	
	public BigDecimal getTotalPrice() {
		return this.totalPrice;
	}
	
    @Override
    public String toString() {
        return "[" + saleId + " | " + soldAt + " | " + status +
                " | 판매 제품 : " + products +
                " | 총액 : " + totalPrice +
                "원 ]";
    }
	
	@Override
	public int compareTo(Sale sale) {
		return this.soldAt.compareTo(sale.soldAt);
	}
	
	public BigDecimal calculateTotalPrice(List<Product> products) {
	    return products.stream()
	            .filter(p -> p != null)
	            .map(p -> p.productPrice.multiply(BigDecimal.valueOf(p.getQuantity())))
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public int getSaleId() {
		return this.saleId;
	}
	public List<Product> getProducts() {
	    return products;
	}
}
