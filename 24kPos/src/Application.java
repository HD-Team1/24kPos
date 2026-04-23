import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Pos.main.PosMain;
import Pos.order.Order;
import Pos.order.Order.orderStatus;
import Pos.product.Product;
import Pos.sale.Sale;
import Pos.sale.Sale.saleStatus;

public class Application {

    public static void main(String[] args) {
        PosMain pos = PosMain.getInstance();

        System.out.println("=== POS 테스트 시작 ===");

        // 1. 초기 재고 세팅
        Product cola1 = new Product(
                1L,
                "콜라",
                new BigDecimal("2000"),
                LocalDateTime.now().plusDays(7)
        );
        cola1.setQuantity(10);

        Product cola2 = new Product(
                2L,
                "콜라",
                new BigDecimal("2000"),
                LocalDateTime.now().plusDays(5)
        );
        cola2.setQuantity(5);

        Product water1 = new Product(
                3L,
                "물",
                new BigDecimal("1000"),
                LocalDateTime.now().plusDays(30)
        );
        water1.setQuantity(20);

        List<Product> colaList = new ArrayList<>();
        colaList.add(cola1);
        colaList.add(cola2);

        List<Product> waterList = new ArrayList<>();
        waterList.add(water1);

        Map<String, List<Product>> products = pos.getProducts();
        products.put("콜라", colaList);
        products.put("물", waterList);

        System.out.println("\n[1] 초기 재고 세팅 완료");
        printPosData(pos);

        // 2. 발주 처리 테스트
        Product cola3 = new Product(
                4L,
                "콜라",
                new BigDecimal("2000"),
                LocalDateTime.now().plusDays(10)
        );
        cola3.setQuantity(8);

        Product water2 = new Product(
                5L,
                "물",
                new BigDecimal("1000"),
                LocalDateTime.now().plusDays(20)
        );
        water2.setQuantity(5);

        Order order1 = new Order(
                new Product[]{cola3, water2},
                orderStatus.REQUESTED
        );

        boolean orderResult = pos.setProducts(order1);

        System.out.println("\n[2] 발주 처리 결과: " + orderResult);
        printPosData(pos);

        // 3. 판매 처리 테스트
        // 콜라 12개, 물 3개 판매 요청
        Product soldCola = new Product(
                100L,
                "콜라",
                new BigDecimal("2000"),
                LocalDateTime.now()
        );
        soldCola.setQuantity(12);

        Product soldWater = new Product(
                101L,
                "물",
                new BigDecimal("1000"),
                LocalDateTime.now()
        );
        soldWater.setQuantity(3);

        Sale sale1 = new Sale(
                new Product[]{soldCola, soldWater},
                saleStatus.COMPLETED
        );

        boolean saleResult = pos.setProducts(sale1);

        System.out.println("\n[3] 판매 처리 결과: " + saleResult);
        System.out.println("콜라는 유통기한이 더 임박한 재고부터 차감되어야 함");
        printPosData(pos);

        // 4. 파일 저장
        pos.save();
        System.out.println("\n[4] 파일 저장 완료");

        // 5. 메모리 초기화
        pos.getProducts().clear();
        pos.getOrders().clear();
        pos.getHistory().clear();

        System.out.println("\n[5] 메모리 초기화 후");
        printPosData(pos);

        // 6. 파일에서 다시 불러오기
        pos.load();

        System.out.println("\n[6] load() 이후");
        printPosData(pos);

        System.out.println("\n=== POS 테스트 종료 ===");
    }

    private static void printPosData(PosMain pos) {
        System.out.println("[상품 목록]");
        if (pos.getProducts().isEmpty()) {
            System.out.println("  (비어 있음)");
        } else {
            for (Map.Entry<String, List<Product>> entry : pos.getProducts().entrySet()) {
                System.out.println("상품명: " + entry.getKey());
                for (Product product : entry.getValue()) {
                    System.out.println("  - " + product);
                }
            }
        }

        System.out.println("\n[주문 목록]");
        if (pos.getOrders().isEmpty()) {
            System.out.println("  (비어 있음)");
        } else {
            for (Order order : pos.getOrders()) {
                System.out.println("  - " + order);
            }
        }

        System.out.println("\n[판매 목록]");
        if (pos.getHistory().isEmpty()) {
            System.out.println("  (비어 있음)");
        } else {
            for (Sale sale : pos.getHistory()) {
                System.out.println("  - " + sale);
            }
        }
    }
}