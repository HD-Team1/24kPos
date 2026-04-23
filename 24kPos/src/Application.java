import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Pos.common.OrderStatus;
import Pos.main.PosMain;
import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;
import Pos.sale.Sale.saleStatus;
import Pos.scheduler.StockMonitorDaemon;

public class Application {
	 public static void main(String[] args) {
	        PosMain posMain = PosMain.getInstance();

	        // 자동저장 데몬 스레드 생성
	        StockMonitorDaemon daemon = new StockMonitorDaemon(posMain);

	        // 데몬 스레드 시작
	        daemon.start();

	        // 프로그램 종료 직전 마지막 저장
	        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	            System.out.println("[종료] 마지막 저장 실행");
	            posMain.save();
	            daemon.stop();
	        }));

	        // POS 실행
	        posMain.run();
	    }
}
