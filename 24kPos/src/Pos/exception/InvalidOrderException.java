package Pos.exception;

public class InvalidOrderException extends PosException {
    public InvalidOrderException(String message) {
        super("ORDER_400", message);
    }
}