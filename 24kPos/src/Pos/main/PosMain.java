package Pos.main;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import Pos.common.OrderStatus;
import Pos.order.Order;
import Pos.product.Product;

public class PosMain {
    
    private final List<Order> orders = new CopyOnWriteArrayList<>(); // [수정] ArrayList는 멀티스레드에서 불안전합니다. CopyOnWriteArrayList 사용!

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
//    public static void main(String[] args) {
//        PosMain pos = new PosMain();
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
//    }
//    =========================================================================================================
}