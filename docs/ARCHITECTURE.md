# YTLoader Android — 아키텍처 · 상세 설계

기준일: 2026-10-31. [SPEC.md](SPEC.md)의 요구사항(FR/NFR)을 **어떻게** 구현하는지 정한다.

표기:
- **[결정]** 이대로 구현한다. 바꿔야 할 이유가 생기면 구현 전에 사용자에게 이유와 함께 묻는다.
- **[구현자 판단]** 구현 세션이 정하고, 정한 내용을 PLAN.md 작업 로그에 한 줄 남긴다.
- **[확인 필요]** 외부 사실(라이브러리 API 등)을 코드/문서로 확인한 뒤 진행한다.

## 목차

1. 기술 스택
2. 프로젝트 구조
3. 런타임 구성과 데이터 흐름
4. yt-dlp 엔진 래퍼
5. yt-dlp 옵션 매핑 (골든 예제)
6. 진행 · 상태 파싱
7. 다운로드 서비스
8. 저장 (MediaStore)
9. 로그인 · 쿠키
10. yt-dlp 업데이트 · 버전
11. UI 구조와 상태
12. 설정 저장
13. 매니페스트 · 권한 · 빌드 설정
14. 오류 처리
15. 보안
16. 테스트 전략

---

## 1. 기술 스택

| 영역 | 선택 | 비고 |
|---|---|---|
| 언어 | Kotlin | [결정] |
| UI | Jetpack Compose + Material 3, 단일 Activity | [결정] |
| 화면 전환 | Navigation 라이브러리 없이 상태 기반 2화면(메인/로그인) | [결정] 화면이 2개뿐 |
| 아키텍처 | ViewModel + StateFlow (단방향 데이터 흐름) | [결정] |
| DI | 수동 DI (`AppContainer`) — Hilt/Koin 미사용 | [결정] 규모 대비 과함 |
| AndroidX 기본 | core-ktx, activity-compose, lifecycle-viewmodel-compose, lifecycle-runtime-compose, Compose BOM의 ui·material3·ui-tooling(debug)·material-icons-core | [결정] |
| 비동기 | kotlinx-coroutines (android) | [결정] |
| 설정 | DataStore Preferences | [결정] |
| Intent 직렬화 | `kotlin-parcelize` 플러그인 (`@Parcelize`) | [결정] 런타임 라이브러리 추가 없음 |
| yt-dlp 엔진 | `io.github.junkfood02.youtubedl-android:library` / `:ffmpeg` **0.18.1** 고정, 저장소 `mavenCentral()` | [결정] aria2c 모듈은 쓰지 않음 |
| 빌드 | Gradle Kotlin DSL + 버전 카탈로그(`gradle/libs.versions.toml`) | [결정] AGP 9의 내장 Kotlin 지원을 쓰므로 `org.jetbrains.kotlin.android` 플러그인은 적용하지 않는다 |
| AGP·Kotlin·Compose BOM 버전 | 구현 시점 최신 안정판 | [구현자 판단] 선택 버전을 작업 로그에 기록 |
| 테스트 | JUnit (JVM 단위 테스트) | [결정] 순수 로직 위주 (16장) |

youtubedl-android 0.18.1 사실 (2026-10-31 확인, GitHub yausername/youtubedl-android):
- 0.18.1(2025-11-16, GitHub상 pre-release): QuickJS 내장. `execute()`가 자동으로
  `--js-runtimes quickjs:<nativeLibraryDir>/libqjs.so`를 붙인다.
- 0.18.0: 16KB 페이지 크기 지원. 내장 Python 3.12, ffmpeg 7.1.
- 라이브러리 minSdk 24, ABI: x86, x86_64, armeabi-v7a, arm64-v8a.
- `execute()`가 `--ffmpeg-location`도 자동으로 붙인다. `--cache-dir`가 없으면 `--no-cache-dir`도 붙인다.
- 라이선스: **GPL-3.0** (저장소 LICENSE, 2026-10-31 확인). 이 라이브러리를 포함해 APK를
  배포하면 앱도 GPL-3.0 호환 라이선스여야 한다 (SPEC O-4, M6에서 사용자 결정).
- 내장 yt-dlp는 2025.11.12. 첫 실행 시 "older than 90 days" 경고가 stderr에 나오며,
  업데이트(STABLE) 후 2026.10.26에서 QuickJS로 JS 챌린지를 푼다(`[jsc:quickjs]`).

## 2. 프로젝트 구조

단일 모듈 `app`. 패키지 루트 `io.github.dev1ck.ytloader` (SPEC O-5, 기본값).

```
app/src/main/java/io/github/dev1ck/ytloader/
├─ YTLoaderApp.kt              Application. AppContainer 생성, 엔진 초기화 시작, 작업 폴더 정리
├─ AppContainer.kt             수동 DI: 아래 싱글턴들을 보관
├─ MainActivity.kt             단일 Activity. 공유 인텐트 수신, 화면 전환, 권한 요청
├─ ui/
│  ├─ main/                    MainScreen, MainViewModel, MainUiState
│  ├─ login/                   LoginScreen(WebView), LoginController
│  └─ theme/
├─ download/
│  ├─ FormatCatalog.kt         6장 카탈로그 (순수)
│  ├─ DownloadOptions.kt       사용자 선택값 (순수 data class)
│  ├─ YtDlpArgsBuilder.kt      DownloadOptions → List<YtOption> (순수)
│  ├─ ProgressParser.kt        출력 줄 → 상태 문구/진행률 (순수)
│  ├─ ErrorHintMapper.kt       마지막 오류 → 안내 문구 (순수)
│  ├─ DownloadState.kt         상태 모델
│  ├─ DownloadRepository.kt    StateFlow<DownloadState> 공유 (서비스 ↔ UI)
│  ├─ DownloadService.kt       포그라운드 서비스: 실행·취소·알림·타임아웃
│  ├─ DownloadNotifications.kt 알림 채널/빌더
│  └─ MediaStoreExporter.kt    작업 폴더 → Download/YTLoader 복사
├─ login/
│  ├─ CookieStore.kt           cookies.txt 경로·상태·삭제, 로그아웃
│  ├─ NetscapeCookieWriter.kt  쿠키 → Netscape 텍스트 (순수)
│  └─ CookieFileValidator.kt   가져온 파일 검증 (순수)
├─ engine/
│  ├─ YtDlpEngine.kt           인터페이스 (테스트 대체 가능)
│  └─ YoutubeDlAndroidEngine.kt youtubedl-android 구현
├─ settings/SettingsRepository.kt  DataStore
└─ util/UrlExtractor.kt        공유 텍스트 → 첫 http(s) URL (순수)
```

"(순수)" 표시 파일은 Android API를 import하지 않는다 → JVM 단위 테스트 대상.
파일 분할은 [구현자 판단]으로 조정 가능하나 순수/비순수 경계는 유지한다.

M2에서 정한 조정 ([구현자 판단]):
- `isExportable`은 `download/ExportRules.kt`(순수)로 분리, MediaStoreExporter가 사용.
- `DownloadOptions`는 순수 경계를 지키려고 `java.io.Serializable`로 두고, Intent에는
  서비스 쪽 `@Parcelize` 래퍼(url, options, cookieFile)로 담는다.
- `YtOption`, `ExecResult`, `UpdateResult`는 `engine/YtDlpEngine.kt`(순수)에 둔다.
- 카탈로그 라벨, 진행 문구, 오류 안내 문구는 순수 로직의 출력이라 Kotlin 상수로 둔다
  (단위 테스트로 SPEC 문구와 대조). 그 외 화면 문자열은 `strings.xml`.

## 3. 런타임 구성과 데이터 흐름

```
MainActivity ──(공유 URL)──▶ MainViewModel ◀──StateFlow── DownloadRepository ◀── DownloadService
     │                         │   │                                                │
     │ (로그인 화면 전환)        │   └─ SettingsRepository (DataStore)                 ├─ YtDlpEngine.execute
     ▼                         │                                                    ├─ ProgressParser
LoginScreen(WebView)           ├─ CookieStore (상태·로그아웃·가져오기)                 ├─ MediaStoreExporter
     └─ 쿠키 감지 → CookieStore ├─ YtDlpEngine (init 상태·version·update)            └─ DownloadNotifications
                               └─ startForegroundService(DownloadService, 요청)
```

- **AppContainer**(Application에서 생성): `SettingsRepository`, `CookieStore`,
  `YtDlpEngine`, `DownloadRepository`, `EngineStatus`(StateFlow: Initializing/Ready/Failed).
- **단일 작업 원칙**: 동시에 하나의 작업(다운로드 또는 업데이트)만. UI가 버튼을 막고,
  서비스도 실행 중이면 새 시작 요청을 무시한다.
- 프로세스가 죽으면 상태는 사라진다(복구하지 않음). 다음 시작 시 작업 폴더만 정리.

## 4. yt-dlp 엔진 래퍼

```kotlin
interface YtDlpEngine {
    suspend fun init()                          // YoutubeDL.init + FFmpeg.init, IO 디스패처, 실패 시 예외
    fun version(): String?                      // 현재 yt-dlp 버전 문자열 (`--version` 실행, 블로킹)
    fun execute(
        url: String,
        options: List<YtOption>,
        processId: String,
        onLine: (progress: Float, etaSec: Long, line: String) -> Unit,
    ): ExecResult                               // 블로킹. 호출자는 IO 스레드에서 호출
    fun cancel(processId: String): Boolean
    fun update(): UpdateResult                  // STABLE 채널. 블로킹
}
data class YtOption(val name: String, val value: String? = null)
```

라이브러리 매핑 (0.18.1 소스 `d725d5c`와 M1 에뮬레이터 실행으로 2026-10-31 확인):
- `YoutubeDL.getInstance().init(ctx)`, `FFmpeg.getInstance().init(ctx)` — 앱 시작 시
  1회, IO 스레드. 첫 실행은 내장 Python 압축 해제로 수 초 걸릴 수 있다.
- `execute(request, processId, callback: (Float, Long, String) -> Unit): YoutubeDLResponse`
  - `YoutubeDLRequest(url)`에 `addOption(name)` / `addOption(name, value)`로 옵션 추가.
  - 실패 시 `YoutubeDLException`, 취소 시 `CanceledException` (또는 `InterruptedException`).
  - 같은 processId 중복 실행은 예외 → 작업마다 UUID 사용.
- 옵션은 이름별 `LinkedHashMap`에 모인다(같은 이름을 여러 번 넣으면 첫 위치에 묶임).
  우리 빌더는 같은 옵션을 반복하지 않으므로 순서가 그대로 유지된다.
- `destroyProcessById(id)`: 메인 프로세스를 `destroy()`하고 맵에서 지운다 → `execute`가
  `CanceledException`. 하위 프로세스 종료 코드는 `pstree -p <processId>`에 **PID가 아니라
  우리가 넘긴 문자열 ID**를 넣어서 사실상 동작하지 않는다. M3에서 확인: 변환 중 취소하면
  ffmpeg가 계속 돌며 결과 파일을 쓴다. → [구현자 판단] 작업 ID를 processId로 쓰고 작업 폴더
  경로에 넣으므로, `cancel()`이 `/proc/*/cmdline`에 그 ID가 들어 있는 자기 UID 프로세스를
  `Process.killProcess`로 함께 종료한다 (M3 재검증: 취소 3초 후 남은 프로세스 없음).
- `updateYoutubeDL(ctx, UpdateChannel.STABLE)`: `DONE` / `ALREADY_UP_TO_DATE`, 실패 시
  `YoutubeDLException`. GitHub API로 최신 태그를 받아 SharedPreferences에 저장한다.
- 버전 조회: `version(ctx)`는 그 SharedPreferences 값이라 **업데이트를 한 번도 안 했으면 null**
  (내장 바이너리 버전을 모름). 따라서 `version()`은 `--version`을 실행해 stdout 첫 줄을
  쓴다 (에뮬레이터 0.7–1.3초). 업데이트 후에는 `version(ctx)`와 같은 값(예 `2026.10.26`).
- 출력 전달: `execute(request, id, redirectErrorStream, callback)`.
  - `false`(기본): stderr는 콜백에 오지 않고, 실패 시 `YoutubeDLException(message = stderr 전체)`.
  - `true`: stderr 줄(`WARNING:`, `ERROR:`)도 콜백으로 오고, 실패 시 예외 메시지는 빈 문자열.
  - 종료 코드는 성공 시에만 `YoutubeDLResponse.exitCode`로 알 수 있다. 실패 시는 모름.
- **우리 사용법**: `redirectErrorStream = true`로 실행해 `ERROR:` 줄을 ProgressParser 규칙 4로
  받는다. `ExecResult`: `exitCode: Int?`(성공 0, 실패 null → 화면에 "알 수 없음"),
  `lastError`(마지막 `ERROR:` 줄, 없으면 예외 메시지의 마지막 비어 있지 않은 줄, 그것도 없으면
  예외 클래스 이름), `cancelled: Boolean`.
  `execute()`는 예외를 밖으로 던지지 않고 항상 `ExecResult`로 바꿔 돌려준다.
- 패키징: README는 `android:extractNativeLibs="true"`를 요구한다(실행 파일을 nativeLibraryDir에서
  직접 실행). AGP에서는 `packaging.jniLibs.useLegacyPackaging = true`로 같은 효과를 낸다
  (샘플 앱과 동일). APK가 커진다(debug 2 ABI 약 137MB).
- 성능 (M1, 에뮬레이터 API 37 x86_64, jNQXAC9IVRw): init 첫 실행 2.0초 / 이후 0.12초,
  `--simulate --print title` 5.8초(내장판) / 10.3초(2026.10.26, QuickJS 사용),
  mp3+썸네일+메타 19.0초 / 24.4초, 결과 669KB. `--cache-dir`는 시간 차이 없음 → 쓰지 않음.

## 5. yt-dlp 옵션 매핑 (골든 예제)

데스크톱판 `Build-YtDlpArgs`와 같은 규칙. **라이브러리가 붙이는
`--ffmpeg-location`, `--js-runtimes`는 넣지 않는다.** URL은 옵션이 아니라
`YoutubeDLRequest(url)`로 전달한다.

### 규칙 (이 순서로 출력)

1. 포맷 블록
   - 오디오: `-f bestaudio/best`. 포맷이 `original`이 아니면 `-x --audio-format <fmt>`,
     그리고 손실 포맷(mp3, m4a, opus)이면 `--audio-quality <q>`.
   - 영상: `-f bv*+ba/b`. 정렬 키 목록 = [`res:<h>` (해상도 지정 시)] +
     [`ext:mp4:m4a` (mp4일 때)] + [`ext:webm:webm` (webm일 때)].
     목록이 비어 있지 않으면 `-S <쉼표로 연결>`. 포맷이 `original`이 아니면
     `--merge-output-format <fmt> --remux-video <fmt>`.
2. `--embed-thumbnail` — 썸네일 지원 포맷이고 체크된 경우만.
3. `--embed-metadata` — 체크된 경우.
4. `--cookies <쿠키 파일 절대경로>` — 로그인 상태이고 [로그인 쿠키 사용]이 켜진 경우.
5. 재생목록 체크 시 `--yes-playlist`, 아니면 `--no-playlist`.
6. `--windows-filenames` — Android 추가. 파일명을 데스크톱판과 같게 만들고
   공용 저장소에서 문제 되는 문자를 피한다.
7. `--newline --progress`
8. `-o <템플릿>`
   - 단일: `<W>/%(title)s.%(ext)s`
   - 재생목록: `<W>/%(playlist_title|Playlist)s/%(playlist_index|0)03d - %(title)s.%(ext)s`

`<W>` = 작업 폴더 절대경로 (8장), `<C>` = 쿠키 파일 절대경로 (9장).

### 골든 예제 (단위 테스트로 그대로 옮긴다)

`YtOption` 목록을 `[name, value?]` 순서로 펼친 토큰 목록과 비교한다.
아래 "기대"는 공백으로 구분한 토큰이며, `-o` 다음 템플릿은 공백을 포함해도 **토큰 하나**다.

```
E1  입력: 오디오 mp3, 품질 0, 썸네일✓ 메타✓ 재생목록✗ 쿠키✗
    기대: -f bestaudio/best -x --audio-format mp3 --audio-quality 0 --embed-thumbnail --embed-metadata --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E2  입력: 오디오 flac, 품질 320K(무시됨), 썸네일✓ 메타✗ 재생목록✗ 쿠키✗
    기대: -f bestaudio/best -x --audio-format flac --embed-thumbnail --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E3  입력: 오디오 original, 품질 0(무시됨), 썸네일✓(미지원 → 제외) 메타✓ 재생목록✗ 쿠키✗
    기대: -f bestaudio/best --embed-metadata --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E4  입력: 오디오 m4a, 품질 192K, 썸네일✗ 메타✓ 재생목록✗ 쿠키✓
    기대: -f bestaudio/best -x --audio-format m4a --audio-quality 192K --embed-metadata --cookies <C> --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E5  입력: 영상 mp4, 해상도 1080, 썸네일✓ 메타✓ 재생목록✗ 쿠키✗
    기대: -f bv*+ba/b -S res:1080,ext:mp4:m4a --merge-output-format mp4 --remux-video mp4 --embed-thumbnail --embed-metadata --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E6  입력: 영상 webm, 최고 화질, 썸네일✓(미지원 → 제외) 메타✓ 재생목록✗ 쿠키✗
    기대: -f bv*+ba/b -S ext:webm:webm --merge-output-format webm --remux-video webm --embed-metadata --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s

E7  입력: 영상 original, 최고 화질, 썸네일✓(미지원 → 제외) 메타✓ 재생목록✓ 쿠키✓
    기대: -f bv*+ba/b --embed-metadata --cookies <C> --yes-playlist --windows-filenames --newline --progress -o <W>/%(playlist_title|Playlist)s/%(playlist_index|0)03d - %(title)s.%(ext)s

E8  입력: 영상 mkv, 해상도 720, 썸네일✗ 메타✗ 재생목록✗ 쿠키✗
    기대: -f bv*+ba/b -S res:720 --merge-output-format mkv --remux-video mkv --no-playlist --windows-filenames --newline --progress -o <W>/%(title)s.%(ext)s
```

쿠키 조건 "로그인 상태 && 사용 켜짐"은 빌더 입력의 `cookieFile: String?`
(null이면 미사용)로 단순화한다. 판단은 호출 측(ViewModel/서비스)에서 한다.

## 6. 진행 · 상태 파싱

```kotlin
sealed interface ParsedLine {
    data class Progress(val percent: Float, val text: String) : ParsedLine      // 규칙 1
    data class PlaylistItem(val index: Int, val count: Int, val text: String) : ParsedLine  // 규칙 2 (진행률 유지, 처리 중 항목 번호 기억)
    data class PostProcessing(val tag: String, val text: String) : ParsedLine   // 규칙 3 (진행률 100)
    data class Error(val line: String) : ParsedLine                             // 규칙 4
}
fun parse(line: String): ParsedLine?   // 매칭 없으면 null (상태 유지)
```

데스크톱판과 같은 정규식을 위에서부터 순서대로 검사한다:

```
1. ^\[download\]\s+([\d.]+)%(.*)$
   → 진행률 = 그룹1, 문구 = "다운로드 중... " + 그룹1 + "%" + (그룹2의 연속 공백을 한 칸으로)
2. ^\[download\] Downloading item (\d+) of (\d+)
   → 문구 = "재생목록 <그룹1> / <그룹2> 항목 처리 중..." (진행률 유지)
3. ^\[(ExtractAudio|Merger|VideoRemuxer|VideoConvertor|EmbedThumbnail|Metadata|FixupM4a|FixupM3u8)\]
   → 진행률 100, 문구 = "변환 / 후처리 중... [<그룹1>]"
4. ^ERROR:
   → 상태 변경 없음, 마지막 오류로 기억
```

라이브러리 콜백의 `progress` 인자도 오지만, 문구 일관성을 위해 **줄 파싱을 기준**으로 한다.
알림 갱신은 최대 초당 2회로 제한한다(알림 갱신 과다 시 시스템이 무시함).

## 7. 다운로드 서비스

**[결정] 포그라운드 서비스, type `dataSync`.**
대안이었던 UIDT 작업(JobScheduler `setUserInitiated`)은 API 34+ 전용이라
minSdk 29에서 이중 구현이 필요하다. dataSync의 Android 15+ 제한(24시간당 6시간)은
개인용 다운로더에 충분하다.

```
start(요청)                                  cancel
  │                                           │
  ├ startForeground(진행 알림, DATA_SYNC)      ├ engine.cancel(processId)
  ├ 상태 Preparing                             └ (execute가 CanceledException → Cancelled)
  ├ 작업 폴더 생성: filesDir/work/<jobId>/
  ├ options = YtDlpArgsBuilder(...)
  ├ engine.execute(url, options, jobId) ── onLine → ProgressParser → 상태/알림
  ├ 성공: MediaStoreExporter.export(작업 폴더) → 상태 Succeeded(uris)
  ├ 실패/취소: 재생목록 모드면 MediaStoreExporter.export(작업 폴더, 처리 중 항목 번호)로
  │            완성된 항목만 저장, 단일 영상이면 저장 안 함.
  │            (M3에서 확인: 8장의 이름 규칙만으로는 변환 전 원본 `002 - x.webm`과 쓰다 만
  │            `002 - x.mp3`를 걸러내지 못한다 → 마지막 "Downloading item N" 의 N번 항목
  │            파일(`NNN - ` 접두사)은 전부 제외. 그 앞 항목들은 후처리까지 끝난 상태다.)
  │            → 상태 Failed(exitCode?, lastError, hint, savedCount) / Cancelled(savedCount)
  ├ finally: 작업 폴더 삭제, stopForeground, stopSelf, 결과 알림
```

- 요청 전달: `Intent` extra(직렬화한 `DownloadOptions` + url + cookieFile?).
- 코루틴: 서비스 전용 `CoroutineScope(SupervisorJob() + Dispatchers.IO)`, `onDestroy`에서 취소.
- `onTimeout(startId, fgsType)` (API 35+): 취소 처리 → 상태 Failed("시스템 제한으로 다운로드가 중단되었습니다.") → `stopSelf()`를 즉시 호출(수 초 안에 안 하면 앱이 크래시).
- 알림 (DownloadNotifications):
  - 채널 `download_progress` (IMPORTANCE_LOW): 진행 알림, ongoing, onlyAlertOnce,
    진행률 0–1000, [취소] 액션(서비스에 ACTION_CANCEL).
  - 채널 `download_result` (IMPORTANCE_DEFAULT): 완료/실패 알림. 완료 알림 탭 →
    첫 파일 `ACTION_VIEW`(content URI + MIME, 읽기 권한 부여).
- 앱 시작 시 `filesDir/work/` 아래 남은 폴더 전부 삭제 (서비스가 돌고 있지 않을 때).

## 8. 저장 (MediaStore)

**[결정] `MediaStore.Downloads`, `RELATIVE_PATH = "Download/YTLoader/<하위 경로>"`.**
Music/Movies 컬렉션은 MIME과 확장자 규칙이 엄격해(.opus, .mkv 등에서 확장자 변경 위험)
Downloads로 통일한다. 음악·동영상 앱은 Download 폴더도 스캔한다.

절차 (작업 폴더의 각 최종 파일마다):
1. 대상 파일: 작업 폴더 아래 일반 파일 중 다음을 제외한 것 (yt-dlp의 중간 산출물):
   - 확장자 `.part`, `.ytdl`, `.temp`, `.frag` 또는 `.part-Frag` 로 시작하는 조각
   - 이름에 `.temp.` 포함 (후처리 중 임시 파일)
   - 이름이 `\.f\d+\.[A-Za-z0-9]+$`에 맞는 파일 (병합 전 개별 스트림, 예: `제목.f137.mp4`)
   - 이미지 `.jpg .jpeg .png .webp` (썸네일 잔여물)
2. 하위 경로 = 작업 폴더 기준 상대 디렉터리(재생목록 폴더). 없으면 빈 값.
3. `ContentValues`: `DISPLAY_NAME`=파일명, `MIME_TYPE`=확장자 기반
   (`MimeTypeMap`, 없으면 `application/octet-stream`), `RELATIVE_PATH`,
   `IS_PENDING=1` → insert → 스트림 복사 → `IS_PENDING=0` update.
4. 복사 실패 시 insert한 항목 삭제 후 오류.
- 결과: 저장된 URI 목록(완료 알림/열기에 사용).
- M3에서 각 포맷(mp3, m4a, opus, flac, wav, mp4, mkv, webm, original)의 **최종 파일명이
  확장자까지 유지되는지** 확인하고 결과를 작업 로그에 남긴다. → 확인됨 (2026-10-31, 10종 모두
  유지, 같은 이름은 ` (1)`로 자동 변경). MIME은 `MimeTypeMap` 그대로 써도 된다.

작업 폴더는 `cacheDir`가 아니라 `filesDir/work/` — 긴 다운로드 중 시스템이 캐시를
비우면 실패하기 때문이다.

## 9. 로그인 · 쿠키

### 쿠키 파일

- 경로: `noBackupFilesDir/cookies.txt` (백업 제외 영역).
- 로그인 상태 = 파일 존재. 표시 날짜 = 파일 수정일(`yyyy-MM-dd`).
  yt-dlp는 실행 후 갱신된 쿠키를 이 파일에 다시 쓴다(데스크톱판과 동일, 정상).

### 앱 내 로그인 (FR-9)

- 시작 URL (데스크톱판과 동일):
  `https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Dko%26next%3Dhttps%253A%252F%252Fwww.youtube.com%252F`
- WebView 설정: JavaScript, DOM storage 켬. `CookieManager.setAcceptCookie(true)`,
  `setAcceptThirdPartyCookies(webView, true)`.
- User-Agent: 기본값으로 시작. Google이 막으면(SPEC O-1) UA에서 `; wv` 토큰을 제거한
  값을 시도한다 — 상수 하나로 바꿀 수 있게 둔다. 결과는 작업 로그에 기록.
- 감지: `onPageFinished` 시점 + 화면이 보이는 동안 1초 간격으로
  `CookieManager.getCookie("https://www.youtube.com")`를 `; `로 나눠 이름 목록을 얻고,
  `SAPISID` 또는 `__Secure-3PAPISID`가 있으면 로그인으로 판단.
- 감지 후: `CookieManager.flush()` → 2초 대기(리다이렉트 마무리) → 다시 읽기 →
  `NetscapeCookieWriter`로 저장 → `use_cookies = true` 저장 → 화면 닫기 → "로그인 완료" 표시.
- 쿠키 값은 로그로 남기지 않는다. 디버그 로그에도 **이름만**.

### Netscape 형식 (NetscapeCookieWriter)

`CookieManager.getCookie()`는 `이름=값`만 준다. 따라서 모든 쿠키를 다음 속성으로 쓴다:

```
# Netscape HTTP Cookie File
.youtube.com	TRUE	/	TRUE	<지금+1년 epoch초>	<이름>	<값>
```

- 값에 `=`가 포함될 수 있으므로 첫 `=` 기준으로만 나눈다. 빈 이름은 버린다.
- 이 방식은 public API만 쓴다. 대안(WebView 내부 SQLite `app_webview/Default/Cookies`를
  직접 읽기 — Seal의 방식)은 내부 형식 의존이라 쓰지 않는다. getCookie 방식으로
  로그인 영상이 실패할 때만 사용자와 상의해 검토한다.

### 가져오기 (FR-10, CookieFileValidator)

- `ActivityResultContracts.OpenDocument`, MIME `text/plain`, `*/*`.
- 1MB 초과 파일은 거부. 줄 단위로 읽어 `#HttpOnly_` 접두사를 제거한 뒤
  탭으로 나눈 필드가 7개이고 도메인이 `youtube.com`으로 끝나는 줄이 1개 이상이면 통과.
- 통과 시 원문 그대로 쿠키 파일 경로에 저장(덮어쓰기).

### 로그아웃

`CookieManager.removeAllCookies` + `flush`, `WebStorage.getInstance().deleteAllData()`,
쿠키 파일 삭제.

## 10. yt-dlp 업데이트 · 버전

- `MainViewModel`에서 IO 디스패처로 `engine.update()` → 이어서 `engine.version()`.
- 다운로드 중에는 버튼 비활성 (3장 단일 작업 원칙). 업데이트는 서비스 없이 실행해도 된다
  (수 초~수십 초, 화면을 떠나면 중단돼도 무방).
- 버전 표시는 엔진 초기화 완료 시 1회, 업데이트 후 1회 조회.

## 11. UI 구조와 상태

- `MainActivity`: `launchMode="singleTask"` — YouTube 앱에서 공유해도 YouTube의 작업
  스택에 두 번째 인스턴스가 생기지 않게 한다. `onCreate`/`onNewIntent`에서 공유 인텐트를
  `UrlExtractor`로 처리해 ViewModel에 전달. 상태 `screen = Main | Login`으로 전환,
  로그인 화면은 `BackHandler`로 닫기.
- `UrlExtractor.extract(text): String?` — 정규식 `https?://\S+`의 첫 매치에서 끝에 붙은
  `. , ! ? ; : ) ] } > " '` 문자를 반복 제거해 반환. 없으면 null.
- `MainUiState` (단일 data class, 파생값은 계산 프로퍼티):
  url, mode, format, quality, thumbnail/metadata/playlist 체크, useCookies,
  loginState(LoggedOut | LoggedIn(date)), engineStatus, downloadState, ytDlpVersion,
  updating, dialog(오류/업데이트 결과/로그아웃 확인 중 하나 또는 null), snackbar 메시지.
- `DownloadState`: `Idle | Preparing | Running(progress, text) | PostProcessing(text) |
  Succeeded(uris) | Failed(exitCode?, lastError, hint, savedCount) | Cancelled(savedCount)`.
- 활성 규칙: `busy = downloadState in (Preparing, Running, PostProcessing) || updating`.
  - busy이면: 입력 컨트롤·[다운로드]·[업데이트]·로그인 영역 비활성. [취소]만 활성(다운로드 중).
  - 엔진이 Ready가 아니면: [다운로드]·[업데이트]만 비활성. 입력과 로그인 영역
    (로그인·가져오기·로그아웃)은 사용 가능 — 엔진 실패와 무관한 기능이기 때문.
- 문자열은 전부 `res/values/strings.xml`.

## 12. 설정 저장

DataStore 키: `mode`, `audio_format`, `audio_quality`, `video_format`, `video_resolution`,
`embed_thumbnail`, `embed_metadata`, `playlist`, `use_cookies`, `notification_permission_asked`
([구현자 판단] 알림 권한을 "첫 다운로드 때 한 번" 묻기 위한 기록). 값은 카탈로그의 **값**
문자열로 저장(인덱스 아님 — 목록 변경에 안전). 알 수 없는 값이면 첫 항목으로.
모드별 포맷/품질을 따로 기억한다.

## 13. 매니페스트 · 권한 · 빌드 설정

- 권한: `INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`,
  `POST_NOTIFICATIONS`. 저장소 권한 없음.
- 서비스: `android:foregroundServiceType="dataSync"`, `exported="false"`.
- MainActivity intent-filter: `ACTION_SEND` + `CATEGORY_DEFAULT` + `mimeType="text/plain"`.
- `android:allowBackup="false"` (쿠키 보호, NFR-2).
- SDK: minSdk 29, targetSdk 36, compileSdk 37. (2026-10-31 사용자 결정: 설치된 플랫폼이
  37이고 compileSdk는 동작을 바꾸지 않음. targetSdk 37은 네이티브 라이브러리 `System.load`
  읽기 전용 강제 등 라이브러리 호환이 검증되지 않아 36 유지.)
- ABI: debug는 `arm64-v8a`, `x86_64`(에뮬레이터), release는 `arm64-v8a`만.
- 릴리스 서명: 저장소 루트 `keystore.properties`(gitignore)에서 읽고, 없으면 release
  서명 설정을 건너뛴다. 키스토어 생성은 사용자와 함께 한다.
- `.gitignore`: `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`,
  `build/`, `.gradle/`, `.idea/`, `*.apk`, `cookies.txt`.

## 14. 오류 처리

- `ErrorHintMapper.hint(lastError: String?): String` — SPEC 7장 규칙.
  기본 안내 "yt-dlp 업데이트 후 다시 시도해 보세요."에, 로그인 관련 키워드가 있으면
  로그인 안내를 덧붙인다.
- 엔진 초기화 실패: `EngineStatus.Failed(message)` → 대화상자, 다운로드·업데이트 비활성.
- 예외를 삼키지 않는다: 모든 실패는 상태(Failed)나 대화상자로 사용자에게 보인다.
- 로그 태그 `YTLoader`. yt-dlp 출력 줄은 debug 빌드에서만 로그 (쿠키 값은 출력에 없음).

## 15. 보안

- 쿠키: 비백업 영역, 백업 비활성, 로그/알림/오류 문구에 값 미노출, 로그아웃 시 완전 삭제.
- WebView: 로그인 화면에서만 사용. 임의 URL 로드 금지(google.com, youtube.com 계열
  외 내비게이션은 외부 브라우저로 넘기거나 차단 — [구현자 판단]).
- 내보내는 컴포넌트는 MainActivity(공유 수신)뿐.

## 16. 테스트 전략

### JVM 단위 테스트 (`.\gradlew.bat testDebugUnitTest`)

| 대상 | 내용 |
|---|---|
| FormatCatalog | SPEC 6장 표와 값·라벨·순서·플래그 일치 |
| YtDlpArgsBuilder | 5장 골든 예제 E1–E8 |
| ProgressParser | SPEC FR-5 표의 각 행 + 매칭 없는 줄 |
| UrlExtractor | `제목 https://youtu.be/x`, URL만, URL 2개(첫 번째), URL 없음, `(https://youtu.be/x).` 처럼 끝 문자 제거(11장 규칙) |
| MediaStoreExporter 제외 규칙 | 8장 제외 패턴을 순수 함수(`isExportable(name)`)로 분리해 테스트: `a.mp3`✓, `a.mp3.part`✗, `a.f137.mp4`✗, `a.temp.mp3`✗, `a.webp`✗, `001 - a.m4a`✓ |
| NetscapeCookieWriter | 헤더, 탭 구분 7필드, 값의 `=` 보존, 빈 이름 제외 |
| CookieFileValidator | 데스크톱판 출력 형식 통과, `#HttpOnly_` 줄 통과, youtube 없음 거부, 빈 파일 거부 |
| ErrorHintMapper | 키워드 각각, 대소문자, null |

테스트는 정답을 검증하기 위한 것이다. 테스트를 통과시키려고 구현에 입력별 분기를
하드코딩하지 않는다.

### 기기 검증 (adb, PowerShell 기준)

```powershell
.\gradlew.bat installDebug
adb shell "am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT 'title https://youtu.be/jNQXAC9IVRw' -n io.github.dev1ck.ytloader/.MainActivity"
New-Item -ItemType Directory -Force build | Out-Null                                # pull 대상 폴더
adb shell screencap -p /sdcard/shot.png; adb pull /sdcard/shot.png build/shot.png   # 스크린샷 (Read로 확인)
adb shell uiautomator dump /sdcard/ui.xml; adb pull /sdcard/ui.xml build/ui.xml    # 버튼 좌표 찾기
adb shell input tap <x> <y>
adb shell ls -l /sdcard/Download/YTLoader/
adb logcat -d -s YTLoader
```

- 기본 테스트 영상: "Me at the zoo" `https://www.youtube.com/watch?v=jNQXAC9IVRw` (19초).
- 재생목록 테스트: 항목 2–3개짜리 짧은 공개 재생목록 — 구현자가 찾고 URL을 작업 로그에 기록.
  (M3: 짧은 목록을 못 찾아 yt-dlp 테스트용 `PLBB231211A4F62143`(TF2, 29개×약 10분)으로
  "2번 항목 중 취소"를 검증.)
- debug 빌드 전용 훅 `DebugDownloadReceiver`(src/debug, shell만 호출 가능한 `DUMP` 권한 요구)로
  옵션을 지정해 다운로드를 시작할 수 있다 (앱이 포그라운드일 때):
  `adb shell "am broadcast -n io.github.dev1ck.ytloader/.DebugDownloadReceiver --es url 'URL' --es mode VIDEO --es format mkv --es quality 720 --ez playlist false"`
- PowerShell에서 `adb exec-out screencap > file`은 바이너리가 깨지므로 위 방식(pull)을 쓴다.
