# 검단·영종 첨부 연결과 응답·남동 DNS 오류 분리

## 현재 단계 / Gate

전 지역 첨부 발견·다운로드 확대 단계다. 제목 → 본문 → 첨부 → 관리자 검증, 제목 제외 원문 비저장, 외부 공고 자동 활성화 금지를 유지한다. HWP 추출기 1.0.16 추가 개선은 보류하며 상시 worker·DB/API/UI·운영 E2E를 포함하는 전체 장기 goal은 미완료다.

- [x] 검단·영종 공식 본문·첨부 프로필 2개 연결.
- [x] 영종 HWPX 1개 65,823byte 실제 다운로드·형식 검증.
- [x] 검단 첨부 응답 검증 오류와 남동 목록 DNS 오류 별도 기록.
- [x] 실제 HTML 계약, Java 235통과/조건부 1생략, Node 23검사, bootJar 검증.
- [x] 기존 241영수증/197공고 보존, 243영수증/199공고 근거 재현.
- [x] 조사 HTML 7개 1,253,536byte 길이·SHA256 대조 후 개별 삭제.
- [ ] 미등록 68지역 연결, 등록 후 다운로드 미확인 33지역 후속.
- [ ] 전국 목록 자동 유입·첨부 텍스트 추출·상시 worker·DB/API/UI·운영 E2E.

## 지역 집계

분모는 2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷의 활성 223지역이다. 이번 운영 조회·변경은 없다. 연수는 해당 스냅샷상 비활성이므로 요청하지 않았다.

| 항목 | 이전 | 현재 |
|---|---:|---:|
| 실제 파일 최소 1개 다운로드 확인 | 121 | **122/223 (54.7%)** |
| 다운로드 미확인 | 102 | **101** |
| 프로필 등록 지역 | 153 | **155** |
| 미등록 지역 | 70 | **68** |
| 등록 후 다운로드 미확인 | 32 | **33** |
| 첨부 오류 관측 지역 | 40 | **41** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

지역 155+기업마당 1=156프로필, 카탈로그 211공고/154대상이다. 신규 expectation=null이며 기존 승인 기대값 1개를 보존했다. 다운로드 확인률은 전체 개발·운영 완료율이 아니다. 오류 지역은 성공 지역과 겹치며 남동처럼 상세 표본 미선정 목록 오류 전체를 포함하지는 않는다.

## 실제 관측

2026-09-30 01:02 KST 로컬 Java 수집 경로의 고정 공개 표본 관측이다.

| 지역 / 공고 | 본문 | 첨부 결과 |
|---|---|---|
| 검단 LGS-000063 / 235 | 225자, 대상·지원 조합 확인 | HWPX 표시 1개 발견, 전송 46,390byte 후 FILE_SIGNATURE / ATTACHMENT_DISPOSITION_INVALID |
| 영종 LGS-000056 / 332418 | 43자, 대상·지원 조합 확인 | HWPX 1개 65,823byte 검증 통과 |

검단 표본은 `청년월세 지원사업 이의신청서`, 영종 표본은 `「2026년 영종구 청년 이사비 지원사업」 지원 대상자 모집 공고`다. 표본은 기술 검증용이며 현재 모집 여부·운영 등록·관리자 확정을 뜻하지 않는다. 신청서 단독 공고를 정밀 분석 완료로 보지 않으며 첨부 역할은 UNKNOWN이다. 본문 ACCEPTED는 규칙상 후보 판정이다.

검단 파일 크기와 해시는 오류 근거이며 수집 성공으로 계수하지 않는다. 응답 검증 오류의 상세 원인은 별도 후속으로 두고 공통 validator를 완화하거나 같은 외부 요청을 반복하지 않았다. 영종은 다운로드 성공이며 텍스트 추출·분류 검증은 미실행이다.

프로필 지문:

- 검단 `811350257855cd256ef813ff10d10b64e2d24e6b42fc312dcde54d9bed8b3681`.
- 영종 `c2dc7616d6be80e3b9494fed91d321cc227d45a67d7f06356be8f675140d9318`.

파일별 SHA256·관측 시각은 `attachment-collection-receipt-index-2026-09-28.json`에 연결된 영수증을 참조한다. 두 관측 모두 파일 원본 삭제를 확인했으며 정책 QA·기대값 승인·운영 E2E는 수행하지 않았다.

## 구현과 수집 범위

`IncheonThirdNoticePage`는 검단 `board-view`와 영종 `cm_board_detail1`에서 제목·본문·첨부 영역을 분리한다. 부서·메뉴·첨부 이름을 본문에 섞지 않는다. 검단은 고정 notice 게시판의 개별 파일 GET 링크만 사용한다. 다운로드·미리보기 버튼은 같은 공고·파일 identity인지 확인한 뒤 무시하며 JavaScript를 실행하지 않는다.

영종은 `cm_file_list2`의 명시적인 download 링크와 같은 파일의 `span.skip` 이름을 읽는다. 같은 파일 식별자의 미리보기 링크는 제외하며 조회하지 않는다. HTTPS 대표 홈페이지 host, 다운로드 path, TP=dn, 양의 파일 번호와 제한된 다운로드 식별 key 형태만 허용한다. 원본 다운로드 식별값은 요청에만 사용하고 감사 locator에는 공고 번호·파일 식별 hash만 남긴다. 파일명·다운로드 주소 원문은 근거 대장에 복사하지 않는다.

source/parser 결합은 검단 HEURISTIC_NOTICE, 영종 SAFE_SAEOL_EMINWON의 기존 스냅샷 값을 유지했다. 영종은 공식 대표 홈페이지에 노출된 상세 표본을 연결한 것으로, 기존 목록 수집기의 새올 URL이 대표 홈페이지 상세 URL로 자동 연결됨을 증명하지 않는다. 전국 자동 목록 유입 연계는 별도 Gate로 남긴다.

10파일 상한, 미지원 형식, 중복·충돌, source identity·host·query·method·redirect 경계, 정상 파일과 오류 공존을 검증했다. 첨부 영역이나 링크 해석 실패를 첨부 없음으로 바꾸지 않는다. 기존 프로필 지문·migration·DB/API/UI·추출기·운영 설정은 변경하지 않았다.

남동 LGS-000059의 등록 공식 목록 `biz.namdong.go.kr`은 DNS 조회 실패(curl exit 6), HTTP 응답·파일 0byte다. 주소를 임의 교체하거나 재요청하지 않았다. 등록 주소 변경 필요 여부는 별도 조사 항목이다.

## 실행 명령 / 결과 / 정리

`SANEB_INCHEON_THIRD_SURVEY_FIXTURE=true`에서 신규 계약 테스트가 30초에 통과했다. 이후 같은 fixture와 기존 읽기 전용 inventory receipt를 사용했다.

```powershell
.\gradlew.bat :test --tests '*IncheonThirdDownloadContractTest' --tests '*IncheonSecondDownloadContractTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=GEOMDAN,YEONGJONG' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
```

Gradle 1분 47초 exit 0. 일반 Java 테스트 236개 중 235통과/1생략, 실패 0개다. 생략은 이전 제물포·미추홀 원본 정리 후 비활성인 조건부 fixture 테스트이며 이번 검단·영종 실제 HTML 테스트는 실행했다. Node 23개·bootJar·243영수증/199공고 재현 검증 통과. 관측 태스크 성공과 개별 파일 성공은 구분한다.

목록·검색·상세 조사 시도 8회(검단 3, 영종 4, 남동 DNS 실패 1), 실제 HTTP 응답 7회다. 관측 요청 예약 상한 합계 8회·4,674,870byte이며 조사 시도 포함 상한은 16회다. 공고별 한도는 6요청·23MiB다. 조사 HTML 7개 1,253,536byte의 길이·해시를 대조한 뒤 개별 삭제했다. 파일 관측 원본도 삭제 확인했다.

운영 DB/설정/정책/worker 변경, ENFORCE, 재분류, 배포는 수행하지 않았다. 브라우저 검증은 현재 사용자 정책에 따라 미실행이다. 다음은 미등록 지역 연결이며 검단 응답 오류·남동 DNS 오류는 별도 후속으로 유지한다.
