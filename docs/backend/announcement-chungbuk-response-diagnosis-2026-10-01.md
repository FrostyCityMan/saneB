# 충북 첨부 응답 형식 진단

## 현재 단계 / Gate

- [x] 이전 Linux 응답이 Content-Type 불일치였음을 코드·보고서로 확인
- [x] 정제된 응답 형식 metadata 추가 및 로컬 집중 검증
- [x] 충북67302 단일 Linux 응답 관측·원인 확인·영수증 반영
- [x] 기존 Citynet 정규화기를 충북에만 연결하고 로컬 검증
- [~] 새 프로필의 실파일 재검증 전 정상 수집으로 집계하지 않음

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

초기 Gradle39초 종료0, JUnit39개 중38통과/1조건부 생략, Node7개 통과. fixture 기반 형식 검증과 외부 실제 파일 관측을 구분한다. 응답 형식 helper가 추가되어 관측 생산 클래스 SHA는 `a16d59b5408ac8cadcaa488c35d0c64cfda324643a96378a3a29a7cfe9bfc5c6`으로 바뀌었다. 기존 영수증의 생산 지문을 바꾸지 않는다. 브라우저는 현재 지시가 없어 정책상 미실행이다.

추가 패키지 점검에서 두 격리 probe JAR에 helper 의존성을 포함했다. 최초 wildcard 설정은 테스트 클래스까지 포함해 새 검증이 실패했으며, helper 본체와 내부 record만 포함하도록 수정했다. 수정 후 같은 집중 Gradle27초 종료0, JUnit40개 중39통과/1조건부 생략, 관련 Node17개 통과다. 생성만 수행했고 운영 설치는 하지 않았다.

직전 HEAD73c7d9f의 [run36777540085](https://github.com/FrostyCityMan/saneB/actions/runs/36777540085)는 루트4,411개 중4,090통과·1실패·320생략이다. workflow 계약 추가 실패는 해결됐고 화천 고정 worker 계약1건이 남는다. 전체 Gate 통과가 아니다.

## 실제 응답 확인

[run36778179658](https://github.com/FrostyCityMan/saneB/actions/runs/36778179658), HEAD `d21402866ebd3b41b9bccefdbdf8602205de2512`, 관측 job `110105196697`, artifact `11127168135`를 확인했다. 생산 클래스 지문은 위 로컬 값과 일치한다.

- MIME: `application/file`
- signature 분류: `OLE_CONTAINER`
- disposition 분류: `ATTACHMENT`, 확장자 `HWP`
- 응답 크기179,201바이트, SHA-256 `ffbbf95d7723a87246f811b9039c47479df7ef8140a1b868c4551a8a40f0f94a`
- 이전 관측과 동일한 파일 해시. 본문 확보와 첨부 발견은 성공했으나 파일은 `ATTACHMENT_CONTENT_TYPE_MISMATCH`로 실패 유지
- 원본 정리true, 운영 쓰기0, 예약4요청/2,510,020바이트. 예약값을 실제 전송량으로 보고하지 않음

관측 job은 부분 실패를 기록하고 종료0이다. **job 성공은 파일 수집 성공이 아니다.** 전체 workflow는 화천 계약으로 실패했고 루트4,414개 중4,093통과·1실패·320생략이다. metadata artifact만 로컬에 보관했으며 원본 HWP는 남기지 않았다.

영수증672→673개, 최신 표본289개, 파일 확인203/223·미확보20개로 재현했다. 전체 집합 수집 Gate16/223은 별도다. 잔여20개를 전부 미구현으로 해석하지 않는다. 등록 프로필은222/223이며 미확보에는 연결·파일 검증 실패가 포함된다.

## 후속 구현의 영향

현재 검사기는 프로필이 명시적으로 선택한 `application/x-msdownload`와 `application/octer-stream`만 구형 MIME 예외로 허용한다. `application/file`은 단순 프로필 설정만으로 허용되지 않는다. 충북에만 적용되도록 예상 형식·signature·attachment disposition·확장자 일치를 유지하는 변경이 필요하다.

`AttachmentProfileFingerprint`는 공통 `AttachmentFileTypeValidator.class`도 포함하므로 이 파일을 직접 변경하면 전체 프로필 지문에 영향을 준다. 기존 영수증 지문 치환이나 전역 MIME 허용으로 검증을 우회하지 않는다. 아래 추가 점검에서 공통 파일 변경을 피할 수 있는 기존 Citynet 정규화기를 찾았다.

최종 Node50개 통과, 영수증673개/289표본 재현 통과. QA 패키지 helper 추가는 로컬에 검증했으며 해당 패키지 수정본을 운영 설치하거나 실제 worker로 실행한 것은 아니다.

## 충북 전용 호환 구현

추가 검색에서 인천·울산·충남 프로필이 이미 사용하는 `CitynetAttachmentFileResponse`를 확인했다. 이 코드는 `application/file`의 실제 signature·attachment disposition·파일명 확장자를 검증한 뒤 내부 legacy-binary 표현으로 정규화한다. `application/x-msdownload`라는 원래 응답을 무조건 수용하지 않는다.

새 `ChungbukCitynetResponseAttachmentProfile`은 기존 충북 프로필만 감싸고 이 helper를 재사용한다. URL·호스트·query·본문·첨부 발견 계약은 기존 delegate에 남긴다. 공통 `AttachmentFileTypeValidator`, 기존 Citynet helper, 공주 프로필 본체는 변경하지 않는다. 공주의 기존 지문 유지, 위험한 응답 및 목적지 차단, 네트워크 호출1회 보존을 단위 검증한다.

집중 Gradle34초 종료0 및 bootJar 실제 실행 통과. 파일 관측 전용 job에는 새 호환 검증도 필수로 추가했다. `[chungbuk-file-recheck-01]` 최초 실행에서 기존 충북67302 한 건만 최대6요청/23MiB로 재검증한다. `responseMetadata`는 downloader 반환값을 관측하므로 새 성공 보고서의 MIME은 정규화 후 내부 값일 수 있다. 원래 `application/file` 응답 근거는 이전 영수증에 보존한다.

새 프로필의 실제 파일 성공은 아직 확인 전이다. 기존 수집 실패를 코드 수정만으로 성공으로 변경하지 않는다. HWP 추출·운영 DB/API·규칙·worker 설정·배포는 이번에도 변경하지 않는다.

기존2026-09-28 읽기 전용 운영 수집원 receipt로 inventory만 다시 생성했다(현재 운영 조회 아님). 전체 target 대조 결과 충북1개만 바뀌었고 나머지는 동일하다. 충북 프로필은 `a03ff4f3c7c294842bf51e948ba8a632dcc3159678f1820d68cccf4684e00683` → `5054c6ec088190c2d510ace225659e9f6d1d936ba82f5da1864c77150529de12`, inventory SHA는 `31d6fabfa89aca60fc7d141bf32740b9453135b038d2bc7b70ab0f9e4a4749b0` → `bcb1f65eeab9daab01c832a390f948256e3eda7b5930cb062405592f3e01cab2`다. 대장 입력 hash를 갱신하고 기존 충북 영수증의 지문은 그대로 보존했다. 현재 코드와 일치하지 않는 과거 실패 근거로 분리되므로 오류 포함 지역 수39→38은 오류 해결을 뜻하지 않는다.

inventory 테스트25초 종료0, Node50개 및 대장673영수증/289표본 재현 통과. 파일 확인203/223과 미확보20개는 유지한다.
