package Pos.exception;

public class OrderNotFoundException extends PosException{

	public OrderNotFoundException(String errorCode, String orderId) {
		super(errorCode, "주문을 찾을 수 없습니다. orderId= " + orderId);
	}

}
