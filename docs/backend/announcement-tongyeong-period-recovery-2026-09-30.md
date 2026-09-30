# 통영 게재기간 확인 절차와 첨부 다운로드 복구

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 연결을 우선하며 정상 파일 보존·개별 실패 기록을 유지한다. 제목→본문→첨부→관리자 최종 검증과 상시 worker·DB/API·운영 E2E 전체 목표는 별도다.

- [x] 통영 파일 형식 오류의 실제 원인이 중간 HTML임을 확인.
- [x] 공식 기간 조회와 최종 POST 진단에서 HWP165,888byte 수신.
- [x] 통영 전용 다운로드 flow·본문 영역 구현, 관련 Java144건 통과.
- [x] 확장 Java375건·worker 패키징·bootJar·실제 Java HWP 수집 경로 검증.
- [x] Node23건·288영수증/229최신 공고 재현, 이전 실패 보존·임시 원본 정리.
- [ ] 첨부 텍스트 추출·운영 반영·운영 E2E·전체 Goal 완료.

## 원인과 구현

통영 LGS-000226 / LOCAL_TONGYEONG_SCMS_V1의 공고49251은 파일 링크가 `FileDownNewPbs.jsp`로 연결된다. 기존 코드는 그 응답2,596byte를 파일로 검증했으나 실제로는 게재기간 조회 폼과 script가 있는 HTML이었다. 이를 단순 HWP 형식 예외로 허용하지 않는다.

공식 절차는 중간 페이지 GET → `OfrAction.do` 기간 조회 POST → `FDSendNewPbs.jsp` 파일 POST다. 통영 전용 `TongyeongPeriodAttachmentDiscoveryProfile`이 기존 SCMS 상세·첨부 발견을 위임하고 이 세 단계를 연결한다. JS 실행 없이 고정 폼·필드·응답만 해석한다. `isHome`은 빈 값을 유지하고 기간 조회를 생략하지 않는다.

실제 기간 응답은 `EmptyYMD`였다. 페이지의 문자열 비교 절차에서 최종 제출되는 값을 확인하고, 통영에 한해 이 정확한 응답을 최종 POST에도 그대로 유지한다. 임의 문자열·오류 HTML·종료된 날짜는 차단한다. 초기 폼의 기간 선입력, 최종 POST부터 실행, 다른 파일 ID·기관으로의 후속 요청도 거부한다. 기존 강북 기간 처리기와 다른 지역 프로필·공통 파일 검증기를 수정하지 않았다.

`LocalGovernmentNoticeProviderContentClient`는 통영의 `form#saeolGosiVO > div.bbs1view1 > div.substance`만 본문으로 사용한다. 기존 메뉴·부서·첨부 이름 혼입을 제거한다. 본문 영역·제목이 중복되거나 사라지면 오류로 남기며 전체 HTML로 대체하지 않는다.

기존 미해석 미리보기 등 첨부 영역 오류는 정상 파일 다운로드를 폐기하지 않는다. 기술 실패를 정상 후보·정책 승인으로 바꾸지 않으며 외부 공고 자동 활성화도 하지 않는다.

## 송파 조사

송파의 공식 검색 필드는 `searchCnd=SJ`가 제목 검색이다. `all` 검색값은 키워드를 화면에 유지하지만 이번 관측에서 목록이 필터링되지 않았다. 공식 제목 검색으로 청년 지원 공고32305를 확보했으나, 이 상세도 기존33174와 마찬가지로 제목 칸이 비어 있었다. 첨부 링크2개가 보였지만 상세 식별 성공·첨부 없음으로 처리하지 않았고 파일은 다운로드하지 않았다. 송파는 별도 상세 식별 오류로 유지한다.

## 검증 기록

초기 계약143건 중8건이 실패했다. 처음 추가한 요청 결합 검사가 GET과 후속 POST가 동일해야 한다고 검사해 공식 후속 단계를 막았다. 원래 파일 ID에 결합된 고정 기간·파일 POST만 허용하도록 수정했고 관련144건 실패·생략0으로 통과했다. 최종 POST 직접 시작과 다른 파일 전환 차단 검증을 추가했다.

최종 확장 Java375건 실패·오류·생략0, bootJar·실제 관측 성공(3분13초), Node23/23을 확인했다. 2026-09-30 10:29:33 KST 관측에서 본문247자 AVAILABLE/ACCEPTED, HWP165,888byte 다운로드·형식 검증 성공이다. 파일 SHA-256은 `3df1f725f507ad3f63f65c8e36191dd1a3acf2797cdd1c1cc51659b2be6beb69`다. 기존 본문10,958자의 메뉴 혼입을 제거했으며, 기존 BODY_GROUP_B_MATCHED에서 TARGET_SUPPORT_CONFIRMED로 바뀐 것은 규칙 변경이 아니라 본문 경계 정제 결과다. 최종 관리자 승인·첨부 추출 결과가 아니다.

미해석 미리보기 등의 발견 오류는 FAILED/complete=false로 유지하고 정상 파일은 보존하여 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`로 기록했다. 상세 식별은 확인되었다. 따라서 **155→156/223(70.0%) 다운로드 확인, 잔여68→67(미등록38+등록 미확인29)**이다. 오류가 있는 수집원38개, 기존3표본·전체 첨부 Gate16충족/207잔여는 유지한다. 분모는 2026-09-28 활성 지역 수집원 스냅샷이며 고유 행정구역·모든 파일·운영 완료율이 아니다.

기존287영수증과 다른 지역의 최신 근거를 보존하고 288영수증/229최신 공고 재현을 확인했다. 신규 프로필·참조 건수 증가는 없고 통영 실행 지문만 변경했다. 지역185+기업마당1프로필,241공고/184대상 카탈로그는 그대로다.

조사는 통영4·송파4=8요청(GET6·POST2)이다. 요청당15초/연결7초/HTML최대2MiB/진단파일최대10MiB/TLS 검증/자동 redirect0을 유지했다. 실제 Java 관측은 최대6요청·23MiB 안에서 예약6회·2,454,068byte였고 조사 포함 예약14회다. 조사 원본9개2,269,615byte는 경로·크기·SHA-256을 대조한 뒤 삭제했다. 관측 원본도 정리되었으며 복구하려면 재수집해야 한다. 보고서·해시는 보존했다. 원본 정리 후 공식 응답 fixture1건은 기본 테스트에서 조건부 생략된다. 사용한 단발 Node와 Gradle/Java는 종료하고 기존 사용자 프로세스는 유지했다.

```powershell
.\gradlew.bat :test --tests '*TongyeongPeriodAttachmentTransferTest' --tests '*GyeongnamThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --no-daemon
.\gradlew.bat :test --tests '*TongyeongPeriodAttachmentTransferTest' --tests '*LegalBoardAttachmentTransferTest' --tests '*GyeongnamThirdDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AttachmentProviderQaCaseExecutorTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=TONGYEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

공식 응답 fixture는 `SANEB_TONGYEONG_SURVEY_FIXTURE=true`일 때만 실행하고 환경은 종료 후 복원한다. 이전 실패 영수증은 `TONGYEONG-49251-BEFORE-PERIOD.json`으로 해시를 대조하여 보존했다. 운영 DB·정책·ENFORCE·기존 데이터·배포는 변경하지 않으며, 브라우저는 현재 지역 연결 단계에서 명시적 실행 지시가 없어 생략한다.
