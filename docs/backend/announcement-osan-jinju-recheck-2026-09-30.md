# 오산·진주 첨부 수집 재검증과 실행 기록 보존

## 현재 단계 / Gate

전 지역의 첨부 발견·다운로드 연결을 우선한다. 정상 파일은 유지하고 본문·발견·다운로드·형식·추출 오류는 분리한다. 제목→본문→첨부→관리자 최종 검증과 자동 활성화 금지는 유지하며 HWP 추출기1.0.16 추가 개선은 보류한다.

- [x] 오산 HWP2개280,576byte 다운로드·형식 검사·임시 원본 정리.
- [x] 진주 본문 영역 교정, 공식 프록시 파일명 공백·구형 MIME·이중 인코딩 헤더 호환, HWPX1개132,063byte 다운로드.
- [x] 공유 클래스 변경의 영향을 받는 고령 HWP3개1,722,880byte 재확인.
- [x] 이전337개 기록 보존, 새10개 추가,347기록/273최신 공고 재현. 반복 실행으로 공고 수를 부풀리지 않음.
- [x] 선택 회귀382개, 마지막 수정 후 선택28개·bootJar·실파일 관측2건, Node23개 통과.
- [~] 다운로드 확인192/223수집원(86.1%),31잔여(미연결5+등록 미확인26).
- [!] 서대문 파일4개 시간 초과, 진주 미리보기 발견 경고, 일부 공개 상세 연결 실패, AWS TLS 검증 실패.
- [ ] 나머지 지역, 상시 목록 유입·worker·추출·임시 DB/API·운영 E2E 후속. 전체 goal은 미완료.

분모는2026-09-28 15:56:12 KST inventory의 활성 지역 수집원223개다. 고유 행정구역 수나 새 운영 조회가 아니다. 코드 등록 프로필219개(지역218+기업마당1), 카탈로그282공고/218대상은 그대로다. 전체 집합 엄격 Gate16/223·207잔여와 오류 보유 지역45개는 다운로드 가능192개와 별도 지표다. 오류 지역에는 진주 등 다운로드 성공 지역도 포함된다.

## 구현과 계약 경계

1. `LocalGovernmentNoticeProviderContentClient`: 진주의 정확한 공식 호스트·상세 경로에서 `form#saeolGosiVO > div.bbs1view1 > div.substance`만 본문으로 선택한다. 중복·제목 누락·선택자 변경은 `BODY_SELECTOR_CHANGED`, 빈 본문은 `BODY_TEXT_EMPTY`로 처리하며 메뉴·푸터로 대체하지 않는다.
2. `GoryeongJinjuAttachmentDiscoveryProfile`: 외부 프록시 query를 해제할 때 나타나는 내부 파일명 공백만 URI query의 `%20`으로 복원한다. 외부 HTTPS 주소는 원문 그대로 사용하며 내부 HTTP 주소를 직접 호출하지 않는다. 공식 호스트·경로·3개 파일 parameter·동일 filename·동일 요청·SSRF 제한은 유지한다.
3. 진주에만 기존 `application/x-msdownload` 호환 및 엄격한 최대2회 UTF-8 헤더 복원을 적용한다. 서명·명시적 예상 형식·attachment filename 검사, HTML/실행파일·경로 이동·제어문자 차단을 유지한다. 전역 MIME 허용 목록과 고령의 호환 설정은 변경하지 않았다.
4. 수집 전용 QA에 `-PsanebCollectionReportLabel=RECHECK-20260930-E` 같은 실행 식별자를 추가했다. `caseCode`는 변하지 않는다. 중복 경로는 HTTP 실행 전에 거부하고 `CREATE_NEW`로 경합에 의한 덮어쓰기도 차단한다. 기존 보고서가 있으면 새 실행 식별자가 필요하다.

DB/API/Flyway V85, A/B 정책, 관리자 검증, HWP 추출기는 변경하지 않았다. 프로필 클래스 지문이 고령·진주 모두에서 바뀌므로 두 곳의 실제 파일을 현재 지문으로 다시 검증했다. inventory는 보관된 비식별 운영 영수증과 현재 로컬 코드만 대조해 재생성했으며 운영 DB를 조회하지 않았다.

## 실제 관측 결과

| 대상 | 최종 관측(KST) | 본문 | 파일 결과 | 별도 오류 |
|---|---|---|---|---|
| 오산50603 | 09-30 20:58:31 |125자, TARGET_SUPPORT_CONFIRMED|HWP2개280,576byte|이번 표본 오류 없음|
| 진주64420 | 09-30 21:16:37 |146자, TARGET_SUPPORT_CONFIRMED|HWPX1개132,063byte|ATTACHMENT_LINK_UNRESOLVED 유지|
| 고령42015 | 09-30 21:16:25 |4,935자, BODY_GROUP_B_MATCHED|HWP3개1,722,880byte|본문은 REVIEW_REQUIRED, 관리자 확정 아님|
| 서대문313956 | 09-30 20:57:39 |135자, TARGET_SUPPORT_CONFIRMED|4개 발견, 다운로드0|4개 모두 FILE_DOWNLOAD/TRANSPORT_TIMEOUT|

진주는 공식 첨부 영역의 `loading_convert` 미리보기 스크립트를 실행하거나 전체 첨부 확인으로 간주하지 않는다. 별도로 검증한 실제 다운로드 링크1개는 정상 수집한다. 따라서 `discoveryComplete=false`, `collectionStageComplete=false`, `downloadedFileCount=1`이 동시에 성립한다. 성공 파일을 버리지 않되 전체 집합·텍스트 추출 성공으로 승격하지 않는다.

진주 이력은 모두 별도 보관했다.

- A: 본문12,439자에 메뉴가 섞여 BODY_GROUP_B_MATCHED, 이후 상세 발견 시간 초과. 유효한 정제 본문 근거로 사용하지 않는다.
- B: 본문146자로 교정, 내부 파일명 공백으로 링크 해석 실패.
- C: 링크 해석 후132,063byte 전송, 구형 MIME 때문에 ATTACHMENT_CONTENT_TYPE_MISMATCH.
- D: MIME 호환 후 이중 인코딩 파일명 때문에 ATTACHMENT_DISPOSITION_INVALID.
- E: 기존 엄격한 헤더 복원 적용 후 동일 SHA-256 파일 검증 성공. 미리보기 발견 경고는 남긴다.

오산 파일 SHA-256은 `f2bd871661ec3133b06fc4c4c0dbaf8fbdf2e0e34beb6434b4c7c84d3dc2461f`(64,000byte), `44621d4f868c69e053df2fe8dbbccb1f2147545aa26481f523296cd59e6b7029`(216,576byte)다. 진주 파일은 `414f9ee23dd32e7c81e99b6391469cbf622721fd6d731003ba0a0443ab9ddd0b`다. HWPX의 ZIP 내부 구조·텍스트 추출은 이번 수집 전용 검증 범위가 아니다.

보고서는 `build/reports/attachment-regional-collection/*-RECHECK-20260930-{A,B,C,D,E}.json` 중 대장에 명시한10개다. 기존 원본 보고서의 해시·상태·샘플은 변경하지 않았다. 현재 지문은 진주 `3431c5b41d324215f765e298ad6acbb5ea2f3640bdd88684941f9c67b3ece887`, 고령 `514d25ee8b01c639704716be6c073060a7e7ef2e4423dabbb27decb79a00b659`다. 오산·서대문 지문은 불변이다.

## 남은 지역 / 다음 실행

미연결5개: 강동(026), 울산 남구(079), 괴산(144), 천안(148), 서천(158). 접두어는 모두 LGS-000이다.

등록 후 다운로드 미확인26개: 은평·서대문·송파·부산시·연제·검단·유성·성남·평택·포천·이천·동두천·강릉·속초·평창·철원·충북도·충주·영동·공주·순창·진도·의성·영덕·성주·합천. 개별 오류는 관측 시점 기준이며 전부 현재 장애로 단정하지 않는다.

이번 공개 상세 점검8곳 중 서대문·오산·진주는200, 강릉은12초 시간 초과, 평택·포천·충북도·공주는 연결 오류 체인이 반환됐다. 원인 없는 동일 요청 반복은 중단하고 공식 주소·응답·파일 계약을 근거로 다음 지역을 진행한다. 다운로드/파싱 실패는 별도 오류로 유지한다.

## 실행 명령 / 검증 결과

- 첫 선택 테스트68개 중66통과·조건부2생략. 생략은 과거 HTML fixture 환경이 필요한 오산·서대문 검사다.
- A 실행: 선택 테스트280개 중278통과·2생략, bootJar UP-TO-DATE. 외부3표본 중 진주 상세 시간 초과로 전체 명령은 실패했다. 서대문 태스크 정상 종료를 파일 수집 성공으로 세지 않는다.
- B: 선택216개·bootJar·진주 부분 관측 통과,1분6초.
- C: 선택163개·bootJar·고령/진주 부분 관측 통과,1분2초.
- D: 선택228개·bootJar·고령/진주 부분 관측 통과,1분6초.
- 확장 회귀: inventory·GyeongnamFirst 계약·본문 client·보고서 archive·probe·파일 형식·worker·intake·catalog 선택382개 통과,3분10초. 마지막 UTF-8 프로필 설정 전 결과이며 전체 프로젝트 suite가 아니다.
- E: `gradlew.bat --no-daemon :test --tests '*AttachmentProviderInventoryAuditTest' --tests '*GyeongnamFirstDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*RegionalCollectionReportArchiveTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GORYEONG,JINJU -PsanebCollectionReportLabel=RECHECK-20260930-E -PsanebCollectionWindowsTrust=true`: 최종 선택28개·bootJar·실파일2표본 통과,1분9초.
- inventory 검사에만 SANEB_ATTACHMENT_TARGET_INVENTORY_AUDIT=true와 보관된 `build/qa-results/target-inventory-20260928-receipt.txt`를 프로세스 범위로 전달하고 이전 환경을 복원했다.
- `node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs`:23개 통과.
- `node scripts/qa/verify-collection-receipt-index.mjs`:347기록/273최신 공고 재현. `node scripts/qa/report-collection-availability.mjs`:192확인/31잔여·오류45 재현.
- `git diff --check`: 통과. 대장 patch 초안에서 JSON 항목 끝 쉼표 누락으로 적용 검증이 거부되어 파일은 그대로였고, 쉼표를 보존한 patch로 재생성 후 재현 검사를 통과했다.

추가 공개 진단14GET은 각12초·2MiB·자동 redirect 금지·TLS 검증·동일 수집기 User-Agent 조건이다. 실제 관측10보고서의 최대 요청 예약 합61회, 기록된 사용 예약48회이며 진단14회와 합산하면 사용 예약 상한62회/전체 예산75회다. 반복 관측을 포함한 성공 파일 관측12회5,581,279byte를 고유12파일로 표현하지 않는다. 이번 고유 파일은6개2,135,519byte다. 원문은 임시 처리 후 정리했고 진단 응답은 파일로 저장하지 않았다.

## 미실행 / 운영 경계

장기 goal 운영 절차에 따라 로컬 코드·실파일·운영 완료 근거를 구분했다. AWS STS 읽기 전용 조회1회는 TLS_VALIDATION_FAILED로 끝났다. 인증 토큰 유효성은 판단할 수 없으며 인증 만료로 단정하지 않는다. 인증서 검증 해제·로그인·SSM·운영 DB/설정/worker/정책/ENFORCE/기존 데이터/배포 변경은 하지 않았다.

브라우저는 현재 요청에 명시 지시가 없어 정책상 미실행이다. Linux 격리 추출·임시 DB/API·전체 프로젝트 테스트도 이번에는 실행하지 않았다. 새 관측 임시 파일 정리=true는 이전 연제/구례 조사 원본 정리 미완료까지 해소했다는 뜻이 아니다. 기존 사용자 output/·scripts/qa/__pycache__/는 보존한다. 일회성 Node와 단일 사용 Gradle 프로세스는 종료한다.
