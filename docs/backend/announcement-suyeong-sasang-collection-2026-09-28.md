# 수영구·사상구 첨부 수집 연결 — 2026-09-28

## 범위와 공식 구조

HWP1.0.16 추출 개선은 중지 상태를 유지한다. 이번 범위는 제목 정책 → 공식 상세 식별 → 첨부 전체 목록 → PDF/HWP/HWPX binary 다운로드 → signature/hash → 원본 정리다. 운영 DB·정책·worker 설정은 변경하지 않는다.

운영15:56:12 읽기 전용 영수증의 활성 기관 `LGS-000041` 수영구는 `SAFE_SAEOL_EMINWON_COMPACT`, `LGS-000042` 사상구는 `SAFE_SAEOL_EMINWON`이다. 이번에 운영 상태를 다시 조회한 것은 아니다.

- 수영구 공식 메뉴 `DOM_000000103001002000`는 같은 기관의 `DOM_000000103001002001`로302이동한다. 실제 메뉴 iframe은 `eminwon.suyeong.go.kr`의 공개 목록이며 상세는 form1 POST·div.view01·h3 제목·dl/dt 첨부파일/dd 전체 목록이다.
- 사상구 공식 메뉴 `DOM_000000101003001000` iframe은 `eminwon.sasang.go.kr` 공개 목록이며 `list_gubun=A`, `epcCheck=Y`를 제공한다. 상세는 form1 POST·table.basic·thead.tb 제목·단일 colspan2 td 내부 strong 첨부파일 라벨이다.
- 두 기관 모두 공식 goDownLoad3인자를 해석해 `/emwp/jsp/ofr/FileDown.jsp` GET으로 파일을 요청한다. 사이트 JavaScript는 실행하지 않았다.
- 새 `BusanStructuredSaeolAttachmentDiscoveryProfile`은 두 실측 레이아웃만 변환하는 공유 어댑터다. 고정 source/parser/host, 유일한 공식 영역, 전체 첨부 칸을 확인하고 임시 DOM만 기존 새올 엔진에 전달한다. 기존 엔진/지역 프로필 지문은 변경하지 않는다.
- 서로 다른 영역의 링크나 미지원 파일을 삭제하지 않는다. 중복 영역·잘못된 제목·다른 호스트·동작 속성이 있는 첨부 칸은 실패한다. 동일 파일의 표시 링크/빈 아이콘 링크는 파일 locator 기준으로 중복 제거한다.

## 고정 표본

| 지역 | 공고 | 공식 첨부 목록 | 제한 |
|---|---|---|---|
| 수영구 |40142|HWPX1|소상공인 전기차 구입비 지원 변경|
| 수영구 |39601|HWPX1|소상공인 전기차 구입비 지원 시행|
| 수영구 |38884|HWP2|청년 시험 응시료 지원|
| 사상구 |40870|HWP1|보조사업자 선정 공고; 신규 모집/지원 자격 확정으로 표현하지 않음|
| 사상구 |40734|HWP2|청년 활동공간 보조사업자 모집|
| 사상구 |40426|HWP2·JPEG1|JPEG 미지원, 전체 다운로드 완료 판정 금지|

모든 catalog 참조는 `expectation:null`이다. 수집 표본은 정책 QA 정상 기대값, 사용자에게 노출할 최종 후보, 운영 활성화 승인과 다르다. 임시 DRAFT seed의 제목 규칙만 확인하며 운영 ACTIVE 규칙을 바꾸지 않는다.

## 외부 요청 예산과 정리

- 사전 조사 수영8·사상9 GET, 총17회. 요청당 연결7초/전체15초/1MiB, 자동 redirect·TLS 우회 없음. 메뉴 이동은 확인한 Location만 별도 요청했다. 사상 첫302 이후 같은 공식 주소200응답을 확인했다.
- 이 회차 상한은 지역별30요청, 사전 조사 포함이다. 수집 실행은 공고당6요청·23MiB 예약, 파일당20MiB로 제한한다. 최대 수영26·사상27요청 예약이며 실제 합계는 실행 후 기록한다. 기존 다른 지역의 누적 예산을 초기화하지 않는다.
- 조사 원본은 작업 전용 `build/temporary-collection-suyeong-sasang-f28a9b91ed9046d29c08bbfdc850061c` 안에만 보관하고 종료 전에 정리한다. 파일 이름/본문/개인정보를 대장에 복사하지 않는다.

## 검증 이력

첫 계약 실행97검사 중1실패: 실제 카탈로그에 새 참조6개를 추가했으나 테스트의 명시 프로필 목록에 새2기관을 빠뜨렸다. 기대값을 낮추지 않고 목록에 실제 새 프로필을 추가했다. 새 레이아웃/공식 저장 HTML/제목 규칙 검사는 통과했다.

```powershell
.\gradlew.bat :test --tests '*BusanStructuredAttachmentCollectionContractTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentProviderInventoryAuditTest' :bootJar --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SUYEONG -PsanebCollectionWindowsTrust=true --no-daemon
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=SASANG -PsanebCollectionWindowsTrust=true --no-daemon
```

저장 HTML 검사는 해당 테스트 프로세스의 `SANEB_BUSAN_STRUCTURED_SAVED_DETAILS`로 opt-in한다. 운영 대상 대조도 기존 비식별 영수증을 읽는 opt-in이다. Windows-ROOT는 로컬 인증서 저장소 선택이며 TLS 우회가 아니다. HWP 추출기 실행, 운영 변경, 브라우저 검증은 수행하지 않는다.

## 실제 결과와 남은 범위

- 로컬 회귀100검사 실패/생략0: 새 계약10·기존 새올23·카탈로그64·실제 등록 대조3. Node19검사 통과, bootJar 실제 생성 성공. 전체 프로젝트/Linux/운영 검증은 재실행하지 않았다.
- 수영구 실제3검사 실패/생략0. 3공고 전체4파일(HWPX2·HWP2),380,681byte 다운로드·signature 검증·원본 정리 완료. 실행기13요청 예약/6,687,378byte 예약. 수집 전용 지역 Gate 충족이다.
- 사상구 실제3검사 중2통과·1실패, 생략0. 40870·40734의 전체3파일과40426의 HWP2파일을 다운로드해 합계HWP5개344,064byte다. 40426의 JPEG1개는 `UNSUPPORTED_NOT_DOWNLOADED`이며 `COLLECTION_COMPLETENESS`에서 실패했다. 제목·첨부 목록을 변경하거나 JPEG를 숨겨 성공 처리하지 않았다. 실행기14요청 예약/6,654,122byte 예약, 모든 임시 원본 정리 확인. 지역 Gate는 미완료다.
- 두 지역 총9파일724,745byte. 실행기27요청/13,341,500byte 예약, 조사17회 포함44회 예약(수영21/30·사상23/30). 본문 상한을 포함한 예약값은 실제 전송량과 구분한다.
- 수영 프로필 hash=`5e2a236471b37107850cc60927e986aef3f482cb690ea5c67e3a69709210e241`, 사상=`de92fea2368dc95631ff69bc6bdeaa1e0faad2360f03517b7bb3282d81d80159`. 이전8충족 지역의 프로필/결과는 그대로 유지됐다.
- JUnit hash: 수영=`aacb2355202e4825f90ba80cdbf682376e32914fac6633635efc6be9f60e72ae`, 사상=`a59c113705dee6a4091ef2147460edb9286861286eb7631023afaaae49acadb4`. 공통 JUnit 경로가 교체되므로 실행 직후 각각 기록했다. 개별 JSON은 `build/reports/attachment-regional-collection/SUYEONG-*.json`, `SASANG-*.json`, 원본 보고서 hash/실행 class hash는 [색인](attachment-collection-receipt-index-2026-09-28.json)에 고정했다.
- 저장 조사 HTML13개158,908byte와 임시 대장 갱신 스크립트1개를 검증 후 삭제했다. 최초302 응답/별도 헤더 조사 응답은 이 저장 합계에 포함되지 않으므로 총 전송량이라고 표현하지 않는다. 사용자 파일/기존 프로세스는 보존했다.
- 등록은 전체25프로필(지역24·기업마당1), 실행 구현 클래스10개다. catalog59참조/21지역 프로필, 보관 기대값1·정상 승인0이다. 정책225대상은 연결25/미등록200(지역199+정부24)이다.
- [최신 수집 대장](attachment-collection-regional-ledger-2026-09-28.json): 활성223지역 중 **9충족/214잔여 = 연결됐지만 미완료15 + 미연결199**. 비활성21지역은 자동 활성화하지 않는다. 보관영수증67개/최신44공고 재현 통과, 성공/미지원/제목중단 근거를 함께 유지한다.

사상 JPEG 지원은 현재 PDF/HWP/HWPX 다운로드·검증 계약 밖이다. 별도 형식 확장 없이 파일을 건너뛰고 지역 완료로 처리하지 않는다. 이번 연결 코드/로컬 수집 성공은 운영 상시 worker 또는 전체 장기 Goal 완료가 아니다. 정책 게시·ENFORCE·기존 데이터 적용은 하지 않았다. 현재 사용자 요청에 브라우저 실행 지시가 없어 정책상 미실행이다.
