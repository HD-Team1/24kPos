package Pos.exception;

public class InvalidQuantityException extends PosException {
    public InvalidQuantityException(int quantity) {
        super("QUANTITY_400", "수량은 1개 이상이어야 합니다: " + quantity);
    }
}