package Pos.main;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import Pos.order.Order;
import Pos.product.Product;
import Pos.sale.Sale;

public class PosMain {
	
	private String pwd;
	public Product[] products;
	private Sale[] history;
	private BigDecimal dailySalesAmount;
	private Order[] orders;
	
	LocalDateTime now = LocalDateTime.now();
	
	Scanner sc = new Scanner(System.in);
	
	public Sale[] getHistory() {
		// 전체 판매 기록 조회
		System.out.println("=======전체 판매 기록 조회=======");

		// 최신순으로 정렬해서 보여주기 위한 복사본 생성
		Sale[] descHistory = Arrays.copyOf(history, history.length);
		Arrays.sort(descHistory, Comparator.reverseOrder());
		
		for (Sale s : descHistory) {
			System.out.println(s.toString());
		}
		
		return descHistory;
	}
	

	public Sale[] getHistoryByDate(LocalDate date) {
		// 특정 날짜 판매 기록 조회
		
		System.out.printf("=======%d년 %d월 %d일 판매 기록 조회=======", date.getYear(), date.getMonth(), date.getDayOfMonth());
		
		// 입력받은 날자에 해당하는 sale 기록만 내림차순으로 정렬함
		// System.out::println은 toString 형식으로 출력하므로 toString 별 명시 X
		return Arrays.stream(history)
				.filter(s -> s.soldAt.toLocalDate().equals(date))
				.sorted(Comparator.reverseOrder())
				.peek(System.out::println)
				.toArray(Sale[]::new);
	}
	
	
	
	public BigDecimal getSalesAmount(int month, int day) {
		if (month >= 1 && month <= 12 && day == 0) { // 특정 월 조회
			System.out.printf("=======%d월 판매 총액 조회=======", month);
			
			return Arrays.stream(history)
					.filter(s -> s.soldAt.getMonthValue() == month)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice)
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 필터링된 객체들의 totalPrice값 누적
			
		} else if (month >= 1 && month <= 12 && day >= 1 && day <= 31) { // 특정 일자 조회
			System.out.printf("=======%d월 %d일 판매 총액 조회=======", month, day);
			
			return Arrays.stream(history)
					.filter(s -> s.soldAt.getMonthValue() == month && s.soldAt.getDayOfMonth() == day)
					.filter(s -> s.getStatus() == Sale.saleStatus.COMPLETED) // 취소된 내역 제외
					.map(Sale::getTotalPrice)
					.reduce(BigDecimal.ZERO, BigDecimal::add); // 필터링된 객체들의 totalPrice값 누적
		} else { // 예외
			return null;
		}
	}

	
	public static void main(String[] args) {
		
	}

}