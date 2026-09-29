# 장성 첫 첨부 수집과 전남 잔여 지역 조사

## 현재 단계 / Gate

지역별 첫 첨부 수집 확대를 이어간다. 제목 → 본문 → 첨부 → 관리자 최종 검증 정책, 정상 파일 보존·개별 오류 분리, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 계속 보류한다.

- [x] 장성 공식 상세·첨부 표·POST 다운로드 연결 추가.
- [x] 공고29332의 HWPX1개 164,493byte 다운로드·signature 검증.
- [x] 계약129건·Node23건·bootJar·실파일 관측1건 통과.
- [x] 131영수증/최신101공고 재현, 과거 영수증·실행 metadata 보존, 임시 원본 정리.
- [!] 영광 접근 차단·완도/신안400·영암 검색 결과 없음·담양 목록 계약 차이 후속 관리.
- [ ] 나머지 지역 연결 및 전체 Goal 분석·worker DB/API·운영 E2E.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷** 기준이다. 이번에는 운영 DB·설정·정책을 조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 다운로드 관측 지역 | 49 | **50/223 (약22.4%)** |
| 다운로드 미관측 지역 | 174 | **173** |
| 로컬 프로필 등록 지역 | 60 | **61** |
| 미등록 지역 | 163 | **162** |
| 등록 후 다운로드 미관측 | 11 | **11** |
| 등록 지역 중 오류 근거 있음 | 10 | **10** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

약22.4%는 첫 파일 다운로드 관측률이며 전체 개발·운영 적용률이 아니다. 엄격 Gate는 다음 지역 착수를 막지 않는다. 미등록 지역의 조사 오류는 위 오류10개와 별도다. 로컬 등록은 지역61+기업마당1=62프로필, 카탈로그는116참조/지역58개다. 기존 expectation1개를 유지하고 새 기대값 승인은 추가하지 않았다.

## 실파일과 사전 조사

장성(LGS-000196) `2026년 하반기 장성군 소상공인 지원사업 공고`(29332): 첨부1개 발견·다운로드·HWPX signature 검증, 본문5,340자 확보. 본문 중간 판정은 `BODY_GROUP_A_MATCHED`, 최종 수집 상태는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`다. 본문 정제 품질·첨부 추출·구간 분석·정책 승인·운영 저장을 검증한 것은 아니다.

| 조사 지역 | 요청 수 | 확인 결과 / 다음 조치 |
|---|---:|---|
| 장성 LGS-000196 | 조사3+관측 예약4=7/30 | 공식 목록·제목 검색·상세를 근거로 연결, 파일 성공 |
| 영암 LGS-000192 | 2 | 공식 검색 `소상공인` 결과0건. 지역 전체 공고·첨부 부재로 간주하지 않음 |
| 영광 LGS-000195 | 1 | HTTP200이지만 접근 차단 페이지. 목록 성공으로 세지 않음 |
| 완도 LGS-000197 | 1 | 공식 목록 HTTP400. 별도 접근 오류 |
| 신안 LGS-000199 | 1 | 공식 목록 HTTP400. 별도 접근 오류 |
| 담양 LGS-000183 | 1 | 현재 대표 페이지는 `/eminwon/getSearchList` 동적 목록과 `/eminwon/searchDetail`을 사용. 저장소 V38의 `RSLT_DATA.boardContentsList`·`/board/detail` 계약과 다름. 운영 DB의 현재 프로필 필드·조회 endpoint는 재조회하지 않았으므로 운영 오설정으로 단정하지 않음. 후속 목록 계약 점검 대상 |
| 고흥 LGS-000186 | 1 | 기준 스냅샷에서 `MANUAL_ONLY`, 비활성. 공개 동적 목록 구조만 확인하고 요청 API 실행·프로필 연결·활성화하지 않음 |

이전 곡성 본문 경로 오류·진도 파일 형식 검증 실패·구례 timeout 등은 이번에 재시도하지 않았다. 수집 가능한 지역 확대를 우선한다.

## 구현과 검증

`JangseongPostAttachmentDiscoveryProfile`은 고정 기관·목록 parser·상세 경로·공식 첨부 표·숨김 필드3개의 다운로드 폼만 허용한다. JavaScript는 실행하지 않고 고정 `goDownLoad` 문자열의 세 인자만 해석한다. 일회성 다운로드 인자는 요청 메모리에만 두고 locator에는 공고 ID와 파일 hash만 남긴다. 동일 요청 이외 redirect, 다른 호스트, 중복 query, 폼 확장, 주입된 script를 차단한다. 미지원 형식·미해석 링크·과도한 폼 크기는 오류로 남기되 정상 파일을 잃지 않는다.

기존 profile hash·migration·DB/API·UI·운영 설정은 변경하지 않았다. 새 참조는 기대값 미승인 상태로 카탈로그에 추가했다.

```powershell
.\gradlew.bat :test --tests '*JeonnamThirdDownloadContractTest' --tests '*JeonnamSecondDownloadContractTest' --no-daemon
.\gradlew.bat :test --tests '*JeonnamThirdDownloadContractTest' --tests '*JeonnamSecondDownloadContractTest' --tests '*JeonnamFirstDownloadContractTest' --tests '*JeonbukSecondDownloadContractTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=JANGSEONG -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

최초 계약12건 성공(57초). 과대 폼 오류 분리 테스트를 추가한 최종 계약129건·bootJar·실파일1건 성공(1분45초). 실패·오류·skip0, Node23/23, 131영수증/최신101공고 재현 일치. 이전130영수증 및 실행 metadata를 보존했다.

조사10요청(GET9·POST1), 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 실파일 관측은 최대6요청·23MiB 안에서 예약4회·2,335,373byte를 사용했다. 조사 포함 예약14회다. HTML10개·745,445byte와 관측 상세·첨부 원본을 삭제했다. 복구 사본은 없으며 hash·비식별 근거만 보존했다. 단발 Node·Gradle은 종료하고 기존 사용자 프로세스는 유지했다.

전체 테스트·Linux worker 임시 DB/API·운영 배포·브라우저 검증은 이번 범위에서 실행하지 않았다. 브라우저는 지역 연결 작업에 명시적 지시가 없어 정책상 생략했다. `[skip deploy]` 변경이며 운영 성공·전체 Goal 완료를 주장하지 않는다. 다음은 경북권 등 미등록162지역의 연결 확대다.
