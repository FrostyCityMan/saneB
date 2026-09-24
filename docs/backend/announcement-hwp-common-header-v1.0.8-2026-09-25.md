# HWP 40바이트 공통 표 헤더 지원 — 1.0.8

## 목적과 근거

달성군 51022의 HWP에서 TABLE_CONTROL_HEADER 19회가 관측되어 표 헤더 검사를 대조했다. 현재 판정은44바이트 또는46+설명문자수×2만 허용하지만, 개체 공통 필드는 ctrl ID를 포함하여40바이트이고 뒤의 확장 필드는 선택적이다.

공개 [hwplib 읽기 구현](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/reader/bodytext/paragraph/control/gso/part/ForCtrlHeaderGso.java)은 instance ID 이후 레코드 종료를 허용한다. [쓰기 구현](https://github.com/neolord0/hwplib/blob/6746c27f17ebf5277493206284aa32044a1839c4/src/main/java/kr/dogfoot/hwplib/writer/bodytext/paragraph/control/gso/part/ForCtrlHeaderGso.java)의 필드 크기와도 대조했다. 해당 구현을 복사하거나 새 의존성을 도입하지 않는다.

이 근거는40바이트 형식의 지원 누락을 증명한다. 달성군 실파일19개가 그 형식인지는 새 격리 실행으로 별도 검증해야 하며, 원인 해결을 미리 확정하지 않는다. PDF 부분 추출이나 미지원 HWP 개체는 이번 변경으로 해결되지 않는다.

## 변경 계약

- 40바이트 TABLE 공통 헤더만 추가 허용한다. 기존44/46+설명 형식과 나머지 거부를 보존한다.
- 모든 셀의 위치·개수·span·겹침·순서·문단·문자수 검증을 유지한다. 헤더만 유효해도 불완전한 표를 완전 추출로 승격하지 않는다.
- 텍스트 순서·셀별 근거 scope·원문 보존, 미지원 record/control의 부분 품질과 관리자 검수는 유지한다.
- 추출기와 기대 runtime 버전은1.0.8이다. 이전 정책 QA 지문을 새 runtime 성공으로 재사용하지 않는다. DB/Flyway·v1/v2·역할/구간 규칙·운영 설정을 변경하지 않는다.

## 검증

- [x] 압축/비압축 OLE40바이트 표의 순서·셀별 scope,39/41/42/43/45/47바이트 거부, 잘못된 셀 geometry·미지원 record 보존을 검증했다. 추출기142건 실패/오류/생략0이다.
- [x] 1차 표적/추출기/패키지/probe/bootJar 빌드1분9초 성공. 확대 첨부 회귀는 별도로 확인한다.
- [x] 확대 `:test --tests 'com.saneb.domain.announcementattachment.*' :attachment-extractor:test :attachmentContractQaTest :attachmentBbsObservationProbeJar :bootJar --no-daemon --max-workers=1`은3분32초 성공. root1986=1963통과/23조건부 생략,패키지20통과·실패/오류0이다. 추출기142건과 bootJar는1차의 동일 소스 결과를 재사용(UP-TO-DATE)했다. Node4·Python24건 통과, PowerShell 구문0오류·diff 검사 통과다.
- [x] 달성군 고정 동일 파일의 새 격리 실행으로 실제 영향을 대조했다. **헤더 오류19회 및 부분 추출은 변하지 않았다.**
- [ ] 전체 Provider 기대값·상시 worker·DB/API·동일SHA 운영 및 관리자 업무 E2E.

전체9Gate=8부분/1차단이다. 로컬 호환성 수정만으로 완전 추출·정상 후보·운영 반영을 선언하지 않는다.

## 실제 파일 비교 범위

기존3공고·전체4파일을 그대로 사용한다. 제목·본문·binary 지문이 달라지면 같은 입력 비교로 간주하지 않는다. 누적29회/25,588,962byte에서 후속 최대18회/69MiB를 예약한다. 공고별 요청6회는 유지하고 byte 상한만24→23MiB로 강화하여 합계 최대97,940,706byte가 전체96MiB 안에 있도록 한다. 한도 초과를 우회하거나 이전 사용량을 해제하지 않는다. 다른 기관 상한은 변경하지 않는다.

전송 전 이전 서울 SSM 결과를 다시 조회하여 성공·정리·동일3사례와 실제 누적값을 확인한다. 새 code/probe 지문을 요구하고 미등록 실행이 있으면 차단한다. 이번 변경은 정책 게시·자동 활성화·운영 기존 데이터 재분류 승인이 아니다.

## 서울 실파일 비교 결과

- 실행 `eee537febbe04841afbed1c53bb5fa27`, SSM `69531e08-a286-4569-9a1d-e268b97e29b9`,55.463초·관측3/3·실패/생략/중단0,Success/exit0이다. 추출기 버전1.0.8을 확인했다.
- 코드 `277718e5e2723c56713f3f2bff23f795019fc06109701ff45cca25d6d065489f`, probe `cb84280d330642016ccfab911d339b54f34fe46b881c22bdf3eb0b240885fc90`, archive `55b31a91b1596e3535931d253bc9057e2ac6d109276a3b7ba1e20ad9655cd626`이다.
- 이전1.0.7 영수증과 새1.0.8 영수증을 직접 비교하여3건의 body/profile/rules 지문·판정,4개 파일의 binary/text/locator 지문·문자/블록 수·품질이 모두 같은 것을 확인했다. 새 완전 추출0·정상 후보0,3건 모두 REVIEW_REQUIRED/ATTACHMENT_INCOMPLETE/wholeText=false다.
- 51022의 TABLE_CONTROL_HEADER19회는 그대로다.40바이트 형식 누락은 수정했으나 이 표본의 원인이 아니었다. 남은 실제 헤더 길이·확장 구조는 미확정이다. 오류를 없애기 위한 임의 길이 허용은 하지 않았으며 동일 입력 재실행을 반복하지 않는다.
- 신규13요청/8,075,889예약byte를 더한 **누적42/60요청·33,664,851/100,663,296byte**, 잔여18회/66,998,445byte다. 현재 전체3건 실행 상한69MiB는 잔여byte를 초과하므로 그대로 재실행할 수 없다. 다음은 원인 중심의 고정 단일 표본 진단을 먼저 설계하고 이 원장을 적용한다.
- 운영 DB 미사용·쓰기0, 운영 JAR 불변·health UP, 원격 unit 비활성·probe/transport 임시 자원 정리를 확인했다. 운영 정책·worker·데이터·배포는 변경하지 않았다. 최종 관리자 확인 요구와 정책/기대값 미승인을 유지한다.
- S3 자기 전송 객체와 plan.cleaned=true를 확인한 뒤 exact path/hash로 검증한 로컬 package.zip만 삭제했다. 재생성 가능한 전송본이며 plan/result 영수증과 사용자 미추적 파일은 보존했다. 소유 Node/Java/PG 잔여0이다.

다음 작업은 달성51022의 미지원 표 헤더 구조와 미지원 control의 비식별 수치 진단, PDF 부분 원인 식별이다. 검토된 기대값·상시 worker/DB/API·운영 E2E는 별도 미완료다. 브라우저는 현재 사용자 명시 요청 정책상 미실행이다.
