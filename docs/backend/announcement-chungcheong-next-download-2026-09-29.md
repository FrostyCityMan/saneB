# 진천·태안 첨부 연결 및 괴산·서천 접근 오류 분리

## 현재 단계 / Gate

성공 파일을 보존하고 수집·파싱 실패는 별도 기록한다. 모든 오류 해결이나3표본 확보를 다른 지역 착수 조건으로 삼지 않는다. 제목 → 본문 → 첨부 → 관리자 최종 검증, 제목 제외 원문 비저장, 자동 활성화 금지 및 HWP 추출기1.0.16 개선 보류를 유지한다.

- [x] 진천 GET·태안 POST 프로필2개 연결 및 본문 확보.
- [x] HWP1개·HWPX1개, 총190,714byte 다운로드.
- [!] 괴산 공식 목록HTTP201·223byte 보안 차단, 서천HTTP200·31byte Request Blocked 응답. 반복 요청하지 않고 미등록 후속으로 분리.
- [x] 계약158건·Node23건·bootJar,185영수증/143공고 재현.
- [x] 조사 원본9개156,593byte 및 관측 임시 원본 정리.
- [ ] 미등록121지역 연결, 등록 후 다운로드 미관측19지역 후속.
- [ ] 전국 목록→상시 worker·추출·DB/API·정책 승인·운영 E2E Gate.

분모는 **2026-09-28 15:56:12 KST 운영 읽기 전용 스냅샷**이다. 이번에 운영 설정을 재조회하거나 변경하지 않았다.

| 집계 | 이전 | 현재 |
|---|---:|---:|
| 첫 파일 다운로드 관측 지역 | 81 | **83/223 (약37.2%)** |
| 다운로드 미관측 지역 | 142 | **140** |
| 로컬 프로필 등록 지역 | 100 | **102** |
| 미등록 지역 | 123 | **121** |
| 등록 후 다운로드 미관측 | 19 | **19** |
| 최신 등록 표본의 발견·파일 오류 지역 | 23 | **23** |
| 기존 3표본·전체 첨부 Gate 충족 / 잔여 | 16 / 207 | **16 / 207** |

다운로드 관측률은 전체 개발·운영 완료율이 아니다. 지역102+기업마당1=103프로필, 카탈로그158참조/지역99개다. 신규 expectation은 null, 기존 승인 기대값1개는 유지한다. 이전183영수증·141공고와 실행 metadata를 보존한다. 괴산·서천 목록 차단은 등록 표본 오류23지역 집계와 별도로 기록한다.

## 실제 결과 및 구현 계약

| 지역 / 표본 | 본문 | 발견 / 다운로드 | 파일 |
|---|---:|---|---|
| 진천 LGS-000143 / 29107 | 468자 | 1 / 1 | HWP75,264byte |
| 태안 LGS-000162 / 44242 | 487자 | 1 / 1 | HWPX115,450byte |

두 결과는 `COLLECTION_ONLY_OBSERVED_NOT_APPROVED`, 본문 중간 판정은 `TARGET_SUPPORT_CONFIRMED`다. 텍스트 추출·구간 분석·DB/API 저장·최종 정책 승인은 이번에 검증하지 않았다.

진천29107은 **2021년 청년4-H회원 창업 지원 보관 공고**다. 현재 모집으로 간주하지 않으며 운영 공고를 생성하지 않았다. 저장소 V61에서 확인한 진천 목록 조건은 `not_ancmt_se_code=01`이며, 같은 범위의 소상공인 검색은 결과가 없고 지원 검색에서 표본을 확보했다. 일반공고까지 수집하는 데 충분한 범위인지는 별도 후속 검토가 필요하다. 운영 목록 조건을 변경하거나 현재 운영 DB 값이 V61과 같다고 단정하지 않았다.

- 진천 `LOCAL_JINCHEON_GET_V1`: `eminwon.jincheon.go.kr`·SAFE_SAEOL_EMINWON 바인딩. 공식 form1/post, contTable 제목과 th 첨부 라벨, FileDown.jsp 평문 GET을 확인했다. 기존 `SaeolGetAttachmentDiscoveryProfile`을 수정 없이 재사용한다.
- 태안 `LOCAL_TAEAN_POST_V1`: `eminwon.taean.go.kr`·SAFE_SAEOL_EMINWON 바인딩. 울산 중구 POST 프로토콜을 바탕으로 별도 클래스를 추가했다. 상세 URL은 `subCheck=N`을 고정하고, 공식100% 고전 표 구조의 중첩 td 첨부 영역과 nnn hidden4개 폼을 검증한다.
- 태안 FileDownNew.jsp POST는 불투명3인자와 고정 `isHome=Y`만 허용한다. `subCheck=Y`로의 변경·폼 필드 추가/누락·고정값 변경·다른 요청 redirect를 거부한다. 암호화 인자를 해독하거나 문서·카탈로그·로그에 저장하지 않는다.
- 고정 함수3인자만 읽으며 스크립트는 실행하지 않는다. 고정 HTTPS443 출처·source/parser·원문 URL hash·10파일 상한·signature/MIME 검증·역할 UNKNOWN을 유지한다. 기존 공유 클래스/hash와 TLS/MIME 정책은 변경하지 않았다.
- 고정 상세 표본의 수집 확인이다. 목록 수집기→상시 worker 자동 유입이나 운영 성공으로 확대 해석하지 않는다. migration·DB/API·화면·운영 정책·worker·추출기 변경 없음.

Profile hash: 진천 `b4dd90b947644c2164fc42e47d5cc2da65eddb70a9c212132f3e6b3b19198ab6`, 태안 `dfb4cbcb3a592471dd9b525c669e1f20a77bafc57047053ed647a650bd6a5715`. Producer class는 `6a551ccf96f79315ba37714d312cdf53069a90ad571a69a44849e3b86a51cd60`다.

## 요청량 / 정리

공식 조사 GET9회: 진천4·괴산1·서천1·태안3. 요청당15초/연결7초/1MiB/자동 redirect0/TLS 검증 유지. 괴산·서천 차단 응답을 HTTP 성공 코드만으로 수집 성공으로 기록하지 않고 추가 요청하지 않았다.

관측 예약 상한8회·본문 예약 포함4,406,264byte. 조사 포함17회 상한이며 실제 HTTP 요청 수와 동일시하지 않는다. 각 관측 최대6요청·23MiB다. 조사 원본9개156,593byte를 길이·SHA256 대조 후 삭제했다. 관측 임시 원본도 정리했고 비식별 hash·결과 metadata만 보존한다. 사용자 output·__pycache__ 및 기존 프로세스는 보존한다.

## 검증 명령 / 결과

```powershell
.\gradlew.bat :test --tests '*ChungcheongNextDownloadContractTest' --tests '*GangwonNextDownloadContractTest' --tests '*UlsanFirstDownloadContractTest' --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AttachmentFileTypeValidatorTest' --tests '*AttachmentProviderInventoryAuditTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' :bootJar :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=JINCHEON,TAEAN' -PsanebCollectionWindowsTrust=true --no-daemon
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

Inventory는 로컬 보관 target-inventory-20260928-receipt.txt를 사용했다. 첫 실행 계약158건 모두 통과·실패/오류/생략0, 실파일 관측·bootJar 포함1분46초 BUILD SUCCESSFUL. Node23건 및185영수증/143공고 재현 통과.

전체 테스트·Linux 임시 DB/API·AWS 운영 조회·배포·브라우저 QA는 미실행이다. 현재 지역 확대 요청에 브라우저 지시가 없어 정책상 생략했다. `[skip deploy]` 범위이며 전체 장기 Goal은 진행 중이다.
