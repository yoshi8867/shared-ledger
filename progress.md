# Shared Ledger App - 개발 진행도

**작성일:** 2026-04-05  
**Phase:** 1 (프로젝트 설정, Room DB, 기본 Navigation, 테마)

---

## 📊 전체 진행도

████████░░░░░░░░░░░░ 40% (4/10)

---

## 📋 단위 작업 목록

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

### 5️⃣ SplashScreen 구현 ⏳
- [ ] ui/screens/splash/SplashScreen.kt
- [ ] 로고/앱명 표시
- [ ] 2초 딜레이 후 로그인 체크
- [ ] LoginScreen 또는 HomeScreen으로 자동 이동
- **테스트:** 앱 시작 시 스플래시 표시 확인

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

## 📝 마지막 업데이트

**2026-04-05** | 4단계 Navigation 시스템 완료 (Routes, NavHost, Splash→Login→Home)

---

## 🎯 다음 단계

**1단계 완료 후:** 사용자 승인 → 3단계 (sample 폴더에서 프로젝트 복사) → 4단계 (구현 시작)
