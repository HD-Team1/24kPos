package Pos.exception.product;

public class ProductNotFoundException extends ProductException{

	public ProductNotFoundException(String errorCode, String message) {
		super(errorCode, "상품을 찾을 수 없습니다. productId=" + message);
	}
	
}
