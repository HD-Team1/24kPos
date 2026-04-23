package Pos.main;

import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import Pos.common.OrderStatus;
import Pos.exception.OrderNotFoundException;
import Pos.exception.PosException;
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

	private List<Order> orders = new CopyOnWriteArrayList<>(); // [수정] ArrayList는 멀티스레드에서 불안전합니다. CopyOnWriteArrayList
																// 사용!

	public Order[] getOrders() {
		return orders.toArray(new Order[0]);
	}

	public Order makeOrder(Product[] products) {
	    if (products == null) {
	        throw new PosException("ORDER_400", "상품 목록이 null입니다.");
	    }

	    Order newOrder = new Order(products, OrderStatus.REQUESTED);
	    orders.add(newOrder); //order list 에 추가
	    setProducts(newOrder); //재고 관리를 위해 setProducts도 호출
	    return newOrder;
	}


	private static PosMain instance = new PosMain();
	private String password;

	private List<Sale> history;
	private Map<String, List<Product>> products; // 제품명을 키로 사용

	// 싱글톤 생성자
	private PosMain() {
		this.history = new ArrayList<>();
		this.products = new HashMap<>();
		this.orders = new CopyOnWriteArrayList<>();
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
    
    public Product barcodeScan(Product product) {
    		return product;
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
                    		Product barcode = null;
                    		List<Product> products = new ArrayList<Product>();
                    		do {
                    				// TODO: (코드 상에서) 여러 품목 바코드찍기 // 그런데 현실이랑 비슷한건 null 될때까지 받는것보다 원하는만큼 받은다음에 버튼 누르면 완료인데 ...
								barcode = this.barcodeScan(null);
								products.add(barcode);
							} while (barcode != null);
                    		break;
                    case "2":
                    		for (Sale sale : this.history) {
								System.out.println(sale.toString());
						}
                    		break;
                    case "3":
                    		System.out.println("거래취소를 원하는 거래의 ID를 입력해주세요.");
                    		String inputId = scanner.next();
                    		for (Sale sale : this.history) {
                    			if (inputId.equals(sale.getSaleId())) this.history.remove(sale);
                    		}
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