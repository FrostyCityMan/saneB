# 충북 첨부 응답 형식 진단

## 현재 단계 / Gate

- [x] 이전 Linux 응답이 Content-Type 불일치였음을 코드·보고서로 확인
- [x] 정제된 응답 형식 metadata 추가 및 로컬 집중 검증
- [~] 충북67302 단일 Linux 응답 관측
- [!] 실제 MIME 확인·호환성 수정·실파일 재검증 전 정상 수집으로 집계하지 않음

기준 HEAD `73c7d9f08935474e4af9f8a334e881c81e00d1bb`. 파일 확인203/223, 잔여20. 전체 goal은 미완료다. HWP 추출 고도화 보류·정상 파일 보존·오류 별도 기록·운영 자동 활성화 금지를 유지한다.

## 근거와 최소 변경

이전 run36775945586은 충북67302 첨부179,201바이트를 수신했지만 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패했다. `AttachmentFileTypeValidator`는 signature와 예상 형식을 먼저 대조한 다음 MIME을 확인한다. 그러나 당시 보고서에는 실제 응답 MIME이 없어 호환 값은 확정할 수 없었다.

이번 로컬 네이티브 HTTP 시도도 실패해 응답을 얻지 못했다. 같은 경로를 반복하지 않는다. 외부 요청을 시작하기 전 약속한 Windows 2요청/각2MiB 한도 내였으며 원본 파일은 작성하지 않았다. 응답에 도달했던 Linux에서 충북 고정1건만 관측한다.

QA helper `ObservationResponseMetadata`는 최대8바이트만 읽어 OLE_CONTAINER/PDF_PREFIX/ZIP_CONTAINER/OTHER를 기록한다. 이는 HWP 내부 구조·HWPX 유효성·텍스트 추출 검증이 아니다. MIME은 길이·형식을 제한하고 매개변수를 제거한다. Content-Disposition은 종류와 PDF/HWP/HWPX 여부만 보존하며 원시 header·파일명·URL·본문을 기록하지 않는다. 애플리케이션 파일 허용 정책은 변경하지 않는다.

workflow는 `[chungbuk-response-observation-01]` 최초 push에서 CHUNGBUK-67302만 최대6요청/23MiB로 실행한다. 기존6지역 표식과는 상호 배타적이며 두 표식이 함께 있으면 외부 관측을 실행하지 않는다. contracts job 종료 후 실행하며 그 job의 실패를 숨기지 않는다. 추출기·운영 DB·설정·배포는 실행하지 않는다.

## 로컬 검증

```powershell
.\gradlew.bat --no-daemon :test --tests '*ObservationResponseMetadataTest' --tests '*AttachmentContractWorkflowTest' --tests '*RegionalTransportLinuxContractTest' --tests '*ChungcheongThirdDownloadContractTest' --tests '*AttachmentFileTypeValidatorTest'
node --test scripts/qa/attachment-regional-linux-workflow.test.mjs scripts/qa/attachment-github-collection-receipts.test.mjs
```

Gradle39초 종료0, Node7개 통과. fixture 기반 형식 검증과 외부 실제 파일 관측을 구분한다. 응답 형식 helper가 추가되어 관측 생산 클래스 SHA는 `a16d59b5408ac8cadcaa488c35d0c64cfda324643a96378a3a29a7cfe9bfc5c6`으로 바뀌었다. 기존 영수증의 생산 지문을 바꾸지 않는다. 브라우저는 현재 지시가 없어 정책상 미실행이다.
