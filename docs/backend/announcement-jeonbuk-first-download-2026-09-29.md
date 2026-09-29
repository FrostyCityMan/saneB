# 전북권 7지역 연결과 실제 첨부 부분 수집

## 현재 단계 / Gate

사용자 지시대로 전 지역의 첫 첨부 수집을 우선하며, 오류 복구·추가 표본·HWP 추출 개선이 다른 지역 연결을 막지 않는다. 장기 목표의 본문/첨부 분석·worker DB/API·최종 검수·운영 E2E는 여전히 미완료다. HWP 추출기 1.0.16 추가 개선은 보류한다.

- [x] 익산·완주·진안·무주·장수·임실·순창 시스템 프로필7개 추가.
- [x] 6개 지역에서 HWP3개·HWPX3개 실제 다운로드·signature 확인.
- [x] 장수 XLSX 미지원, 순창 상세 HTTP400, 본문 호스트 오류를 별도 보존.
- [x] 로컬 계약 검사100건·bootJar·Node23건·111영수증 재현 검증.
- [!] 실파일 검증은 **7건 중6통과·1실패(순창)**. 전체 성공으로 보고하지 않는다.
- [ ] 나머지 지역 연결 및 전체 장기 목표 Gate.

대상은 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성223지역이다. 운영 대상/정책/DB를 이번에 재조회하거나 수정하지 않았다.

| 지표 | 이전 | 현재 |
|---|---:|---:|
| 실제 다운로드 관측 지역 | 33 | **39** |
| 다운로드 미관측 지역 | 190 | **184** |
| 로컬 프로필 연결 지역 | 41 | **48** |
| 미등록 지역 | 182 | **175** |
| 기존 3표본·전체 파일 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

미관측184 = 미등록175 + 등록 미관측9. 39/223(약17.5%)는 첫 파일 다운로드 관측률이며 전체 목표나 운영 적용률이 아니다. 현재 프로필 근거에서 발견/파일 오류가 남은8지역도 전체 blocker 수를 의미하지 않는다.

## 실제 지역별 결과

| 지역 / 소스 | 고정 공고 | 첨부 결과 | 본문 / 남은 오류 |
|---|---|---|---|
| 익산 / LGS-000166 | 74400 영세소상공인 카드수수료 | HWP1개, 85,504byte | 본문279자 확보·2차 규칙 `TARGET_SUPPORT_CONFIRMED` |
| 완주 / LGS-000170 | 43355 소상공인 카드수수료 | HWP1개, 84,480byte | 본문 호스트 오류 |
| 진안 / LGS-000171 | 33307 카드수수료 수정 공고 | HWPX1개, 81,286byte | 본문 호스트 오류 |
| 무주 / LGS-000172 | 34488 카드형 상품권 결제수수료 | HWPX1개, 70,168byte | 본문 호스트 오류 |
| 장수 / LGS-000173 | 32491 영세소상공인 카드수수료 | HWP1개, 72,192byte / XLSX1개 미지원 | 본문 호스트 오류, 전체 첨부 처리 미완료 |
| 임실 / LGS-000174 | 33179 특례보증·이차보전 | HWPX1개, 68,531byte | 본문 호스트 오류 |
| 순창 / LGS-000175 | 31739 소상공인 카드수수료 | 첨부 발견용 상세 요청 HTTP400, 파일0 | 본문464자 확보·2차 규칙 확인; 첨부 없음으로 판정하지 않음 |

파일6개 총 **462,161byte**. 장수는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`, 순창은 `INCOMPLETE/DETAIL_DISCOVERY/ATTACHMENT_HTTP_400`이다. 나머지5공고는 수집 단계만 완료했으며 추출·정책 승인·운영 DB/API 성공이 아니다.

순창은 사전 curl 상세 조회와 본문 클라이언트에서 정상 응답을 받았지만, 별도 첨부 발견 클라이언트의 요청은 HTTP400이었다. 요청 경로별 차이를 기록하고 반복 호출·보안 검사 우회 없이 후속 오류로 남겼다. 본문 확보를 첨부 발견 성공으로 대신하지 않는다. 완주 메뉴의 공개 리디렉션은 별도 확인했으나 운영 source URL은 변경하지 않았다.

## 코드와 계약

`JeonbukAttachmentProfileConfiguration`은 기관·목록 parser·공식 호스트를 고정한다. 익산·진안·임실·순창은 기존 새올 GET 구현, 장수는 기존 form/98% 구현을 재사용한다. `FileboxSaeolAttachmentDiscoveryProfile`은 완주·무주의 공식 `table.tstyle` 제목과 `div.filebox`를 확인하고 `goDownLoad`의 세 문자열만 파싱한다. JavaScript 실행은 없으며, 공식 첨부 영역 전체와 미해석 링크를 보존한다. 임의 추가 스크립트·구조 변화·잘못된 소스는 실패로 기록한다.

기존 프로필 구현/hash·migration·API·자동 활성화 정책은 변경하지 않았다. QA catalog의 신규7개 expectation은 null이며 **49프로필(지역48+기업마당1), 103참조/지역45개, 정상 승인0**이다. XLSX 지원을 추가하지 않았다.

## 요청량·원본 정리

- 조사 **28회(GET20·POST7·HEAD1)**, 임시 HTML27개/859,098byte. 요청당15초/1MiB/자동 redirect0, HTTPS 검증 유지.
- 수집 관측 **27요청 예약 / 15,199,508byte 예약**. 실제 본문2·상세7·파일6 =15회 전송. 공고별 상한6요청/23MiB.
- 합계 실제43회 / 상한 예약55회. 지역 누적 예약: 익산7/30·완주10/30·진안8/30·무주8/30·장수8/30·임실8/30·순창6/30.
- 관측 harness가 상세/파일 원본을 삭제했고 모든 결과의 `originalFilesRemoved=true`를 확인했다. 조사 HTML27개도 소유 경로 확인 후 삭제했다. 복구용 원문은 남기지 않고 비식별 결과·hash·용량만 보존한다.
- 운영 DB·정책·설정·설치 변경0. 원격 서버 QA·브라우저 실행0. 사용한 단발 Node와 Gradle은 종료했다.

## 검증 명령과 결과

```powershell
.\gradlew.bat :test --tests '*JeonbukFirstDownloadContractTest' --tests '*ChungcheongFirstDownloadContractTest' --tests '*AttachmentCollectionOnlySummaryTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=IKSAN,WANJU,JINAN,MUJU,JANGSU,IMSIL,SUNCHANG' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*JeonbukFirstDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

첫 검사33/33·bootJar 성공(1분4초). 실파일 관측6통과/1실패로 Gradle 실패(1분7초)를 그대로 기록했다. 마지막 계약 검사100/100·bootJar 성공(1분20초). 마지막 검사에서 inventory export용 환경을 임시 주입해 보관된 읽기 전용 영수증과 로컬 등록을 대조한 뒤 원래 환경을 복원했다. Node23/23, **111영수증/최신88공고 재현 일치**. 기존 실패·조사 이력 metadata도 보존했다.

전체 프로젝트 테스트·Linux worker DB/API·운영 배포는 이번에 재실행하지 않았다. 브라우저는 현재 지역 연결 요청에서 명시되지 않아 사용자 정책상 생략했다. 전체 Goal 완료로 보고하지 않는다.

생산자 class hash `8898f3dcba87d8dee6d9ada9e2828936f6f87118bd45e8a289678430994b1222`와 고정 표본 helper class hash `8c056944e4b0d8fe574076dfcab6ba5cd39c45adbfcec8d671302f2b770c7740`를 기록했다. [근거 index](attachment-collection-receipt-index-2026-09-28.json)의 `jeonbukFirstDownloadRun`에 예산·결과·JUnit hash가 있다.

다음은 미등록175지역의 연결 확대다. 순창 상세 요청 차이·5지역 본문 호스트 문제·장수 미지원 형식·추가 표본 검증은 별도 후속 항목으로 유지한다.
