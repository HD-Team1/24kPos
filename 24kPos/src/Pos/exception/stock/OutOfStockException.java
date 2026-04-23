package Pos.exception.stock;

public class OutOfStockException extends StockException{

	public OutOfStockException(String errorCode, String productId) {
		super(errorCode, "재고가 없습니다. productId= " + productId);
	}

}
