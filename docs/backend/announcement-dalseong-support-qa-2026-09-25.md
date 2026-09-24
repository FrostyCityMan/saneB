# 달성군 지원사업 고정 표본 QA — 2026-09-25

## 범위와 현재 기준

후속1.0.8 비교 실행까지의 최신 누적은42/60요청·33,664,851/100,663,296byte다. 표 헤더 오류19회·4파일 부분 추출은 해소되지 않았다. [새 실행 결과와 남은 진단](announcement-hwp-common-header-v1.0.8-2026-09-25.md)을 따른다. 아래16/29회 원장은 각 선행 단계 당시 값으로 새 실행의 잔여 한도가 아니다.

전체 격리 QA 승인 안에서 등록되어 있지만 catalog 참조가 없는 `LOCAL_DAEGU_DALSEONG_GET_V1`의 서로 다른 지원사업 3건을 검증한다. 새 엔진이나 운영 설정을 추가하지 않는다. 제목→본문→전체 첨부→관리자 최종 검증이라는 목표를 유지한다. 파일을 다운로드할 수 있다는 사실은 완전 추출·구간 판정·정상 후보·정책 QA 통과를 의미하지 않는다.

- 기관 `LGS-000052`, parser `SAFE_SAEOL_EMINWON`, 기존 HTTPS host/path/query·공인주소 pinning·첨부 공식 영역 제한을 유지한다.
- 이번 고정 3건의 누적 한도는 60요청/96MiB다. 각 직접 상세 조회는 최대1MiB/15초/redirect0/재시도0이며 사전 3요청을 보수적으로3MiB 예약했다. 외부 검색 도구의 조회량은 이 프로그램 원장과 별도다. 과거 구조 QA 표본53932와 보은/남구 원장은 변경하거나 초기화하지 않는다.
- 로컬 검증은 공고별 최대8요청/24MiB/120초, 파일20MiB다. 본문은 별도 공고별 최대2시도/2MiB 응답/redirect0이며 보수적으로6요청/12MiB를 예약한다. 사전3+다운로드24+본문6=33요청, 예약 상한3+72+12=87MiB로 누적 한도 안이다.
- 파일은 임시 위치에만 내려받아 signature만 검사하고 삭제한다. Windows에서 외부 binary 텍스트를 파싱하지 않는다. 운영 DB·정책·worker·기존 데이터·배포는 변경하지 않는다.

## 공식 페이지에서 직접 확인한 목록

| 공고 | 제목 | 전체 첨부 |
|---|---|---|
| 51022 | 2026년 달성군 중소기업 경영안정자금(이차보전) 지원사업 공고 | PDF 예약 매뉴얼1 + HWP 공고문1 |
| 52145 | 2026년 달성군 소상공인 카드수수료 지원사업 공고 | HWP 공고문1 |
| 51075 | 「2026년 달성청년 자격증 응시료 지원사업」공고 | HWP 공고문1 |

공식 `eminwon.dalseong.daegu.kr`의 `OfrAction.do` 상세에서 HTTP200과 제목/전체 goDownLoad 목록을 직접 확인했다. 제목 검증은 유일한 POST form1 → table.bbsView → 직접 소속 제목 th[scope=row]의 다음 td[colspan=3]만 사용한다. caption·중첩 표·중복 form/table·다른 제목으로 일치를 대신할 수 없다. 기존 남구 제목 구조는 변경하지 않는다.

## 선택하지 않은 부산 본청 표본

부산 본청78332·76795·76865를 검색해 고정 상세3건을 직접 조회했으나 동일412자 응답으로 공고 구조가 없었다. 76795에 기존 허용 curPage=1을 포함한1회 진단에서 HTTP401을 확인했다. 총4직접 요청·보수적4MiB 상한, 첨부 다운로드0이다. 앞선3건의 HTTP status는 당시 기록하지 않았으므로401로 일괄 단정하지 않는다. 인증을 우회하지 않고 재시도를 중단했다. 검색된 주소·웹 검색의 메뉴만 있는 응답을 정상 참조나 수집 성공으로 등록하지 않았다.

## 로컬 실제 검증 결과

`attachmentProfileDiscoveryQa --tests '*SaeolAttachmentProfileLiveQaTest.fixedDalseongSupportReferencesValidateTitleAndWholeFileSignature' --tests '*MeasuredBodyContentLiveQaTest.readsDalseongSupportBodyThroughProductionPinnedTransport'`를 Windows-ROOT 신뢰 저장소와 검증된 TLS로 실행했다. 빌드26초 성공, 전체 파일3시험4.922초·본문3시험0.622초,6/6 통과·실패/오류/생략0이다. 본문은3건 모두 AVAILABLE·비어 있지 않음·attempt1/redirect0을 확인했다. 본문 텍스트는 보고서에 복사하지 않았다.

| 공고 / 형식 | byte | 실제 binary SHA256 |
|---|---:|---|
| 51022 PDF | 1,242,737 | `1dc0fd0deeb1d892bb125ec567c71bc3111fb9ecf861f52e7107c6877ec88fdb` |
| 51022 HWP | 141,824 | `6dd8d582a0c8d6cbe2f22376002d737eb15aab98a28ac0e8f612bdc1e1837fb4` |
| 52145 HWP | 250,880 | `c3488feba7f7b3addafb0e6037be47f1e34193e8137769b514ed2453bd27e98c` |
| 51075 HWP | 124,416 | `f400d97b469c0473a78d10a91ec0d9bc2956d0ecbba2df6546a8191564b729ad` |

프로필 지문은 `762db71421004b6f7689ef5cfcd9cd45e595e85219886292d83559949005d162`다. 3건 모두 고정 제목 일치·FOUND·전체2/1/1파일·경고0·UNKNOWN 역할 유지·임시 파일 정리를 확인했다. 실제 다운로드 경로 상세3+파일4=7요청/1,784,433예약byte이며 파일 합계1,759,857byte다. 서명 검사는 파일 형식만 증명하며 내용이 완전하다는 뜻은 아니다.

사전 상세3요청/3MiB와 본문 보수 예약6요청/12MiB를 더한 **현재 원장16/60요청·17,513,073/100,663,296예약byte**, 잔여44요청/83,150,223byte다. 본문 실제 attempt는 총3이지만 후속 누적 계산에서는 예약6을 해제하지 않는다. 다음 실행은 이 원장에서 차감해야 하며 반복 실행으로 초기화하지 않는다. 영수증은 `build/reports/attachment-profile-discovery-qa/LOCAL_DAEGU_DALSEONG_GET_V1-{51022,52145,51075}.json`과 `build/test-results/attachmentProfileDiscoveryQa/TEST-*.xml`이다.

후속 공통 관측기의 달성 profile 예산도 진단 flag와 무관하게 공고별6요청/24MiB로 고정했다. 3공고 최대18요청/72MiB는 현재 원장과 합쳐34요청/93,010,545byte로 한도 안이다. 이는 실행을 완료했다는 뜻이 아니며 서울 전송/판정기와 누적 원장 대조를 연결한 뒤 수행해야 한다.

`provider-qa-catalog-v2.json`에 공식 source identity3건만 추가했다. catalogVersion=`2026-09-25-dalseong-support-references-v2`,36참조/12기관/기대값1/정상0이다. 신규3건 expectation은 null이고 기존33건을 JSON 전체 비교로 보존했다. 등록7엔진/19프로필은 그대로이며 참조 없는 등록 프로필8→7이다. catalog/코드 지문이 바뀌므로 과거 QA snapshot을 현재 성공으로 재사용하지 않는다. API·V1~V85 migration·운영 설치 catalog는 변경하지 않았다.

## 서울 격리 추출 결과 — 2026-09-25 01:04~01:05 KST

- 실행 `1c25284886454085b39e272799932bd7`, SSM `7f20c102-42e2-4ac8-8af1-ac718c225a5d`, `DALSEONG_OBSERVATION` 1회. 63초, 관측 시험3/3·실패/생략/중단0, SSM Success/exit0이다. 관측 계약 통과이지 완전 추출·정상 후보·정책 QA 통과가 아니다.
- 실행 코드 지문 `927818e19548730dcb682b615b65f328690a9faadb77d08da1162799ee75dee3`, probe `6ebfa5c0938575ab48f5d41186a8d87b6b7a735f858b3a37f6c98a517f87784e`, archive `d62b33ed6825ab8079fab48631ccc5e08473f1c39b9862ddff3a13fed191e087`이다. 전송은138파일/188,238,744byte이며 다운로드 공고 원문을 포함하지 않았다.
- 제목3건 COMBINATION_MATCHED, 본문3건 AVAILABLE/완료/attempt1/redirect0, 공식 첨부 발견3건 FOUND/완료와 전체2/1/1파일을 확인했다. 본문 길이는97/132/395자다. 4개 binary는 위 로컬 다운로드 SHA256과 동일하며 추출기1.0.7이다.

| 공고 / 파일 | 추출 문자 / 블록 | 품질 | 확인된 HWP 부분 사유 |
|---|---:|---|---|
| 51022 PDF | 1,629 / 21 | PARTIAL_TEXT | 해당 없음; PDF 세부 원인은 이번 metadata로 확정하지 않음 |
| 51022 HWP | 8,993 / 347 | PARTIAL_TEXT | UNSUPPORTED_RECORD 2, UNSUPPORTED_CONTROL 3, TABLE_CONTROL_HEADER 19 |
| 52145 HWP | 6,703 / 321 | PARTIAL_TEXT | UNSUPPORTED_RECORD 5, UNSUPPORTED_CONTROL 2 |
| 51075 HWP | 2,294 / 95 | PARTIAL_TEXT | UNSUPPORTED_RECORD 6, UNSUPPORTED_CONTROL 3 |

3공고 모두 `REVIEW_REQUIRED / ATTACHMENT_INCOMPLETE`, wholeText=false, `ATTACHMENT_ROLE_UNKNOWN / ATTACHMENT_TEXT_INCOMPLETE`, 관리자 최종 검증 요구를 유지한다. 규칙은 임시 DB 초안 seed이며 운영 규칙 적용 증거가 아니다. 지원대상·형태는 관측값이고 검토된 기대값은 여전히 없다. 실제 상시 worker의 저장·API·관리자 DRAFT는 이번 관측으로 검증하지 않았다.

신규 보수적 요청 예약은5+4+4=13회, 예약byte는3,489,905+2,356,224+2,229,760=8,075,889다. 이전 원장16회/17,513,073byte를 유지하여 **누적29/60회·25,588,962/100,663,296byte, 잔여31회·75,074,334byte**다. 새 실행은 이 원장을 차감해야 한다. 이전16회 기준의 전송 guard는 이번 실행 이후 재사용하지 않는다.

원격 unit 비활성·probe/transport 임시 원본과 자원 정리, 운영 DB 미사용/쓰기0, 설치 JAR 불변·health UP을 확인했다. 설치 JAR 지문은 `12985f8cd4d710f24da85d2f82c9556d719264aef5f43ac1a57239687bc9c2bc`다. S3 자기 객체 삭제·plan.cleaned=true를 확인하고 exact path/hash를 검증한 로컬 package.zip만 삭제했다. 재생성 가능한 전송본이며 plan/result 영수증과 사용자 미추적 파일은 보존했다. 운영 설치·정책·worker·DB·배포 변경은 없다.

이번 증분 검증 `:test --tests '*AnnouncementAttachmentBbsObservationProbeTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1`은1분44초 성공했다. Java54(32+22)·패키지20건 실패/오류/생략0, Node4·Python24건 통과다. probe JAR는 새로 생성했고 변경 없는 본체/bootJar는 UP-TO-DATE다. 선행3f4e6d7 [Linux36023716864](https://github.com/FrostyCityMan/saneB/actions/runs/36023716864)는 마지막 조회 in_progress로 새 증분 CI 성공을 의미하지 않는다.

## 실행 준비 및 선행 검증 기록

### 서울 격리 실행 계약 연결

`DALSEONG_OBSERVATION` 명시 모드를 Java probe·Bash·임시 Python runner와 로컬 패키지/전송 도구에 연결했다. 고정3공고·전체4파일·선행 binary4개 지문·profile·기간·본문/발견 완료·형식·추출 버전·구간 지문을 검사한다. 부분 추출도 관측 결과로 남기되 wholeText=false/REVIEW_REQUIRED를 강제하며 정책·기대값 승인과 운영 쓰기는 금지한다. 기본/다른 지역 모드·기존 예산은 바꾸지 않는다.

전송 전 누적16요청/17,513,073byte를 로컬 다운로드 영수증과 본문 JUnit3건으로 재검증한다. 동일 plan 재전송과 이미 전송된 달성 plan이 있는 새 실행을 거부한다. 최대18요청/72MiB/20분·CPU1·메모리768MiB·임시공간1GiB이며 운영 설치/환경/DB는 공유하지 않는다. 기존 공개 요청은 profile/DNS/TLS 경계, 실제 파일 추출은 별도 network namespace 제한을 사용한다.

첫 전송 시도는 JUnit XML의 method 이름을 기대한 로컬 선행 검증에서 중단됐다. 실제 XML의 고정 한글 표시명3건과 정확히 비교하도록 고쳤다. 이 시도는 uploaded=false/commandId=null이므로 S3 전송·SSM 실행·공고 요청이 없었다. 이와 별도로 수정 중 PowerShell 줄바꿈 구문 오류는 실행 전에 발견·수정했고 재검사0오류다. 실패를 정상 실행이나 외부 사용량으로 기록하지 않는다.

표적 계약 검증 `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`은2분6초 성공했다. Java113·패키지20건 실패/오류/생략0, 새 catalog를 포함한 bootJar 생성 완료다. 제목 차용/중복/중첩 구조를 거부하고 source identity·제목 선행 규칙·기존 정상 표본 부족·QA snapshot 계약을 검증했다. 이는 Linux 공식 파일 추출이나 운영 적용 증거가 아니다.

확대 `:test --tests 'com.saneb.domain.announcementattachment.*' :bootJar --no-daemon --max-workers=1`은3분20초 성공,1983건=1960통과/23조건부 생략·실패/오류0이다. 이 확대 실행 이후의 달성 관측 예산 제한 증분은 별도 표적 재검증으로 구분한다. `node --test scripts/qa/attachment-bbs-observation-probe.test.mjs`4건 통과·생략0, `git diff --check`도 통과했다. 확대 실행의 bootJar는 앞선 생성물 UP-TO-DATE다.

최종 예산 제한 증분은 `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --no-daemon --max-workers=1`로1분39초·22건 통과/실패/오류/생략0을 확인했다. 당시 소유 단기 Node와 Gradle 실행은 종료했고 workspace Node/Java/PG 잔여0이다. 선행 `bd68cfa` [Linux36021816332](https://github.com/FrostyCityMan/saneB/actions/runs/36021816332)는 success이며 달성 변경의 새 CI와 구분한다. 이 선행 단계에서는 서울 호출·운영 상태 재조회·운영 배포를 실행하지 않았고, 후속 서울 실측은 위 별도 절에 기록했다.

## 남은 업무

- [x] 직접 제목·파일 전체 목록 확인, 기존 transport로 다운로드/signature 및 본문 수집 검증.
- [x] 서울 제한 격리에서 본문3건·전체4파일 추출과 부분 품질/검수 유지·임시 자원 정리 확인.
- [ ] PDF 부분 원인·HWP 미지원 구조 개선, 실제 텍스트·구간/역할 검토, worker·DB/API와 관리자 검수/DRAFT.
- [ ] 검토된 기대값과 전체 Provider coverage. 자동 승인하지 않는다.

브라우저 검증은 현재 명시 요청 정책에 따라 미실행이다.
