package Pos.main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
    

    public List<Order> getOrders() {
        return orders;
    }

    public List<Sale> getHistory() {
        return history;
    }
   
    public Map<String, List<Product>> getProducts() {
        return products;
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
    
    public boolean setProducts(Sale sale) {
        if (sale == null || sale.getProducts() == null) {
            return false;
        }

        for (Product soldProduct : sale.getProducts()) {
            String productName = soldProduct.getProductName();
            int requiredQuantity = soldProduct.getQuantity();

            List<Product> stockList = products.get(productName);

            if (stockList == null || stockList.isEmpty()) {
                return false;
            }

            // 유통기한이 빠른 순으로 정렬
            stockList.sort(Comparator.comparing(Product::getExpiredAt));

            int remainToReduce = requiredQuantity;
            Iterator<Product> iterator = stockList.iterator();

            while (iterator.hasNext() && remainToReduce > 0) {
                Product stockProduct = iterator.next();
                int stockQuantity = stockProduct.getQuantity();

                if (stockQuantity <= remainToReduce) {
                    remainToReduce -= stockQuantity;
                    stockProduct.setQuantity(0);
                    iterator.remove();
                } else {
                    stockProduct.setQuantity(stockQuantity - remainToReduce);
                    remainToReduce = 0;
                }
            }

            // 필요한 수량만큼 다 못 줄였으면 실패
            if (remainToReduce > 0) {
                return false;
            }

            // 해당 상품 리스트가 비었으면 map에서 제거
            if (stockList.isEmpty()) {
                products.remove(productName);
            }
        }

        history.add(sale);
        return true;
    }

    // ----------------------------
    // 오버로딩: 발주 처리
    // ----------------------------
    public boolean setProducts(Order order) {
        if (order == null || order.getProducts() == null) {
            return false;
        }

        for (Product orderedProduct : order.getProducts()) {
            String productName = orderedProduct.getProductName();

            products.putIfAbsent(productName, new ArrayList<>());
            products.get(productName).add(orderedProduct);
        }

        orders.add(order);
        return true;
    }

    // ----------------------------
    // 오버로딩: 폐기 처리
    // ----------------------------
    public boolean setProducts(Product product) {
        if (product == null) {
            return false;
        }

        String productName = product.getProductName();
        List<Product> stockList = products.get(productName);

        if (stockList == null || stockList.isEmpty()) {
            return false;
        }

        boolean removed = stockList.removeIf(
                stockProduct -> stockProduct.getProductId() == product.getProductId()
        );

        if (stockList.isEmpty()) {
            products.remove(productName);
        }

        return removed;
    }
}