package Pos.main;

import java.math.BigDecimal;
import java.time.LocalDate;

import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;

public class PosMain {

    // 필드 선언
    private String pwd;
    public Product[] products;
    private Sale[] history;
    private BigDecimal dailySalesAmount;
    private Order[] orders;

    // 발주 목록 조회
    public Order[] getOrders() {
        return null;
    }
    
    // 발주 생성
    public Order makeOrder(Product[] products) {
        return null;
    }

    // 발주 취소
    public boolean cancelOrder(Order order) {
        return false;
    }
    
    public static void main(String[] args) {
    	System.out.println("main");
		
	}
}