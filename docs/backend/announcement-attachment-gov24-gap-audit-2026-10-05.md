# 정부24 본문·첨부 경로 미완료 감사

## 기준과 판정

2026-10-05, 저장소 `23f8eae05fd42dae12c0f57a12f548d41601f5f0` 기준이다. 정부24 목록 수집 구현은 존재하지만 공식 상세 본문 보완과 첨부 발견 프로필은 미완료다. API key 설정만으로 해결되는 운영 설정 문제와 혼동하지 않는다. 전체 goal/Gate는 미완료이며 이 문서는 구현 완료 증거가 아니다.

## 현재 확인한 근거

| 항목 | 근거 | 판정 |
| --- | --- | --- |
| 공식 목록 | `Gov24PublicServiceAnnouncementSourceProviderClient`의 `/api/gov24/v3/serviceList`, 조합 검색 전달, 응답 envelope 검사 | 구현 존재; 이번 실제 인증 수집은 미실행 |
| 현재 본문 | 같은 클래스 `selectProviderItem`이 목록의 지원내용/요약을 선택 | 공식 상세 본문 전체 확보와 다름 |
| 공식 상세 API | `src/main/java`, `scripts`의 Java/Node/Python에서 `serviceDetail` 연결 없음 | 추가 구현 필요 |
| 첨부 metadata | 같은 클래스가 첨부를 제거한 raw payload와 빈 `attachments`를 반환 | 목록으로부터 첨부 수집하지 않음 |
| 첨부 발견 | `announcementattachment/discovery`에 정부24 프로필 없음 | 공식 상세·파일 경로 관측 후 구현 필요 |
| 누락 표시 | `AttachmentProviderQaPlan`이 정부24를 전체 대상에 포함하고 `PROFILE_MISSING` 반환 | 미지원 대상을 분모에서 숨기지 않음 |
| DB 코드 | V78이 실제 코드 `GOV24_PUBLIC_SERVICE`를 허용; 입력 별칭 `GOV24` 유지 | 코드 정합성이 첨부 지원을 의미하지 않음 |

검토한 테스트는 `Gov24PublicServiceAnnouncementSourceProviderClientTest`, `AttachmentProviderQaPlanTest`다. 후자의 등록 컨텍스트는 일부 설정만 포함하므로 해당 테스트의 프로필 수를 전체 지역 수로 보고하지 않는다.

## 공식 명세 재확인

- [공공데이터포털](https://www.data.go.kr/data/15113968/openapi.do)
- [공식 OpenAPI JSON](https://infuser.odcloud.kr/api/stages/44436/api-docs?1684891964110)

인증 없는 공개 명세 GET으로 확인했다. 실제 서비스 데이터 API와 파일 다운로드는 호출하지 않았다.

명세는 `serviceList`, `serviceDetail`, `supportConditions`를 정의한다. 목록에는 상세조회URL이 있고, 상세에는 서비스ID·지원내용·구비서류·온라인신청사이트URL 등의 문자열이 있다. 이번에 확인한 목록/상세 schema에는 파일 manifest가 정의되어 있지 않다. 이는 실제 공식 페이지에 첨부가 없다는 증거가 아니다. 문자열 내부 링크 포함 여부, 실제 파일 제공 경로도 아직 확인하지 않았다.

따라서 구비서류 안내를 추출 파일로 저장하거나, 빈 목록으로 `NO_FILES`를 만들거나, 온라인 신청 링크를 다운로드 링크로 추측해서는 안 된다.

## 다음 구현 및 검증 순서

1. 공식 상세 표본 확보: 서비스ID가 같은 목록·상세·공식 페이지를 대조한다. 허용 호스트/경로와 파일 소속을 확정한다. key와 원본 개인정보는 문서·로그에 남기지 않는다.
2. 제목 통과 후 상세 본문 보완: 공식 상세 API 연결 시 기존 호출 상한에 추가 요청을 산입하고, 서비스ID 불일치·빈 응답·오류를 구분한다. 본문 snapshot/hash 및 재시도 계약을 보존한다. 목록 단계에서 모든 상세를 선제 호출하지 않는다.
3. 첨부 프로필: 실제 관측된 공식 영역과 다운로드 규칙으로 구현한다. 메뉴·관련 링크·신청 사이트를 첨부로 추정하지 않는다. 확인된 첨부 없음과 발견 실패를 분리한다.
4. 실패 독립 처리: 가능한 파일은 수집·추출·저장하고 실패 파일은 사유를 별도로 남긴다. 일부 성공을 전체 첨부 검증 완료로 승격하지 않는다.
5. 격리 worker/DB/API 검증 후 정확한 운영 단건 범위를 승인받아 관리자 화면까지 확인한다. COLLECT_ONLY 결과를 판정 적용·DRAFT 생성 성공으로 보고하지 않는다.

성공 기준: 동일 서비스의 제목→본문→발견한 첨부→파일별 추출/오류→DB/API/관리자 화면 근거가 이어지고, 첨부 없음·미지원·부분 실패가 구별된다. 실패 기준: 임의 링크 탐색, 합성 응답을 실수집으로 보고, 전체 대상에서 정부24 제외, 정상 후보 자동 활성화, 운영 설정·정책 무승인 변경.

## 범위와 미실행

실행 명령:

```powershell
.\gradlew.bat :test --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --tests '*AttachmentProviderQaPlanTest' --offline --no-daemon --console=plain --max-workers=1
git diff --check
```

결과: Gradle 45초 성공. XML 기준 목록 제공자40건·QA 계획6건, 총46건 통과/실패0/오류0/생략0. 합성 입력과 등록 계약 검증이며 실제 정부24 수집 증거가 아니다. Node 단회 실행으로 XML 건수를 확인했고 프로세스는 종료했다. 단회 Gradle 실행도 종료했다. 앱 코드 변경이 없으므로 bootJar/전체 회귀/운영 배포는 실행하지 않았다.

이번 작업은 공개 명세·로컬 코드 감사와 오프라인 회귀에 한정한다. 운영 DB/정책/worker 설정, 양평 단건 수집 예약, 배포, 브라우저 조작은 하지 않았다. 양평 SRC-017546 수집 승인 대기는 정부24 구현 누락과 별개다.

## 후속 증분: 공식 상세 응답 검증 모듈

`Gov24ServiceDetailResponse`를 추가했다. 공식 상세의 단건 응답(page=1/perPage=1)을 검사하고 요청 서비스ID와 응답 서비스ID의 일치를 요구한다. 빈 결과, 잘못된 건수/형식, 다른 서비스, 빈 제목을 성공 본문으로 바꾸지 않는다. 구비서류 안내·온라인 신청 링크·지원내용은 별도 필드이며 파일 descriptor를 만들지 않는다. 오류와 `toString`에는 원격 텍스트/URL을 복사하지 않는다.

아직 HTTP 호출과 서비스 흐름에는 연결하지 않았다. 준비 모듈이며 정부24 상세 수집 완료가 아니다. 다음 연결 시 `AnnouncementSourceServiceImpl.selectProviderContent`가 현재 목록 본문 우선/지자체 전용이라는 점을 반영해야 한다. 기존 `ProviderContentRequest`에 가짜 지자체 ID를 넣지 않고, 제목 gate 통과 후 호출·요청 예산·상세 실패 보존·본문 출처 `PROVIDER_FULL_TEXT`·snapshot/hash 경계를 함께 검증해야 한다. 첨부 OFF/COLLECT_ONLY 계약을 이용해 기존 판정을 무조건 바꾸지 않는다. 운영 활성화는 별도다.

검증 명령:

```powershell
.\gradlew.bat :test --tests '*Gov24ServiceDetailResponseTest' --tests '*Gov24PublicServiceAnnouncementSourceProviderClientTest' --offline --no-daemon --console=plain --max-workers=1
```

26초 성공. XML 기준 상세 응답13건·기존 목록40건, 총53건 통과/실패0/오류0/생략0이다. 합성 입력 검증이며 Node와 단회 Gradle은 종료했다. bootJar/전체 테스트/실제 API/브라우저/운영 배포는 이번 증분에서 실행하지 않았다.
