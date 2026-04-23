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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import Pos.sale.Sale;

public class PosMain {
	
	Scanner sc = new Scanner(System.in);

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
    
    // 전체 판매 기록 조회
    public List<Sale> getHistory() {
        System.out.println("=======전체 판매 기록 조회=======");

        // 최신순 정렬
        List<Sale> descHistory = new ArrayList<>(history);
        descHistory.sort(Comparator.reverseOrder());

        for (Sale s : descHistory) {
            System.out.println(s);
        }

        return descHistory;
    }
    
    // 특정 일자 판매 기록 조회
    public List<Sale> getHistoryByDate(LocalDate date) {
        System.out.printf("=======%d년 %d월 %d일 판매 기록 조회=======",
                date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        List<Sale> result = history.stream()
                .filter(s -> s.soldAt.toLocalDate().equals(date))
                .sorted(Comparator.reverseOrder())
                .toList();

        result.forEach(System.out::println);

        return result;
    }

    // 월별/일별 판매 총액 조회
    public BigDecimal getSalesAmount(int month, int day) {
        if (month < 1 || month > 12 || day < 0 || day > 31) {
        	// 예외처리 추가 
            return null;
        }
        
        // day == 0일 경우 특정 월 조회, month&day값 둘 다 존재할 경우 특정 일자 조회

        System.out.printf(day == 0
                ? "=======%d월 판매 총액 조회======="
                : "=======%d월 %d일 판매 총액 조회=======",
                month, day);

        return history.stream()
                .filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED)
                .filter(s -> s.soldAt.getMonthValue() == month)
                .filter(s -> day == 0 || s.soldAt.getDayOfMonth() == day)
                .map(Sale::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // 판매 거래
    public Sale makeSale(List<Product> requestedProducts) {
    	
    	Map<String, Integer> requestedMap = new HashMap<>();
    	
    	// 콜라, 콜라, 맥주와 같이 개별 품목 형식으로 요청이 들어오므로 먼저 HashMap 형태로 변환함
    	for (Product p : requestedProducts) {
    		if (p == null) continue;
    		requestedMap.put(p.productName, requestedMap.getOrDefault(p.productName, 0) + 1);
    	}
    	
    	// 상품별로 판매 가능 여부 검사 및 유통기한 만료, 품절 관련 처리
    	Map<String, Integer> soldMap = checkAvailability(requestedMap);

    	// 재고 차감
    	deductStock(soldMap);

    	// 실제로 판매된 개수
    	int totalSoldCount = soldMap.values().stream()
    			.mapToInt(Integer::intValue)
    			.sum();

    	// soldMap 기준으로 Sale에 전달할 상품 목록 생성
    	Product[] soldProducts = new Product[totalSoldCount];
    	int idx = 0;

    	for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {
    		String productName = entry.getKey();
    		int soldQuantity = entry.getValue();

    		if (soldQuantity <= 0) continue;

    		// 요청 상품 목록에서 해당 상품의 샘플 Product를 찾음
    		Product sample = null;
    		for (Product p : requestedProducts) {
    			if (p != null && p.productName.equals(productName)) {
    				sample = p;
    				break;
    			}
    		}

    		if (sample == null) continue;

    		for (int i = 0; i < soldQuantity; i++) {
    			soldProducts[idx++] = sample;
    		}
    	}

    	// 거래 시간과 총 금액은 Sale 생성자 안에서 계산됨
    	return new Sale(soldProducts, Sale.saleStatus.COMPLETED);
    }
    
    private Map<String, Integer> checkAvailability(Map<String, Integer> requestedMap) {
    	// 실제 판매된 상품, 개수
    	Map<String, Integer> soldMap = new HashMap<>();

    	// 상품명별로 판매 가능 여부 검사
    	for (Map.Entry<String, Integer> entry : requestedMap.entrySet()) {
    		String productName = entry.getKey();
    		int requestedQuantity = entry.getValue();

    		// 해당 상품 전체 재고 조회
    		// products는 제품명을 키로 사용하므로 해당 상품명에 해당하는 재고 목록을 가져옴
    		List<Product> stocks = products.get(productName);
    		if (stocks == null) {
    			stocks = new ArrayList<>();
    		}

    		// 만료된 재고는 수량 변경
    		int expiredCount = 0;
    		for (Product stock : stocks) {
    			if (stock != null && stock.isExpired() && stock.getQuantity() > 0) {
    				expiredCount += stock.getQuantity();
    				stock.setQuantity(0);
    			}
    		}

    		if (expiredCount > 0) {
    			System.out.printf("유통기한 만료로 %s %d개를 폐기처리 했습니다.%n", productName, expiredCount);
    		}

    		// 현재 판매 가능한 재고 개수
    		int availableQuantity = 0;
    		for (Product stock : stocks) {
    			if (stock != null && !stock.isExpired()) {
    				availableQuantity += stock.getQuantity();
    			}
    		}

    		// 판매하려던 상품이 품절인 경우
    		if (availableQuantity < requestedQuantity) {
    			System.out.printf("%s이(가) 품절입니다. 현재 구매 가능한 개수는 %d개입니다.%n", productName, availableQuantity);
    			System.out.println("1. 품절 제외 나머지 구매하기 2. 구매 제품 다시 선택하기");

    			int choice = sc.nextInt();

    			if (choice == 1) {
    				soldMap.put(productName, availableQuantity);
    			} else if (choice == 2) {
    				System.out.println("거래가 취소되었습니다.");
    				return null;
    			} else {
    				System.out.println("잘못된 입력입니다. 거래를 취소합니다.");
    				return null;
    			}
    		} else {
    			soldMap.put(productName, requestedQuantity);
    		}
    	}
    	
    	return soldMap;
    }
    
    // 재고 차감
    private void deductStock(Map<String, Integer> soldMap) {
    	// 상품별 처리
    	for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {
    		String productName = entry.getKey();
    		int saleQuantity = entry.getValue();

    		if (saleQuantity <= 0) continue;

    		// 상품명이 동일한 물품의 전체 재고를 가져옴
    		List<Product> stocks = products.get(productName);
    		// 상품 자체가 존재하지 않을 경우
    		if (stocks == null) continue;

    		// 판매 가능한 제품 묶음
    		// Product는 제품&유통기한별로 저장되기 때문
    		List<Product> availableStocks = new ArrayList<>();

    		// 유통기한이 지나지 않았고 개수가 1개 이상일 경우 판매 가능 리스트에 추가
    		for (Product stock : stocks) {
    			if (stock != null && !stock.isExpired() && stock.getQuantity() > 0) {
    				availableStocks.add(stock);
    			}
    		}

    		// 선입선출을 위해 유통기한 오름차순으로 정렬
    		availableStocks.sort(Comparator.comparing(prod -> prod.expiredAt));

    		// 아직 판매 처리중인 수량
    		int remain = saleQuantity;

    		for (Product stock : availableStocks) {
    			if (remain == 0) break; // 모든 상품 처리가 완료된 경우

    			int stockQty = stock.getQuantity();

    			if (stockQty <= remain) { 
    				// 재고 수량이 판매해야 하는 수량보다 적을 경우
    				// 재고 수량 0으로 세팅, 판매하지 못한 수량은 remain에 저장 후 다음 묶음에서 처리
    				stock.setQuantity(0);
    				remain -= stockQty;
    			} else {
    				// 재고 수량이 아직 남아있을 경우
    				stock.setQuantity(stockQty - remain);
    				remain = 0;
    			}
    		}

    		// 남은 재고 계산 후 3개 이하일 경우 재고 부족 메세지 출력
    		int leftQuantity = 0;
    		for (Product stock : stocks) {
    			if (stock != null && !stock.isExpired()) {
    				leftQuantity += stock.getQuantity();
    			}
    		}

    		if (leftQuantity <= 3) {
    			System.out.printf("[재고 부족] %s의 남은 재고는 %d개입니다.%n", productName, leftQuantity);
    		}
    	}
    }
    
    // 거래 취소
	public boolean cancelSale(Sale sale) {
		if (sale.getStatus() == Sale.saleStatus.CANCELED) {
			// 이미 취소된 내역일 경우 false
			System.out.println("이미 취소된 거래 내역입니다.");
			return false;
		}
		
		// 유통기한이 아직 지나지 않은 제품일 경우 재고 개수 복원
		for (Product p : sale.products) {
			if (p.expiredAt.isAfter(LocalDateTime.now())) {
				p.setQuantity(p.getQuantity() + 1);
			}
		}

		sale.setStatus(Sale.saleStatus.CANCELED);
		
		System.out.println("거래 취소가 완료되었습니다.");
		
		return true;
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