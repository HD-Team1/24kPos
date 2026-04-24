package Pos.main;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import Pos.common.OrderStatus;
import Pos.exception.InvalidOrderException;
import Pos.exception.InvalidQuantityException;
import Pos.exception.InvalidSaleException;
import Pos.exception.OutOfStockException;
import Pos.exception.PosException;
import Pos.exception.ProductNotFoundException;
import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;

public class PosMain {

    // =========================
    // 1. Static Field
    // =========================
    private static PosMain instance = new PosMain();

    // =========================
    // 2. Member Fields
    // =========================
    private String pwd;
    private BigDecimal dailySalesAmount;

    private List<Sale> history;
    private Map<String, List<Product>> products;
    private List<Order> orders;

    // =========================
    // 3. Constructor
    // =========================
    private PosMain() {
        this.history = new ArrayList<>();
        this.products = new HashMap<>();
        this.orders = new CopyOnWriteArrayList<>();
    }

    // =========================
    // 4. Singleton
    // =========================
    public static PosMain getInstance() {
        return instance;
    }

    // =========================
    // 5. Authentication
    // =========================
    public void setPassword(String password) {
        this.pwd = password;
    }

    public boolean login(String inputPassword) {
        if (this.pwd == null) {
            System.out.println("비밀번호가 설정되지 않았습니다.");
            return false;
        }

        return this.pwd.equals(inputPassword);
    }

    // =========================
    // 6. Getter
    // =========================
    public List<Sale> getHistory() {
        return history;
    }

    public Map<String, List<Product>> getProducts() {
        return products;
    }

    public List<Order> getOrders() {
        return orders;
    }

    // =========================
    // 7. File Save / Load
    // =========================
    public void save() {
        PosFileManager.saveProducts(products);
        PosFileManager.saveOrders(orders);
        PosFileManager.saveSales(history);
    }

    public void load() {
        this.products = PosFileManager.loadProducts();
        this.orders = new CopyOnWriteArrayList<>(PosFileManager.loadOrders());
        this.history = PosFileManager.loadSales();
    }

    // =========================
    // 8. Sales Query
    // =========================
    public List<Sale> getHistoryByDate(LocalDate date) {
        System.out.printf("=======%d년 %d월 %d일 판매 기록 조회=======%n",
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth());

        return history.stream()
                .filter(s -> s.soldAt.toLocalDate().equals(date))
                .sorted(Comparator.reverseOrder())
                .peek(System.out::println)
                .toList();
    }

    public BigDecimal getSalesAmount(int month, int day) {
        if (month < 1 || month > 12) {
            throw new PosException("DATE_400", "월 입력값이 올바르지 않습니다. month=" + month);
        }

        if (day < 0 || day > 31) {
            throw new PosException("DATE_400", "일 입력값이 올바르지 않습니다. day=" + day);
        }

        return history.stream()
                .filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED)
                .filter(s -> s.soldAt.getMonthValue() == month)
                .filter(s -> day == 0 || s.soldAt.getDayOfMonth() == day)
                .map(Sale::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =========================
    // 9. Order
    // =========================
    public Order makeOrder(List<Product> products) {
        if (products == null || products.isEmpty()) {
            throw new InvalidOrderException("발주 상품 목록이 비어 있습니다.");
        }

        for (Product product : products) {
            if (product == null) {
                throw new InvalidOrderException("발주 상품에 null이 포함되어 있습니다.");
            }

            if (product.getQuantity() <= 0) {
                throw new InvalidQuantityException(product.getQuantity());
            }
        }

        Order newOrder = new Order(products, OrderStatus.REQUESTED);

        orders.add(newOrder);
        setProducts(newOrder);

        return newOrder;
    }

    public boolean setProducts(Order order) {
        if (order == null) {
            throw new InvalidOrderException("발주 정보가 null입니다.");
        }

        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new InvalidOrderException("발주 상품 목록이 비어 있습니다.");
        }

        for (Product orderedProduct : order.getProducts()) {
            if (orderedProduct == null) {
                throw new InvalidOrderException("발주 상품에 null이 포함되어 있습니다.");
            }

            if (orderedProduct.getQuantity() <= 0) {
                throw new InvalidQuantityException(orderedProduct.getQuantity());
            }

            String productName = orderedProduct.getProductName();

            products.putIfAbsent(productName, new CopyOnWriteArrayList<>());
            products.get(productName).add(orderedProduct);
        }

        return true;
    }

    // =========================
    // 10. Sale
    // =========================
    public Sale makeSale(List<Product> requestedProducts) {
        if (requestedProducts == null || requestedProducts.isEmpty()) {
            throw new InvalidSaleException("판매 요청 상품이 비어 있습니다.");
        }

        Map<String, Integer> requestedMap = new HashMap<>();

        for (Product p : requestedProducts) {
            if (p == null) {
                throw new InvalidSaleException("판매 요청 상품에 null이 포함되어 있습니다.");
            }

            if (p.getQuantity() <= 0) {
                throw new InvalidQuantityException(p.getQuantity());
            }

            requestedMap.put(
                    p.getProductName(),
                    requestedMap.getOrDefault(p.getProductName(), 0) + p.getQuantity()
            );
        }

        Map<String, Integer> soldMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : requestedMap.entrySet()) {
            String productName = entry.getKey();
            int requestedQuantity = entry.getValue();

            List<Product> stockList = products.get(productName);

            if (stockList == null || stockList.isEmpty()) {
                throw new ProductNotFoundException(productName);
            }

            int expiredCount = stockList.stream()
                    .filter(Product::isExpired)
                    .mapToInt(Product::getQuantity)
                    .sum();

            if (expiredCount > 0) {
                System.out.printf("[판매 제외] 유통기한 만료 상품: %s %d개%n",
                        productName,
                        expiredCount);
            }

            int availableQuantity = stockList.stream()
                    .filter(p -> !p.isExpired())
                    .mapToInt(Product::getQuantity)
                    .sum();

            if (availableQuantity < requestedQuantity) {
                throw new OutOfStockException(productName, requestedQuantity, availableQuantity);
            }

            soldMap.put(productName, requestedQuantity);
        }

        List<Product> soldProducts = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : soldMap.entrySet()) {
            String productName = entry.getKey();
            int quantity = entry.getValue();

            List<Product> stockList = products.get(productName);

            Product sample = stockList.stream()
                    .filter(p -> !p.isExpired())
                    .findFirst()
                    .orElseThrow(() -> new ProductNotFoundException(productName));

            Product sold = new Product(productName, sample.productPrice, sample.getExpiredAt());
            sold.setQuantity(quantity);
            soldProducts.add(sold);
        }

        return new Sale(soldProducts, Sale.saleStatus.COMPLETED);
    }

    public Sale processSale(List<Product> requestedProducts) {
        Sale sale = makeSale(requestedProducts);
        setProducts(sale);
        return sale;
    }

    public boolean setProducts(Sale sale) {
        if (sale == null || sale.getProducts() == null || sale.getProducts().isEmpty()) {
            throw new InvalidSaleException("판매 정보가 올바르지 않습니다.");
        }

        validateSaleStock(sale);
        decreaseStockBySale(sale);

        history.add(sale);
        return true;
    }

    private void validateSaleStock(Sale sale) {
        for (Product soldProduct : sale.getProducts()) {
            String productName = soldProduct.getProductName();
            int requiredQuantity = soldProduct.getQuantity();

            if (requiredQuantity <= 0) {
                throw new InvalidQuantityException(requiredQuantity);
            }

            List<Product> stockList = products.get(productName);

            if (stockList == null || stockList.isEmpty()) {
                throw new ProductNotFoundException(productName);
            }

            int totalStock = stockList.stream()
                    .mapToInt(Product::getQuantity)
                    .sum();

            if (totalStock < requiredQuantity) {
                throw new OutOfStockException(productName, requiredQuantity, totalStock);
            }
        }
    }

    private void decreaseStockBySale(Sale sale) {
        for (Product soldProduct : sale.getProducts()) {
            String productName = soldProduct.getProductName();
            int requiredQuantity = soldProduct.getQuantity();

            List<Product> stockList = products.get(productName);
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
    }

    // =========================
    // 11. Disposal
    // =========================
    public boolean setProducts(Product product) {
        if (product == null) {
            throw new PosException("PRODUCT_400", "폐기할 상품 정보가 null입니다.");
        }

        String productName = product.getProductName();
        List<Product> stockList = products.get(productName);

        if (stockList == null || stockList.isEmpty()) {
            throw new ProductNotFoundException(productName);
        }

        boolean removed = stockList.removeIf(
                stockProduct -> stockProduct.getProductId() == product.getProductId()
        );

        if (!removed) {
            throw new ProductNotFoundException(productName + " / ID=" + product.getProductId());
        }

        if (stockList.isEmpty()) {
            products.remove(productName);
        }

        return true;
    }

    // =========================
    // 12. Main Run Loop
    // =========================
    public void run() {
        Scanner scanner = new Scanner(System.in);

        setPassword("12345678");
        load();

        loginLoop(scanner);

        System.out.println("====================================");
        System.out.println("   편의점 POS 시스템을 시작합니다.   ");
        System.out.println("====================================");

        while (true) {
            try {
                printMainMenu();

                String choice = scanner.next();

                if (choice.equals("0")) {
                    save();
                    System.out.println("데이터를 저장하고 시스템을 종료합니다.");
                    break;
                }

                switch (choice) {
                    case "1":
                        handleProductMenu(scanner);
                        break;
                    case "2":
                        printStockStatus();
                        break;
                    case "3":
                        handleSalesMenu(scanner);
                        break;
                    case "4":
                        handleTradeMenu(scanner);
                        break;
                    default:
                        System.out.println("잘못된 메뉴 선택입니다.");
                }

            } catch (PosException e) {
                System.out.println("[POS 오류] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("[입력 오류] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[알 수 없는 오류] " + e.getMessage());
            }
        }

        scanner.close();
    }

    // =========================
    // 13. Run Helper Methods
    // =========================
    private void loginLoop(Scanner scanner) {
        while (true) {
            System.out.println("====================================");
            System.out.print("   비밀번호를 입력하세요:    ");

            if (login(scanner.next())) {
                System.out.println("로그인 성공!");
                break;
            }

            System.out.println("비밀번호가 틀렸습니다.");
        }
    }

    private void printMainMenu() {
        System.out.println("\n[1]상품관리 [2]재고현황 [3]매출 [4]거래 [0]종료");
        System.out.print("메뉴 선택: ");
    }

    private void handleProductMenu(Scanner scanner) {
        System.out.println("상품관리 메뉴");
        System.out.println("[1]상품 조회 [2]상품 폐기 [3]발주내역 조회");
        System.out.println("[4]상품발주 등록 [0]돌아가기");
        System.out.print("상세 메뉴 선택: ");

        String subChoice = scanner.next();

        switch (subChoice) {
            case "1":
                printProductList();
                break;
            case "2":
                disposeProduct(scanner);
                break;
            case "3":
                printOrderList();
                break;
            case "4":
                registerOrder(scanner);
                break;
            case "0":
                break;
            default:
                System.out.println("잘못된 선택입니다.");
        }
    }

    private void printProductList() {
        System.out.println("\n====================================================");
        System.out.println("                  [ 전체 상품 목록 ]                   ");
        System.out.println("====================================================");

        if (products.isEmpty()) {
            System.out.println("등록된 상품이 없습니다.");
            return;
        }

        for (String name : products.keySet()) {
            List<Product> list = products.get(name);

            int totalQty = list.stream()
                    .mapToInt(Product::getQuantity)
                    .sum();

            BigDecimal price = list.get(0).productPrice;

            System.out.println("상품명: " + name);
            System.out.println("   └─ 가격: " + price + "원");
            System.out.println("   └─ 총 재고: " + totalQty + "개");

            System.out.println("   [상세 재고]");
            for (Product p : list) {
                String expiredInfo = p.isExpired() ? "※ 유통기한만료 ※" : "정상";

                System.out.println("     └─ ID: " + p.getProductId()
                        + " | 수량: " + p.getQuantity()
                        + " | 기한: " + p.getExpiredAt()
                        + " [" + expiredInfo + "]");
            }

            printRelatedOrders(name);

            System.out.println("\n----------------------------------------------------");
        }
    }

    private void printRelatedOrders(String productName) {
        System.out.println("   [발주 내역]");

        boolean foundOrder = false;

        for (Order o : orders) {
            for (Product op : o.getProducts()) {
                if (op.getProductName().equals(productName)) {
                    System.out.println("     └─ 주문ID: #" + o.getOrderId()
                            + " | 상태: [" + o.getStatus() + "]"
                            + " | 발주수: " + o.getProducts().size() + "개"
                            + " | 일시: " + o.toString().split("주문시간=")[1]);
                    foundOrder = true;
                    break;
                }
            }
        }

        if (!foundOrder) {
            System.out.println("     └─ 기록 없음");
        }
    }

    private void disposeProduct(Scanner scanner) {
        System.out.println("\n====================================================");
        System.out.println("                  [  상품 폐기  ]                   ");
        System.out.println("====================================================");
        System.out.print("폐기할 상품의 ID를 입력하세요: ");

        long inputId = scanner.nextLong();

        Product targetProduct = null;
        String targetName = null;

        for (Map.Entry<String, List<Product>> entry : products.entrySet()) {
            for (Product p : entry.getValue()) {
                if (p.getProductId() == inputId) {
                    targetProduct = p;
                    targetName = entry.getKey();
                    break;
                }
            }

            if (targetProduct != null) {
                break;
            }
        }

        if (targetProduct == null) {
            throw new PosException("PRODUCT_404",
                    "입력하신 상품ID에 해당하는 상품을 찾을 수 없습니다. ID=" + inputId);
        }

        System.out.println("폐기 대상 확인: " + targetName + " (ID: " + inputId + ")");
        System.out.print("정말 폐기하시겠습니까? (y/n): ");

        if (scanner.next().equalsIgnoreCase("y")) {
            setProducts(targetProduct);
            System.out.println("해당 상품이 재고에서 폐기되었습니다.");
        } else {
            System.out.println("폐기가 취소되었습니다.");
        }
    }

    private void printOrderList() {
        System.out.println("\n====================================================");
        System.out.println("                [ 전체 발주 내역 조회 ]                ");
        System.out.println("====================================================");

        if (orders.isEmpty()) {
            System.out.println("현재 등록된 발주 내역이 없습니다.");
            return;
        }

        for (Order o : orders) {
            System.out.println(o);

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

    private void registerOrder(Scanner scanner) {
        System.out.println("\n====================================================");
        System.out.println("                [ 신규 상품 발주 등록 ]                ");
        System.out.println("====================================================");

        System.out.print("발주 상품명: ");
        String name = scanner.next();

        System.out.print("발주 단가: ");
        BigDecimal price = scanner.nextBigDecimal();

        System.out.print("발주 수량: ");
        int qty = scanner.nextInt();

        Product p = new Product(name, price, LocalDateTime.now().plusDays(7));
        p.setQuantity(qty);

        List<Product> orderItems = new ArrayList<>();
        orderItems.add(p);

        Order newOrder = makeOrder(orderItems);

        System.out.println("발주 등록 및 재고 반영이 완료되었습니다.");
        System.out.println("   - 생성된 발주번호: #" + newOrder.getOrderId());
        System.out.println("   - 생성된 상품고유ID: " + p.getProductId());
        System.out.println("   - 품목: " + name + " x " + qty);
    }

    private void printStockStatus() {
        System.out.println("\n====================================================");
        System.out.println("                 [ 재고 현황 상세 ]                  ");
        System.out.println("====================================================");

        Map<String, List<Product>> stock = getProducts();

        if (stock.isEmpty()) {
            System.out.println("현재 등록된 재고가 없습니다.");
            return;
        }

        for (Map.Entry<String, List<Product>> entry : stock.entrySet()) {
            String productName = entry.getKey();
            List<Product> list = entry.getValue();

            int total = list.stream()
                    .mapToInt(Product::getQuantity)
                    .sum();

            BigDecimal productPrice = list.get(0).productPrice;
            String status = total <= 5 ? "⚠ 품절임박" : "정상";

            System.out.println("상품명: " + productName
                    + " | 가격: " + productPrice
                    + " | 총재고: " + total
                    + " | 상태: " + status);

            System.out.println("   [상세 목록]");
            for (Product product : list) {
                System.out.println("   └─ ID: " + product.getProductId()
                        + " | 수량: " + product.getQuantity()
                        + " | 유통기한: " + product.getExpiredAt());
            }

            System.out.println("----------------------------------------------------");
        }
    }

    private void handleSalesMenu(Scanner scanner) {
        System.out.println("매출: [1]일별 조회 [2]기간 조회");
        String salesChoice = scanner.next();

        switch (salesChoice) {
            case "1":
                printSalesByDate(scanner);
                break;
            case "2":
                printSalesByPeriod(scanner);
                break;
            default:
                System.out.println("잘못된 메뉴 선택입니다.");
        }
    }

    private void printSalesByDate(Scanner scanner) {
        System.out.print("조회를 원하는 달을 입력해주세요. 예: 5 : ");
        int inputMonth = Integer.parseInt(scanner.next());

        System.out.print("조회를 원하는 날짜를 입력해주세요. 예: 15 : ");
        int inputDay = Integer.parseInt(scanner.next());

        LocalDate inputDate = LocalDate.of(2026, inputMonth, inputDay);
        List<Sale> sales = PosFileManager.loadSalesByDate(inputDate);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Sale sale : sales) {
            System.out.println(sale);

            if (sale.getStatus() == Sale.saleStatus.COMPLETED) {
                totalAmount = totalAmount.add(sale.getTotalPrice());
            }
        }

        System.out.println("----------------------------------------------------");
        System.out.println(inputDate + " 매출 총 금액은 " + totalAmount + "원입니다.");
    }

    private void printSalesByPeriod(Scanner scanner) {
        System.out.print("(From) 월 입력: ");
        int startMonth = Integer.parseInt(scanner.next());

        System.out.print("(From) 일 입력: ");
        int startDay = Integer.parseInt(scanner.next());

        System.out.print("(To) 월 입력: ");
        int endMonth = Integer.parseInt(scanner.next());

        System.out.print("(To) 일 입력: ");
        int endDay = Integer.parseInt(scanner.next());

        LocalDate startDate = LocalDate.of(2026, startMonth, startDay);
        LocalDate endDate = LocalDate.of(2026, endMonth, endDay);

        List<Sale> salesPeriod = PosFileManager.loadSalesByPeriod(startDate, endDate);
        BigDecimal sumPeriod = BigDecimal.ZERO;

        for (Sale sale : salesPeriod) {
            sumPeriod = sumPeriod.add(sale.getTotalPrice());
            System.out.println(sale);
        }

        System.out.println("기간 내의 매출액 합산: " + sumPeriod);
    }

    private void handleTradeMenu(Scanner scanner) {
        System.out.println("거래: [1]거래요청 [2]거래내역");
        String tradeChoice = scanner.next();

        switch (tradeChoice) {
            case "1":
                requestTrade(scanner);
                break;
            case "2":
                printTradeHistory();
                break;
            default:
                System.out.println("잘못된 메뉴 선택입니다.");
        }
    }

    private void requestTrade(Scanner scanner) {
        System.out.println("\n====================================================");
        System.out.println("                  [ 거래 요청 ]");
        System.out.println("====================================================");
        
        printStockStatus();

        List<Product> requestProducts = new ArrayList<>();

        while (true) {
            System.out.print("상품명 입력 (상품 입력 종료를 원하시면 0번을 눌러주세요): ");
            String productName = scanner.next();

            if ("0".equals(productName)) {
                break;
            }

            System.out.print("수량 입력: ");
            int quantity = scanner.nextInt();

            Product requestProduct = new Product(
                    productName,
                    BigDecimal.ZERO,
                    LocalDateTime.now()
            );
            requestProduct.setQuantity(quantity);

            requestProducts.add(requestProduct);
        }

        if (requestProducts.isEmpty()) {
            System.out.println("입력된 상품이 없습니다. 거래를 취소합니다.");
            return;
        }

        Sale processedSale = processSale(requestProducts);

        System.out.println("거래 완료");
        System.out.println(processedSale);
    }

    private void printTradeHistory() {
        if (history.isEmpty()) {
            System.out.println("거래 내역이 없습니다.");
            return;
        }

        for (Sale sale : history) {
            System.out.println(sale);
        }
    }
}