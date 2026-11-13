# 근무표 APK 만들기

준비물(PC): Node.js 18 이상, Java 17, Android Studio

1. 이 폴더에서 터미널을 열고 `./setup.sh` 실행
   (Windows는 Git Bash에서 실행하거나, setup.sh 안의 명령을 순서대로 직접 입력)
2. `npx cap open android` 로 Android Studio 열기 (처음엔 Gradle 동기화가 끝날 때까지 기다리기)
3. 메뉴 Build > Build Bundle(s) / APK(s) > Build APK(s)
4. 완료 알림의 locate를 누르면 app-debug.apk 위치가 열려요
   (보통 android/app/build/outputs/apk/debug/)
5. APK를 폰으로 옮겨 설치 (설정에서 "알 수 없는 앱 설치" 허용 필요)

참고
- 앱 화면은 www/index.html 입니다. 고친 뒤에는 `npx cap sync android` 후 다시 빌드하세요.
- 알람 버튼은 AlarmPlugin.java 로 시계 앱을 직접 엽니다.
- 근무표 데이터는 앱 안(WebView)에 저장되며, 앱을 삭제하면 사라집니다.

## PC 없이 만들기 (GitHub Actions)
1. 이 폴더 전체를 GitHub 저장소에 올려요 (.github 폴더 포함).
2. 저장소의 Actions 탭 > Build APK > Run workflow 를 눌러요.
3. 끝나면 실행 결과 화면 아래 Artifacts의 worktable-apk 를 내려받아 압축을 풀면 app-debug.apk 예요.
