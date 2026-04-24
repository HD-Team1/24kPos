package Pos.exception;

public class ProductNotFoundException extends PosException {
    public ProductNotFoundException(String productName) {
        super("PRODUCT_404", "상품을 찾을 수 없습니다: " + productName);
    }
}