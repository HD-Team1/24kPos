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

        Order order1 = new Order(
               
                new Product[]{cola1, water1},
                orderStatus.REQUESTED
        );

        Order order2 = new Order(
                
                new Product[]{cola2},
                orderStatus.COMPLETED
        );

        Sale sale1 = new Sale(
                
                new Product[]{cola1},
                saleStatus.COMPLETED
        );

        Sale sale2 = new Sale(
                
                new Product[]{water1},
                saleStatus.CANCELED
        );

        pos.getOrders().add(order1);
        pos.getOrders().add(order2);

        pos.getHistory().add(sale1);
        pos.getHistory().add(sale2);

        pos.save();
        System.out.println("저장 완료");

        System.out.println("\n=== 저장 직후 메모리 데이터 ===");
        printPosData(pos);

        pos.getProducts().clear();
        pos.getOrders().clear();
        pos.getHistory().clear();

        System.out.println("\n=== 메모리 초기화 후 ===");
        printPosData(pos);

        pos.load();

        System.out.println("\n=== load() 이후 ===");
        printPosData(pos);
    }

    private static void printPosData(PosMain pos) {
        System.out.println("[상품 목록]");
        for (Map.Entry<String, List<Product>> entry : pos.getProducts().entrySet()) {
            System.out.println("상품명: " + entry.getKey());
            for (Product product : entry.getValue()) {
                System.out.println("  - " + product);
            }
        }

        System.out.println("\n[주문 목록]");
        for (Order order : pos.getOrders()) {
            System.out.println(order);
        }

        System.out.println("\n[판매 목록]");
        for (Sale sale : pos.getHistory()) {
            System.out.println(sale);
        }
    }
}