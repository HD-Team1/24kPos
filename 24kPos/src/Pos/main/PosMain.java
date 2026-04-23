package Pos.main;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import Pos.order.Order;
import Pos.product.Product;
import java.util.concurrent.CopyOnWriteArrayList;
import Pos.common.OrderStatus;
import Pos.exception.OrderNotFoundException;
import Pos.exception.PosException;
import java.util.Iterator;
import Pos.sale.Sale;

public class PosMain {
	
	private String pwd;
	public Product[] products;
	private Sale[] history;
	private BigDecimal dailySalesAmount;
	private Order[] orders;
	
	LocalDateTime now = LocalDateTime.now();
	
	Scanner sc = new Scanner(System.in);
	
	public Sale[] getHistory() {
		// 전체 판매 기록 조회
		System.out.println("=======전체 판매 기록 조회=======");
    
		// 최신순으로 정렬해서 보여주기 위한 복사본 생성
		Sale[] descHistory = Arrays.copyOf(history, history.length);
		Arrays.sort(descHistory, Comparator.reverseOrder());
		
		for (Sale s : descHistory) {
			System.out.println(s.toString());
		}
		
		return descHistory;
	}
	

	public Sale[] getHistoryByDate(LocalDate date) {
		// 특정 날짜 판매 기록 조회
		
		System.out.printf("=======%d년 %d월 %d일 판매 기록 조회=======", date.getYear(), date.getMonth(), date.getDayOfMonth());
		
		// 입력받은 날자에 해당하는 sale 기록만 내림차순으로 정렬함
		// System.out::println은 toString 형식으로 출력하므로 toString 별 명시 X
		return Arrays.stream(history)
				.filter(s -> s.soldAt.toLocalDate().equals(date))
				.sorted(Comparator.reverseOrder())
				.peek(System.out::println)
				.toArray(Sale[]::new);
	}
	
	
	
	public BigDecimal getSalesAmount(int month, int day) {
		if (month >= 1 && month <= 12 && day == 0) { // 특정 월 조회
			System.out.printf("=======%d월 판매 총액 조회=======", month);
			
			return Arrays.stream(history)
					.filter(s -> s.soldAt.getMonthValue() == month)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice)
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 필터링된 객체들의 totalPrice값 누적
			
		} else if (month >= 1 && month <= 12 && day >= 1 && day <= 31) { // 특정 일자 조회
			System.out.printf("=======%d월 %d일 판매 총액 조회=======", month, day);
			
			return Arrays.stream(history)
					.filter(s -> s.soldAt.getMonthValue() == month && s.soldAt.getDayOfMonth() == day)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice)
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 필터링된 객체들의 totalPrice값 누적
		} else { // 예외
			return null;
		}
	}
	
	
	public Sale makeSale(Product[] requestedProducts) {
		
		Map<String, Integer> requestedMap = new HashMap<>();
		
		// 콜라, 콜라, 맥주와 같이 개별 품목 형식으로 요청이 들어오므로 먼저 HashMap형태로 변환함
		for (Product p : requestedProducts) {
		    requestedMap.put(p.productName,
		            requestedMap.getOrDefault(p.productName, 0) + 1);
		}
		
		// 실제 판매된 상품, 개수
		Map<String, Integer> soldMap = new HashMap<>();

	    // 상품명별로 판매 가능 여부 검사
	    for (Map.Entry<String, Integer> entry : requestedMap.entrySet()) {
	        String productName = entry.getKey();
	        int requestedQuantity = entry.getValue();

	        // 해당 상품 전체 재고 조회
			// Product 정보가 유통기한별로 묶어서 저장돼있으므로 특정 상품에 해당하는 전체 재고를 가져옴
	        Product[] stocks = Arrays.stream(products)
	                .filter(prod -> prod != null)
	                .filter(prod -> prod.productName.equals(productName))
	                .toArray(Product[]::new);
			
			// 만료된 재고는 수량 변경
			int expiredCount = 0;
			for (Product stock : stocks) {
			    if (stock.isExpired() && stock.getQuantity() > 0) {
			        expiredCount += stock.getQuantity();
			        stock.setQuantity(0);
			    }
			}
			
	        if (expiredCount > 0) {
	            System.out.printf("유통기한 만료로 %s %d개를 폐기처리 했습니다.%n", productName, expiredCount);
	        }
			
			// 현재 판매 가능한 재고 개수
			int availableQuantity = Arrays.stream(stocks)
			        .filter(stock -> !stock.isExpired())
			        .mapToInt(Product::getQuantity)
			        .sum();
			
			// 판매하려던 상품이 품절인 경우
			if (availableQuantity < requestedQuantity) {
				System.out.printf("%s이(가) 품절입니다. 현재 구매 가능한 개수는 %d개입니다.", productName, availableQuantity);
				System.out.println("1. 품절 제외 나머지 구매하기 2. 구매 제품 다시 선택하기");
				
				int choice = sc.nextInt();
				
				if (choice == 1) {
					soldMap.put(productName, availableQuantity);
				}
				else if (choice == 2) {
					System.out.println("거래가 취소되었습니다.");
					return null;
				} else {
					System.out.println("잘못된 입력입니다. 거래를 취소합니다.");
					return null;
				}
			}
			
			soldMap.put(productName,  requestedQuantity);
	    }
	    
	    // 재고 차감
	    for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {
	    	String productName = entry.getKey();
	        int saleQuantity = entry.getValue();

	        if (saleQuantity <= 0) continue;

	        Product[] stocks = Arrays.stream(products)
	                .filter(prod -> prod != null)
	                .filter(prod -> prod.productName.equals(productName))
	                .filter(prod -> !prod.isExpired())
	                .filter(prod -> prod.getQuantity() > 0)
	                .sorted(Comparator.comparing(prod -> prod.expiredAt))
	                .toArray(Product[]::new);

	        int remain = saleQuantity;

	        for (Product stock : stocks) {
	            if (remain == 0) break;

	            int stockQty = stock.getQuantity();

	            if (stockQty <= remain) {
	                stock.setQuantity(0);
	                remain -= stockQty;
	            } else {
	                stock.setQuantity(stockQty - remain);
	                remain = 0;
	            }
	        }

	        // 재고 부족 메시지
	        int leftQuantity = Arrays.stream(products)
	                .filter(prod -> prod != null)
	                .filter(prod -> prod.productName.equals(productName))
	                .filter(prod -> !prod.isExpired())
	                .mapToInt(Product::getQuantity)
	                .sum();

	        if (leftQuantity <= 3) {
	            System.out.printf("[재고 부족] %s의 남은 재고는 %d개입니다.%n", productName, leftQuantity);
	        }
	
	    }

	    // 실제로 판매된 개수
	    int totalSoldCount = soldMap.values().stream()
	            .mapToInt(Integer::intValue)
	            .sum();
	    
	    // soldMap 기준으로 Sale용 Product[] 생성
	    Product[] soldProducts = new Product[totalSoldCount];
	    int idx = 0;

	    for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {
	        String productName = entry.getKey();
	        int soldQuantity = entry.getValue();

	        // soldMap에서 Product 배열로 변환하는 과정에서 사용되는 임시값
	        Product sample = Arrays.stream(requestedProducts)
	                .filter(p -> p != null)
	                .filter(p -> p.productName.equals(productName))
	                .findFirst()
	                .orElse(null);

	        for (int i = 0; i < soldQuantity; i++) {
	            soldProducts[idx++] = sample;
	        }
	    }
		
		// 거래 시간과 총 금액은 Sale 생성자 안에서 계산됨
		return new Sale(soldProducts, Sale.saleStatus.COMPLETED);
	}
	
	public boolean cancelSale(Sale sale) {
		if (sale.getStatus() == Sale.saleStatus.CANCELED) {
			// 이미 취소된 내역일 경우 false
			System.out.println("이미 취소된 거래 내역입니다.");
			return false;
		}
		
		// 유통기한이 아직 지나지 않은 제품일 경우 재고 개수 복원
		for (Product p : sale.products) {
			if (p.expiredAt.isBefore(now)) {
				p.setQuantity(p.getQuantity() + 1);
			}
		}
		
		sale.setStatus(Sale.saleStatus.CANCELED);
		
		System.out.println("거래 취소가 완료되었습니다.");
		
		return true;
	}

	
	public static void main(String[] args) {
		
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
		this.orders = new CopyOnWriteArrayList<>(PosFileManager.loadOrders());
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

			products.putIfAbsent(productName, new CopyOnWriteArrayList<>());
			products.get(productName).add(orderedProduct);
		}

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

		boolean removed = stockList.removeIf(stockProduct -> stockProduct.getProductId() == product.getProductId());

		if (stockList.isEmpty()) {
			products.remove(productName);
		}

		return removed;
	}

}