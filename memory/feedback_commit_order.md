---
name: 커밋 순서 규칙 (android-dev-coder.md 4단계)
description: 구현 후 사용자 테스트 먼저, 그 다음 commit/push
type: feedback
---

구현 완료 후 반드시 이 순서를 지킬 것:
1. 구현 완료
2. 자체 테스트
3. **사용자에게 앱 설치 테스트 요청** (테스트 포인트 명시)
4. 테스트 확인 후 GitHub commit/push + progress.md 업데이트
5. 다음 작업 구현할지 물어보기

**Why:** android-dev-coder.md 4단계 지침이며, 사용자가 테스트 전에 push한 것에 대해 직접 지적함.
**How to apply:** CalendarViewTab처럼 구현이 끝난 직후, commit 전에 반드시 사용자 테스트 요청을 먼저 보낼 것.
