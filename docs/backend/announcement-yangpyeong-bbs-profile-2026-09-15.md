# 양평군 BBS 본문·첨부 모델

후속 [세 단계 격리 관측](announcement-yangpyeong-three-stage-observation-2026-09-15.md)은 현재 DRAFT 제목 규칙에서312241/311846을 통과 후보,311507을 제목 제외로 구분한다. 아래 과거 BODY/다운로드3건을 필터링 통과3건으로 계산하지 않는다. 전체 실파일/정책 QA·운영 완료 여부는 후속 기록을 따른다.

## 현재 단계 / Gate

P3 기관 모델 구현 증분이며 전체 Gate는 **Not ready**다. 제목 → 정제 본문 → 실제 PDF/HWP/HWPX 텍스트 → 최종 관리자 검증 순서를 유지한다. 본문 A/B·부족은 첨부 단계를 끊지 않는다. 이미지 첨부를 없던 파일로 취급하지 않는다.

## 구현 전 범위·검증 계획

- V61/V62의 `LGS-000110 / HEURISTIC_NOTICE`, `www.yp21.go.kr`, `bbsNo=5 / key=1119`에만 결합한다. 운영 설정 확인이나 활성화가 아니다.
- 실측한 단일 `div.p-wrap.bbs.bbs__view > table.p-table.block`의 자체 제목과 `내용` 표제 옆 `td.p-table__content`를 요구한다. 기존 기관의 `td[title=내용]` 계약은 바꾸지 않는다. 메뉴·담당자·파일명은 BODY에 포함하지 않는다.
- `파일` 셀의 직접 `ul.p-attach`와 다운로드 anchor만 발견한다. 상세와 `/www/downloadBbsFile.do?atchmnflNo=양의정수`에만 요청한다. `/common/program/synap.jsp?fileName=/DATA/bbs/5/UUID.확장자`는 같은 공식 host/게시판/확장자의 표시 구조만 검증하며 절대로 요청하지 않는다. 실제 파일 ID가 없는 미리보기 URL을 다운로드 증거나 파일 소유 증거로 사용하지 않는다.
- 공개 표본312241은 HWPX1,311846은 PDF1/JPG1,311507은 PNG1/PDF1이다. JPG/PNG는 지원 형식 밖이며 descriptor/전체 분모에 유지하고 다운로드하지 않는다. 일부 지원 파일 성공으로 전체 파일 분석 완료를 만들지 않는다.
- 제한 헤더/8byte 관측에서312241의 파일은 HTTP200/application/octer-stream/ZIP이었다. 실제 전체 다운로드 때 기존 엄격 signature/형식/Content-Disposition/UTF-8 octet 검증을 거친다. TLS/형식 검사를 완화하지 않는다.
- 단위시험은 고정 URL/기관, 표제·셀 소속·중복·중첩/빈 BODY, 지원·미지원 파일 보존, 미리보기 요청 금지, 다른 기관으로 예외 전파 금지를 검증한다. opt-in 공식 시험은 BODY3건 및 발견5파일 전부의 상태와 지원3파일 다운로드/signature를 검증한다.
- 실제 Linux 추출·역할·내용 기대값·worker DB/API/UI·운영/브라우저는 별도 Gate다. catalog 참조는 기대값null로 유지한다.

## 결과

- [x] 고정 profile `LOCAL_YANGPYEONG_BBS_V1`과 전용 BODY 정제를 연결했다. 기존 엔진6종/전체 profile17(지자체16)/전용 BODY16기관/추출 형식3이다. 미연결 기관은 전체 대상에서 제외하지 않는다.
- [x] 표적199건은43초에 실패·생략0으로 통과했다. preview 경로·확장자/추가 query·중첩 제목/본문·셀의 소속/중복·다른 기관으로 예외 전파를 검증했다.
- [x] opt-in 실제6건은23초에 실패·생략0으로 통과했다. BODY3건은 AVAILABLE/시도1/redirect0이며 HWPX1/PDF2는 전체 다운로드·signature·Content-Disposition 검증을 통과했다. JPG/PNG2개는 요청하지 않고 미지원 상태로 보존했다.
- [x] catalog schema2의 참조24건/실행 기대값0/중복0이다. 실제 기대값·정책 성공을 만들지 않았고 이전 schema1을 보존했다.
- [x] 전체 회귀·원래 임시 Flyway 검증은4분27초 성공했다. root2580=2319통과/261조건부 생략/실패0, 별도 패키지20/20·Flyway3/3이다. extractor/bootJar는 UP-TO-DATE이며 새로운 재실행 성공으로 세지 않는다. 앞선 표적 검증에서 생성한 JAR의 SHA256은 `9a447b6adce7ce0372d318118af7717848991c8162822610d159946cff47c902`다.
- [ ] 실제 파일 텍스트와 전체 대상 QA.
- [ ] 동일 SHA Linux/운영 배포와 운영 브라우저.

기존 migration·v1·API/DB shape·운영 정책·기존 데이터와 사용자 Word 산출물을 변경하지 않는다.

## 실제 공개 표본과 한계

| 공식 공고 | BODY | 발견 전체 | 요청·signature | 보존 상태 |
|---|---|---|---|---|
| [312241](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=312241) | AVAILABLE | HWPX1 | 1개/116,740bytes | 발견·다운로드·signature 통과; 추출 미실행 |
| [311846](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=311846) | AVAILABLE | PDF1/JPG1 | PDF1/237,822bytes | DISCOVERY_WITH_UNSUPPORTED_FILES |
| [311507](https://www.yp21.go.kr/www/selectBbsNttView.do?key=1119&bbsNo=5&nttNo=311507) | AVAILABLE | PNG1/PDF1 | PDF1/173,670bytes | DISCOVERY_WITH_UNSUPPORTED_FILES |

첨부 상세3+지원 파일3=6요청, 예약1,486,696bytes, 원본 정리3/3, DB 쓰기0이다. BODY는 별도3요청이다. 앞선 구조·8byte 관측 트래픽을 포함한 전체 요청량이 아니다. 실파일3개는 모두 기존 `application/octer-stream` 예외와 UTF-8 header octet 복원 계약을 통과했으며 새로운 MIME 예외를 추가하지 않았다. 미리보기와 이미지2개 요청은0이며 발견된 파일5개의 상태를 모두 보고한다.

혼합 형식2공고는 `allFilesDownloaded=false`다. 이미지가 존재하는 다중 첨부를 지원 파일만 남겨 정상 다중첨부 QA로 세지 않는다. 이번 검증은 이미지 OCR/추출 지원을 추가하지 않으며 역할은 UNKNOWN이다. 실제 Linux 텍스트·지원대상/지원형태 분류·worker DB/API/UI·최종 정상 후보·운영 성공이 아니다. BBS 공통 코드 변경으로 이전 profile QA hash를 재사용하지 않는다.

```powershell
.\gradlew.bat :test --tests '*StandardBbsAttachmentDiscoveryProfileTest' --tests '*LocalGovernmentNoticeProviderContentClientTest' --tests '*AttachmentProviderQaPlanTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' bootJar --no-daemon --console=plain --max-workers=1
.\gradlew.bat attachmentProfileDiscoveryQa --tests '*StandardBbsAttachmentProfileLiveQaTest.discoversYangpyeongFilesWithoutHidingUnsupportedImages' --tests '*StandardBbsBodyContentLiveQaTest.readsYangpyeongBodyWithoutRequestingFiles' '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NUL' --no-daemon --console=plain --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest flywayIntegrationTest -PsanebFlywayEphemeral=true bootJar installAttachmentContractQa --no-daemon --console=plain --max-workers=1
```

메타데이터 보고서는 `build/reports/attachment-profile-discovery-qa/LOCAL_YANGPYEONG_BBS_V1-*.json`과 `build/test-results/attachmentProfileDiscoveryQa/TEST-*.xml`이다. 원문 파일·본문 텍스트·파일명을 문서/로그에 복사하지 않았다. AWS 재인증 응답 대기이며 이번 운영 조회/변경·브라우저 검증은 미실행이다.

직전 `5b24bb8`의 Linux 실행33은 원래 Flyway3건과 실제 worker12·독립221/221을 포함해 성공했다. 이번 양평 코드의 동일 SHA 검증이나 운영 증거로 계산하지 않는다. 사용한 단발 Node·Gradle·임시 PostgreSQL은 종료하고 사용자 Word2개를 보존했다.
