# YTLoader Android

yt-dlp 기반 개인용 YouTube 다운로더의 Android(Kotlin, Jetpack Compose) 버전.
데스크톱판(Windows PowerShell WinForms)의 기능을 다시 구현한다. Google Play 배포 대상이 아니다.

## 문서

- `docs/SPEC.md` — 기능 명세: 무엇을, 언제 완료인가 (FR/NFR, 수용 기준, 열린 문제)
- `docs/ARCHITECTURE.md` — 설계: 어떻게 ([결정] / [구현자 판단] / [확인 필요] 표기)
- `docs/PLAN.md` — 마일스톤 체크리스트 + 작업 로그. **작업을 시작할 때 먼저 읽고,
  마일스톤을 끝낼 때마다 갱신한다.** 컨텍스트가 압축돼도 이 파일로 이어서 진행한다.
- `docs/reference/YTLoader.ps1` — 데스크톱판 원본. 명세가 다루지 않는 세부 동작의 기준.

## 환경

- Windows, PowerShell. Gradle은 `.\gradlew.bat`로 실행한다.
- 테스트 기기는 `adb devices`로 확인. 스크린샷은
  `New-Item -ItemType Directory -Force build | Out-Null; adb shell screencap -p /sdcard/shot.png; adb pull /sdcard/shot.png build/shot.png`
  (PowerShell에서 `exec-out ... >` 리다이렉트는 PNG를 깨뜨린다).
- 공백이 든 인텐트 값은 원격 셸에서 쪼개지므로 `adb shell "am start ... --es KEY 'a b'"`처럼
  명령 전체를 큰따옴표, 값을 작은따옴표로 감싼다.

## 명령

```powershell
.\gradlew.bat testDebugUnitTest      # 단위 테스트 — 변경 후 항상 실행
.\gradlew.bat assembleDebug          # 빌드
.\gradlew.bat lintDebug              # 린트
.\gradlew.bat installDebug           # 기기 설치
adb logcat -d -s YTLoader            # 앱 로그 (태그 YTLoader)
```

## 작업 방식

- 마일스톤 단위로: 구현 → 완료 조건 검증 → PLAN.md 체크·작업 로그 → 커밋.
- "완료"라고 보고할 때는 증거를 함께 적는다: 테스트 통과 수, 명령 출력 요약,
  스크린샷 경로. 기기 동작은 실제로 실행해서 확인한다.
- PLAN.md의 ⏸ 지점에서는 멈추고 보고한 뒤 사용자 응답을 기다린다.
- [결정] 항목을 바꿔야 한다고 판단되면, 바꾸기 전에 이유와 대안을 사용자에게 묻는다.
- 문서와 실제가 다르면(라이브러리 API 등) 코드를 맞춘 뒤 문서도 고친다.

## 경계

- 의존성은 ARCHITECTURE.md 1장 목록만 쓴다. 추가가 필요하면 이유와 함께 먼저 묻는다.
- JDK·SDK·에뮬레이터 이미지 설치 등 시스템 변경은 사용자 승인 후에만.
- 쿠키 값과 cookies.txt 내용은 로그·커밋·보고 어디에도 출력하지 않는다 (이름만 허용).
- `keystore.properties`, `*.jks`, `local.properties`, `cookies.txt`는 커밋하지 않는다.
- 원격 저장소 push는 사용자가 요청할 때만.
- 사용자 Google 계정 로그인이 필요한 검증은 직접 시도하지 않고 사용자에게 요청한다.
