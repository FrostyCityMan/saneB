# BBS 첨부 요청의 리다이렉트 식별자 고정

## 2026-09-21 후속 관측 결과

- 수정 커밋 bbc8639의 [Actions35606668928](https://github.com/FrostyCityMan/saneB/actions/runs/35606668928)은 실패로 종료했다. 태백 TITLE은 조합 충족이지만 BODY는2회 TIMEOUT/0자였고 상세 발견도 TRANSPORT_TIMEOUT이다. 보안 차단 응답은 아니며 파일 다운로드/추출은 시작되지 않았다. JUnit1건 실패, 운영 DB 쓰기0·원본 정리true다.
- 관측 metadata는 새 profileHash `8cf428f1…`를 확인하지만 실제 파일이 없으므로 기존 기대값/관측 시각을 갱신할 근거는 아니다. catalog/assertion을 변경하지 않는다.
- 기존 CI는 외부 관측 실패 후 코드/DB 계약을 생략했다. 후속 workflow는 취소되지 않은 경우 이 계약을 실행해 별도 근거를 확보하되 외부 실패를 숨기지 않는다. 독립 산출물·정책 부모 검증은 계약 성공 뒤에만 실행한다. 이번 변경은 새 실파일 성공이나 배포를 의미하지 않는다.

## 발견·영향 범위

옥천의 기존3표본을 세 단계 관측기에 연결하던 중, 같은 기관의 다른 공고번호로 이동하는 요청이 허용되는 회귀 실패를 확인했다. 원인은 `StandardBbsAttachmentDiscoveryProfile.selectApprovedRequest(initial, request)`가 `NOTICE_BOUND` 형식인 철원에만 최초 요청 비교를 적용하고 나머지8개 BBS 프로필에서는 각 URL의 기관/게시판/경로 유효성만 확인한 것이다.

각 URL이 개별적으로 유효해도 최초 공고·첨부와 같은 대상이라는 뜻은 아니다. 다른 공고 상세나 다른 첨부 파일의 302 응답을 따라가면 원래 공고의 근거로 다른 자료를 연결할 수 있다. 합성 재현이며 실제 운영에서 잘못 연결된 행을 관측한 것은 아니다. 09-21 마지막 운영 조회에서 첨부 데이터/worker 작업은0이고 worker는 비활성이었다.

## 변경 계약

- BBS9개 프로필 모두 최초 요청과 후속 요청의 유효성을 검사하고 경로 및 해석된 전체 query 파라미터의 일치를 요구한다.
- 상세의 `nttNo`, 다운로드의 `atchmnflNo`, 적용되는 `key`/`bbsNo`가 달라지거나 상세와 다운로드 경로가 바뀌면 전송하지 않는다.
- query 순서만 다른 같은 요청은 허용한다. 기관/포트/메서드/중복 파라미터/세션 경로의 기존 검증과 철원 전용 소속 조건은 유지한다.
- 기존 `AttachmentProfileDownloadFlow`가 worker/QA의 최초 요청을 callback에 고정하므로 추가 DB/API 계약은 필요하지 않다. 다른 엔진의 합법적인 다단계 다운로드는 이번 변경 대상이 아니다. 나머지 엔진의 소속 검증은 별도 대조가 필요하다.
- HTML 첨부 영역, 파일 이름, 역할 규칙, 키워드 규칙, V1~V83, 운영 데이터는 바꾸지 않는다. 제목→본문→첨부→최종 관리자 검증을 유지한다.

## 검증·배포 경계

- 기존8개 BBS 프로필에 공고 변경/첨부 변경/요청 종류 변경/null/POST 차단 및 파라미터 순서 변경 허용 회귀를 추가한다. 철원은 기존 전용 회귀를 함께 실행한다.
- 실제 production 다운로드 흐름과 pinned client에 고정302 응답을 주입하여 다른 대상으로의 두 번째 DNS·HTTP, 파일 생성, bytes 예약이 일어나지 않는지 검사한다. 외부 HTTP 요청을 수행하는 시험은 아니다.
- 새 테스트의 초기70건 실행은2실패였다. 하나는 새 옥천 제목의 초기 가정 오류(`지원사업`은 STRONG), 다른 하나는 위 리다이렉트 허용 결함이다. 제목은 DB seed의 실제 근거로 정정하고 운영 규칙은 수정하지 않는다.
- 공통 BBS 구현 지문이 바뀌므로 보관된 태백 기대값1건은 새 코드에서 재관측·내용 대조 전 사용할 수 없다. catalog 지문/관측 시각/assertion을 자동 교체하거나 실패 검사를 생략하지 않는다. 옛7a5ef22의 Linux 통과는 새 수정본의 통과가 아니다.
- 미배포 로컬 수정이다. 기존7a5ef22 재배포 승인 요청 이후 발견한 내용이므로 현재 재배포보다 수정본 검증이 먼저다. 취소된 배포를 임의 재개하지 않으며 새 배포 후보 SHA와 영향도를 다시 제시한다.

전체 ATT62/Gate0~8 완료, 정상 후보 coverage, 정책 게시·ENFORCE·기존 데이터 처리 또는 운영 브라우저 E2E를 이 수정으로 대체하지 않는다.

## 로컬 검증 결과

- 최초70건의2실패 이후 신규 fixture의 빈 POST form 생성 오류8건과 최초 요청 DNS 호출 수를1로 가정한 오류3건을 수정했다. 빈 POST는 Request 생성자 자체가 거부하며, 최초 검증은 DNS를3번 호출하므로 redirect 응답 시점의 횟수와 최종 횟수가 같은지를 검증한다. production 보안 검사를 완화하지 않았다.
- 최종 표적179건/실패·오류·생략0,1분15초 성공이다. 기존8개 BBS와 철원 경계, 옥천 DRAFT/title/catalog source, 혼합 역할 UNKNOWN, 실제 다운로드 흐름+고정302 전송 fixture를 포함한다. 외부 HTTP/운영 DB 쓰기0이다.
- 전송 fixture3건 모두 최초 HTTP1회 이후 다른 공고/첨부/경로로 향하는 응답을 `ATTACHMENT_PATH_NOT_APPROVED`로 거부했다. redirect 이후 DNS 추가0·HTTP 추가0·bytes 예약0·파일 생성0이다.
- 전체 회귀는4분56초 failure다. XML root2744=2478통과/265조건부 생략/구 태백 profile 지문 불일치1실패/오류0이다. 새 코드 지문 `8cf428f1ca718f67960130dc0398aa354679179d3cc2638cd6d47f51ffc17e88`과 보관 기대값 `627d3f60…`의 차이를 유지했다. 검사를 삭제하거나 값을 덮어쓰지 않았다.
- 같은 명령에서 `--continue`로 bootJar·QA 설치·probe 생성 및 패키징20건은 성공했다. 추출기88건은 UP-TO-DATE로 기존 결과 재사용이다. 전체 빌드 성공은 아니다. 웹 JAR SHA256은 `9e10d863d9c83cdc488487c80806d141a76e664296f9b4d7e94a4c558cd15fdc`다.
- 새 코드의 실제 태백1공고/전체2파일을 기존 명시적 `[taebaek-revalidation-observation]` 경로에서 관측한다. 상한44요청/80MiB/420초, 원본 정리·운영 쓰기0이며 기대값 승인은 별도다. 기존 전체 계약 검사는 그대로 실행하고 실패 결과도 보관한다. 옥천 관측 그룹은 연결만 했으며 실제 Linux 실행은 후속이다.
- 검증 종료 후 이번 Gradle/임시 PostgreSQL 프로세스가 없음을 확인했고 단발 Node 링크 검증62개/누락0·제한된 자격증명 패턴 검증6파일/발견0으로 종료했다. 사용자의 기존 Node 프로세스와 output 파일은 종료·삭제하지 않았다.

```powershell
.\gradlew.bat :test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentDocumentRoleClassifierTest' --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*CheorwonAttachmentDiscoveryProfileTest' --tests '*AttachmentPinnedDownloadClientTest' --no-daemon --console=plain --max-workers=1
```
