# Data Layer

데이터 접근 및 관리를 담당하는 계층입니다.

## 구조

- `db/` - Room Database (Local SQLite)
  - `entity/` - 데이터베이스 엔티티
  - `dao/` - Data Access Objects
  - `converter/` - TypeConverters
  - `AppDatabase.kt` - Database 싱글톤

- `network/` - Retrofit & API 통신
  - `api/` - API 서비스 정의
  - `interceptor/` - OkHttp 인터셉터
  - `model/` - API 요청/응답 모델

- `repository/` - Repository 구현체
  - Repository는 Domain과 Data를 연결
