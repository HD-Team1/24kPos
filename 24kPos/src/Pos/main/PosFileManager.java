package Pos.main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;

public class PosFileManager {
    private static final String BASE_DIR = "C:\\data"; // C드라이브 밑에 data폴더

    private PosFileManager() {}

    // stock용: data/stock/2026/04/2026-04-23.txt
    private static Path createTodayStockFilePath() throws IOException {
        LocalDate now = LocalDate.now();

        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String fileName = String.format("%d-%02d-%02d.txt",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth());

        Path dirPath = Paths.get(BASE_DIR, "stock", year, month);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        return dirPath.resolve(fileName);
    }

    // order / sale용: data/order/2026/04/23/1.txt
    private static Path createTodayItemFilePath(String category, int id) throws IOException {
        LocalDate now = LocalDate.now();

        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());
        String fileName = id + ".txt";

        Path dirPath = Paths.get(BASE_DIR, category, year, month, day);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        return dirPath.resolve(fileName);
    }

    // 특정 날짜 폴더 경로
    private static Path getDatedDirectoryPath(String category, LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());
        String day = String.format("%02d", date.getDayOfMonth());

        return Paths.get(BASE_DIR, category, year, month, day);
    }

    // ---------------- stock 저장 / 불러오기 ----------------

    public static void saveProducts(Map<String, List<Product>> products) {
        try {
            Path filePath = createTodayStockFilePath();

            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
                oos.writeObject(products);
            }

            System.out.println("상품 데이터 저장 완료: " + filePath);

        } catch (IOException e) {
            System.out.println("상품 데이터 저장 실패");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, List<Product>> loadProducts() {
        try {
            Path filePath = createTodayStockFilePath();

            if (!Files.exists(filePath)) {
                return new HashMap<>();
            }

            try (ObjectInputStream ois =
                         new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
                return (Map<String, List<Product>>) ois.readObject();
            }

        } catch (Exception e) {
            System.out.println("상품 데이터 불러오기 실패");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ---------------- order 저장 / 불러오기 ----------------

    // 주문 1건 저장
    public static void saveOrder(Order order) {
        try {
            Path filePath = createTodayItemFilePath("order", order.getOrderId());

            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
                oos.writeObject(order);
            }

            System.out.println("발주 데이터 저장 완료: " + filePath);

        } catch (IOException e) {
            System.out.println("발주 데이터 저장 실패");
            e.printStackTrace();
        }
    }

    // 오늘 날짜 기준 모든 주문 읽기
    public static List<Order> loadOrders() {
        return loadOrdersByDate(LocalDate.now());
    }

    // 특정 날짜 주문 읽기
    public static List<Order> loadOrdersByDate(LocalDate date) {
        List<Order> orders = new ArrayList<>();
        Path dirPath = getDatedDirectoryPath("order", date);

        if (!Files.exists(dirPath)) {
            return orders;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
            for (Path path : stream) {
                try (ObjectInputStream ois =
                             new ObjectInputStream(new FileInputStream(path.toFile()))) {
                    Order order = (Order) ois.readObject();
                    orders.add(order);
                } catch (Exception e) {
                    System.out.println("발주 파일 불러오기 실패: " + path);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("발주 폴더 조회 실패");
            e.printStackTrace();
        }

        return orders;
    }
    
    public static List<Order> loadOrdersByPeriod(LocalDate start, LocalDate end) {
        List<Order> orders = new ArrayList<>();

        if (start.isAfter(end)) {
            return orders;
        }

        LocalDate current = start;

        while (!current.isAfter(end)) {
            Path dirPath = getDatedDirectoryPath("order", current);

            if (Files.exists(dirPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
                    for (Path path : stream) {
                        try (ObjectInputStream ois =
                                     new ObjectInputStream(new FileInputStream(path.toFile()))) {
                            Order order = (Order) ois.readObject();
                            orders.add(order);
                        } catch (Exception e) {
                            System.out.println("발주 파일 불러오기 실패: " + path);
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("발주 폴더 조회 실패: " + dirPath);
                    e.printStackTrace();
                }
            }

            current = current.plusDays(1);
        }

        return orders;
    }

    // 여러 주문 한 번에 저장
    public static void saveOrders(List<Order> orders) {
        for (Order order : orders) {
            saveOrder(order);
        }
    }

    // ---------------- sale 저장 / 불러오기 ----------------

    // 판매 1건 저장
    public static void saveSale(Sale sale) {
        try {
            Path filePath = createTodayItemFilePath("sale", sale.getSaleId());

            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
                oos.writeObject(sale);
            }

            System.out.println("판매 데이터 저장 완료: " + filePath);

        } catch (IOException e) {
            System.out.println("판매 데이터 저장 실패");
            e.printStackTrace();
        }
    }

    // 오늘 날짜 기준 모든 판매 읽기
    public static List<Sale> loadSales() {
        return loadSalesByDate(LocalDate.now());
    }

    // 특정 날짜 판매 읽기
    public static List<Sale> loadSalesByDate(LocalDate date) {
        List<Sale> sales = new ArrayList<>();
        Path dirPath = getDatedDirectoryPath("sale", date);

        if (!Files.exists(dirPath)) {
            return sales;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
            for (Path path : stream) {
                try (ObjectInputStream ois =
                             new ObjectInputStream(new FileInputStream(path.toFile()))) {
                    Sale sale = (Sale) ois.readObject();
                    sales.add(sale);
                } catch (Exception e) {
                    System.out.println("판매 파일 불러오기 실패: " + path);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("판매 폴더 조회 실패");
            e.printStackTrace();
        }

        return sales;
    }

    public static List<Sale> loadSalesByPeriod(LocalDate start, LocalDate end) {
        List<Sale> sales = new ArrayList<>();

        if (start.isAfter(end)) {
            return sales;
        }

        LocalDate current = start;

        while (!current.isAfter(end)) {
            Path dirPath = getDatedDirectoryPath("sale", current);

            if (Files.exists(dirPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
                    for (Path path : stream) {
                        try (ObjectInputStream ois =
                                     new ObjectInputStream(new FileInputStream(path.toFile()))) {
                            Sale sale = (Sale) ois.readObject();
                            sales.add(sale);
                        } catch (Exception e) {
                            System.out.println("판매 파일 불러오기 실패: " + path);
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("판매 폴더 조회 실패: " + dirPath);
                    e.printStackTrace();
                }
            }

            current = current.plusDays(1);
        }

        return sales;
    }
    
    // 여러 판매 한 번에 저장
    public static void saveSales(List<Sale> sales) {
        for (Sale sale : sales) {
            saveSale(sale);
        }
    }
}