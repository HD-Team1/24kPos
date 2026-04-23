package Pos.exception.sale;

import Pos.exception.PosException;

public abstract class SaleException extends PosException{

	public SaleException(String errorCode, String message) {
		super(errorCode, message);
		// TODO Auto-generated constructor stub
	}

}
