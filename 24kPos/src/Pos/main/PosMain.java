package Pos.main;

import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import Pos.common.OrderStatus;
import Pos.order.Order;
import Pos.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import Pos.sale.Sale;

public class PosMain {
    
    private List<Order> orders = new CopyOnWriteArrayList<>(); // [수정] ArrayList는 멀티스레드에서 불안전합니다. CopyOnWriteArrayList 사용!

    public Order[] getOrders() {
        return orders.toArray(new Order[0]);
    }

    public Order makeOrder(Product[] products) {
        Order newOrder = new Order(products, OrderStatus.REQUESTED);
        orders.add(newOrder);
        System.out.println(Thread.currentThread().getName() + " → 주문 생성 ID: " + newOrder.getOrderId());
        return newOrder;
    }

    public boolean cancelOrder(Order order) {
        if (order == null) return false;
        synchronized (order) {
            if (order.getStatus() == OrderStatus.REQUESTED) {
                order.setStatus(OrderStatus.CANCELED);
                System.out.println(Thread.currentThread().getName() + " → 주문 취소 성공: " + order.getOrderId());
                return true;
            }
        }
        System.out.println(Thread.currentThread().getName() + " → 취소 실패 (ID: " + (order != null ? order.getOrderId() : "null") + ")");
        return false;
    }
    
    public void updateStatus(Order order, OrderStatus newStatus) {
        if (order == null) return;
        synchronized (order) {
            OrderStatus current = order.getStatus();
            boolean isAllowed = (current == OrderStatus.REQUESTED && newStatus == OrderStatus.PROCESSING) ||
                                (current == OrderStatus.PROCESSING && newStatus == OrderStatus.COMPLETED);

            if (isAllowed) {
                order.setStatus(newStatus);
                System.out.println(Thread.currentThread().getName() + " → 상태 변경 성공: " + current + " -> " + newStatus);
            } else {
                System.out.println(Thread.currentThread().getName() + " → 잘못된 상태 변경: " + current + " -> " + newStatus);
            }
        }
    }
    
    
// =================================== 아래는 main용 코드 ===================================
    public static void main(String[] args) {
        PosMain pos = new PosMain();
        pos.run();
//        
//        // [추가] 자동 작업 스레드: 시스템이 돌아가는 와중에 경쟁을 유발합니다.
//        Thread autoWorker = new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(5000);
//                    Order[] currentOrders = pos.getOrders();
//                    if (currentOrders.length > 0) {
//                        pos.updateStatus(currentOrders[0], OrderStatus.PROCESSING);
//                    }
//                } catch (InterruptedException e) { break; }
//            }
//        }, "AUTO_WORKER");
//        autoWorker.setDaemon(true);
//        autoWorker.start();
//
//        // [인터랙티브 모드]
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("=== POS 시스템 (멀티스레드 경쟁 모드) ===");
//        
//        while (scanner.hasNext()) {
//            String cmd = scanner.next();
//            if (cmd.equals("exit")) break;
//            
//            try {
//                switch (cmd) {
//                    case "create": pos.makeOrder(new Product[0]); break;
//                    case "list": for (Order o : pos.getOrders()) System.out.println(o); break;
//                    case "cancel": pos.cancelOrder(findOrder(pos, scanner.nextInt())); break;
//                    case "process": pos.updateStatus(findOrder(pos, scanner.nextInt()), OrderStatus.PROCESSING); break;
//                    case "complete": pos.updateStatus(findOrder(pos, scanner.nextInt()), OrderStatus.COMPLETED); break;
//                }
//            } catch (Exception e) { System.out.println("명령어 입력 오류!"); }
//        }
//        scanner.close();
//    }

//    private static Order findOrder(PosMain pos, int id) {
//        for (Order o : pos.getOrders()) if (o.getOrderId() == id) return o;
//        return null;
    }
//    =========================================================================================================
    private static PosMain instance = new PosMain();
    private String password;
    
    private List<Sale> history;
    private Map<String, List<Product>> products; // 제품명을 키로 사용
    
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
    
    public void run() {
        Scanner scanner = new Scanner(System.in);
        
        // 초기 설정 
        setPassword("12345678");
        
       
        // [전원 관리] 포스기를 켜면 데이터 로드 
        load();
        
        // 로그인 절차
        while (true) {
        	System.out.println("====================================");
            System.out.print("   비밀번호를 입력하세요:    ");
            
            if (login(scanner.next())) {
                System.out.println("로그인 성공!");
                break;
            }
            System.out.println("비밀번호가 틀렸습니다.");
        }
        
        System.out.println("====================================");
        System.out.println("   편의점 POS 시스템을 시작합니다.   ");
        System.out.println("====================================");
        
        //POS기 가동 -> 메인 메뉴 
        while (true) {
            System.out.println("\n[1]상품관리 [2]재고관리 [3]매출 [4]거래 [0]종료");
            System.out.print("메뉴 선택: ");
            String choice = scanner.next();
            
            // 0. 종료 시 메모리 데이터를 파일로 즉시 저장
            if (choice.equals("0")) {
                save();
                System.out.println("데이터를 저장하고 시스템을 종료합니다.");
                break;
            }

            switch (choice) {
                case "1":
                    System.out.println("상품관리");
                    break;
                case "2":
                    System.out.println("재고관리");
                    break;
                case "3":
                    System.out.println("매출: [1]일별 조회 [2]기간 조회");
                    String choice2 = scanner.next();
                    switch (choice2) {
                    case "1":
                    		System.out.println("조회를 원하는 달을 입력해주세요. (예: 5)");
                    		int inputMonth = Integer.parseInt(scanner.next());
                    		System.out.println("조회를 원하는 날짜를 입력해주세요. (예: 15)");
                    		int inputDay = Integer.parseInt(scanner.next());
                    		LocalDate inputDate = LocalDate.of(2026, inputMonth, inputDay);
                    		List<Sale> sales = PosFileManager.loadSalesByDate(inputDate);
                    		for (Sale sale : sales) {
							System.out.println(sale.toString());
						}
                    		break;
                    case "2":
                    		System.out.println("(From)조회를 원하는 달을 입력해주세요. (예: 3)");
                			int startMonth = Integer.parseInt(scanner.next());
                			System.out.println("(From)조회를 원하는 날짜를 입력해주세요. (예: 2)");
                			int startDay = Integer.parseInt(scanner.next());
                			System.out.println("(To)조회를 원하는 달을 입력해주세요. (예: 5)");
                    		int endMonth = Integer.parseInt(scanner.next());
                    		System.out.println("(To)조회를 원하는 날짜를 입력해주세요. (예: 15)");
                    		int endDay = Integer.parseInt(scanner.next());
                    		LocalDate startDate = LocalDate.of(2026, startMonth, startDay);
                    		LocalDate endDate = LocalDate.of(2026, endMonth, endDay);
                    		List<Sale> salesPeriod = PosFileManager.loadSalesByPeriod(startDate, endDate);
                    		BigDecimal sumPeriod = new BigDecimal(0);
                    		for (Sale sale : salesPeriod) {
                    			sumPeriod.add(sale.getTotalPrice());
                    			System.out.println(sale.toString());
                    		}
                    		System.out.println("기간 내의 매출액 합산: " + sumPeriod);
                    		break;
                    	default:
                    		System.out.println("잘못된 메뉴 선택입니다.");
                    		break;
                    }
                    break;
                case "4":
                    System.out.println("거래: [1]거래요청 [2]거래내역 [3]거래취소");
                    String choice3 = scanner.next();
                    switch(choice3) {
                    case "1":
                    		// TODO: (여러 개?) 바코드 입력 받아서(바로 Product 로 들어올 것) Product[] 에 들어가고 그걸로 Sale 생성
                    		break;
                    case "2":
                    		for (Sale sale : this.history) {
								System.out.println(sale.toString());
						}
                    		break;
                    case "3":
                    		// TODO: 거래취소
                    		break;
                    default:
                    		break;
                    }
                    break;
                default:
                    System.out.println("잘못된 메뉴 선택입니다.");
                    break;
            }
        }
        scanner.close();
    }
}