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
	private BigDecimal dailySalesAmount;

	private static PosMain instance = new PosMain();

	private List<Sale> history;
	private Map<String, List<Product>> products; // 제품명을 키로 사용
	private List<Order> orders = new CopyOnWriteArrayList<>(); // [수정] ArrayList는 멀티스레드에서 불안전합니다. CopyOnWriteArrayList
																// 사용

	LocalDateTime now = LocalDateTime.now();

	Scanner sc = new Scanner(System.in);

	public List<Sale> getHistoryByDate(LocalDate date) {

		System.out.printf("=======%d년 %d월 %d일 판매 기록 조회=======%n", date.getYear(), date.getMonthValue(), // 🔥 getMonth()
																										// 말고
																										// getMonthValue()
				date.getDayOfMonth());

		return history.stream().filter(s -> s.soldAt.toLocalDate().equals(date)).sorted(Comparator.reverseOrder()) // 최신순
																													// (내림차순)
				.peek(System.out::println).toList(); // Java 16+
	}

	public BigDecimal getSalesAmount(int month, int day) {
		if (month >= 1 && month <= 12 && day == 0) { // 특정 월 조회
			System.out.printf("=======%d월 판매 총액 조회=======%n", month);

			return history.stream().filter(s -> s.soldAt.getMonthValue() == month)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

		} else if (month >= 1 && month <= 12 && day >= 1 && day <= 31) { // 특정 일자 조회
			System.out.printf("=======%d월 %d일 판매 총액 조회=======%n", month, day);

			return history.stream().filter(s -> s.soldAt.getMonthValue() == month && s.soldAt.getDayOfMonth() == day)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
		} else {
			throw new IllegalArgumentException("월 또는 일 입력값이 올바르지 않습니다.");
		}
	}

	public Sale makeSale(List<Product> requestedProducts) {

		Map<String, Integer> requestedMap = new HashMap<>();

		// 1. 요청 상품 → 개수 집계
		for (Product p : requestedProducts) {
			requestedMap.put(p.productName, requestedMap.getOrDefault(p.productName, 0) + p.getQuantity());
		}

		Map<String, Integer> soldMap = new HashMap<>();

		// 2. 판매 가능 여부 검사
		for (Map.Entry<String, Integer> entry : requestedMap.entrySet()) {

			String productName = entry.getKey();
			int requestedQuantity = entry.getValue();

			List<Product> stockList = products.get(productName);

			if (stockList == null || stockList.isEmpty()) {
				return null;
			}

			// 유통기한 만료 처리
			int expiredCount = 0;
			for (Product stock : stockList) {
				if (stock.isExpired() && stock.getQuantity() > 0) {
					expiredCount += stock.getQuantity();
					stock.setQuantity(0);
				}
			}

			if (expiredCount > 0) {
				System.out.printf("유통기한 만료로 %s %d개 폐기%n", productName, expiredCount);
			}

			// 현재 판매 가능한 수량
			int availableQuantity = stockList.stream().filter(p -> !p.isExpired()).mapToInt(Product::getQuantity).sum();

			if (availableQuantity < requestedQuantity) {
				System.out.printf("%s 재고 부족 (%d개 가능)%n", productName, availableQuantity);
				return null;
			}

			soldMap.put(productName, requestedQuantity);
		}

		
		// 3. Sale용 List 생성
		List<Product> soldProducts = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {

			String productName = entry.getKey();
			int qty = entry.getValue();

			Product sample = requestedProducts.stream().filter(p -> p.productName.equals(productName)).findFirst()
					.orElse(null);

			if (sample != null) {
				Product sold = new Product(productName, sample.productPrice, LocalDateTime.now());
				sold.setQuantity(qty);
				soldProducts.add(sold);
			}
		}

		return new Sale(soldProducts, Sale.saleStatus.COMPLETED);
	}

	public List<Order> getOrders() {
		return orders;
	}

	public Order makeOrder(List<Product> products) {
		if (products == null) {
			throw new PosException("ORDER_400", "상품 목록이 null입니다.");
		}

		Order newOrder = new Order(products, OrderStatus.REQUESTED);

		orders.add(newOrder); // order list 에 추가
		setProducts(newOrder); // 재고 관리를 위해 setProducts도 호출
		return newOrder;
	}

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
		this.pwd = password;
	}

	// 로그인
	public boolean login(String inputPassword) {
		if (this.pwd == null) {
			System.out.println("비밀번호가 설정되지 않았습니다.");
			return false;
		}

		return this.pwd.equals(inputPassword);
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

		// 1단계: 먼저 전체 판매 가능 여부 검사
		for (Product soldProduct : sale.getProducts()) {
			String productName = soldProduct.getProductName();
			int requiredQuantity = soldProduct.getQuantity();

			List<Product> stockList = products.get(productName);

			if (stockList == null || stockList.isEmpty()) {
				return false;
			}

			int totalStock = 0;
			for (Product stockProduct : stockList) {
				totalStock += stockProduct.getQuantity();
			}

			if (totalStock < requiredQuantity) {
				return false; // 총 재고 부족 -> 거래 불가
			}
		}

		// 2단계: 실제 FIFO 차감
		for (Product soldProduct : sale.getProducts()) {
			String productName = soldProduct.getProductName();
			int requiredQuantity = soldProduct.getQuantity();

			List<Product> stockList = products.get(productName);

			// 유통기한 빠른 순 정렬 = 선입선출처럼 처리
			stockList.sort(Comparator.comparing(Product::getExpiredAt));

			int remainToReduce = requiredQuantity;
			List<Product> toRemove = new ArrayList<>();

			for (Product stockProduct : stockList) {
				if (remainToReduce <= 0) {
					break;
				}

				int stockQuantity = stockProduct.getQuantity();

				if (stockQuantity <= remainToReduce) {
					remainToReduce -= stockQuantity;
					stockProduct.setQuantity(0);
					toRemove.add(stockProduct);
				} else {
					stockProduct.setQuantity(stockQuantity - remainToReduce);
					remainToReduce = 0;
				}
			}

			stockList.removeAll(toRemove);

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

	public Sale processSale(List<Product> requestedProducts) {
		Sale sale = makeSale(requestedProducts);

		if (sale == null) {
			return null;
		}

		boolean success = setProducts(sale);

		if (!success) {
			return null;
		}

		return sale;
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

		// POS기 가동 -> 메인 메뉴
		while (true) {
			System.out.println("\n[1]상품관리 [2]재고현황 [3]매출 [4]거래 [0]종료");
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
				System.out.println("상품관리 메뉴");
				System.out.println("[1]상품 조회 [2]상품 폐기 [3]발주내역 조회");
				System.out.println("[4]상품발주 등록 [0]돌아가기");
				System.out.print("상세 메뉴 선택: ");
				String subChoice = scanner.next();

				switch (subChoice) {
				case "1": // 상품 조회
					System.out.println("\n====================================================");
					System.out.println("                  [ 전체 상품 목록 ]                   ");
					System.out.println("====================================================");

					if (products.isEmpty()) {
						System.out.println("등록된 상품이 없습니다.");
					} else {
						for (String name : products.keySet()) {
							List<Product> list = products.get(name);

							// 이 상품의 총 재고 합치기
							int totalQty = list.stream().mapToInt(Product::getQuantity).sum();
							// 첫 번째 상품에서 가격 가져오기
							java.math.BigDecimal price = list.get(0).productPrice;

							// [기본 정보 출력]
							System.out.println("상품명: " + name);
							System.out.println("   └─ 가격: " + price + "원");
							System.out.println("   └─ 총 재고: " + totalQty + "개 ");

							// [상세 재고 정보]
							System.out.println("   [상세 재고]");
							for (Product p : list) {
								String expiredInfo = p.isExpired() ? "※ 유통기한만료 ※" : "정상";
								System.out.println("     └─ ID: " + p.getProductId() + " | 수량: " + p.getQuantity()
										+ " | 기한: " + p.getExpiredAt() + " [" + expiredInfo + "]");
							}

							// [관련 발주 내역]
							System.out.println("   [발주 내역]");
							boolean foundOrder = false;
							for (Order o : orders) {
								for (Product op : o.getProducts()) {
									if (op.getProductName().equals(name)) {
										// 상태, 상품 수, 주문시간 출력
										System.out.println("     └─ 주문ID: #" + o.getOrderId() + " | 상태: ["
												+ o.getStatus() + "]" + " | 발주수: "
												+ (o.getProducts() == null ? 0 : o.getProducts().size()) + "개"
												+ " | 일시: " + o.toString().split("주문시간=")[1]);
										foundOrder = true;
										break;
									}
								}
							}
							if (!foundOrder) {
								System.out.println("     └─ 기록 없음");
							}

							System.out.println("\n----------------------------------------------------");
						}
					}
					break;

				case "2": // 상품 폐기
					System.out.println("\n====================================================");
					System.out.println("                  [  상품 폐기  ]                   ");
					System.out.println("====================================================");
					System.out.print("폐기할 상품의 ID를 입력하세요: ");
					long inputId = scanner.nextLong();

					Product targetProduct = null;
					String targetName = null;

					for (Map.Entry<String, List<Product>> entry : products.entrySet()) {
						List<Product> list = entry.getValue();
						for (Product p : list) {
							if (p.getProductId() == inputId) {
								targetProduct = p;
								targetName = entry.getKey();
								break;
							}
						}
						if (targetProduct != null)
							break;
					}

					if (targetProduct != null) {
						System.out.println(" 폐기 대상 확인: " + targetName + " (ID: " + inputId + ")");
						System.out.print("정말 폐기하시겠습니까? (y/n): ");
						if (scanner.next().equalsIgnoreCase("y")) {

							if (setProducts(targetProduct)) {
								System.out.println("해당 상품이 재고에서 폐기되었습니다.");
							} else {
								System.out.println("폐기 처리 중 오류가 발생했습니다.");
							}
						} else {
							System.out.println("폐기가 취소되었습니다.");
						}
					} else {
						System.out.println(" 입력하신 상품ID (" + inputId + ") 에 해당하는 상품을 찾을 수 없습니다.");
					}

					break;

				case "3": // 발주내역 조회
					System.out.println("\n====================================================");
					System.out.println("                [ 전체 발주 내역 조회 ]                ");
					System.out.println("====================================================");

					if (orders.isEmpty()) {
						System.out.println("현재 등록된 발주 내역이 없습니다.");
					} else {
						for (Order o : orders) {
							System.out.println(o.toString());

							// 상세 목록
							if (o.getProducts() != null) {
								System.out.print("   └─ 포함 품목: ");
								for (int i = 0; i < o.getProducts().size(); i++) {
									Product p = o.getProducts().get(i);
									System.out.print(p.getProductName() + "(" + p.getQuantity() + "개)");

									if (i < o.getProducts().size() - 1) {
										System.out.print(", ");
									}
								}
								System.out.println();
							}
							System.out.println("----------------------------------------------------");
						}
						System.out.println("총 " + orders.size() + "건의 발주 내역이 조회되었습니다.");
					}
					break;

				case "4": // 상품발주 등록
					System.out.println("\n====================================================");
					System.out.println("                [ 신규 상품 발주 등록 ]                ");
					System.out.println("====================================================");

					// 1. 순수하게 상품 정보만 입력받기 (ID는 신경 안 써도 됨!)
					System.out.print(" 발주 상품명: ");
					String name = scanner.next();

					System.out.print(" 발주 단가: ");
					java.math.BigDecimal price = scanner.nextBigDecimal();

					System.out.print(" 발주 수량: ");
					int qty = scanner.nextInt();

					// 클래스 내부에서 Product ID를 자동 생성
					Product p = new Product(name, price, LocalDateTime.now().plusDays(7));
					p.setQuantity(qty);

					List<Product> orderItems = new ArrayList<>();
					orderItems.add(p);
					Order newOrder = makeOrder(orderItems);

					orders.add(newOrder);

					if (newOrder != null) {
						System.out.println(" 발주 등록 및 재고 반영이 완료되었습니다.");
						System.out.println("   - 생성된 발주번호: #" + newOrder.getOrderId());
						System.out.println("   - 생성된 상품고유ID: " + p.getProductId());
						System.out.println("   - 품목: " + name + " x " + qty);
					} else {
						System.out.println(" 발주 등록에 실패했습니다.");
					}
					break;
				default:
					System.out.println("잘못된 선택입니다.");
				}
				break;
			case "2":
				System.out.println("\n====================================================");
				System.out.println("                 [ 재고 현황 상세 ]                  ");
				System.out.println("====================================================");

				Map<String, List<Product>> stock = this.getProducts();

				if (stock.isEmpty()) {
				    System.out.println("현재 등록된 재고가 없습니다.");
				    break;
				}

				for (Map.Entry<String, List<Product>> entry : stock.entrySet()) {
				    String name = entry.getKey();
				    List<Product> list = entry.getValue();

				    int total = list.stream().mapToInt(Product::getQuantity).sum();
				    BigDecimal price = list.get(0).productPrice;

				    String status = (total <= 5) ? "⚠ 품절임박" : "정상";

				    // 요약
				    System.out.println("상품명: " + name + " | 가격: " + price + " | 총재고: " + total + " | 상태: " + status);

				    // 🔥 여기 핵심: productId 출력
				    System.out.println("   [상세 목록]");
				    for (Product p : list) {
				        System.out.println("   └─ ID: " + p.getProductId()
				                + " | 수량: " + p.getQuantity()
				                + " | 유통기한: " + p.getExpiredAt());
				    }

				    System.out.println("----------------------------------------------------");
				}
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
				System.out.println("거래: [1]거래요청 [2]거래내역");
				String choice3 = scanner.next();
				switch (choice3) {
				case "1":
					System.out.println("\n====================================================");
					System.out.println("                  [ 거래 요청 ]");
					System.out.println("====================================================");

					List<Product> requestProducts = new ArrayList<>();

					while (true) {
						System.out.print("상품명 입력 (종료하려면 0): ");
						String productName = scanner.next();

						if ("0".equals(productName)) {
							break;
						}

						System.out.print("수량 입력: ");
						int quantity = scanner.nextInt();

						// 요청용 Product
						Product requestProduct = new Product(productName, BigDecimal.ZERO, LocalDateTime.now());
						requestProduct.setQuantity(quantity);

						requestProducts.add(requestProduct);
					}

					if (requestProducts.isEmpty()) {
						System.out.println("입력된 상품이 없습니다. 거래를 취소합니다.");
						break;
					}

					Sale sale = processSale(requestProducts);

					if (sale == null) {
						System.out.println("거래 실패");
					} else {
						System.out.println("거래 완료");
						System.out.println(sale);
					}
					break;
				case "2":
					for (Sale sale1 : this.history) {
						System.out.println(sale1.toString());
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