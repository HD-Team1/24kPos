package Pos.exception;

public class PosException extends RuntimeException{
	private final String errorCode;
	
	public PosException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
	public String getErrCode() {
		return this.errorCode;
	}
}
