package Pos.exception.product;

import Pos.exception.PosException;

public abstract class ProductException extends PosException{

	public ProductException(String errorCode, String message) {
		super(errorCode, message);
		// TODO Auto-generated constructor stub
	}

}
