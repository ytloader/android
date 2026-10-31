# YTLoader Android — 구현 계획 · 진행 기록

이 파일은 구현 세션의 체크리스트이자 기억 장치다. 세션이 바뀌거나 컨텍스트가 압축돼도
여기만 읽으면 이어서 진행할 수 있게 유지한다.

- 마일스톤은 순서대로 진행한다. 위험이 큰 것(엔진 동작)을 먼저 확인하도록 배치했다.
- 항목을 끝내면 체크하고, 마일스톤을 끝내면 "작업 로그"에 결과와 **증거**(명령 출력
  요약, 테스트 수, 스크린샷 경로)를 남긴 뒤 커밋한다.
- ⏸ 표시는 사용자 확인이 필요한 지점이다. 그 지점에서 멈추고 보고한다.

## M0 환경 준비

현재 상태(2026-10-31 확인): 이 PC에는 JDK, Android SDK, Android Studio가 **없고**
`adb`만 있다(winget `Google.PlatformTools`).

- [ ] 확인: `java -version`(17 이상), `$env:ANDROID_HOME`, `gradle --version`, `adb devices`
- [ ] ⏸ 없는 항목에 대해 아래 두 방법을 제시하고 사용자가 고르게 한다. 승인 후 설치.
      - A (GUI, 사용자가 일부 진행): `winget install Google.AndroidStudio` → **사용자가**
        Android Studio를 한 번 실행해 설정 마법사로 SDK·에뮬레이터 설치. 세션은 Studio에
        포함된 JBR(`C:\Program Files\Android\Android Studio\jbr`)을 `JAVA_HOME`으로 쓴다.
      - B (CLI, 세션이 전부 진행): `winget install Microsoft.OpenJDK.21` + Android
        command-line tools(공식 zip) → `sdkmanager --licenses`,
        `sdkmanager "platform-tools" "platforms;android-36" "build-tools;<최신 36.x>"`
        (에뮬레이터가 필요하면 `"emulator" "system-images;android-36;google_apis;x86_64"` 추가).
      - 둘 다 Gradle CLI가 필요하다(래퍼 생성용 1회): `winget install Gradle.Gradle`.
      - `ANDROID_HOME`은 사용자 환경 변수로 설정, `local.properties`의 `sdk.dir`도 기록.
- [ ] 프로젝트 파일은 마법사 없이 직접 작성한다(settings.gradle.kts, build.gradle.kts,
      gradle/libs.versions.toml, app 모듈, `.gitignore`) → `gradle wrapper`로
      `gradlew.bat`·`gradle/wrapper/*` 생성 → 이후로는 래퍼만 사용.
- [ ] 테스트 기기: 실기기(USB 디버깅) 우선, 없으면 x86_64 에뮬레이터(AVD) 생성
- [ ] 완료 조건: 빈 Compose 화면 앱이 `.\gradlew.bat assembleDebug`로 빌드되고
      기기에서 실행된 스크린샷 확보

## M1 엔진 스파이크 (진행 여부 판단)

목적: youtubedl-android 0.18.1이 기기에서 실제로 YouTube를 받는지, 얼마나 걸리는지 확인.

- [ ] M0 골격에 ARCHITECTURE 1장 의존성 추가: youtubedl-android 0.18.1
      (library, ffmpeg, `mavenCentral()`), 라이브러리 README의 패키징 설정 적용
- [ ] ARCHITECTURE 4장의 [확인 필요] 항목을 라이브러리 소스로 확인하고 문서 갱신
      (버전 조회 API, stderr 전달 방식, 패키징 요구사항, 라이선스)
- [ ] 임시 디버그 코드로 측정 (jNQXAC9IVRw):
      - init 소요 시간 (첫 실행 / 두 번째 실행)
      - `--simulate --print title` 소요 시간, 출력에 JS 런타임 경고가 있는지
      - mp3 + 썸네일 실제 다운로드 소요 시간, 결과 파일 크기
- [ ] ⏸ 측정값 보고 (SPEC O-2 판단 근거). 사용자 확인 후 M2 진행
- 스파이크용 임시 코드는 M3에서 정식 구조로 대체하고 지운다.

## M2 순수 로직 + 단위 테스트

- [ ] FormatCatalog, DownloadOptions
- [ ] YtDlpArgsBuilder — 골든 예제 E1–E8
- [ ] ProgressParser
- [ ] UrlExtractor
- [ ] NetscapeCookieWriter, CookieFileValidator
- [ ] ErrorHintMapper
- [ ] MediaStoreExporter의 파일 제외 규칙(`isExportable`)
- [ ] 완료 조건: `.\gradlew.bat testDebugUnitTest` 전부 통과 (테스트 수 기록)

## M3 다운로드 기본 흐름

- [ ] AppContainer, 엔진 초기화 상태(EngineStatus), 시작 시 작업 폴더 정리
- [ ] DownloadService (FGS dataSync, 진행/결과 알림, 취소, onTimeout)
- [ ] DownloadRepository, MediaStoreExporter
- [ ] MainScreen/MainViewModel: FR-1, FR-3, FR-4, FR-5, FR-6, FR-7
- [ ] 알림 권한 요청 (API 33+, 첫 다운로드 때)
- [ ] SPEC O-3: 포맷별 썸네일 삽입 확인 → 안 되는 포맷은 카탈로그에서 썸네일 미지원 처리 후 보고
- [ ] 완료 조건 (기기):
      - E1(mp3), E5(mp4 1080) 실제 다운로드 → `adb shell ls -l /sdcard/Download/YTLoader/` 결과
      - 다운로드 중 홈으로 나가도 완료됨, 진행 알림 스크린샷
      - 재생목록 다운로드 중 취소 → 공용 폴더에 부분 파일 없음
      - 포맷 10종의 최종 파일명·확장자 유지 결과표:
        오디오 mp3 / m4a / opus / flac / wav / original, 영상 mp4 / mkv / webm / original

## M4 공유 · 설정 유지 · 업데이트

- [ ] FR-2 공유 인텐트 (singleTop, onNewIntent, 진행 중 처리)
- [ ] FR-11 DataStore 설정 유지
- [ ] FR-8 업데이트 + 버전 표시
- [ ] 완료 조건: adb 공유 명령 후 URL이 채워진 스크린샷, 앱 재시작 후 옵션 복원 확인,
      업데이트 결과 대화상자 스크린샷

## M5 로그인

- [ ] FR-9 LoginScreen(WebView) + 쿠키 감지 + 저장 + 자동 닫기
- [ ] FR-10 cookies.txt 가져오기
- [ ] 로그아웃, [로그인 쿠키 사용] 스위치, 옵션 빌더 연동
- [ ] 구현자 검증: 사용자가 준 데스크톱판 cookies.txt를 `adb push`로 넣고 가져오기 →
      로그인 상태 표시, 다운로드 옵션에 `--cookies` 포함(로그로 확인, 값은 출력 금지)
- [ ] ⏸ 사용자 검증 요청: 실제 계정으로 앱 내 로그인 → 비공개/연령 제한 영상 다운로드.
      SPEC O-1 결과(로그인 차단 여부, UA 조정 필요 여부)를 기록

## M6 마감

- [ ] SPEC 7장 표의 각 행을 확인하고 결과를 작업 로그에 표로 남김
- [ ] ⏸ 앱 라이선스 결정(SPEC O-4), 릴리스 키스토어 생성(사용자와 함께)
- [ ] release 빌드(arm64-v8a, 서명), README(설치·사용법·Play 미배포 이유)
- [ ] 완료 조건: `.\gradlew.bat testDebugUnitTest lintRelease assembleRelease` 성공

---

## 작업 로그

형식: `### YYYY-MM-DD Mx 제목` + 무엇을 했는지 / 증거 / 결정·변경 사항 / 남은 문제.
[구현자 판단]으로 정한 내용도 여기에 한 줄씩 남긴다.

(아직 없음)
