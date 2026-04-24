package Pos.exception;

public class InvalidSaleException extends PosException {
    public InvalidSaleException(String message) {
        super("SALE_400", message);
    }
}