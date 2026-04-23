package Pos.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;

public class PosMain {

    private static PosMain instance = new PosMain();
    private String password;
    
    private List<Sale> history;
    private Map<String, List<Product>> products; // 제품명을 키로 사용
    private List<Order> orders;
    
    
	// 비밀번호 설정
    public void setPassword(String password) {
        this.password = password;
    }

    // 로그인
    public boolean login(String inputPassword) {
        if (this.password == null) {
            System.out.println("비밀번호가 설정되지 않았습니다.");
            return false;
        }

        return this.password.equals(inputPassword);
    }
    

    // 싱글톤 생성자
    private PosMain() {
        this.history = new ArrayList<>();
        this.products = new HashMap<>();
        this.orders = new ArrayList<>();
    }

    // 인스턴스 가져오기
    public static PosMain getInstance() {
        return instance;
    }
    

    public Map<String, List<Product>> getProducts() {
        return products;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public List<Sale> getHistory() {
        return history;
    }
   
    
    // 전체 파일로 저장하기
    public void save() {
        PosFileManager.saveProducts(products);
        PosFileManager.saveOrders(orders);
        PosFileManager.saveSales(history);
    }

    // 전체 파일로 읽어서 객체로 저장
    public void load() {
        this.products = PosFileManager.loadProducts();
        this.orders = PosFileManager.loadOrders();
        this.history = PosFileManager.loadSales();
    }
    
}