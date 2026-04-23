package Pos.exception.order;

import Pos.exception.PosException;

public abstract class OrderException extends PosException{

	public OrderException(String errorCode, String message) {
		super(errorCode, message);
		// TODO Auto-generated constructor stub
	}

}
