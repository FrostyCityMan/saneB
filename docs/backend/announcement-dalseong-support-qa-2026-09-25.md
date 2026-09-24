# 달성군 지원사업 고정 표본 QA — 2026-09-25

## 범위와 현재 기준

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

## 미완료

표적 계약 검증 `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentProviderQaCatalogTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*SaeolGetAttachmentDiscoveryProfileTest' :attachmentContractQaTest :bootJar --no-daemon --max-workers=1`은2분6초 성공했다. Java113·패키지20건 실패/오류/생략0, 새 catalog를 포함한 bootJar 생성 완료다. 제목 차용/중복/중첩 구조를 거부하고 source identity·제목 선행 규칙·기존 정상 표본 부족·QA snapshot 계약을 검증했다. 이는 Linux 공식 파일 추출이나 운영 적용 증거가 아니다.

확대 `:test --tests 'com.saneb.domain.announcementattachment.*' :bootJar --no-daemon --max-workers=1`은3분20초 성공,1983건=1960통과/23조건부 생략·실패/오류0이다. 이 확대 실행 이후의 달성 관측 예산 제한 증분은 별도 표적 재검증으로 구분한다. `node --test scripts/qa/attachment-bbs-observation-probe.test.mjs`4건 통과·생략0, `git diff --check`도 통과했다. 확대 실행의 bootJar는 앞선 생성물 UP-TO-DATE다.

최종 예산 제한 증분은 `:test --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --no-daemon --max-workers=1`로1분39초·22건 통과/실패/오류/생략0을 확인했다. 소유 단기 Node와 Gradle 실행은 종료했고 workspace Node/Java/PG 잔여0이다. 선행 `bd68cfa` [Linux36021816332](https://github.com/FrostyCityMan/saneB/actions/runs/36021816332)는 success이며 이번 달성 변경의 새 CI와 구분한다. 이번 서울 호출·운영 상태 재조회·운영 배포는 실행하지 않았다.

- [x] 직접 제목·파일 전체 목록 확인, 기존 transport로 다운로드/signature 및 본문 수집 검증.
- [ ] 실제 텍스트·구간/역할 검토, 서울 제한 격리 추출, worker·DB/API와 관리자 검수/DRAFT.
- [ ] 검토된 기대값과 전체 Provider coverage. 자동 승인하지 않는다.

브라우저 검증은 현재 명시 요청 정책에 따라 미실행이다.
