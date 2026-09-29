# 경주·경산·의성 첨부 연결과 수집 오류 분리

## 현재 단계 / Gate

성공 파일 보존·개별 오류 분리 방식으로 전 지역 첨부 연결을 확대한다. 3표본 확보나 모든 실패 해결은 다음 지역 착수의 선행조건이 아니다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지와 HWP 추출기 1.0.16 추가 개선 보류를 유지한다.

- [x] 경주·경산·의성 공식 검색·상세·첨부 구조 확인.
- [x] 공통 처리기1개·시스템 프로필3개·미승인 참조3개 추가.
- [x] 경주3파일·경산1파일 다운로드 및 HWP signature 검증.
- [x] 의성 발견 성공·다운로드 TLS 실패를 별도 기록.
- [x] 계약164건·Node23건·bootJar 및 관측3공고 실행 완료.
- [x] 141영수증/최신108공고 재현, 최초 실패 기록 보존, 임시 원본 정리.
- [ ] 영주·성주·예천 등 경북 잔여 지역 연결.
- [ ] 목록부터 상시 worker·본문/첨부 분석·DB/API·운영 E2E 전체 Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번 실행은 새 운영 조회나 운영 DB·정책·설정 변경을 포함하지 않는다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 다운로드 관측 지역 | 54 | **56/223 (약25.1%)** |
| 다운로드 미관측 지역 | 169 | **167** |
| 로컬 프로필 등록 지역 | 65 | **68** |
| 미등록 지역 | 158 | **155** |
| 등록 후 다운로드 미관측 | 11 | **12** |
| 등록 지역 중 파일·발견 오류 근거 있음 | 10 | **11** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약25.1%는 다운로드 관측률이며 전체 개발·운영 완료율이 아니다. 지역68+기업마당1=69프로필, 카탈로그123참조/지역65개다. 기존 expectation1개를 보존하고 신규 기대값은 승인하지 않았다. 의성은 연결만 등록됐고 다운로드 성공 지역에 포함하지 않는다.

## 실제 결과와 실패 원인

| 지역 / 공고 | 본문 확보 | 첨부 발견 | 최종 다운로드 |
|---|---:|---:|---|
| 경주 LGS-000202 / 240954 | 767자 | 3개 | HWP3개, 88,064 + 448,512 + 88,064byte |
| 경산 LGS-000210 / 250988 | 3,735자 | 1개 | HWP1개, 67,584byte |
| 의성 LGS-000211 / 39093 | 481자 | 1개 | FILE_DOWNLOAD / TLS_FAILED, 성공0개 |

검증된 파일은 총4개 **692,224byte**다. 3공고 모두 제목 조합 통과·상세 식별·첨부 발견 완료다. 경주·경산 상태는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 의성은 `COLLECTION_ONLY_PARTIAL_NOT_APPROVED`다. 본문 중간 결과는 `ACCEPTED/TARGET_SUPPORT_CONFIRMED`지만 최종 승인·활성화가 아니다. 본문 정제 품질·첨부 텍스트 추출·구간 분석은 검증하지 않았다. 경산은 본문 영역에 동적 script가 있으므로 확보 길이만으로 원문 전체를 확보했다고 단정하지 않는다. 경주의 과거 공고는 파일 검증용이지 현재 모집 중인 공고가 아니다.

최초 실행에서는 경주·경산이 파일 전송 후 `FILE_SIGNATURE/TRANSPORT_FAILED`로 기록됐다. 제한된 추가 진단에서 두 기관 모두 HWP signature·`application/haansofthwp`와 UTF-8 한글 Content-Disposition을 확인했다. 기존 엄격 UTF-8 헤더 복원 기능을 두 기관에만 적용한 후 동일 binary hash의4파일이 통과했다. MIME·signature 검사를 완화하거나 추출기를 수정하지 않았다.

의성 최초 실패는 `ATTACHMENT_HOST_NOT_APPROVED`였다. 대표 누리집 다운로드 응답이 같은 기관 `eminwon.uiseong.go.kr`의 고정 `/emwp/jsp/ofr/FileDown.jsp`로 이동함을 확인했다. 검증된 공식 다운로드에서만 해당 호스트·경로·공개 파일3query를 허용하도록 연결했다. 재실행은 **TLS_FAILED**로 종료됐다. TLS 세부 원인은 아직 확인하지 않았으며 인증서 검증 해제·HTTP 강등·무한 재시도를 하지 않는다. 오류로 유지하고 다른 지역을 진행한다.

최초 실패3영수증과 재실행3영수증을 모두 보존하고 최신3공고만 집계한다. 실패를 지우거나 중복 실행을 추가 지역으로 세지 않는다. QA 오류 분류기도 고정 signature·형식·MIME·Content-Disposition 오류 코드를 분리하도록 보완했으며 외부 예외 원문은 여전히 출력하지 않는다. 이 변경은 QA 보고용이고 운영 오류 계약 변경이 아니다.

## 구현 경계와 미확인 항목

`GyeongbukBoardAttachmentDiscoveryProfile`은 경주·경산의 `#viewBoardContent` 제목·첨부 영역, 의성의 `div.boardView` 제목·첨부 영역만 읽는다. 경주·경산의 공식 utility script는 규칙 확인용으로 읽었을 뿐 실행하지 않았고 `openDownloadFiles`의 숫자 인자를 고정 경로에 매핑한다. 의성은 같은 공고 ID가 포함된 직접 링크를 사용한다.

- 미리보기는 직전의 인식된 다운로드와 공고·파일 ID가 일치할 때만 보조 링크로 구분한다. 뷰어를 수집 성공으로 세지 않는다.
- 공식 목록의 검색·페이지 query와 말단 `&`를 수용하되 기관·메뉴·공고 ID·행동 코드는 고정한다. 중복 query·임의 항목·다른 호스트·script 주입은 거부한다.
- 오류 링크·미지원 형식은 정상 파일을 버리지 않는다. 역할은 UNKNOWN으로 유지하며 파일명으로 최종 분류를 승인하지 않는다.
- 기존 프로필 실행 코드·hash·migration·DB/API·화면·정책·운영 worker는 변경하지 않았다.
- 경주 저장소 원본 목록은 mnu_uid2912이며 현 공식 검색 폼·검색 결과는423을 사용한다. 이번에는 실제423 상세를 검증했다. 운영 목록 parser의 현재 필드·자동 유입·스케줄러부터의 E2E는 재검증하지 않았다.

## 요청 예산과 정리

사전 검색·상세·공식 JS 조사8회(GET6·POST2), 추가 파일/redirect 진단3회(GET3), 총11회다. 관측 예약은 최초14회+재실행15회=29회다. 조사와 관측 예약 합계40회이며 관측 예약에는 본문 상한이 포함되어 실제 HTTP 횟수와 동일하다고 표현하지 않는다.

| 지역 | 이번 조사·진단 | 최초+재관측 예약 | 이번 합계 | 직전 목록 조사1회 포함 누적 |
|---|---:|---:|---:|---:|
| 경주 | 4 | 6+6 | 16 | 17/30 |
| 경산 | 4 | 4+4 | 12 | 13/30 |
| 의성 | 3 | 4+5 | 12 | 13/30 |

각 조사15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 각 관측 최대6요청·23MiB, 두 실행의 예약 byte 합계15,114,240이다. 조사·진단 임시파일13개(HTML6·JS2·헤더3·binary2), 총1,335,977byte와 관측 임시 원본을 삭제했다. 복구 사본은 없고 hash·비식별 metadata만 보존한다. 단발 Node·Gradle은 종료하며 기존 사용자 프로세스·미추적 파일은 유지한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*GyeongbukSecondDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --no-daemon
.\gradlew.bat :test --tests '*GyeongbukSecondDownloadContractTest' --tests '*GyeongbukFirstDownloadContractTest' --tests '*JeonbukSecondDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GYEONGJU,GYEONGSAN,UISEONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

초기 단위37건 통과(47초), 최초 확장140건·bootJar·관측 실행 완료(1분22초, 당시 파일 성공0). 보완 후 최종164건·bootJar·관측 실행 완료(1분32초, 파일4성공/1실패). 테스트 실패·오류·skip0, Node23/23. 관측 harness3통과를 실제3지역 파일 성공으로 표현하지 않는다. 141영수증/108공고 재현 성공, 이전135영수증과 기존 실행 metadata 보존.

전체 테스트·Linux worker 임시 DB/API·AWS 운영 조회·운영 배포·브라우저 QA는 실행하지 않았다. 브라우저는 현재 지역 연결 작업에서 명시적 지시가 없어 정책상 생략했다. `[skip deploy]` 변경이며 전체 Goal은 계속 진행 중이다.
