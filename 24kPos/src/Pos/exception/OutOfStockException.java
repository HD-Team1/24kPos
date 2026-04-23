package Pos.exception;

public class OutOfStockException extends PosException{

	public OutOfStockException(String errorCode, String productId) {
		super(errorCode, "재고가 없습니다. productId= " + productId);
	}

}
