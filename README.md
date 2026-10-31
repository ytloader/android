# YTLoader (Android)

<img src="docs/design/icon/icon-512.png" width="96" alt="YTLoader 아이콘">

[yt-dlp](https://github.com/yt-dlp/yt-dlp) 기반 개인용 YouTube 다운로더입니다.
Windows 데스크톱판 YTLoader와 같은 옵션으로 오디오·영상을 받습니다.

- YouTube 앱에서 **공유 → YTLoader**로 URL을 넘기거나, 앱에서 붙여넣어 받습니다.
- 다운로드는 백그라운드에서 계속되고 알림으로 진행률을 봅니다.
- 앱 안에서 yt-dlp를 업데이트합니다 (YouTube 변경으로 다운로드가 깨질 때).
- YouTube 로그인(쿠키)으로 비공개·연령 제한·멤버십 영상을 받습니다.

## Google Play에 없는 이유

Google Play 정책상 게시할 수 없습니다: YouTube 서비스 약관에 어긋나는 다운로드 기능,
Play 밖에서 실행 코드를 내려받는 기능(앱 내 yt-dlp 업데이트), 저작물 다운로드 앱 금지.
그래서 GitHub Releases의 APK로만 배포합니다.

## 설치

- 요구 사항: Android 10 이상, arm64 기기 (대부분의 최근 휴대폰).
- Releases에서 `YTLoader-<버전>.apk`를 받아 엽니다. 처음이면 브라우저(또는 파일 앱)에
  "출처를 알 수 없는 앱 설치"를 허용하라는 안내가 나옵니다.
- 앱 크기가 큰 것(약 80MB)은 Python·yt-dlp·ffmpeg가 들어 있기 때문입니다.

## 사용법

1. YouTube 앱에서 영상 → **공유** → **YTLoader**. 또는 입력란의 붙여넣기 아이콘.
   클립보드에 YouTube 링크가 있으면 입력란 아래에 붙여넣기 제안이 뜹니다.
   링크를 넣으면 잠시 뒤 영상 제목이 보입니다.
2. 모드(오디오만 / 영상+오디오), 포맷·음질(해상도) 칩, 썸네일·메타데이터·재생목록 스위치를 고릅니다.
   고른 옵션은 다음에도 그대로 유지됩니다.
3. 하단의 **[다운로드]**. 처음 한 번 알림 권한을 묻습니다 (거부해도 다운로드는 됩니다).
4. 파일은 **`Download/YTLoader/`**에 저장됩니다. 재생목록은 `Download/YTLoader/<재생목록 이름>/`.
   완료되면 하단의 **[열기]**나 완료 알림으로 파일을 엽니다.

다운로드가 실패하면 오른쪽 위 **⚙ 설정 → 다운로드 엔진 → [업데이트]** 후 다시 시도해 보세요.

### 비공개 · 연령 제한 · 멤버십 영상

- **⚙ 설정 → [로그인]**: 앱 안 브라우저에서 Google 계정으로 로그인하면 자동으로 닫히고 "로그인됨"으로 바뀝니다.
- 로그인이 막히면 **[cookies.txt 가져오기]**: PC의 데스크톱판이 만든
  `%LOCALAPPDATA%\YTLoader\cookies.txt`(또는 브라우저 확장으로 내보낸 Netscape 형식 파일)를
  휴대폰으로 옮겨 선택합니다.
- 계정 보호를 위해 **보조 계정** 사용을 권장합니다. 공개 영상만 받을 때는 [로그인 쿠키 사용]을 꺼도 됩니다.
- 쿠키는 앱 전용 영역에만 저장되고 백업되지 않습니다. [로그아웃]하면 모두 지워집니다.

## 빌드

JDK 17 이상(Android Studio 내장 JBR 가능)과 Android SDK(compileSdk 37)가 필요합니다.

```powershell
.\gradlew.bat testDebugUnitTest   # 단위 테스트
.\gradlew.bat assembleDebug       # debug APK (arm64-v8a, x86_64)
.\gradlew.bat assembleRelease     # release APK (arm64-v8a)
```

release 서명은 저장소 루트의 `keystore.properties`(커밋하지 않음)에서 읽습니다.
파일이 없으면 서명하지 않은 APK가 만들어집니다.

```properties
storeFile=release.jks
storePassword=...
keyAlias=ytloader
keyPassword=...
```

설계 문서: [docs/SPEC.md](docs/SPEC.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 라이선스

[GPL-3.0](LICENSE). 이 앱은 GPL-3.0인
[youtubedl-android](https://github.com/yausername/youtubedl-android)를 포함하며, 그 안에
yt-dlp(Unlicense), Python, ffmpeg, QuickJS가 들어 있습니다.
