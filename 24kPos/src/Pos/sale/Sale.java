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
	public Product[] getProducts() {
	    return products;
	}
	public BigDecimal getTotalPrice() {
		return this.totalPrice;
	}
	@Override
	public String toString() {
		String string = "거래ID=" + this.saleId +
						" | 상태=" + this.status +
						"\n거래상품=\n";
		for (Product product : this.products) {
			string += product.toString();
		}
		string += "\n총거래액=" + this.totalPrice +
				" | 거래일시=" + this.soldAt;
		return string;
	}
	
}
