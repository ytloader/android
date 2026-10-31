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

- [x] 확인: `java -version`(17 이상), `$env:ANDROID_HOME`, `gradle --version`, `adb devices`
      → 실제 상태는 위 설명과 다름. 작업 로그 2026-10-31 M0 참고.
- [x] ⏸ 없는 항목에 대해 아래 두 방법을 제시하고 사용자가 고르게 한다. 승인 후 설치.
      - A (GUI, 사용자가 일부 진행): `winget install Google.AndroidStudio` → **사용자가**
        Android Studio를 한 번 실행해 설정 마법사로 SDK·에뮬레이터 설치. 세션은 Studio에
        포함된 JBR(`C:\Program Files\Android\Android Studio\jbr`)을 `JAVA_HOME`으로 쓴다.
      - B (CLI, 세션이 전부 진행): `winget install Microsoft.OpenJDK.21` + Android
        command-line tools(공식 zip) → `sdkmanager --licenses`,
        `sdkmanager "platform-tools" "platforms;android-36" "build-tools;<최신 36.x>"`
        (에뮬레이터가 필요하면 `"emulator" "system-images;android-36;google_apis;x86_64"` 추가).
      - 둘 다 Gradle CLI가 필요하다(래퍼 생성용 1회): `winget install Gradle.Gradle`.
      - `ANDROID_HOME`은 사용자 환경 변수로 설정, `local.properties`의 `sdk.dir`도 기록.
- [x] 프로젝트 파일은 마법사 없이 직접 작성한다(settings.gradle.kts, build.gradle.kts,
      gradle/libs.versions.toml, app 모듈, `.gitignore`) → `gradle wrapper`로
      `gradlew.bat`·`gradle/wrapper/*` 생성 → 이후로는 래퍼만 사용.
- [x] 테스트 기기: 실기기(USB 디버깅) 우선, 없으면 x86_64 에뮬레이터(AVD) 생성
      → 사용자 결정: 모든 검증은 에뮬레이터, 완성 후 마지막에 실기기 테스트
- [x] 완료 조건: 빈 Compose 화면 앱이 `.\gradlew.bat assembleDebug`로 빌드되고
      기기에서 실행된 스크린샷 확보

## M1 엔진 스파이크 (진행 여부 판단)

목적: youtubedl-android 0.18.1이 기기에서 실제로 YouTube를 받는지, 얼마나 걸리는지 확인.

- [x] M0 골격에 ARCHITECTURE 1장 의존성 추가: youtubedl-android 0.18.1
      (library, ffmpeg, `mavenCentral()`), 라이브러리 README의 패키징 설정 적용
- [x] ARCHITECTURE 4장의 [확인 필요] 항목을 라이브러리 소스로 확인하고 문서 갱신
      (버전 조회 API, stderr 전달 방식, 패키징 요구사항, 라이선스)
- [x] 임시 디버그 코드로 측정 (jNQXAC9IVRw):
      - init 소요 시간 (첫 실행 / 두 번째 실행)
      - `--simulate --print title` 소요 시간, 출력에 JS 런타임 경고가 있는지
      - mp3 + 썸네일 실제 다운로드 소요 시간, 결과 파일 크기
- [x] ⏸ 측정값 보고 (SPEC O-2 판단 근거). 사용자 확인 후 M2 진행
- 스파이크용 임시 코드는 M3에서 정식 구조로 대체하고 지운다.

## M2 순수 로직 + 단위 테스트

- [x] FormatCatalog, DownloadOptions
- [x] YtDlpArgsBuilder — 골든 예제 E1–E8
- [x] ProgressParser
- [x] UrlExtractor
- [x] NetscapeCookieWriter, CookieFileValidator
- [x] ErrorHintMapper
- [x] MediaStoreExporter의 파일 제외 규칙(`isExportable`)
- [x] 완료 조건: `.\gradlew.bat testDebugUnitTest` 전부 통과 (테스트 수 기록)

## M3 다운로드 기본 흐름

- [x] AppContainer, 엔진 초기화 상태(EngineStatus), 시작 시 작업 폴더 정리
- [x] DownloadService (FGS dataSync, 진행/결과 알림, 취소, onTimeout)
- [x] DownloadRepository, MediaStoreExporter
- [x] MainScreen/MainViewModel: FR-1, FR-3, FR-4, FR-5, FR-6, FR-7
- [x] 알림 권한 요청 (API 33+, 첫 다운로드 때)
- [x] SPEC O-3: 포맷별 썸네일 삽입 확인 → 안 되는 포맷은 카탈로그에서 썸네일 미지원 처리 후 보고
- [x] 완료 조건 (기기):
      - E1(mp3), E5(mp4 1080) 실제 다운로드 → `adb shell ls -l /sdcard/Download/YTLoader/` 결과
      - 다운로드 중 홈으로 나가도 완료됨, 진행 알림 스크린샷
      - 재생목록 다운로드 중 취소 → 공용 폴더에 부분 파일 없음
      - 포맷 10종의 최종 파일명·확장자 유지 결과표:
        오디오 mp3 / m4a / opus / flac / wav / original, 영상 mp4 / mkv / webm / original

## M4 공유 · 설정 유지 · 업데이트

- [x] FR-2 공유 인텐트 (singleTop, onNewIntent, 진행 중 처리)
- [x] FR-11 DataStore 설정 유지
- [x] FR-8 업데이트 + 버전 표시
- [x] 완료 조건: adb 공유 명령 후 URL이 채워진 스크린샷, 앱 재시작 후 옵션 복원 확인,
      업데이트 결과 대화상자 스크린샷

## M5 로그인

- [x] FR-9 LoginScreen(WebView) + 쿠키 감지 + 저장 + 자동 닫기
- [x] FR-10 cookies.txt 가져오기
- [x] 로그아웃, [로그인 쿠키 사용] 스위치, 옵션 빌더 연동
- [x] 구현자 검증: 사용자가 준 데스크톱판 cookies.txt를 `adb push`로 넣고 가져오기 →
      로그인 상태 표시, 다운로드 옵션에 `--cookies` 포함(로그로 확인, 값은 출력 금지)
- [ ] ⏸ (최종 실기기 테스트로 미룸) 사용자 검증 요청: 실제 계정으로 앱 내 로그인 → 비공개/연령 제한 영상 다운로드.
      SPEC O-1 결과(로그인 차단 여부, UA 조정 필요 여부)를 기록

## M6 마감

- [x] SPEC 7장 표의 각 행을 확인하고 결과를 작업 로그에 표로 남김
- [x] ⏸ 앱 라이선스 결정(SPEC O-4) → GPL-3.0 결정·적용, 릴리스 키스토어 생성(사용자와 함께)
- [x] release 빌드(arm64-v8a, 서명), README(설치·사용법·Play 미배포 이유)
- [x] 완료 조건: `.\gradlew.bat testDebugUnitTest lintRelease assembleRelease` 성공

## M7 UI/UX 개편 (2026-10-31 사용자 요청)

- [x] 사용자 결정: 화면 구조 A(칩 + 하단 고정 버튼, 로그인·업데이트는 ⚙ 설정 시트), 고정 브랜드 색,
      추가 기능 FR-12 [열기] · FR-13 클립보드 제안 · FR-14 제목 미리보기 (SPEC 4·5장 갱신)
- [x] 테마·메인 화면·설정 시트·벡터 아이콘, ViewModel(제목 조회·클립보드), 엔진 `query()`
- [x] 앱 아이콘: 사용자가 GPT로 만든 원본 → `docs/design/icon/`, 적응형 아이콘 적용
- [x] 완료 조건: 단위 테스트·린트·릴리스 빌드 통과, 에뮬레이터 스크린샷, 실기기 릴리스 앱 업데이트
- [x] FR-12 확장: 완료 카드 [열기]·[공유]·[폴더], 완료 알림 [공유]·[폴더], 설정 "저장 위치" → 폴더
- [ ] 실기기(삼성)에서 [폴더]가 어떤 앱으로 열리는지 확인

---

## 작업 로그

형식: `### YYYY-MM-DD Mx 제목` + 무엇을 했는지 / 증거 / 결정·변경 사항 / 남은 문제.
[구현자 판단]으로 정한 내용도 여기에 한 줄씩 남긴다.

### 2026-10-31 M0 환경 확인 (⏸ 대기)

- 첫 커밋 `7ad7bea` (문서 + `.gitignore`).
- 실제 환경 (위 "현재 상태" 설명과 다름 — 방법 A의 대부분이 이미 설치돼 있음):
  - Android Studio 설치됨 (`AI-262.10968.63`). JBR = OpenJDK **25.0.4**.
  - SDK `%LOCALAPPDATA%\Android\Sdk`: platforms **android-37.0만** 있음(36 없음),
    build-tools 36.0.0, platform-tools, emulator, system-image android-37.0
    google_apis_playstore x86_64, `licenses/android-sdk-license` 있음. cmdline-tools 없음.
  - AVD `Medium_Phone_API_37.0` (x86_64, RAM 2GB). WHPX 사용 가능.
  - PATH에 `java`·`gradle` 없음, `JAVA_HOME`·`ANDROID_HOME` 미설정.
  - `adb devices`: 연결된 기기 없음.
- 사용자 결정 (2026-10-31):
  - `JAVA_HOME`(Studio JBR), `ANDROID_HOME`(위 SDK) 사용자 환경 변수 설정 — 설치 위치 그대로.
  - **compileSdk 37 + targetSdk 36** (ARCHITECTURE 13장·SPEC NFR-1 갱신).
  - Gradle은 설치하지 않고 임시 폴더에 받은 9.8.2 zip(sha256 확인)으로 `gradle wrapper`만 1회 실행.
  - **모든 검증은 에뮬레이터**로 진행하고, 완성 후 사용자가 실기기로 최종 테스트한다.
    따라서 M1 ⏸(측정 보고)는 측정값이 비실용적일 때만 멈추고, 아니면 기록 후 계속한다.
    실기기·계정이 필요한 검증(FR-2 YouTube 앱 공유, FR-9 실제 로그인)은 마지막 실기기 테스트로 미룬다.

### 2026-10-31 M0 환경 준비 완료

- [구현자 판단] 버전: Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21(Compose·Parcelize 플러그인),
  Compose BOM 2026.09.00, core-ktx 1.19.1, activity-compose 1.13.0, lifecycle 2.11.0,
  coroutines 1.11.0, DataStore 1.3.0, JUnit 4.13.2. JDK = Studio JBR 25.0.4.
- AGP 9 내장 Kotlin 사용 → `org.jetbrains.kotlin.android` 플러그인 미적용 (ARCHITECTURE 1장에 기록).
- Activity 테마는 AppCompat/MDC 의존성 없이 `android:Theme.Material.Light.NoActionBar`.
- 증거: `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL (36 tasks).
  에뮬레이터 `Medium_Phone_API_37.0`(API 37, x86_64)에 `installDebug` 후 실행,
  스크린샷 `build/m0_empty.png` (빈 화면에 "YTLoader").

### 2026-10-31 M1 엔진 스파이크 완료

- youtubedl-android 0.18.1 library·ffmpeg 추가, `packaging.jniLibs.useLegacyPackaging = true`,
  `INTERNET` 권한. 임시 코드 `SpikeRunner.kt` (MainActivity에 `--es spike <step>`로 실행, M3에서 삭제).
- [확인 필요] 해소 (소스 `d725d5c` + 실행, ARCHITECTURE 1·4장 갱신):
  - 버전: `version(ctx)`는 업데이트 전 null → `--version` 실행으로 조회하도록 설계 변경.
  - stderr: `redirectErrorStream=true`면 `ERROR:` 줄이 콜백으로 옴(채택). 실패 시 종료 코드는 알 수 없음.
  - 패키징: README의 `extractNativeLibs=true` = AGP `useLegacyPackaging=true`.
  - 라이선스: GPL-3.0 (SPEC O-4에 기록, M6 사용자 결정).
  - `destroyProcessById`의 하위 프로세스 종료는 PID 대신 ID 문자열을 써서 동작하지 않음 → M3 취소 검증 때 확인.
- 측정 (에뮬레이터 API 37 x86_64, jNQXAC9IVRw):

  | 항목 | 내장 2025.11.12 | 업데이트 후 2026.10.26 |
  |---|---|---|
  | init 첫 실행 / 이후 | 1,993 ms / 120–150 ms | — |
  | `--version` | 1,275 ms | 657 ms |
  | `--simulate --print title` | 5,962 ms | 10,942 ms (`[jsc:quickjs]` 사용) |
  | 같은 명령 + `--cache-dir` (2회) | 5,789 / 5,741 ms | 10,390 / 10,216 ms |
  | mp3 + 썸네일 + 메타 | 19,033 ms, 669,059 B | 24,367 ms, 669,087 B |
  | 업데이트 | DONE 2,524 ms, 재실행 ALREADY_UP_TO_DATE 596 ms | |
  | 잘못된 ID | `ERROR: [youtube] xxxxxxxxxxx: This video is unavailable` (YoutubeDLException) | |

  JS 런타임 관련 경고 없음. 내장판은 "older than 90 days" 경고, 추출 시 SABR 경고(정상 동작).
- 결정: 에뮬레이터 기준 실용적 → 사용자 지시(모든 검증 에뮬레이터, 완성 후 실기기)대로 멈추지 않고 M2 진행.
  실기기 측정은 최종 실기기 테스트에서 다시 기록한다.

### 2026-10-31 M2 순수 로직 + 단위 테스트 완료

- FormatCatalog, DownloadOptions, YtDlpArgsBuilder, ProgressParser, UrlExtractor,
  NetscapeCookieWriter, CookieFileValidator, ErrorHintMapper, ExportRules(`isExportable`), YtDlpEngine 인터페이스.
- 증거: `.\gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL, **46 tests, 0 failures**
  (ArgsBuilder 9 — E1–E8 + 템플릿 단일 토큰, Catalog 6, Parser 6, Url 6, Export 3,
  CookieWriter 6, CookieValidator 6, ErrorHint 4).
- [구현자 판단] 파일 배치·Serializable·문구 상수 위치는 ARCHITECTURE 2장 "M2에서 정한 조정"에 기록.
- [구현자 판단] CookieFileValidator 도메인 판정은 `youtube.com` 또는 `.youtube.com`으로 끝나는 것만
  (`notyoutube.com` 같은 유사 도메인 거부). ErrorHint의 로그인 안내는 기본 안내 다음 줄에 붙인다.

### 2026-10-31 M3 다운로드 기본 흐름 완료

- AppContainer·EngineStatus·YTLoaderApp(시작 시 `files/work` 정리 + 엔진 init), YoutubeDlAndroidEngine,
  DownloadState/Repository, DownloadService(dataSync, 진행·결과 알림, 취소 액션, onTimeout),
  DownloadNotifications, MediaStoreExporter, MainScreen/MainViewModel/MainUiState, 알림 권한(첫 다운로드 때 1회).
  `SpikeRunner` 삭제. debug 전용 `DebugDownloadReceiver` 추가(ARCHITECTURE 16장).
- 발견 → 수정 ([구현자 판단], ARCHITECTURE 4·6·7장 갱신):
  1. 변환 중 취소 시 ffmpeg 하위 프로세스가 살아남아 계속 씀(라이브러리 버그 확인)
     → `cancel()`이 명령줄에 작업 ID가 있는 자기 UID 프로세스도 종료.
  2. 재생목록 취소 시 처리 중이던 항목의 원본 `.webm`과 쓰다 만 `.mp3`가 공용 저장소로 나감
     (이름 규칙으로 구별 불가) → 마지막 "Downloading item N"의 N번 항목 파일 제외.
     `ParsedLine.Status` → `PlaylistItem(index, count, text)`.
- 증거 (에뮬레이터 API 37):
  - E1 mp3: UI로 실행 → `Me at the zoo.mp3` 669,087 B. 화면 `build/m3_e1_done.png`, 알림 `build/m3_e1_notification.png`.
  - E5 mp4 1080: `Me at the zoo.mp4` 873,186 B (영상 코덱 av1 — 데스크톱판과 같은 `-S` 규칙의 결과).
  - 홈으로 나간 뒤에도 완료: 10분 영상 mp3 19,661,081 B, 진행 알림 `build/m3_progress_notification.png`
    ("다운로드 중... 92.1% of 8.69MiB ..."), 완료 알림 `build/m3_done_notification.png`,
    완료 알림 탭 → YouTube Music 미리듣기로 열림(`build/m3_open_file.png`).
  - 재생목록 취소(`PLBB231211A4F62143`, 2번 항목 다운로드 중 / 변환 중 각 1회): [취소] 후 2.7초에 서비스 종료,
    3초 후 libpython/libffmpeg 프로세스 없음, `files/work` 비어 있음, 공용 저장소에는
    `Team Fortress 2 [2010 Version]/001 - ... Intro.mp3`(18,019,840 B, ffprobe 628.9초 + png 표지)만 남음.
    화면 "다운로드가 취소되었습니다. (완료된 1개 파일은 저장됨)" `build/m3_playlist_cancel.png`.
  - 실패 대화상자(잘못된 ID): 종료 코드 알 수 없음 + ERROR 줄 + 안내 `build/m3_failure_dialog.png`.
  - FR-1: 빈 URL → "URL을 입력해 주세요." 스낵바, [붙여넣기] → 필드에 `https://youtu.be/jNQXAC9IVRw` (`build/m3_paste.png`).
  - `testDebugUnitTest` 47 통과, `lintDebug` 통과(경고 7: 의도된 targetSdk 36, release arm64만, SPEC 문구의 "...",
    앱 아이콘 없음 → M6).
- 포맷 10종 결과 (jNQXAC9IVRw, 모두 최종 확장자 유지, 썸네일은 ffprobe로 확인):

  | 모드 | 포맷 | 저장 파일 | 크기(B) | 썸네일 |
  |---|---|---|---|---|
  | 오디오 | mp3 | Me at the zoo.mp3 | 669,087 | png attached_pic |
  | 오디오 | m4a | Me at the zoo.m4a | 800,759 | png attached_pic (mutagen) |
  | 오디오 | opus | Me at the zoo.opus | 702,025 | png attached_pic (mutagen) |
  | 오디오 | flac | Me at the zoo.flac | 1,696,852 | png attached_pic (mutagen) |
  | 오디오 | wav | Me at the zoo.wav | 3,649,274 | 미지원(옵션 제외) |
  | 오디오 | original | Me at the zoo.webm | 253,351 | 미지원 |
  | 영상 | mp4 | Me at the zoo.mp4 | 873,186 | png attached_pic (mutagen) |
  | 영상 | mkv | Me at the zoo.mkv | 498,523 | 첨부 파일 (ffmpeg) |
  | 영상 | webm | Me at the zoo (1).webm | 546,265 | 미지원 |
  | 영상 | original | Me at the zoo (2).webm | 475,307 | 미지원 |

- SPEC O-3 해소: 표 변경 없음.
- 남은 것: "로그인 쿠키" 전달은 M5, 설정 유지·공유·업데이트 버튼은 M4.

### 2026-10-31 M4 공유 · 설정 유지 · 업데이트 완료

- FR-2: `ACTION_SEND text/plain` intent-filter, `singleTask`(ARCHITECTURE 11장 기준 — PLAN의 "singleTop" 표기와 다름),
  `onCreate`(새로 만들 때만)·`onNewIntent`에서 UrlExtractor → ViewModel. 자동 시작 없음.
- FR-11: `SettingsRepository`(DataStore). 불러오기 전에는 저장하지 않음. `notification_permission_asked` 키 추가.
- FR-8: [yt-dlp 업데이트] 버튼 + 결과 대화상자, 진행 중 "yt-dlp 업데이트 중..." 및 다른 조작 비활성.
- 모드 라벨이 비활성 시 흐려지지 않던 것 수정.
- 증거 (에뮬레이터):
  - 공유(앱 종료 상태): CLAUDE.md의 명령 → URL 필드 `https://youtu.be/jNQXAC9IVRw` (`build/m4_share.png`).
  - 공유(실행 중): `look (https://www.youtube.com/watch?v=wRGMlG96Gwg).` → 끝 문자 제거된 URL로 교체,
    MainActivity 인스턴스 1개(dumpsys `Hist` 1줄).
  - URL 없는 공유 → "공유된 내용에서 URL을 찾지 못했습니다.", 다운로드 중 공유 → URL만 채우고
    "진행 중인 다운로드가 끝난 뒤 시작하세요." (`build/m4_share_busy.png`).
  - 설정 유지: 영상 MKV/720p + 메타✗ + 재생목록✓ → 오디오 FLAC으로 바꾼 뒤 강제 종료·재실행 →
    오디오 FLAC(음질 비활성), 썸네일✓ 메타✗ 재생목록✓, 영상 전환 시 MKV/720p 복원, URL 빈 값
    (`build/m4_restored_audio.png`, `build/m4_restored_video.png`).
  - 업데이트: `pm clear` 후 표시 v2025.11.12 → [업데이트] → "최신 버전으로 업데이트했습니다 (v2026.10.26)"
    (`build/m4_update_done.png`), 표시 v2026.10.26 = 라이브러리 SharedPrefs `dlpVersion` 2026.10.26.
    다시 누르면 "이미 최신 버전입니다 (v2026.10.26)". 네트워크 끔 → "업데이트 실패: failed to update youtube-dl:
    Unable to resolve host "api.github.com"..." (`build/m4_update_failed.png`), 버전 유지.
  - 실기기 YouTube 앱 → 공유 확인은 최종 실기기 테스트로 미룸.

### 2026-10-31 M5 로그인 구현 완료 (사용자 실계정 검증은 최종 실기기 테스트로 미룸)

- CookieStore(`no_backup/cookies.txt`, 상태·저장·가져오기·로그아웃), LoginScreen(WebView, 1초 폴링 +
  onPageFinished 감지, flush → 2초 → 저장 → 닫기), 메인 화면 로그인 영역(상태·스위치·설명·로그아웃 확인),
  `cookieFile = 로그인 && 스위치`일 때만 `--cookies`. 다운로드가 끝나면 로그인 표시(파일 수정일) 갱신.
  UA는 기본값, `STRIP_WV_FROM_USER_AGENT` 상수 하나로 `; wv` 제거 전환 가능(SPEC O-1 대비).
- 데스크톱판 cookies.txt는 받지 못했고 `%LOCALAPPDATA%\YTLoader`에도 없음 → **더미 값**으로 검증
  (실제 쿠키 값은 쓰지도 출력하지도 않음).
- 증거 (에뮬레이터):
  - 로그아웃 상태 `build/m5_logged_out.png`, 로그인 화면(배너 + Google Sign in 페이지) `build/m5_login_screen.png`,
    뒤로가기 → "로그인이 취소되었습니다.".
  - 감지 흐름: DevTools 프로토콜로 WebView에 더미 `SAPISID`(.youtube.com) 설정 → 자동으로 닫히고
    "● 로그인됨 (2026-10-31)", 스위치 켜짐, "로그인 완료" (`build/m5_logged_in.png`). logcat에는
    `login cookies detected: SAPISID`(이름만). cookies.txt는 헤더 + 7필드 1줄(값은 마스킹해 확인).
  - 다운로드 인자: 스위치 켬 → python 명령줄에 `--cookies /data/user/0/io.github.dev1ck.ytloader/no_backup/cookies.txt`,
    다운로드 완료. 스위치 끔 → `--cookies` 없음.
  - 로그아웃: 확인 대화상자 `build/m5_logout_confirm.png` → "○ 로그인 안 됨", cookies.txt 삭제,
    로그인 화면을 다시 열어도 자동 감지되지 않음(WebView 쿠키 삭제 확인).
  - 가져오기: google.com만 있는 파일 → "YouTube 쿠키가 들어 있는 cookies.txt가 아닙니다."(상태 유지,
    `build/m5_import_invalid.png`). 데스크톱판 형식(CRLF, `#HttpOnly_` 줄 포함) 더미 파일 → "cookies.txt를 가져왔습니다.",
    로그인됨 + 스위치 다시 켜짐(`build/m5_import_valid.png`), 저장 파일 md5 = 원본.
- 남은 것 (최종 실기기 테스트): 실제 계정 앱 내 로그인 → 비공개/연령 제한 영상 다운로드, SPEC O-1 결과 기록,
  데스크톱판 실제 cookies.txt 가져오기.

### 2026-10-31 M6 마감 (진행 중 — ⏸ 라이선스·키스토어 대기)

- SPEC 7장 표 확인 (에뮬레이터):

  | 상황 | 결과 | 증거 |
  |---|---|---|
  | URL 비어 있음 | OK — "URL을 입력해 주세요.", 시작 안 함 | M3 |
  | 공유 텍스트에 URL 없음 | OK | M4 |
  | yt-dlp 실패 (일반) | OK — 대화상자(종료 코드 알 수 없음 + ERROR 줄 + 업데이트 안내) | `build/m3_failure_dialog.png` |
  | 로그인 키워드 안내 | 단위 테스트로 확인(ErrorHintMapperTest 4건). 기기 재현용 비공개 영상 없음 | — |
  | 네트워크 끊김 | OK — yt-dlp 오류("Unable to download API page ... Errno 7")로 처리 | `build/m6_network_off.png` |
  | 엔진 초기화 실패 | 재현 불가 — 코드 경로 확인(EngineStatus.Failed → 대화상자, 다운로드·업데이트 비활성, 로그인 영역은 사용 가능) | — |
  | 알림 권한 거부 | OK — `pm revoke` 후 다운로드 완료, 앱 알림 0건 | — |
  | dataSync 시간 제한 | OK — `device_config ... data_sync_fgs_timeout_duration 15000` + 백그라운드 → 16초에 중단, "시스템 제한으로 다운로드가 중단되었습니다." 알림·대화상자, 앱 프로세스 생존, 하위 프로세스 없음, 공용 저장소 부분 파일 없음. 이때는 즉시 `stopSelf`가 우선이라 재생목록 완성 항목도 저장하지 않음 | `build/m6_fgs_timeout_notification.png` |
  | 앱 프로세스 강제 종료 | OK — 다운로드 중 `force-stop` → `files/work/<id>` 남음 → 재실행 시 삭제 | — |
  | 같은 이름 존재 | OK — ` (1)`, ` (2)` | M3 표 |
  | 저장 공간 부족 | 재현 안 함 — 코드 경로: 복사 예외 → insert 항목 삭제 → Failed("저장 실패: ..."), 작업 폴더 삭제 | — |
  | 파일명 특수문자 | OK — `Pyro [1/3]` → `Pyro [1⧸3]` (`--windows-filenames`, 데스크톱판과 같은 규칙) | M3 |
  | 업데이트 중 네트워크 오류 | OK | `build/m4_update_failed.png` |
  | 로그인 막힘 | 배너 고정 문구 표시. 실제 차단 여부는 실기기 검증 | `build/m5_login_screen.png` |

- 시간 제한 테스트 중 발견: 앱이 백그라운드일 때 `startForegroundService`가 거부되면 예외로 앱이 죽고
  상태가 Preparing에 묶임(실사용 경로인 UI 버튼은 포그라운드라 해당 없음, debug 훅에서 발생)
  → `startDownload`에서 `IllegalStateException`을 잡아 Failed로 돌리도록 수정.
- 앱 아이콘(적응형, 빨간 배경 + 흰 다운로드 화살표, `build/m6_launcher.png`), 릴리스 서명 설정
  (`keystore.properties` 있으면 사용, 없으면 미서명), README 작성(라이선스 절은 결정 대기).
- 증거: `.\gradlew.bat testDebugUnitTest lintRelease assembleRelease` → BUILD SUCCESSFUL, 47 tests,
  lint 오류 0(경고 7: targetSdk 36·release arm64만·SPEC 문구 "..." 4건·`fullBackupContent` 권고 — 모두 의도),
  `app-release-unsigned.apk` 78.3MB (arm64-v8a).
- ⏸ 사용자 결정 대기: 앱 라이선스(SPEC O-4), 릴리스 키스토어 생성(사용자와 함께).
- 2026-10-31 사용자 결정: 앱 라이선스 **GPL-3.0** → `LICENSE`(gnu.org 원문), README·SPEC O-4 갱신.
  실기기 연결됨: SM-S948N, Android 17 (API 37), arm64-v8a.

### 2026-10-31 M6 키스토어 · 릴리스 서명 · 실기기 자동 테스트

- 키스토어: 사용자가 `C:\Users\firej\keys\ytloader-release.jks`(PKCS12, 별칭 `ytloader`, RSA 4096, 10000일) 생성,
  `keystore.properties`(Git 제외) 작성. 서명 인증서 `CN=dev1ck, O=YTLoader`,
  SHA-256 `9d45d10c55603e4d3586c6e149a4432e6bb6a6431df378e7bdfffd5b7fa8e981`.
  주의: 비밀번호를 입력한 파일 내용이 세션 대화에 표시됐음 → 사용자에게 알림(필요하면 `keytool -storepasswd`로 변경).
- 증거: `testDebugUnitTest lintRelease assembleRelease` → BUILD SUCCESSFUL, 47 tests, lint 오류 0,
  `app-release.apk` 78.3MB, `apksigner verify` 통과(v2, 서명자 1).
- 실기기 (SM-S948N, Android 17 / API 37, arm64-v8a) — 측정은 uiautomator 확인 지연 약 1초 포함:

  | 항목 | 결과 |
  |---|---|
  | 엔진 준비 (첫 실행 / 두 번째) | 3.3초 / 2.5초 |
  | mp3 + 썸네일 + 메타 (내장 2025.11.12) | 12.3초, 661,756 B |
  | yt-dlp 업데이트 | 5.1초 → v2026.10.26 |
  | mp3 / mp4 1080 (2026.10.26) | 14.4초 / 16.4초 (mp4 썸네일 mutagen) |
  | 화면 잠금 상태 | 재생목록 다운로드가 백그라운드에서 계속 진행(5개 항목 완료 확인) |
  | 다운로드 중 강제 종료 | 하위 프로세스 없음, 작업 폴더 남음 → 다음 실행 시 삭제, 공용 저장소에 부분 파일 없음 |
  | 재생목록 변환 중 취소 | 2.7초에 서비스 종료, 하위 프로세스 없음, `001 - ... Intro.mp3`(18,003,547 B)만 저장, "(완료된 1개 파일은 저장됨)" |
  | 릴리스 APK 설치 | 공유 인텐트로 URL 채움 → 알림 권한 요청 → 12.2초에 완료 (`build/device_release_done.png`) |

  SPEC O-2: 실기기에서도 실용적 → 해소.
- 테스트 중에만 `stay_on_while_plugged_in`을 켰고(원래 0) 끝난 뒤 0으로 되돌림. 휴대폰의 테스트 파일은 삭제.
  휴대폰에는 현재 **릴리스 서명 앱**이 설치돼 있음(디버그 앱은 제거).
- 남은 것 (사용자 직접): YouTube 앱 → 공유, 실제 계정 앱 내 로그인 → 비공개/연령 제한 영상(SPEC O-1), 데스크톱판 cookies.txt 가져오기.

### 2026-10-31 M7 UI/UX 개편

- 화면: URL 카드(붙여넣기/지우기 아이콘, 키보드 완료 시 닫힘) → 클립보드 제안 칩 → 제목 카드 →
  세그먼트(오디오만/영상+오디오) → 포맷·품질 FilterChip(가로 스크롤) → 옵션 스위치 카드(설명 포함),
  하단 고정 영역(결과 줄 + [다운로드] / 진행 패널 퍼센트·상세·[취소]). 앱바에 로그인 칩 + ⚙.
  설정 시트: 로그인·쿠키·로그아웃, yt-dlp 버전·[업데이트], 저장 위치, 버전·라이선스.
- 테마: 고정 브랜드 색(라이트·다크), 모서리 12/16/24/32dp. 기기 배경화면 색은 쓰지 않음.
- 새 순수 로직: `TitlePreview`(옵션: `--simulate --flat-playlist --playlist-items 1 --yes/--no-playlist
  --no-warnings [--cookies] --print "%(playlist_title|)s<TAB>%(title)s"`, 출력 해석), `UrlExtractor.isYouTube`.
  엔진 `query()`(stderr 합치지 않음). 다운로드 골든 예제(E1–E8)는 변경 없음.
- 아이콘: 사용자 업로드 2장 평가 — ① 완성 아이콘 콘셉트·안전 영역 적합, 세로 1% 아래 치우침,
  ② 전경은 실제 투명 PNG(1254px, 32bpp). → `docs/design/icon/icon-full-source.png`,
  `icon-foreground-source.png`로 이름 변경·이동, `icon-512.png`(README용) 생성.
  전경을 중앙 정렬·82% 축소해 mipmap 5종(108–432px) 생성(첫 시도 100%는 원형 마스크에서 너무 커 보여 축소),
  배경은 원본 색(#FD6F53→#EC3B47→#D30839) 그라데이션 벡터, 단색 테마 아이콘 = 같은 전경. 이전 벡터 아이콘 삭제.
- 증거 (에뮬레이터, `build/r1_*.png`): 빈 화면 `r1_main_empty`, 클립보드 제안 `r1_suggestion`,
  제목 미리보기(제안 칩 → 9.4초 후 "Me at the zoo") `r1_title`, 영상 모드 진행 패널 `r1_progress`(15% ...),
  완료 + [열기] `r1_done`([열기] → Google Photos로 열림), 설정 시트 `r1_settings`, 다크 모드 `r1_dark`,
  런처 아이콘 `r1_launcher`.
- 수정: 린트 오류(LocalContext로 문자열 조회) → `stringResource`로 변경.
- 알려진 동작: 화면에 떠 있는 상태에서 앱을 재설치(프로세스 강제 종료)하면 다음 실행 때 처음 받은
  공유 인텐트를 다시 처리할 수 있음(저장 상태가 없어서). 일반 사용(백그라운드 종료)에서는 해당 없음.

### 2026-10-31 M7 다크 모드 색 조정

- 사용자 피드백 "다크 모드가 너무 어둡다" → 검토: 밝기는 M3 표준 다크 톤이지만 ① 선택 상태가 탁한 갈색
  (`#5D3F3B`), ② 배경·카드·하단 바 밝기 차이가 거의 없음, ③ 배경에 적갈색 기운 때문에 칙칙하게 보임.
- 사용자 선택(권장안 A+B+C): 다크 표면을 중립 회색(배경 `#121212`, 카드 `#1E1E1E`, 하단 바 `#242424`,
  시트 카드 `#2C2C2C`)으로, 선택 칩·세그먼트는 짙은 레드(`#7A1A22`). 주 버튼(연분홍)은 유지.
  제목 카드와 설정 시트 카드는 두 모드 모두 `surfaceContainerHigh`(시트 배경과 같은 색이라 안 보이던 문제도 해결).
- 증거: 에뮬레이터 `build/r2_dark_main.png`, `r2_dark_settings.png`, `r2_light_main.png`, `r2_light_settings.png`.
  `testDebugUnitTest lintRelease assembleRelease` 성공(55 tests, lint 오류 0), 휴대폰 릴리스 앱 업데이트 완료.

### 2026-10-31 M7 완료 후 공유 · 폴더 열기 (FR-12 확장)

- 사용자 요청: 받은 파일을 바로 공유하고, 파일 앱에서 저장 위치로 바로 이동. 설계를 먼저 제시하고 결정받음:
  완료 카드(A안) + 완료 알림 [공유]·[폴더] + 설정 "저장 위치" 탭. 마지막 결과 유지(앱 재시작 후)는 하지 않음.
- 설계 전 확인: 에뮬레이터에서 `ACTION_VIEW` + 폴더 문서 URI(`vnd.android.document/directory`)가
  시스템 파일 앱(`com.google.android.documentsui` FilesActivity)으로 `Download › YTLoader`를 바로 연다
  (`build/r3_folder_view.png`). 휴대폰은 연결이 끊겨 삼성 동작은 미확인.
- 구현: `SavedFile`(실제 저장 이름·크기·MIME·하위 폴더), `ShareRules`(순수, 테스트 3), `FileActions`,
  `DownloadState`가 저장 파일 목록을 가짐, 완료 카드 UI, 알림 동작, 설정 시트 저장 위치 클릭, `<queries>`.
- 증거 (에뮬레이터): 단위 테스트 58 통과, `lintDebug` 오류 0.
  - 단일 완료 카드 `build/r3_card_single.png` ("Me at the zoo (5).mp3 · 669 kB · Download/YTLoader" — MediaStore가
    붙인 실제 이름 표시). [공유] → 시스템 공유 시트 "Sharing 1 file" `build/r3_share_sheet.png`.
    [폴더] → 파일 앱 "Files in YTLoader".
  - 재생목록 취소(일부 저장) 카드 `build/r3_card_partial.png` ("완료된 1개 저장됨 · 18 MB · Download/YTLoader/Team
    Fortress 2 [2010 Version]", [공유]·[폴더]). [폴더] → "Files in Team Fortress 2 [2010 Version]"
    `build/r3_folder_playlist.png`.
  - 완료 알림 [공유]·[폴더] `build/r3_notification.png`, 알림 [폴더] → 파일 앱. 설정 "저장 위치" → 파일 앱.
  - 카드의 파일을 셸에서 지운 뒤 [공유] → "파일을 찾을 수 없습니다. 지웠거나 옮겼을 수 있습니다.".

