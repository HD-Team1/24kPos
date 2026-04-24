# 🏪 편의점 재고 관리 시스템 (24kPos)

#### 편의점에서 상품의 입고, 판매, 재고 관리 및 유통기한을 효율적으로 관리하기 위한 시스템

---

## 🚀 프로젝트 소개

기존 편의점 재고 관리 방식은 단순 수량 중심으로 관리되어  
유통기한 관리 및 재고 소진 최적화에 어려움이 있었습니다.

이 프로젝트는 **유통기한 기반 재고 관리(FIFO)**와 **실시간 재고 흐름 추적**을 통해  
보다 효율적인 상품 관리 시스템을 구현하는 것을 목표로 개발되었습니다.

---

## 🛠 기술 스택 (Tech Stack)

- **Language**: Java  
- **Architecture**: 객체지향 설계 (OOP)  
- **Collection**: Map, List, Set  
- **Design Pattern**: Singleton  
- **Thread**: Daemon Thread  
- **I/O**: File I/O (DataOutputStream, ObjectStream)  
- **기타**: 예외 처리, Stream API 일부 적용  

---

## 📂 프로젝트 구조

```
src
├── Pos.common        # 공통 상수 및 Enum
│    ├── Const.java
│    └── OrderStatus.java
│
├── Pos.exception     # 커스텀 예외 처리
│    ├── PosException.java
│    ├── OutOfStockException.java
│    ├── ProductNotFoundException.java
│    └── …
│
├── Pos.product       # 상품 도메인
│    └── Product.java
│
├── Pos.order         # 주문 도메인
│    └── Order.java
│
├── Pos.sale          # 판매 도메인
│    └── Sale.java
│
├── Pos.main          # 핵심 실행 로직 (POS)
│    ├── PosMain.java
│    └── PosFileManager.java
│
├── Pos.scheduler     # 백그라운드 작업
│    └── StockMonitorDaemon.java
│
└── Application.java  # 프로그램 진입점
```
---

## ⚙️ 주요 기능

- 📦 상품 입고 (재고 추가)
- 🛒 상품 판매 (재고 차감)
- 📊 재고 조회 (상품별 관리)
- ⏰ 유통기한 기반 자동 정렬 (FIFO 방식)
- 💾 파일 저장 및 불러오기
- 📅 일별 매출 조회

---

## 💡 핵심 구현 내용

### 1. 유통기한 기반 재고 관리 (FIFO)

- 동일 상품을 여러 개의 객체로 분리하여 관리
- 유통기한이 빠른 상품부터 판매되도록 설계

👉 재고 구조 예시
콜라:
* 2026-04-28 (5개)
* 2026-04-30 (10개)

---

### 2. Collection 기반 재고 구조 설계

- `Map<String, List<Product>>` 구조 사용
- 상품명 기준으로 그룹화하고, 유통기한별로 관리

👉 설계 의도:
- 빠른 조회 (Key 기반 접근)
- 유통기한 정렬 및 FIFO 처리 용이

---

### 3. 파일 기반 데이터 영속성

- 프로그램 종료 후에도 데이터 유지
- `DataOutputStream`, `ObjectOutputStream` 활용

👉 설계 의도:
- DB 없이도 상태 유지
- 경량 시스템 구성

---

### 4. 공통 예외 처리 설계

- 재고 부족 (`OutOfStockException`)
- 존재하지 않는 상품 (`ProductNotFoundException`)
- 잘못된 입력 (`InvalidQuantityException` 등)

👉 설계 의도:
- 비즈니스 로직과 에러 처리 분리
- 명확한 에러 메시지 제공

---

### 5. 데몬 스레드를 활용한 재고 모니터링 ⭐

- `StockMonitorDaemon`을 통해 백그라운드에서 재고 상태 관리

👉 설계 의도:
- 메인 로직과 독립적인 작업 처리
- 실시간 재고 상태 감시 구조 구현

---


## 📈 성과

- 유통기한 기반 재고 관리 시스템 구현
- 객체지향 설계 능력 향상
- 예외 처리 및 파일 I/O 경험 확보
- 멀티스레드(데몬 스레드) 활용 경험
