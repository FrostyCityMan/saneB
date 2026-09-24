# 부산 남구 지원사업 고정 표본과 3단계 관측

## 범위와 판정

전체 격리 QA 승인 범위에서 기존 `LOCAL_BUSAN_NAMGU_GET_V1` 프로필의 지원사업3건을 확인했다. 새 엔진/프로필을 추가하지 않았으며 코드 등록7엔진/19프로필은 그대로다. 목표인 제목→정제 본문→전체 첨부 텍스트→관리자 최종 검증을 유지한다.

**관측3건은 통과했지만 HWP3파일 모두 PARTIAL_TEXT다. 정상 공고·정상 기대값·정책 QA 통과가 아니다.** 관리자 검수와 수동 원문 확인이 남는다. 운영 DB/정책/worker 설정/공고 활성화/기존 데이터/배포는 변경하지 않았다.

## 공식 표본 선정과 계약

기존 남구46034는 구조 검증용 과거 표본이며 지원 후보나 정상3공고를 증명하지 않는다. 공식 검색 결과에서 아래 지원사업을 확인한 뒤 프로필이 허용하는 기존7개 query의 고정 상세 주소로 직접 제목과 전체 첨부를 확인했다. 검색 도구의 canonical 주소 열기는 실패했으나 실제 pinned Java 요청은 성공했다. 검색 결과만으로 성공을 기록한 것이 아니다.

| 고정 표본 | 공식 제목 | 직접 확인한 파일 |
|---|---|---|
| [44466](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=44466&subCheck=Y) | 2026년 청년 사업자 임차료 지원사업 참여자 모집 공고 | HWP1 |
| [44381](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=44381&subCheck=Y) | 2026년 남구 청년 자격시험 응시료 지원사업 참가자 모집 공고 | HWP1 |
| [42871](https://eminwon.bsnamgu.go.kr/emwp/gov/mogaha/ntis/web/ofr/action/OfrAction.do?context=NTIS&homepage_pbs_yn=Y&jndinm=OfrNotAncmtEJB&method=selectOfrNotAncmt&methodnm=selectOfrNotAncmtRegst&not_ancmt_mgt_no=42871&subCheck=Y) | 2025년 남구 청년 자기개발 도서구입비 지원사업 참여자 모집 변경공고 | HWP1 |

- 기관 `LGS-000034`, 목록 parser `SAFE_SAEOL_EMINWON_LEGACY`, 기존 exact host/path/query·HTTPS·공인주소/redirect·파일 signature 경계를 유지했다. 현재 운영 목록 binding을 다시 조회한 것은 아니다.
- 제목은 기존 실측 구조인 유일한 POST form1→table.table_03→직접 소속 th[colspan=4]에서 확인한다. 중복·다른 제목·중첩 표에서 제목 차용을 거부한다. 임의 제목/URL/기관/파일을 실행 입력으로 받지 않는다.
- classpath catalog schema2에 `NAMGU-44466/44381/42871`의 canonical identity를 추가했다. 총33참조/11기관/저장된 기대값1/정상0이다. 새3건은 모두 expectation:null이다. 태백 기대값 내용·시각·profile hash와 schema1/기존 migration/v1 API는 수정하지 않았다.
- 새 catalogVersion은 `2026-09-24-namgu-support-references-v2`다. 전체 catalog/코드 지문이 달라지므로 이전 정책 QA snapshot을 새 catalog 성공으로 재사용할 수 없다. 운영에 설치된 catalog30과 구분한다. 등록19개 중 catalog 참조가 없는 프로필은9→8개다. 전체 미등록 기관은 별도로 남는다.

## 1. 로컬 실제 발견·다운로드·형식 검증

`attachmentProfileDiscoveryQa --tests '*SaeolAttachmentProfileLiveQaTest.fixedNamguSupportReferencesValidateTitleAndWholeFileSignature'`를 Windows 신뢰 저장소로 실행했다. TLS 검증을 해제하지 않았다. 3실행/3통과/실패·생략0, JUnit3.723초다.

사례별 최대8요청/24MiB/120초, 파일20MiB 상한이다. 실제는 각 상세1+파일1로 **총6요청/248,801예약byte**, 파일 합계232,960byte다. Windows에서 외부 binary를 파싱하지 않았으며 각 임시 HTML/HWP 삭제를 확인했다. 비식별 근거는 `build/reports/attachment-profile-discovery-qa/LOCAL_BUSAN_NAMGU_GET_V1-{id}.json`이다.

## 2. 서울 제목→본문→HWP 격리 추출·분류

명시 `NAMGU_OBSERVATION` 모드만 실행했다. 기존 태백/옥천/보은 모드는 보존하고 새 모드는 실제3건·각20요청/32MiB로 한정한다. 서울 최대60요청/96MiB/20분·CPU1개/메모리768MiB/임시1GiB다. 현재 코드를 임시 패키지로 전송하며 운영 설치 경로를 교체하지 않는다. 동일 계획 재시작과 다른 표본으로의 변경을 거부한다.

- execution `f5bada861ef2444fb0cacad8543804ff`, SSM `9156b397-2da9-4e25-be4e-8f933145d084`.
- 관측3/3·실패/생략/중단0,46.347초. 실제 예약12요청/6,540,257byte다. 이번 고정3건의 로컬+서울 합계18요청/6,789,058예약byte이며 웹 검색/검색 도구 접근을 포함한 전체 네트워크 호출 수는 아니다. 과거 다른 남구 공고의 사용량을 초기화하는 기록도 아니다.
- 임시 원본/격리 프로세스 정리, unit inactive, 운영 JAR 불변, health UP을 확인했다. 소유 S3 전송 객체 부재와 plan.cleaned=true를 확인하고 로컬 자기 package.zip만 hash/경로 대조 후 삭제했다. 재생성 가능한 전송 패키지는 제거했고 plan/result JSON 영수증은 보존했다.

| 표본 | 제목 / 본문 | 추출 문자 / block | 품질 / 최종 판정 |
|---|---|---:|---|
| NAMGU-44466 | COMBINATION_MATCHED / AVAILABLE·202자 | 5,031 / 345 | PARTIAL_TEXT / REVIEW_REQUIRED |
| NAMGU-44381 | COMBINATION_MATCHED / AVAILABLE·358자 | 3,307 / 160 | PARTIAL_TEXT / REVIEW_REQUIRED |
| NAMGU-42871 | COMBINATION_MATCHED / AVAILABLE·576자 | 4,152 / 267 | PARTIAL_TEXT / REVIEW_REQUIRED |

전체 목록 각1파일·FOUND/발견 complete, 본문 attempts1/redirect0을 확인했다. HWP binary hash는 선행 로컬 다운로드와 세 파일 모두 일치한다. 본문 판정은 ACCEPTED이나 첨부 불완전으로 최종 판정이 REVIEW_REQUIRED이고 `isWholeTextAnalysisComplete=false`다. 파일 제목이나 본문 성공으로 첨부 완전성을 대체하지 않는다.

상시 worker의 DB/API 저장·검수 확인/DRAFT 생성 증거는 이번 관측에 포함되지 않는다. 별도 loopback 임시 DB의 Flyway DRAFT 규칙을 읽어 사용했지만 운영 DB는 사용하지 않았다. 이번 결과에는 HWP 레코드 구조 진단이 전송되지 않았으므로 부분 추출의 직접 원인을 그림/표/양식으로 단정하지 않는다. 기존 extractor의 검증된 hwpStructure metadata를 관측 보고서에 연결하고 필요한 추가 관측의 예산을 대조하는 것이 다음 분석 단계다.

## 재현 지문

- 실행 codeHash: `32d813afcc3177697013a9eea4b1b607aca062b6b7106f90815ea4a836cce2ef`
- probeHash: `08342ab6de2aa618d5ee192923349b5ad68d38cb24d6cf2203bbb984e17e4345`
- 전송 ZIP SHA256: `fb2b7f85709201499720ab58e227567fdf4fe2562e0b057efe9ff661c171515f`
- 운영 불변 JAR SHA256: `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`
- 로컬 영수증: `build/temporary-bbs-qa-f5bada861ef2444fb0cacad8543804ff/plan.json`, `result.json`.

## 3. HWP 구조 진단 연결 후 서울 재관측

선행 관측에서는 추출기가 반환하던 `hwpStructure`가 공통 관측 보고서에 누락됐다. 검증된 section/record/level/tag별 count만 전달하도록 수정했다. 원문·파일명·컨트롤 payload는 추가하지 않는다. 기존 4필드·정수 범위·태그 정렬·합계 검증과 deep copy를 재사용하며, 잘못된 진단은 실패한다. PARTIAL_TEXT에서 역할/구간 성공 근거를 생성하지 않는다. 추출기1.0.5·분류기·catalog·운영 코드는 바꾸지 않았다.

새 NAMGU_OBSERVATION 예산은 각5요청/24MiB, 전체15요청/72MiB로 축소했다. 이전 서울 영수증과 로컬 signature 사용량18요청/6,789,058byte를 실시간 대조·차감해 고정3건 총60요청/96MiB 안에서 수행했다. 같은 plan 재시작·누락된 영수증·동일 probe 반복·추가 실행 예약은 거부한다. 보은의 별도 누적 예산은 변경하지 않는다.

- execution `3c38378daf3745ac96ee3a19815c5b57`, SSM `0ff3408e-de3b-437d-81e4-339cd33fd625`.
- 결과3/3·44.502초·실패/생략/중단0, 실제 예약12요청/6,540,257byte다. 앞선 로컬+서울과 합친 고정3건 누적은 **30요청/13,329,315byte**다. 웹 검색·과거 다른 남구 표본을 포함한 전체 지역 누적값은 아니다.
- codeHash는 선행 `32d813afcc3177697013a9eea4b1b607aca062b6b7106f90815ea4a836cce2ef`와 동일하다. 새 probeHash는 `8479fa9dc95f2689d1c93992a11f150a3712970ac1124cc65862fb9dfb13660f`, ZIP SHA256은 `3f181f985e2f7326625b83c02a2d3d3f953866ebc7b85016d5591dc180d05ae4`다.
- 세 공고의 본문 hash·HWP binary hash·추출 text hash는 선행 결과와 모두 동일하다. PARTIAL_TEXT·REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE·최종 관리자 검증 필요 상태도 동일하다.

| 표본 | section / record / 최대 level | 원인 범위 확인 |
|---|---:|---|
| 44466 | 1 / 1,960 / 3 | tag87 1개가 존재한다. 현재 HwpSectionText의 미해석 record 분기가 부분 처리를 수행하는 직접 조건이다. 다른 원인이 없다는 뜻은 아니다. |
| 44381 | 1 / 891 / 3 | tag66~75 및77만 관측됐다. 종류 집계만으로는 표/셀/컨트롤 연결 실패와 다른 부분 처리 경로를 구분할 수 없다. 원인 미확정이다. |
| 42871 | 1 / 1,467 / 6 | tag76 12개·tag82 8개가 존재하며 현재 미해석 record 분기의 부분 처리 조건이다. 이것이 유일 원인인지는 미확인이다. |

근거 코드는 `attachment-extractor/src/main/java/com/saneb/extractor/HwpSectionText.java`의 `insertRecord`다. 위 숫자를 임의로 그림/수식/장식이라고 해석하거나 무시하지 않는다. **다운로드 실패가 아니라 현재 추출기의 구조 해석 범위 문제라는 점을2건에서 좁혔지만, 이를 해결한 상태는 아니다.** 44381은 표/컨트롤/문단 앵커별 실패 사유를 원문 없는 고정 코드·수치로 구분한 뒤 재관측해야 한다. 구조가 검증되지 않은 텍스트를 정상 후보에 사용하지 않는다.

원격 unit inactive·원본/임시 DB/transport 정리·JAR 불변·health UP, 소유 S3 객체 부재를 확인했다. 로컬 자기 ZIP도 경로/hash/cleaned 상태 확인 후 삭제했으며 plan/result JSON은 보존했다. 운영 DB/정책/worker/기존 데이터/배포 변경은 없다.

검증 명령/결과:

- `:test --tests '*AnnouncementAttachmentOfficialObservationContractTest' --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*HwpStructureDiagnosticTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1`:34초 성공, 표적44·패키지20건 실패/생략0. probe 재생성, bootJar 동일 코드 UP-TO-DATE.
- `:test --tests '*AnnouncementAttachment*Observation*Test' --tests '*Hwp*Test' :attachment-extractor:test :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`:1분28초 성공, root68=66통과/조건부 생략2·실패/오류0. extractor122·패키지20·bootJar는 변경 없는 산출물 UP-TO-DATE로 재사용했으며 이번 명령에서 재실행된 것으로 세지 않는다.
- Node/Bash4·Python21건 실패/생략0, PowerShell 구문0오류. PATH의 python은 Store alias라 실행되지 않아 번들 Python의 절대 경로로21건을 실행했다. 소유 시험 프로세스 종료, 기존 사용자 Node/output/cache 보존.
- 선행047a1d8 Linux36006726822는 실행 중으로 확인했다. 원격 실행을 재시작·취소하지 않았고 이번 진단 코드의 전체 CI 성공으로 표현하지 않는다.

## 검증과 남은 Gate

### 후속 1.0.6 부분 처리 사유 진단 계약

- 목적:44381의 PARTIAL_TEXT를 무조건 해제하는 것이 아니라, 표/셀/문단/컨트롤 검증 중 실제 실패한 조건을 확인한다. 검증 후 지원 가능한 구조만 별도 변경한다.
- 격리 IPC에 선택적 `hwpPartialCauses:[{code,count}]`를 추가한다. 고정 enum29종·정수1~33,554,432·중복 없는 고정 순서만 허용한다. 원문/파일명/컨트롤 payload/외부 오류문을 담지 않는다. 기존 HWP structure4필드는 보존한다.1.0.6 HWP 응답에서 새 진단 누락은 실패한다. 구버전 응답의 생략은 호환한다.
- COMPLETE_TEXT는 빈 사유 목록, PARTIAL_TEXT는1개 이상, OCR_REQUIRED는 문서 구조에 따라 빈 목록 또는 사유가 가능하다. 사유 수는 실패 이벤트 수이며 문서 수/고유 원인 수/누락 문자 수가 아니다. 표 검증은 기존 첫 실패 반환 순서를 보존하므로 보고되지 않은 다른 원인이 없다는 뜻이 아니다.
- 추출기 Gradle/version·런타임 identity를1.0.6으로 맞춘다. 진단 추가로 실행 지문이 바뀌므로 이전 runtime/정책 QA snapshot을 새 성공으로 재사용하지 않는다. 기존 v1 API·DB/Flyway·A/B/FORM 정책·자동 활성화 금지는 불변이다. 운영 설치/정책은 별도이며 이번 변경으로 갱신하지 않는다.
- 성공 조건: 기존 합성 HWP의 text/block/quality 경계 유지, 정상 빈 원인·비정상 구체 사유, 임의 payload/코드/중복/역순/범위/품질 모순 거부, 같은 고정 실파일의 이전 binary/text/본문 hash 대조, 원본/임시 자원 정리. 실제 실파일 결과와 새 CI는 실행 후 별도 기록한다.
- 실패 조건: 부분 판정을 정상으로 완화, 원문 누출, 다른 파일을 같은 표본으로 간주, 원인 미확정을 해결로 보고, 이전 영수증 미차감 또는 예산 초과, 운영 DB/정책/worker 변경.

### 1.0.6 실제 재관측 결과

- execution `bfc3baf61e684f22bb839c2a9d7fb305`, SSM `96ae2857-b60d-4ddb-858b-445ee63a9dc0`.47.050초·3/3·실패/생략/중단0이다. 실제12요청/6,540,257byte, 고정3건 누적 **42요청/19,869,572byte**다. 전체60요청/96MiB 안에서 기존 영수증2회와 로컬 signature 사용량을 차감했다.
- codeHash `8c84b30d6e55f2d63bbf5c81761c2bda62154df64720fbbfc7d49a79241fc5ae`, probeHash `87c2a795b51d0db035eb11f66eacdc9e69348b5be2632904efd7553d7a15d670`, ZIP SHA256 `2ca16c30bb4770bd77cd123475d448b0603956e0263e1a22eebc873032e1e630`.
- 세 파일 모두 extractorVersion1.0.6이며 이전 본문/binary/text hash가 동일하다. PARTIAL_TEXT·REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE·관리자 최종 검증 필요는 유지했다. 실제 원본이나 추출 본문을 진단 보고서에 저장하지 않았다.

| 공고 | 실제 부분 처리 이벤트 |
|---|---|
| 44466 | UNSUPPORTED_RECORD1·UNSUPPORTED_CONTROL1 |
| 44381 | UNSUPPORTED_INLINE_CONTROL1·MISSING_CONTROL1·UNSUPPORTED_CONTROL1 |
| 42871 | UNSUPPORTED_RECORD20·UNSUPPORTED_CONTROL7 |

44381은 표/셀 검증 실패 코드가 관측되지 않았고, 인라인/컨트롤 처리로 범위가 좁혀졌다. 다만 `MISSING_CONTROL`에는 미지원 inline을 분리하는 내부 placeholder의 후속 처리도 포함된다. 위3개 이벤트를 서로 독립적인3개 원인이나 원본 자체의 앵커 유실로 단정하지 않는다. 다음은 해당 컨트롤의 고정 종류·문서 내 범위와 텍스트 보존 조건을 확인하는 것이다. 지원하지 않는 컨트롤을 장식으로 가정해 무시하거나 파일 전체를 정상으로 승격하지 않는다.

원격 unit/원본/임시 DB/transport 정리·JAR 불변·health UP을 확인하고 S3 자기 객체 및 로컬 자기 ZIP을 제거했다. plan/result JSON은 보존했다. 운영 설치·DB·정책·worker·기존 데이터·배포는 변경하지 않았으며 브라우저는 현재 명시 요청 정책상 미실행이다.

검증:

- 추출기123건 실패/생략0. IPC 고정 코드29종 일치·범위·중복/역순·원문 필드·품질 모순 거부, HWP 표/셀/앵커/레코드 원인과 기존 text/block/quality 경계를 검증했다.
- 후속 probe 컴파일에서 기존 diagnostic 매개변수와 지역 변수명의 충돌1건을 수정했다. 표적43·패키지20·bootJar/probe 재생성은55초 성공/실패·생략0이다.
- 확대 `:test --tests 'com.saneb.domain.announcementattachment.*' :attachment-extractor:test :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`:3분16초 성공,root1965=1943통과/22조건부 생략·실패/오류0. extractor123/패키지20/bootJar는 앞서 실행·생성한 동일 코드 결과 UP-TO-DATE다. Node4·Python21건 실패/생략0.
- 새 HWP IPC의 진단 누락 거부/1.0.5 생략 호환 및 기존 worker probe를 추가로 검증했다. `:test --tests '*HwpPartialDiagnosticTest' --tests '*AnnouncementAttachmentOfficialWorkerProbeTest' :bootJar --no-daemon --max-workers=1`은22초/30건 실패·생략0이다. 변경 없는 bootJar는 UP-TO-DATE이며 종료 후 Java/PG0, 사용한 단발 Node 종료와 기존 사용자 프로세스 보존을 확인했다.
- 선행047a1d8 [Linux36006726822](https://github.com/FrostyCityMan/saneB/actions/runs/36006726822) success/artifact XML 확인:root3007=2721통과/286조건부 생략·실패/오류0,별도 extractor122·패키지20·jobPG206·migration18·정책부모PG2·runtime5·workerPG12·Flyway3은 실패/오류/생략0. 이는 이번1.0.6의 CI 통과 근거가 아니다.48fb835 Linux36007924157은 실행 중으로 확인했다.

- 최초 제목 구조 단위 시험3건 중 중첩 표 차용1건이 실패했다. assertion을 완화하지 않고 중첩 표를 명시적으로 거부했으며3건 재검증과 실제 사이트3건이 통과했다.
- 참조·제목·catalog·probe 표적 시험/QA 패키지20/bootJar는1분56초 성공. Node/Bash4·Python21건 실패/생략0. CI에는 단위 검증만 연결되며 남구 실파일 요청을 자동 실행하지 않는다.
- 확대 회귀 첫 실행은 기존 정책 snapshot 시험의 catalog30 고정 수량1건이 실패했다.33개 전체 고정 참조와 실행0·정책QA false를 유지하도록 수량을 갱신했다. 재실행 `:test --tests 'com.saneb.domain.announcementattachment.*' :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`은3분22초 성공이다. XML1960=1938통과/22조건부 생략·실패/오류0, 패키지20건 실패/오류/생략0이다. bootJar는 동일 코드 산출물 UP-TO-DATE이며 Node 확대12·Python21건 실패/생략0도 확인했다.
- 선행caba129 [Linux36002040454](https://github.com/FrostyCityMan/saneB/actions/runs/36002040454) success/artifact를 확인했다. root3000=2715통과/285조건부 생략·실패/오류0, 별도 extractor122·패키지20·jobPG206·migration18·정책부모PG2·runtime5·workerPG12·Flyway3은 실패/오류/생략0이다. 이번 남구 변경의 CI 증거로 대체하지 않는다.
- 남구 정상3공고와 정상 다중 첨부1건은 아직 충족하지 않았다. 단일 HWP3건을 다중 첨부나 정상3건으로 계산하지 않는다. 전체 Provider/형식·부분 추출 해소·DB/API worker·정책 게시·기존 데이터·동일SHA 운영 배포/브라우저 E2E가 남는다. 브라우저는 현재 명시 요청 정책에 따라 미실행이다.

전체9Gate=8부분/1차단, goal active를 유지한다.
