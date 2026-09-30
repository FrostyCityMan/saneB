# 태백·제천 수집 확인과 충주 파일 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드를 먼저 연결한다. 성공 파일과 실패 파일을 분리하며 제목→본문→첨부→관리자 최종 검증, 상시 worker·DB/API·운영 E2E 전체 목표는 유지한다.

- [x] 등록되어 있으나 현재 근거 대장에 표본이 없던 태백·제천·충주를 기존 고정 참조로 선택.
- [x] 제목 정책과 기존 음성 표본, 일반 추출 QA 예산을 보존한 수집 전용 배치 추가.
- [x] 실제 Java 경로에서 태백·제천 HWPX3개 다운로드 확인, 충주 HTTP400 별도 기록.
- [x] Node23건 및 291영수증/232최신 공고 재현, 임시 원본 정리.
- [x] 확장 Java173건 실패·생략0, 실제 probe JAR 패키징·bootJar 검증.
- [ ] 첨부 추출·운영 반영·운영 E2E·전체 Goal 완료.

## 변경 범위

`ExistingProfileDownloadCases`가 기존 TAEBAEK/JECHEON/CHUNGJU 그룹에서 정확히 세 참조만 선택한다. `EXISTING_FIRST` 수집 전용 실행은 각 공고 최대6요청·23MiB로 제한한다. 일반 추출 실행의44요청·80MiB 예산과 기존 부정 표본을 변경하지 않는다. 신규 처리기·카탈로그·Flyway·API·운영 설정 변경은 없다.

## 실제 관측

2026-09-30 10:41 KST, 각 표본1회 실행 결과다.

| 수집원 / 공고 | 본문 | 첨부 결과 | 판정 |
|---|---|---|---|
| 태백 LGS-000121 /184816 |431자, AVAILABLE/ACCEPTED|HWPX2개 82,695+67,020byte|수집 확인|
| 제천 LGS-000138 /403530 |526자, AVAILABLE/ACCEPTED|HWPX1개 130,752byte|수집 확인|
| 충주 LGS-000137 /70852 |523자, AVAILABLE/ACCEPTED|HWP 링크1개 발견 후 ATTACHMENT_HTTP_400|파일 오류, 다운로드 미확인|

세 공고 모두 상세 식별과 발견 FOUND/complete=true를 확인했다. 태백·제천은 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 충주는 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 충주 실패를 첨부 없음·정상 후보·추출 성공으로 변경하지 않는다. 다운로드3개 합계280,467byte다.

파일 SHA-256:

- 태백82,695byte: `424bde05e7a87baaab4a7261bb2266e2ebde4d9d733c9986596507e79f14220a`
- 태백67,020byte: `67dfc0af4cf21d0e9363132cc663154086aa5fc18625f443f43021704703247a`
- 제천130,752byte: `9e4d05dd0dcfa7b460b4c82f613c6166defeb5fe487b536ad5d3f1c5a804b5fc`

**156→158/223수집원(70.9%) 다운로드 확인, 잔여67→65(미등록38+등록 미확인27)**다. 오류를 가진 수집원은38→39개이며 성공 수집원과 겹칠 수 있다. 기존3표본·전체 첨부 Gate는16충족/207잔여로 유지한다. 분모는2026-09-28 활성 지역 수집원 스냅샷이며 고유 행정구역 수나 운영 완료율이 아니다.

원주491704 상세는 별도1GET/200으로 제목과 HWPX 링크4개만 관측했다. 실제 파일 요청·제목 정책 통과 검증은 하지 않았으므로 성공 집계에 포함하지 않는다. 후속은 실제 제목 정책을 확인한 뒤 기존 원주 처리기로 연결한다. 충주는 현재 파일 서버400 원인을 별도 점검하며 다른 수집원을 막지 않는다.

## 검증과 자원

최초 관련 Java62건 실패·생략0 및 bootJar·실제 관측 태스크 성공을 확인했다. 배치의 부정 표본 검사에는 정확한3개 목록 검증을 추가하여 표본 누락 시 공허하게 통과하지 않도록 했다. 최종 확장 Java173건 실패·생략0, 실제 probe JAR 패키징·bootJar 통과(2분19초)를 확인했다. Node23건도 실패·생략0이다. 단발 Node와 이번 Gradle/Java 작업은 종료하고 기존 사용자 프로세스는 유지했다.

세 관측의 본문 포함 요청 예약13회·7,169,939byte, 원주 조사1회와 합산 요청 상한14회다. 원주 조사는 요청15초/연결7초/최대2MiB/TLS 검증/자동 redirect0이었다. 조사 원본1개279,234byte는 경로·크기·SHA-256 확인 후 삭제했다. 복구에는 재수집이 필요하다. 관측 원본도 정리되었으며 비식별 보고서·해시는 보존한다. 기존288영수증·229공고와 다른 모든 지역 근거는 변경 없이 보존했다.

```powershell
.\gradlew.bat :test --tests '*ExistingProfileDownloadContractTest' --tests '*AnnouncementAttachmentBbs*ContractTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' --no-daemon
.\gradlew.bat :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=EXISTING_FIRST' -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :test --tests '*ExistingProfileDownloadContractTest' --tests '*AnnouncementAttachmentBbs*ContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

운영 DB·정책·ENFORCE·기존 데이터·배포는 변경하지 않았다. 현재 지역 연결 단계에서 브라우저 실행 지시가 없어 사용자 정책에 따라 생략한다. 수집 확인을 첨부 텍스트 추출·정책 승인·운영 E2E 통과로 표현하지 않는다.
