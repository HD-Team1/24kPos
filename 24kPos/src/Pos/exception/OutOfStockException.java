package Pos.exception;

public class OutOfStockException extends PosException {
    public OutOfStockException(String productName, int requested, int available) {
        super("STOCK_400",
                productName + " 재고 부족: 요청 수량=" + requested + ", 가능 수량=" + available);
    }
}