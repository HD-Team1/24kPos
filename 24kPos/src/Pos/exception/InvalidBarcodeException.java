package Pos.exception;

public class InvalidBarcodeException extends PosException{

	public InvalidBarcodeException(String errorCode, String message) {
		super(errorCode, "유효하지 않은 바코드입니다: " + message);
	}

}
