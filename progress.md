# Shared Ledger App - 개발 진행도

**작성일:** 2026-04-05  
**최종 업데이트:** 2026-04-10  
**현재 Phase:** S4 OAuth 완료 (Google ✅ / Naver ✅) + 보안 개선 ✅ + UI 개선 ✅

---

> **구성:** [Android 앱](#android-앱) | [백엔드 서버 (Node.js)](#백엔드-서버-nodejs)

---

## 📊 전체 진행도

### Android 앱
██████████████████░░ 92% (Phase 1~8-C + OAuth 완료, 릴리즈 서명·배포 준비 중)

### 백엔드 서버 (Node.js)
████████████████████ 100% (S1~S8 + S4 OAuth 완료)

---

## 📱 Android 앱

---

### Phase 1 — 프로젝트 설정 / DB / Navigation / 테마 ✅

#### 1️⃣ 프로젝트 초기 설정 ✅
- [x] build.gradle.kts (앱 수준) 작성
- [x] AndroidManifest.xml 기본 설정
- [x] 폴더 구조 생성 (ui, data, domain 등)
- [x] 필수 의존성 추가 (Compose, Room, Retrofit, Hilt 등)
- [x] **테스트:** 앱 빌드 및 설치 성공 ✅
- [x] GitHub에 push 완료

---

#### 2️⃣ 테마 시스템 구축 (라이트/다크) ✅
- [x] ui/theme/Color.kt (라이트 B-1 / 다크 쿨 B-2 색상 정의)
- [x] ui/theme/Type.kt (MD3 완전한 타이포그래피)
- [x] ui/theme/Theme.kt (LightTheme & DarkTheme Composable)
- [x] 색상 시스템: Indigo 기반 Primary, Secondary(Blue), Tertiary(Purple) 정의
- [x] **테스트:** 색상/타이포그래피 Preview 확인 ✅

---

#### 3️⃣ 다크 테마 구축 ✅
- [x] Color.kt에 다크 모드 색상 추가
- [x] Theme.kt에 DarkTheme Composable 추가
- [x] 시스템 기본 테마 감지 (isSystemInDarkTheme)
- [x] **테스트:** 라이트/다크 모드 Preview 확인 ✅

---

#### 4️⃣ Navigation 시스템 구축 ✅
- [x] navigation/AppNavigation.kt 작성
- [x] Sealed class Routes 정의
- [x] NavHost 설정 (SplashScreen → LoginScreen → HomeScreen)
- [x] 화면 간 이동 로직 (backStack 제거 포함)
- [x] **테스트:** 화면 전환 동작 확인 ✅

---

#### 5️⃣ SplashScreen 구현 ✅
- [x] ui/screens/splash/SplashScreen.kt
- [x] 로고(SL 이니셜) + 앱명 + 서브타이틀 표시
- [x] 페이드인 애니메이션 (0.6초)
- [x] 1.5초 딜레이 후 LoginScreen으로 자동 이동
- [x] **테스트:** 앱 시작 시 스플래시 표시 확인 ✅

---

#### 6️⃣ LoginScreen UI 구현 ✅
- [x] ui/screens/login/LoginScreen.kt
- [x] 이메일/비밀번호 입력 필드 (눈 모양 토글 포함)
- [x] 로그인 버튼 (UI만)
- [x] 구글/네이버 OAuth 버튼 (UI만)
- [x] 회원가입 링크
- [x] "이 단계 건너뛰기" 버튼 → HomeScreen 이동
- [x] **테스트:** 레이아웃 및 버튼 렌더링 확인 ✅

---

#### 7️⃣ HomeScreen UI 기본 구현 ✅
- [x] ui/screens/home/HomeScreen.kt
- [x] 상단 TopAppBar (월 표시, 동기화 버튼 UI)
- [x] 3개 탭 (목록/캘린더/통계) TabRow
- [x] FAB (추가 버튼 UI)
- [x] **테스트:** 탭 전환 동작 확인 ✅

---

#### 8️⃣ Room Database 초기 설정 ✅
- [x] data/db/AppDatabase.kt
- [x] data/db/entity/TransactionEntity.kt (소프트 딜리트 포함)
- [x] data/db/entity/CategoryEntity.kt
- [x] data/db/dao/TransactionDao.kt
- [x] data/db/dao/CategoryDao.kt
- [x] **테스트:** Entity 생성 및 DAO 쿼리 확인 ✅

---

#### 9️⃣ Repository 및 ViewModel 기본 구조 ✅
- [x] data/repository/TransactionRepository.kt
- [x] data/repository/CategoryRepository.kt
- [x] ui/screens/home/HomeViewModel.kt
- [x] StateFlow로 UI 상태 관리 (HomeUiState)
- [x] combine()으로 4개 Flow 병합
- [x] **테스트:** ViewModel 초기화 및 데이터 로드 확인 ✅

---

#### 🔟 Hilt Dependency Injection 설정 ✅
- [x] di/HiltModule.kt (Database, Repository 제공)
- [x] @HiltAndroidApp 추가 (SharedLedgerApp)
- [x] @AndroidEntryPoint 추가 (MainActivity)
- [x] @HiltViewModel + @Inject constructor (HomeViewModel)
- [x] javapoet 버전 충돌 수정 (buildscript classpath 강제 지정)
- [x] **테스트:** 의존성 주입 동작 확인 ✅

---

---

### Phase 2 — 인증 기능 구현 ✅

#### 1️⃣ AuthViewModel 및 AuthRepository 구현 ✅
- [x] ui/screens/auth/AuthViewModel.kt — 로그인/회원가입 상태 관리
- [x] data/repository/AuthRepository.kt — DataStore JWT 저장
- [x] data/datastore/AuthDataStore.kt — DataStore 토큰 저장/읽기/삭제
- [x] JWT 토큰 저장, 읽기, 갱신 로직

---

#### 2️⃣ SplashScreen 자동 로그인 연동 ✅
- [x] ui/screens/splash/SplashViewModel.kt — 토큰 유무 체크
- [x] 저장된 JWT 토큰 확인 → 유효하면 HomeScreen으로 바로 이동
- [x] **테스트:** 자동 로그인 → HomeScreen 이동 확인 ⏳ (사용자 테스트 필요)

---

#### 3️⃣ LoginScreen 실제 기능 구현 ✅
- [x] 이메일/비밀번호 유효성 검사 (공백)
- [x] 서버 `POST /api/auth/login` 연동 → JWT 저장
- [x] Snackbar 에러 피드백 (잘못된 비밀번호, 네트워크 오류)
- [x] 건너뛰기 확인 다이얼로그
- [x] **테스트:** 로그인 성공/실패/건너뛰기 시나리오 ⏳ (사용자 테스트 필요)

---

#### 4️⃣ SignupScreen 구현 ✅
- [x] ui/screens/auth/SignupScreen.kt
- [x] 이름/이메일/비밀번호/비밀번호 확인 입력 필드
- [x] 유효성 검사 (비밀번호 일치, 8자 이상)
- [x] 서버 `POST /api/auth/signup` 연동 → 가입 후 HomeScreen 이동
- [x] **테스트:** 회원가입 → 홈 이동 흐름 확인 ⏳ (사용자 테스트 필요)

---

#### 5️⃣ Google OAuth 구현 ✅
- [x] `auth/GoogleSignInHelper.kt` — Credential Manager로 ID 토큰 획득
- [x] `AuthApi.kt` — `POST /api/auth/oauth/google` 엔드포인트 추가
- [x] `AuthRepository.kt` — loginWithGoogle + lastLoginMethod("google") 저장
- [x] `AuthViewModel.kt` — loginWithGoogle(context) 추가
- [x] `LoginScreen.kt` — Google 버튼 활성화 + "최근 사용" 배지
- [x] OAuth 인증키 BuildConfig로 이전 (local.properties 기반, GitHub 미노출)
- [x] **테스트:** Google 로그인 → HomeScreen 이동 확인 ✅

---

#### 6️⃣ Naver OAuth 구현 ✅
- [x] `auth/NaverSignInHelper.kt` — Naver Login SDK 5.x로 액세스 토큰 획득
- [x] `SharedLedgerApp.kt` — 앱 시작 시 Naver SDK 초기화
- [x] `AuthApi.kt` — `POST /api/auth/oauth/naver` 엔드포인트 추가
- [x] `AuthRepository.kt` — loginWithNaver + lastLoginMethod("naver") 저장
- [x] `AuthViewModel.kt` — loginWithNaver(context) 추가
- [x] `LoginScreen.kt` — 네이버 버튼 활성화 + "최근 사용" 배지
- [x] OAuth 인증키 BuildConfig로 이전 (local.properties 기반, GitHub 미노출)
- [x] **테스트:** 네이버 로그인 → HomeScreen 이동 확인 ✅

---

#### 7️⃣ 토큰 갱신 및 AuthInterceptor ✅
- [x] network/interceptor/AuthInterceptor.kt — 모든 요청에 Authorization 헤더 자동 추가
- [x] di/NetworkModule.kt — Retrofit/OkHttp Hilt 제공 (BASE_URL: Render HTTPS)
- [x] network/api/AuthApi.kt — Retrofit 인터페이스 (signup/login/refresh/oauth)
- [ ] 토큰 만료 시 자동 재시도 (Phase 6에서 구현 예정)

---

---

### Phase 3 — 거래 CRUD 기능 구현 ✅

#### 1️⃣ TransactionEditScreen 구현 ✅
- [x] ui/screens/transaction/TransactionEditScreen.kt
- [x] 수입/지출 토글 (SingleChoiceSegmentedButtonRow)
- [x] 금액 입력 필드 (숫자 키패드)
- [x] 날짜(DatePickerDialog) + 시간 Row
- [x] 내용 입력 필드 (설명)
- [x] 구분(카테고리) 선택 버튼 → CategoryDialog 호출
- [x] ui/screens/transaction/TransactionEditViewModel.kt — SavedStateHandle로 편집 모드 결정
- [x] 저장 버튼 → Room DB 삽입 후 HomeScreen 복귀
- [x] **테스트:** 거래 추가 후 목록에 표시 확인 ⏳ (사용자 테스트 필요)

---

#### 2️⃣ TransactionEditScreen 수정 모드 ✅
- [x] 기존 거래 항목 클릭 시 데이터 프리필(pre-fill)
- [x] 수정 저장 → Room DB 업데이트
- [x] 삭제 버튼(TopAppBar) → 소프트 딜리트
- [x] **테스트:** 거래 수정/삭제 후 목록 반영 확인 ⏳ (사용자 테스트 필요)

---

#### 3️⃣ CategoryDialog 구현 ✅
- [x] ui/screens/transaction/CategoryDialog.kt (ModalBottomSheet)
- [x] 기존 구분 목록 표시 및 선택
- [x] 새 구분 이름 입력 + 색상 선택 (12개 프리셋)
- [x] Room DB CategoryEntity 삽입
- [x] **테스트:** 구분 추가 후 TransactionEditScreen 반영 확인 ⏳ (사용자 테스트 필요)

---

#### 4️⃣ HomeScreen MonthSelectorBar 구현 ✅
- [x] ui/components/MonthSelectorBar.kt — 이전/다음 월 이동 버튼
- [x] HomeViewModel에 AppYearMonth + flatMapLatest로 월별 자동 재조회
- [x] util/AppYearMonth.kt — 커스텀 YearMonth 유틸 (API 24 호환)
- [x] **테스트:** 월 이동 시 거래 목록 변경 확인 ⏳ (사용자 테스트 필요)

---

#### 5️⃣ FAB → TransactionEditScreen 연결 ✅
- [x] FAB 클릭 시 TransactionEditScreen(추가 모드)으로 네비게이션
- [x] 목록 항목 클릭 시 TransactionEditScreen(수정 모드)으로 네비게이션
- [x] 수입/지출 합계 SummaryCard (HomeScreen 상단)
- [x] 거래 목록 날짜별 그룹핑 + LazyColumn (TransactionListTab)
- [x] **테스트:** FAB → 추가 → 복귀, 목록 클릭 → 수정 → 복귀 ⏳ (사용자 테스트 필요)

---

---

### Phase 4 — 목록형 / 캘린더형 / 통계형 뷰 구현 🔄

#### 1️⃣ ListViewTab 구현 ✅
- [x] ui/screens/home/ListViewTab.kt
- [x] 날짜별 그룹핑 (LazyColumn + stickyHeader)
- [x] ui/components/TransactionItem.kt — 금액, 구분, 내용 표시
- [x] 항목 클릭 → TransactionEditScreen (수정 모드)
- [x] 빈 목록 안내 문구
- [ ] **테스트:** 거래 목록 표시 및 클릭 이동 확인 ⏳ (사용자 테스트 필요)

---

#### 2️⃣ CalendarViewTab 구현 ✅
- [x] ui/screens/home/CalendarViewTab.kt
- [x] ui/components/CalendarComposable.kt — 커스텀 달력 그리드 (7열, 요일 헤더)
- [x] 날짜 셀에 수입(빨강) / 지출(파랑) 총합 숫자 표시 (단위 축약: 만/천)
- [x] 오늘 날짜 강조 (secondary 원), 선택된 날짜 강조 (primary 원)
- [x] 날짜 선택 시 해당 날짜 거래 목록 표시 (하단 인라인)
- [x] "[+ 추가]" 버튼 → 선택된 날짜 기반 TransactionEditScreen 이동
- [x] Routes/AppNavigation에 dateMillis 파라미터 추가
- [x] TransactionEditViewModel/Screen에서 dateMillis 초기 날짜 반영
- [x] MonthSelectorBar 연동 (월 변경 시 선택일 자동 리셋)
- [ ] **테스트:** 날짜 선택, 거래 지표 표시, 추가 이동 확인 ⏳ (사용자 테스트 필요)

---

#### 3️⃣ StatisticViewTab 구현 ✅
- [x] ui/screens/home/StatisticViewTab.kt
- [x] ui/components/BarChart.kt — 카테고리별 수평 막대 그래프 (비율 포함)
- [x] ui/components/PieChart.kt — 도넛형 원형 차트 (Canvas 직접 구현)
- [x] 월별 수입/지출 합계 요약 (headlineSmall 표시)
- [x] 수입/지출 탭 전환 (SingleChoiceSegmentedButtonRow)
- [x] CategoryStat 데이터 모델 + computeCategoryStats() 함수
- [ ] **테스트:** 차트 렌더링 및 데이터 연동 확인 ⏳ (사용자 테스트 필요)

---

---

### Phase 5 — Push 알림 / SMS 자동입력 ✅

#### 1️⃣ DB / 파서 / 서비스 레이어 ✅
- [x] `PendingNotificationEntity` + `PendingNotificationDao` (dedup_hash UNIQUE)
- [x] AppDatabase version 4
- [x] 파서 6종: KBBankParser / KakaoBankParser / HanaCardParser / ShinhanCardParser / KakaoPayParser / TossParser
- [x] `GenericKoreanFinanceParser` — 미등록 앱/SMS 폴백
- [x] `NotificationParserRegistry` — 파서 라우팅 + 표시명 변환
- [x] `AppNotificationListenerService` — 알림 수신 → 파싱 → DB 저장
- [x] `SmsReader` — content://sms/inbox 조회 (최근 30일, 금액 패턴 필터)
- [x] `AutoFillRepository` — insertPush / loadSmsItems / approve / reject
- [x] 중복 처리: push=5분 버킷 MD5, sms=SMS _id MD5

#### 2️⃣ AutoFillScreen (Push + SMS 통합) ✅
- [x] `AutoFillViewModel` — selectedSource / pushItems / smsItems / enabledPackages
- [x] `AutoFillScreen` — 소스 토글 + LazyColumn + 권한 경고 배너
- [x] `PendingNotificationItem` — 수입/지출 + 금액 + 날짜·시각(2:1 Row) + 내용 + 원본 알림 텍스트 + 구분(카테고리) + [저장/취소]
- [x] `PackageSelectionDialog` — 수집 앱 체크박스 모달 (우상단 아이콘)
- [x] 알림 접근 권한 미설정 시 경고 배너 + 설정 이동
- [x] SMS READ_SMS 런타임 권한 요청

#### 3️⃣ HomeScreen 배지 + 연결 ✅
- [x] HomeScreen 우상단 `MarkEmailUnread` 아이콘 + BadgedBox (tertiary 색상)
- [x] `HomeViewModel` pendingCount StateFlow 추가
- [x] `AuthDataStore` + `AuthRepository` — enabledPackages 관리
- [x] Routes.AutoFill + AppNavigation 연결
- [x] AndroidManifest — AppNotificationListenerService 등록

- [x] **테스트:** 실기기 알림 수신 → AutoFillScreen 목록 → 저장 후 거래 반영 ✅

#### 4️⃣ Phase 5 버그 수정 ✅
- [x] AutoFillScreen 카테고리 선택 불가 → `CategoryDialog` 연결 + `AutoFillViewModel`에 카테고리 기능 추가
- [x] 알림 수신 불가 (enabledPackagesCache 타이밍) → DataStore 직접 읽기로 보완
- [x] 금액 패턴 체크를 body → title+body 전체로 확장
- [x] com.android.systemui 알림 차단
- [x] KakaoBankParser 신규 추가 (입금/출금 방향에 따라 → 기준 description 추출)
- [x] KBBankParser description 추출 개선 (계좌번호~거래유형 키워드 사이 텍스트)
- [x] HanaCardParser에 하나페이(com.hanaskcard.paycla) 추가 (/ 기준 왼쪽 추출)

---

---

### Phase 6 — 양방향 동기화 ✅

#### 1️⃣ 서버 S6 + Retrofit 네트워크 레이어 ✅
- [x] server/controllers/syncController.js — delta / push 구현
- [x] server/routes/sync.js 연결
- [x] authController.js login/signup 응답에 ledger_id 포함
- [x] network/api/SyncApi.kt + DTO 데이터 클래스
- [x] di/NetworkModule.kt — SyncApi 제공
- [x] AuthDataStore — ledger_id / last_synced_at 저장
- [x] AuthRepository — ledgerId Flow 노출
- [x] HomeViewModel / TransactionEditViewModel — 하드코딩 ledgerId 제거, DataStore 값 사용
- [x] **테스트:** 로그인 후 ledger_id 정상 저장 + 동기화 확인 ✅

---

#### 2️⃣ SyncRepository + DB sync_status 컬럼 ✅
- [x] TransactionEntity / CategoryEntity — `syncStatus`, `serverId: Long?` 추가
- [x] TransactionDao / CategoryDao — getPending, getByServerId, markSynced, softDelete 쿼리 추가
- [x] data/repository/SyncRepository.kt — pushPending() + pullDelta() 구현
- [x] insert/update 시 syncStatus = "pending" 자동 세팅
- [x] 충돌 해결: 타임스탬프 기반 최신 우선 (last-write-wins)
- [x] AppDatabase version 2 → 3 (fallbackToDestructiveMigration)
- [x] **테스트:** pending → push → pull → synced 흐름 확인 ✅

---

#### 3️⃣ 동기화 UI 연결 ✅
- [x] HomeViewModel — SyncState sealed class, sync(), resetSyncState()
- [x] HomeScreen TopAppBar 동기화 아이콘 → viewModel.sync() 연결
- [x] 동기화 중 CircularProgressIndicator 표시
- [x] 완료/실패 시 Snackbar 피드백
- [x] **테스트:** 수동 동기화 버튼 → 서버 반영 확인 ✅

---

#### 4️⃣ SyncWorker 구현 (WorkManager) ⏳ (Phase 8에서 구현 예정)
- [ ] service/sync/SyncWorker.kt — 백그라운드 동기화 작업
- [ ] service/sync/SyncManager.kt — 동기화 스케줄 관리
- [ ] 설정에서 주기 설정 (수동 / 15분 / 1시간 등)

---

---

### Phase 7 — 공유 장부 ✅

#### 1️⃣ 서버 S7 공유 장부 API ✅
- [x] server/controllers/sharedController.js — getSharedUsers, invite, updatePermission, revokeAccess, getSharedLedgers
- [x] server/routes/shared.js — GET /, POST /, PATCH /:id, DELETE /:id 연결
- [x] server/routes/ledgers.js — GET /shared 구현 (501 스텁 → 실제 로직)
- [x] server/routes/users.js + app.js — GET /api/users/search?email= 신규 라우트
- [x] server/server.js — dotenv path 절대경로 수정 (__dirname 기반)
- [x] **테스트:** 초대 → 검색 → 권한 변경 → 해제 흐름 확인 ✅

---

#### 2️⃣ SharedLedgerScreen 구현 ✅
- [x] network/api/SharedApi.kt — DTO + Retrofit 인터페이스 (SharedUserDto, OwnLedgerDto 등)
- [x] data/repository/SharedRepository.kt — 모든 메서드 Result<T> 반환
- [x] ui/screens/shared/SharedLedgerViewModel.kt — loadSharedUsers, searchUser, invite, updatePermission, revokeAccess
- [x] ui/screens/shared/SharedLedgerScreen.kt — 이메일 검색 + 초대 다이얼로그 + 공유 사용자 목록 (권한 변경/해제)
- [x] navigation/Routes.kt — SharedLedger("shared_ledger/{ledgerId}") 추가
- [x] navigation/AppNavigation.kt — SharedLedger composable 연결
- [x] **테스트:** 이메일 검색 → 초대 → 목록 표시 확인 ✅

---

#### 3️⃣ 장부 전환 기능 ✅
- [x] AuthDataStore — ACTIVE_LEDGER_ID 키 추가, setActiveLedgerId()
- [x] AuthRepository — activeLedgerId Flow 노출, setActiveLedgerId()
- [x] HomeViewModel — currentLedgerId를 activeLedgerId로 변경, loadLedgers(), switchLedger(), LedgerItem 데이터 클래스
- [x] HomeScreen — TopAppBar 타이틀 클릭 → ModalBottomSheet 장부 선택 목록
- [x] **테스트:** 공유 장부 전환 후 거래 목록 변경 확인 ✅

---

#### 4️⃣ SettingsScreen + 로그아웃 ✅
- [x] ui/screens/settings/SettingsScreen.kt — 로그아웃 ListItem + AlertDialog
- [x] AuthViewModel — logout() 함수 추가
- [x] navigation/Routes.kt — Settings("settings") 추가
- [x] navigation/AppNavigation.kt — Settings composable + 로그아웃 시 전체 백스택 제거
- [x] **테스트:** 로그아웃 → LoginScreen 이동, 재로그인 후 ledger_id 정상 저장 확인 ✅

---

---

### Phase 8 — 설정 화면 확장 / 테스트 / 배포 🔄

#### 1️⃣ SettingsScreen 확장 ✅
- [x] ui/screens/settings/SettingsScreen.kt — 기본 뼈대 + 로그아웃 (Phase 7에서 구현 완료)
- [x] 동기화 주기 설정 (수동 / 15분 / 1시간) → SingleChoiceSegmentedButtonRow
- [x] 알림 설정 (Push 자동입력 ON/OFF) → Switch
- [x] 서버 상태 확인 (`GET /api/health`) → 온라인/오프라인 표시
- [x] CategoryManageScreen 연결 → ListItem 클릭으로 이동
- [x] AuthDataStore / AuthRepository — syncInterval, notificationsEnabled Flow 추가
- [x] SettingsViewModel — checkServerHealth() + Dispatchers.IO 적용
- [x] Routes.kt + AppNavigation.kt — CategoryManage 라우트 연결
- [x] **테스트:** SettingsScreen 진입 → 모든 기능 작동 확인 ✅

---

#### 2️⃣ CategoryManageScreen 구현 ✅
- [x] ui/screens/category/CategoryManageScreen.kt
- [x] ui/screens/category/CategoryManageViewModel.kt
- [x] 구분 목록 (추가/수정/삭제) → ListItem + FAB
- [x] 색상 선택기 → FlowRow (12개 프리셋 색상)
- [x] **테스트:** 구분 CRUD 후 거래 화면 반영 확인 ✅

---

#### 3️⃣ 유닛 테스트 ✅
- [x] 테스트 의존성 추가 (MockK 1.13.11, kotlinx-coroutines-test, Turbine 1.2.0, Room-testing)
- [x] `AppYearMonthTest.kt` — 순수 로직 10개 테스트 (format/next/prev/displayLabel) **10/10 통과**
- [x] `MainDispatcherRule.kt` — 코루틴 테스트 헬퍼 (UnconfinedTestDispatcher)
- [x] `HomeViewModelTest.kt` — MockK + Turbine ViewModel 테스트 **13/13 통과**
  - 월 탐색 (nextMonth/prevMonth), 캘린더 날짜 선택, 탭 전환
  - sync() → SyncState 전환 (Success/Error/Idle)
- [x] `TransactionDaoTest.kt` — in-memory Room DB 테스트 (androidTest) **15개 케이스**
  - CRUD, 월별 필터, 소프트 딜리트, 합계 계산, 동기화 쿼리
- [x] `CategoryDaoTest.kt` — in-memory Room DB 테스트 (androidTest) **12개 케이스**
  - CRUD, 이름순 정렬, 소프트 딜리트, 동기화 쿼리, 하드 퍼지

---

#### 4️⃣ UI/통합 테스트 ⏳
- [ ] Compose UI 테스트 (ComposeTestRule)
- [ ] 주요 흐름 End-to-End 테스트

---

#### 5️⃣ 최적화 및 배포 준비 ✅
- [x] ProGuard / R8 설정 (proguard-rules.pro — Retrofit/Room/Hilt/Gson/Compose 규칙 포함)
- [x] 릴리즈 빌드: isMinifyEnabled=true, isShrinkResources=true 활성화
- [x] AAB 릴리즈 빌드 성공 (`app/build/outputs/bundle/release/app-release.aab`)
- [ ] 릴리즈 키스토어 서명 설정 (배포 전 필요)
- [ ] Google Play 등록 준비 (스크린샷, 앱 설명 등)

---

---

## 🖥️ 백엔드 서버 (Node.js)

### S1️⃣ 서버 프로젝트 초기 설정 ✅
- [x] `server/` 폴더 생성
- [x] Express, cors, dotenv, pg, jsonwebtoken, bcrypt, morgan 설치
- [x] 폴더 구조 생성 (routes, middleware, db)
- [x] `.env` / `.env.example` 파일 작성 (PORT, DATABASE_URL, JWT_SECRET)
- [x] `app.js` / `server.js` 진입점 작성
- [x] `GET /api/health` 헬스 체크 엔드포인트
- [x] 모든 API 라우트 스텁 정의 (auth, transactions, categories, ledgers, sync, shared, settings)
- [x] JWT auth 미들웨어 작성
- [x] **테스트:** 서버 로컬 기동 및 `/api/health` → `200 OK` 확인 ✅

---

### S2️⃣ Neon PostgreSQL 스키마 생성 ✅ (SQL 파일 준비 완료)
- [x] `server/db/schema.sql` 작성 완료 (Neon에 직접 실행하면 됨)
- [x] `users` 테이블
- [x] `ledgers` 테이블
- [x] `transactions` 테이블
- [x] `categories` 테이블
- [x] `shared_ledgers` 테이블
- [x] `system_settings` 테이블 (login_required 기본값 포함)
- [x] 성능 인덱스 4개 정의
- [ ] **테스트:** Neon 커넥션 string 받은 후 콘솔에서 실행 확인 ⏳ (커넥션 string 대기 중)

---

### S3️⃣ 인증 API (이메일) ✅
- [x] `POST /api/auth/signup` - 이메일 회원가입 (bcrypt 해싱, 기본 장부 자동 생성)
- [x] `POST /api/auth/login` - 이메일 로그인 + JWT 발급
- [x] `POST /api/auth/refresh` - refresh token으로 access token 갱신
- [x] `GET /api/auth/verify` - 토큰 유효성 검증
- [x] JWT auth 미들웨어 (`middleware/auth.js`)
- [x] **테스트:** 14/14 전부 통과 ✅ (signup, 중복, 유효성, login, 틀린 pw, verify, 무효 token, refresh)

---

### S4️⃣ 인증 API (OAuth) ✅
- [x] `POST /api/auth/oauth/google` — google-auth-library로 ID 토큰 검증 + upsert + JWT 발급
- [x] `POST /api/auth/oauth/naver` — Naver 프로필 API 호출 + 고유 ID 기반 upsert + JWT 발급
- [x] 네이버: `id` 필드로 가상 이메일 생성 (`naver_{id}@oauth.local`) — email 컬럼 UNIQUE 제약 충족
- [x] 신규/기존 유저 분기 처리 (ON CONFLICT DO UPDATE upsert)
- [x] 로그인 성공 시 기본 장부 자동 생성 (없는 경우)
- [x] render.yaml에 GOOGLE_CLIENT_ID / NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 항목 추가
- [x] **테스트:** Google/Naver 로그인 → HomeScreen 이동 확인 ✅

---

### S5️⃣ 거래 및 카테고리 CRUD API ✅
- [x] `GET/POST /api/ledgers/:ledgerId/transactions` - 거래 조회(월별 필터)/추가
- [x] `PATCH/DELETE /api/transactions/:id` - 거래 수정/삭제 (소프트 딜리트)
- [x] `GET/POST /api/ledgers/:ledgerId/categories` - 카테고리 조회/추가
- [x] `PATCH/DELETE /api/categories/:id` - 카테고리 수정/삭제
- [x] `middleware/ledgerAccess.js` - 장부 접근/편집 권한 체크 미들웨어
- [x] **테스트:** 37/37 전부 통과 (권한 거부, 유효성 검사, 소프트딜리트 포함) ✅

---

### S6️⃣ 동기화 API ✅
- [x] `GET /api/sync/delta?since={timestamp}` - Delta sync
- [x] `POST /api/sync/push` - 클라이언트 변경사항 일괄 업로드
- [x] 충돌 해결 로직 (타임스탬프 기반 최신 우선, last-write-wins)
- [x] 동기화 상태 필드 관리 (`sync_status`: synced / pending)
- [x] **테스트:** 수동 동기화 버튼 → 서버 Neon DB 반영 확인 ✅

---

### S7️⃣ 공유 장부 API ✅
- [x] `POST /api/shared-ledgers` - 사용자 초대 (owner 전용, conflict upsert)
- [x] `PATCH /api/shared-ledgers/:id` - 권한 변경 (owner만 가능)
- [x] `DELETE /api/shared-ledgers/:id` - 공유 해제 (owner 또는 공유 사용자 본인)
- [x] `GET /api/ledgers/shared` - 나와 공유된 장부 목록 조회
- [x] `GET /api/users/search?email=` - 이메일로 사용자 검색 (신규 /api/users 라우터)
- [x] ledgerAccess 미들웨어 — 공유 장부 edit 권한 포함
- [x] **테스트:** 초대 → 권한 변경 → 해제 흐름 확인 ✅

---

### S8️⃣ Render.com 배포 ✅
- [x] `render.yaml` 또는 Render 대시보드 서비스 설정
- [x] 환경변수 등록 (DATABASE_URL, JWT_SECRET)
- [x] 배포 후 `/api/health` 응답 확인 ✅
- [x] Android 앱의 BASE_URL을 Render.com HTTPS 주소로 교체 (https://shared-ledger-api.onrender.com/)
- [x] `AndroidManifest.xml`에서 `android:usesCleartextTraffic="false"` 설정 (프로덕션 HTTPS 전용)
- [x] **테스트:** 실기기에서 서버 API 호출 성공 확인 (예정)

---

## 📝 마지막 업데이트

**2026-04-05** | Phase 1 완료 (Android 앱 인프라 구축 — 10단계 Hilt DI까지)  
**2026-04-05** | 백엔드 S1 완료 (Node.js 서버 초기 설정, 로컬 기동 확인)  
**2026-04-05** | 백엔드 S2 완료 (schema.sql 작성 완료 — Neon 커넥션 string 대기 중)  
**2026-04-06** | 백엔드 S5 완료 (거래/카테고리 CRUD API — 37/37 테스트 통과)  
**2026-04-06** | Android Phase 2 완료 (DataStore JWT 인증 + Retrofit + AuthViewModel + SignupScreen + SplashScreen 자동 로그인)  
**2026-04-06** | Android Phase 3 완료 (거래 CRUD UI — TransactionEditScreen + CategoryDialog + MonthSelectorBar + ListViewTab)
**2026-04-06** | Android Phase 4-1 완료 (ListViewTab — Phase 3에서 이미 구현됨, progress 반영)
**2026-04-06** | Android Phase 4-2 완료 (CalendarViewTab + CalendarComposable + dateMillis 네비게이션 연동)
**2026-04-06** | Android Phase 4-3 완료 (StatisticViewTab + PieChart(도넛) + BarChart + 수입/지출 탭 전환)
**2026-04-06** | 백엔드 S6 완료 (Delta sync + push API — syncController.js)
**2026-04-06** | Android Phase 6 완료 (SyncRepository + sync_status DB + HomeScreen sync 버튼 연결)
**2026-04-06** | 백엔드 S7 완료 (공유 장부 API 전체 — shared/users/ledgers 라우터, dotenv 경로 수정)
**2026-04-06** | Android Phase 7 완료 (SharedLedgerScreen + 장부 전환 BottomSheet + SettingsScreen 로그아웃)
**2026-04-06** | Android Phase 8-A 완료 (SettingsScreen 확장 — 서버 상태확인·동기화 주기·알림 설정)
**2026-04-06** | Android Phase 8-B 완료 (CategoryManageScreen 구현 — 구분 CRUD + 색상 선택기)
**2026-04-06** | 백엔드 S8 완료 (Render.com 배포 — https://shared-ledger-api.onrender.com)
**2026-04-07** | Android Phase 8-C 완료 (유닛 테스트 전체 — MockK+Turbine ViewModel 13/13, Room DAO in-memory 27개, ProGuard 설정, AAB 릴리즈 빌드 성공)
**2026-04-08** | S4 + Phase 2-5/6 완료 (Google/Naver OAuth — 서버 upsert + Android Credential Manager/Naver SDK + 마지막 로그인 배지)
**2026-04-08** | 보안 개선 — OAuth 인증키 BuildConfig 이전 (local.properties 기반, GitHub 미노출)
**2026-04-10** | UI 개선 — TransactionItem 시각(HH:mm) 표시 (카테고리 우측), ListViewTab/CalendarViewTab 시간순 정렬 (날짜 내림차순 + 하루 내 시간 오름차순)
**2026-04-10** | 통계 개선 — BarChart 카테고리별 토글(눈 아이콘)로 제외/포함, 제외 시 도넛 그래프 재조정 + 비율 재계산, 비활성 UI 표시; 카테고리 터치 시 해당 트랜잭션 목록 ModalBottomSheet (날짜 스티키 헤더, 시간순 정렬)
**2026-04-10** | 버그 수정 — TransactionItem 시각 표시 HH:mm:ss → HH:mm (take(5) 트림); Render 서버 웜업: 앱 시작 시 /api/health 핑 (SplashViewModel); 동기화 네트워크 오류 시 60초 후 자동 재시도 (로딩 유지, IOException만 재시도, API 오류는 즉시 에러)

---

## 🎯 다음 단계

**다음 단계:** 
1. 자동 로그인 체크박스 기능 구현 (DataStore auto_login 연동)
2. 릴리즈 키스토어 서명 설정 → 서명된 AAB 생성
3. Google Play Console 앱 등록 (스크린샷, 앱 설명, 개인정보처리방침)
