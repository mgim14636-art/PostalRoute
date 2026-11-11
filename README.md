# PostalRoute

우체국 집하 차량의 동선을 자동으로 계산해주는 웹 서비스예요. 집하지 주소, 파렛 수, 요청 시간을 입력하면 시간 약속을 지키면서 이동 거리를 최대한 줄이는 차량별 방문 순서를 계산해줘요.

## 주요 기능

- **동선 최적화**
  - 요청 시간이 있는 집하지는 그 시간 약속을 지키는 선에서 차량을 배정해요.
  - 같은 시간대에 여러 곳을 가야 하면, 목적지 수가 적을 땐 모든 조합을 다 따져서 이동 거리가 가장 짧은 배정을 찾아요.
  - 시간이 없는 집하지는 기존 경로에 끼워 넣었을 때 거리가 가장 적게 늘어나는 자리를 찾아 넣어요 (도착해서 싣고 내리는 시간까지 고려함).
  - 평파렛/롤파렛 적재 기준표를 기준으로 차량 톤수를 자동 배정하고, 한 차로 못 싣는 양이면 자동으로 여러 대로 나눠요.
  - 엑셀 파일로 집하지 목록을 한 번에 가져올 수 있어요 (등록된 업체명이면 주소도 자동으로 채워줌).
- **이력 관리**: 계산했던 결과를 날짜별로 조회하고, 다시 불러와서 재계산하거나 삭제할 수 있어요.
- **업체 관리**: 자주 쓰는 업체명과 주소를 등록해두면 동선 입력할 때 이름만으로 바로 찾을 수 있어요. 엑셀로 한 번에 등록할 수도 있어요.
- **엑셀 다운로드**: 계산 결과를 실제 종이 배차표와 같은 형식(차량번호/용적/구간별 업체·시간·수량)으로 내려받을 수 있어요.

## 기술 스택

- **Backend**: Spring Boot 4.1.1, Java 17, Gradle
- **DB**: MariaDB
- **Frontend**: 순수 HTML/CSS/JavaScript (별도 프레임워크 없음)
- **외부 API**: 카카오 지도(주소 → 좌표 변환), 카카오 모빌리티(실제 도로 거리/시간 계산)
- **엑셀 처리**: 가져오기는 SheetJS, 내보내기(서식 포함)는 ExcelJS

## 실행 방법

1. MariaDB에 `postal_db` 데이터베이스를 만들어요.
2. `src/main/resources/application.properties.example` 파일을 복사해서 같은 폴더에 `application.properties`로 만들고, 본인의 DB 비밀번호와 카카오 API 키를 입력해요.
   ```
   spring.datasource.url=jdbc:mariadb://localhost:3306/postal_db
   spring.datasource.username=root
   spring.datasource.password=본인_DB_비밀번호
   kakao.api.key=본인_카카오_API_키
   ```
3. 아래 명령어로 실행해요.
   ```
   ./gradlew bootRun
   ```
4. 브라우저에서 `http://localhost:8080/login.html` 로 접속해요.

## 프로젝트 구조

```
src/main/java/com/postalroute/backend/
├── route/      # 동선 최적화 핵심 로직 (시간 기준 묶기, 용량 분할, 거리 계산)
├── vehicle/    # 차량 톤수별 적재 기준표
├── history/    # 이력 저장/조회/삭제
├── company/    # 업체(주소록) 관리
└── user/       # 로그인/회원가입

src/main/resources/static/   # 화면 (로그인, 동선 최적화, 이력 관리, 업체 관리 등)
```
