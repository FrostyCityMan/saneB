# 다운로드 미확인 지역 재검증과 부분 성공 보존

## 현재 단계 / Gate

- [x] 기존 프로필의 고정 공고 8개 재검증 및 성공·실패 기록 분리
- [x] 유성 HWP 1개, 합천 HWPX 1개 실제 다운로드·파일 형식·해시 확인
- [~] **196/223 활성 수집원(87.9%) 다운로드 확인, 잔여 27개**
- [ ] 미연결 2개: 울산 남구(079), 천안(148)
- [ ] 연결됐으나 다운로드 미확인 25개
- [ ] 전체 첨부 집합·추출·DB/API/worker·운영 E2E 검증

분모는 2026-09-28T15:56:12.116778+09:00 운영 인벤토리의 활성 수집원이며 고유 지자체 수나 현재 운영 재조회 결과가 아니다. 87.9%는 최소 1개 유효 파일 다운로드를 확인한 수집원 비율이지 전체 프로젝트 진행률이 아니다. 엄격한 전체 집합 Gate는 16/223, 오류가 있는 수집원은 47개로 성공 집계와 중복된다.

## 실제 결과

| 수집원 / 고정 공고 | 본문 | 첨부 결과 | 판정 |
| --- | --- | --- | --- |
| 서대문 / 313956 | 135자, AVAILABLE | 4개 모두 다운로드 시간 초과 | 다운로드 미확인 유지 |
| 유성 / 49380 | 495자, AVAILABLE | HWP 68,096byte 성공, PDF 시간 초과 | **부분 성공 신규 확인** |
| 합천 / 44432 | 4,517자, AVAILABLE·A그룹 검수 | HWPX 87,214byte 성공, 다른 HWPX 시간 초과 | **부분 성공 신규 확인**, 미해결 첨부 링크 경고 유지 |
| 평택 / 95902 | FETCH_FAILED | 상세 TRANSPORT_FAILED | 미완료 |
| 포천 / 64129 | FETCH_FAILED | 상세 TRANSPORT_FAILED | 미완료 |
| 강릉 / 60798 | FETCH_FAILED | 상세 TLS_FAILED | 미완료 |
| 충북 / 67302 | FETCH_FAILED | 상세 TRANSPORT_FAILED | 미완료 |
| 공주 / 59971 | FETCH_FAILED | 상세 TRANSPORT_FAILED | 미완료 |

서대문·유성은 전체 첨부 목록 발견이 확인됐으나 다운로드가 일부 또는 전부 실패했다. 합천은 `ATTACHMENT_LINK_UNRESOLVED` 경고가 있어 전체 발견 완료로 판단하지 않는다. 확보된 파일 2개는 총 155,310byte이며 다른 파일 실패 때문에 성공 증거를 폐기하지 않는다. 모든 표본은 기존 고정 공식 공고이고 최신 공고 유입을 검증한 것은 아니다.

유성 HWP SHA-256: `9cb8c8c5835fe7bbadef21acac3cf7073f82210b5cfa7220b5f28717bf158413`

합천 HWPX SHA-256: `f129885d3ecea8354d46a341ce4c27d074adc7ef514dfc6549a9b45a2e437af1`

파일 원본은 검증 후 제거됐으며 위 해시·형식·크기와 결과 기록을 보존했다. 이번 단계에서 추출기는 실행하지 않았다. 본문 AVAILABLE과 파일 다운로드 성공을 문서 추출·종합 분류·관리자 최종 승인으로 표현하지 않는다.

## 실행 명령과 예산

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=SEODAEMUN,YUSEONG_SUPPORT,HAPCHEON' -PsanebCollectionReportLabel=RECOVERY-20260930-G -PsanebCollectionWindowsTrust=true
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=PYEONGTAEK,POCHEON,GANGNEUNG,CHUNGBUK,GONGJU' -PsanebCollectionReportLabel=RECOVERY-20260930-G -PsanebCollectionWindowsTrust=true
```

- 두 배치를 순차 실행했다. 첫 배치 3개는 부분 관측 결과로 종료0, 두 번째 배치 5개는 상세 조회 미완료로 종료1이었다. 관측 태스크 통과 여부와 개별 다운로드 성공 여부를 구분한다.
- 두 배치 합계 상한은 50요청 예약·187MiB. 보고서의 본문 포함 요청 예약 상한 합계는 32회, 예약 바이트 합계는 20,433,408byte다. 예약량은 실제 전송량이 아니다.
- 관측 시각은 2026-09-30 22:24:03~22:26:12 KST. 모든 8개 보고서의 `originalFilesRemoved=true`, `productionWriteCount=0`을 확인했다.
- Windows 신뢰 저장소를 사용하는 기존 옵션을 적용했으며 TLS 검증 해제·UA 위장·인증 우회는 하지 않았다.
- 응용 코드·프로필·분류 규칙·카탈로그는 변경하지 않았다. 새 실행 라벨로 과거 보고서 덮어쓰기를 방지했다.

보고서는 `build/reports/attachment-regional-collection/{공고코드}-RECOVERY-20260930-G.json`에 보관한다. 공고코드는 `SEODAEMUN-313956`, `YUSEONG-49380`, `HAPCHEON-44432`, `PYEONGTAEK-95902`, `POCHEON-64129`, `GANGNEUNG-60798`, `CHUNGBUK-67302`, `GONGJU-59971`이다. 각 보고서 SHA-256은 영수증 인덱스에 기록했다.

- 동일 producer 클래스 SHA-256: `c07a99fbf6860e24060e8e381065c0622ebef2efc88ba4e0544619bdc7ca4f97`
- 인벤토리 SHA-256: `0192c274451d544440ec998be93999dfd1cda98026433fe67692bebfd6691b60`
- 기존 352기록은 보존하고 **360기록 / 최신 276공고**로 갱신했다. 이번 8개 외 공고와 지역의 근거는 변경하지 않았다.

## 미연결 지역 조사 및 잔여 항목

- [!] 울산 남구: 기존 공식 새올 검색 폼에 검색어를 비운 공개 POST도 20초 시간 초과였다. 이전 검색어 포함 요청 실패와 별도 기록한다. 이번 조사 1요청·2MiB 상한, HTML 원문 파일 저장 없음. 반복 실패이므로 같은 요청을 다시 반복하지 않으며 서버 응답·공식 대체 경로 확인이 필요하다.
- [ ] 천안: 공식 도메인 검색 5개 질의에서 [공식 새올 상세 115419](https://eminwon.cheonan.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&epcCheck=Y&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=115419&not_ancmt_se_code=01%2C04%2C05&subCheck=Y)를 후속 경로로 확인했다. 검색 결과 제목은 `2026년 노후경유차 조기폐차 지원사업(3차)`다. 직접 HTTP·본문·첨부 성공은 미검증이고 제목 조합 충족 여부도 확인해야 한다. 제목 조건을 우회하거나 다운로드 성공으로 집계하지 않는다.
- [ ] 다운로드 미확인 25개: 은평, 서대문, 송파, 부산광역시, 연제, 검단, 성남, 평택, 포천, 이천, 동두천, 강릉, 속초, 평창, 철원, 충북, 충주, 영동, 공주, 서천, 순창, 진도, 의성, 영덕, 성주. 이 중 이번 관측 외 지역의 과거 오류가 현재도 동일하다고 단정하지 않는다.

## 검증과 범위

검증 명령:

```powershell
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Node 테스트23개 전부 통과, 360기록/276공고와196/223·잔여27·오류47·엄격16 재현, `git diff --check` 통과를 확인했다. 이번 회차는 응용 코드 변경이 없어 별도 전체 Java 단위·통합 테스트와 bootJar를 재실행하지 않았다. 과거 회차의 통과를 이번 실행으로 표시하지 않는다.

운영 DB·endpoint·worker·정책·ENFORCE·재분류·배포, Flyway V85, HWP 추출기1.0.16은 변경하지 않았다. AWS·운영 E2E 미실행, 브라우저는 현재 명시 지시가 없어 정책상 미실행이다. 이번 원본 정리 확인은 과거 연제·구례 임시 폴더 정리 차단의 해소를 의미하지 않는다. 작업 브랜치 `[skip deploy]` 기록·푸시 범위이며 전체 goal은 계속 미완료다.
