package Pos.exception.sale;

public class InvalidBarcodeException extends SaleException{

	public InvalidBarcodeException(String errorCode, String message) {
		super(errorCode, "유효하지 않은 바코드입니다: " + message);
	}

}
