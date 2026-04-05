# Shared Ledger App - 개발 진행도

**작성일:** 2026-04-05  
**Phase:** 1 (프로젝트 설정, Room DB, 기본 Navigation, 테마)

---

> **구성:** [Android 앱](#android-앱) | [백엔드 서버 (Node.js)](#백엔드-서버-nodejs)

---

## 📊 전체 진행도

### Android 앱
██████████░░░░░░░░░░ 50% (5/10)

### 백엔드 서버 (Node.js)
░░░░░░░░░░░░░░░░░░░░ 0% (0/8)

---

---

## 📱 Android 앱

### 1️⃣ 프로젝트 초기 설정 ✅
- [x] build.gradle.kts (앱 수준) 작성
- [x] AndroidManifest.xml 기본 설정
- [x] 폴더 구조 생성 (ui, data, domain 등)
- [x] 필수 의존성 추가 (Compose, Room, Retrofit)
- [x] **테스트:** 앱 빌드 및 설치 성공 ✅
- [x] GitHub에 push 완료

---

### 2️⃣ 테마 시스템 구축 (라이트/다크) ✅
- [x] ui/theme/Color.kt (라이트/다크 모드 색상 정의)
- [x] ui/theme/Type.kt (MD3 완전한 타이포그래피)
- [x] ui/theme/Theme.kt (LightTheme & DarkTheme Composable)
- [x] 색상 시스템: Indigo 기반 Primary, Secondary(Teal), Tertiary(Purple) 정의
- [x] **테스트:** 색상/타이포그래피 Preview 확인 ✅

---

### 3️⃣ 테마 시스템 구축 (다크) ✅
- [x] Color.kt에 다크 모드 색상 추가
- [x] Theme.kt에 DarkTheme Composable 추가
- [x] 시스템 기본 테마 감지 (isSystemInDarkTheme)
- [x] **테스트:** 라이트/다크 모드 Preview 확인 ✅

---

### 4️⃣ Navigation 시스템 구축 ✅
- [x] navigation/AppNavigation.kt 작성
- [x] Sealed class Routes 정의
- [x] NavHost 설정 (SplashScreen → LoginScreen → HomeScreen)
- [x] 화면 간 이동 로직 (backStack 제거 포함)
- [x] **테스트:** 화면 전환 동작 확인 ✅

---

### 5️⃣ SplashScreen 구현 ✅
- [x] ui/screens/splash/SplashScreen.kt
- [x] 로고(SL 이니셜) + 앱명 + 서브타이틀 표시
- [x] 페이드인 애니메이션 (0.6초)
- [x] 1.5초 딜레이 후 LoginScreen으로 자동 이동
- [x] **테스트:** 앱 시작 시 스플래시 표시 확인 ✅

---

### 6️⃣ LoginScreen 구현 ⏳
- [ ] ui/screens/login/LoginScreen.kt
- [ ] 이메일/비밀번호 입력 필드
- [ ] 로그인 버튼 (UI만, 기능 미구현)
- [ ] 구글/네이버 OAuth 버튼 (UI만)
- [ ] 회원가입 링크
- [ ] "이 단계 건너뛰기" 버튼
- **테스트:** 레이아웃 및 버튼 렌더링 확인

---

### 7️⃣ HomeScreen 기본 구현 ⏳
- [ ] ui/screens/home/HomeScreen.kt
- [ ] 상단 Header (월 선택, 동기화 상태)
- [ ] 3개 탭 (목록/캘린더/통계) - 탭만 표시
- [ ] FAB (추가 버튼)
- **테스트:** 탭 전환 동작 확인

---

### 8️⃣ Room Database 초기 설정 ⏳
- [ ] data/db/AppDatabase.kt
- [ ] data/db/entity/TransactionEntity.kt (소프트 딜리트 포함)
- [ ] data/db/entity/CategoryEntity.kt
- [ ] data/db/dao/TransactionDao.kt
- [ ] data/db/dao/CategoryDao.kt
- **테스트:** Entity 생성 및 DAO 쿼리 테스트

---

### 9️⃣ Repository 및 ViewModel 기본 구조 ⏳
- [ ] data/repository/TransactionRepository.kt
- [ ] data/repository/CategoryRepository.kt
- [ ] ui/screens/home/HomeViewModel.kt
- [ ] StateFlow로 UI 상태 관리
- **테스트:** ViewModel 초기화 및 데이터 로드 테스트

---

### 🔟 Hilt Dependency Injection 설정 ⏳
- [ ] HiltModule.kt (Database, Repository, ViewModel DI)
- [ ] @HiltAndroidApp 추가 (Application 클래스)
- [ ] @AndroidEntryPoint 추가 (Activity)
- **테스트:** 의존성 주입 동작 확인

---

---

---

## 🖥️ 백엔드 서버 (Node.js)

### S1️⃣ 서버 프로젝트 초기 설정 ⏳
- [ ] 프로젝트 폴더 생성 및 `npm init`
- [ ] Express, cors, dotenv, pg (node-postgres) 등 필수 패키지 설치
- [ ] 폴더 구조 생성 (routes, controllers, middleware, db 등)
- [ ] `.env` 파일 설정 (PORT, DATABASE_URL, JWT_SECRET 등)
- [ ] `app.js` / `server.js` 기본 진입점 작성
- [ ] `GET /api/health` 헬스 체크 엔드포인트 (Render.com sleep wake용)
- **테스트:** 서버 로컬 기동 및 `/api/health` 응답 확인

---

### S2️⃣ Neon PostgreSQL 스키마 생성 ⏳
- [ ] Neon 프로젝트 생성 및 연결 문자열 확보
- [ ] `users` 테이블 생성
- [ ] `ledgers` 테이블 생성
- [ ] `transactions` 테이블 생성
- [ ] `categories` 테이블 생성
- [ ] `shared_ledgers` 테이블 생성
- [ ] `system_settings` 테이블 생성 (동적 로그인 모드)
- [ ] `user_notification_apps` 테이블 생성
- **테스트:** Neon 콘솔에서 테이블 생성 확인

---

### S3️⃣ 인증 API (이메일) ⏳
- [ ] `POST /api/auth/signup` - 이메일 회원가입 (bcrypt 해싱)
- [ ] `POST /api/auth/login` - 이메일 로그인 + JWT 발급
- [ ] `POST /api/auth/refresh` - 토큰 갱신
- [ ] JWT 미들웨어 (`authMiddleware.js`)
- **테스트:** 회원가입 → 로그인 → 토큰 검증 흐름 확인

---

### S4️⃣ 인증 API (OAuth) ⏳
- [ ] `POST /api/auth/google` - Google OAuth 토큰 검증 + JWT 발급
- [ ] `POST /api/auth/naver` - Naver OAuth 토큰 검증 + JWT 발급
- [ ] OAuth 신규/기존 유저 분기 처리 (upsert)
- **테스트:** Google/Naver 토큰으로 로그인 및 JWT 수신 확인

---

### S5️⃣ 거래 및 카테고리 CRUD API ⏳
- [ ] `GET/POST /api/ledgers/:ledgerId/transactions` - 거래 조회/추가
- [ ] `PATCH/DELETE /api/transactions/:id` - 거래 수정/삭제 (소프트 딜리트)
- [ ] `GET/POST /api/ledgers/:ledgerId/categories` - 카테고리 조회/추가
- [ ] `PATCH/DELETE /api/categories/:id` - 카테고리 수정/삭제
- [ ] 권한 체크 미들웨어 (장부 소유자 or 공유 편집 권한)
- **테스트:** CRUD 전체 흐름 및 권한 거부 케이스 확인

---

### S6️⃣ 동기화 API ⏳
- [ ] `POST /api/sync` - Delta sync (마지막 동기화 시간 이후 변경분 반환)
- [ ] `POST /api/sync/batch` - 클라이언트 변경사항 일괄 업로드
- [ ] 충돌 해결 로직 (타임스탬프 기반 최신 우선)
- [ ] 동기화 상태 필드 관리 (`syncStatus`: synced / pending / conflict)
- **테스트:** 오프라인 변경 후 동기화 시 데이터 정합성 확인

---

### S7️⃣ 공유 장부 API ⏳
- [ ] `POST /api/ledgers/:ledgerId/share` - 사용자 초대 (이메일로 검색)
- [ ] `PATCH /api/shared-ledgers/:id` - 권한 변경 (view/edit)
- [ ] `DELETE /api/shared-ledgers/:id` - 공유 해제
- [ ] `GET /api/ledgers/shared` - 나와 공유된 장부 목록 조회
- **테스트:** 초대 → 권한 변경 → 해제 흐름 확인

---

### S8️⃣ Render.com 배포 ⏳
- [ ] `render.yaml` 또는 Render 대시보드 서비스 설정
- [ ] 환경변수 등록 (DATABASE_URL, JWT_SECRET, 각 OAuth 키)
- [ ] 배포 후 `/api/health` 응답 확인
- [ ] Android 앱의 BASE_URL을 Render.com 주소로 교체
- **테스트:** 실기기에서 서버 API 호출 성공 확인

---

## 📝 마지막 업데이트

**2026-04-05** | 5단계 SplashScreen 완료 (페이드인 애니메이션, 자동 이동)

---

## 🎯 다음 단계

**1단계 완료 후:** 사용자 승인 → 3단계 (sample 폴더에서 프로젝트 복사) → 4단계 (구현 시작)
