package Pos.exception.stock;

import Pos.exception.PosException;

public abstract class StockException extends PosException{

	public StockException(String errorCode, String message) {
		super(errorCode, message);
		// TODO Auto-generated constructor stub
	}

}
