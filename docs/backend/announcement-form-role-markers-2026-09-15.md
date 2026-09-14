# 양식 역할의 입력·서명 표식 보완

## 단계 / 근거

P3 실제 첨부 판정 개선이며 전체 Gate0~8/ATT001~062는 Not ready다. 태백184816의 Linux 관측(실행28, SHA5c6ff8d)은 HWPX2개 모두 COMPLETE_TEXT였지만 첫 파일은 MIXED_DOCUMENT_ROLES, 둘째 파일은 ROLE_STRUCTURE_INCOMPLETE였다. 관측 metadata는 둘째 파일의 초기 FORM 제목, 단일 신뢰 block 안의 콜론 없는 전체 줄 `성명` 표식(공백 정규화 기준), 괄호 주변을 포함할 가능성이 있는 `서명또는인` 표식을 보여준다. 원문을 읽은 것처럼 단정하지 않는다.

현재1.0.1의 APPLICANT_FIELD는 콜론을 필수로 하고 `성명` 내부 공백을 허용하지 않는다. SIGNATURE_FIELD는 괄호 없는 `서명 또는 인`으로 끝나는 줄만 인식한다. 실제 파일의 행별 정확한 보완 효과는 새 Linux 관측으로 확인해야 한다. 첫 파일의 혼합 역할은 정상 검수 이유이므로 완화하지 않는다.

## 구현 명세

- 규칙 버전을 `document-role-1.0.2`로 올리고 규칙 지문을 새로 계산한다. 기존1.0.1 정책/실행/근거를 재해석하거나 갱신하지 않는다. 새 정책의 수정·QA·게시 승인 전에는 운영에 적용하지 않는다.
- APPLICANT_FIELD는 단일 줄 전체가 `신청인`, `사업자등록번호`, `성명` 중 하나인 입력 표제도 인정한다. 단어 내부의 수평 공백은 허용한다. 기존 콜론+값 형식은 유지하되 임의 설명 문장·복합 항목·다른 단어의 부분 일치는 허용하지 않는다.
- SIGNATURE_FIELD는 기존 표식에 균형 잡힌 `(서명 또는 인)` 종료 표식만 추가한다. 수평 공백은 허용하되 표식 뒤 설명 문장이 이어지거나 줄/block 경계를 넘는 문자열을 합성하지 않는다.
- FORM은 여전히 초기3개 비어 있지 않은 줄/앞600 code point 안의 FORM 제목, 입력 표식, 서명 표식 전부를 요구한다. COMPLETE_TEXT·전체 유효 block·위치/지문·상한·같은 파일 근거 검증을 유지한다.
- NOTICE/GUIDE/REFERENCE 조건, 혼합 문서 UNKNOWN, 불완전/OCR/불확실 구조, MANUAL/PROFILE 보존, 제목 제외·BODY A/B 후 첨부 진행·자동 ACTIVE 금지는 변경하지 않는다.

## DB/API 영향

V82는 규칙 버전/지문을 문자열·SHA로 고정하며 기존 APPLICANT_FIELD/SIGNATURE_FIELD 코드와3개 FORM 근거를 이미 지원한다. 새 DDL이나 과거 migration 수정은 필요하지 않다. v2 shape 및 v1 계약도 그대로다. 기존버전 snapshot은 현재 규칙 아님으로 거부하고, 역할 규칙이 없는 legacy snapshot은 자동 판정하지 않는다. 새 정책 초안 생성/수정은 서버 현재 버전을 기록한다.

## 검증 체크리스트

- [x] 양식 표제의 공백/콜론 유무, 괄호형 서명 양성·문장/부분 일치/누락 음성을 검증했다. 역할52·저장 규칙7·종합 분류19·관측 계약5·정책27·snapshot12, 총122건/생략0이49초에 통과했다.
- [x] 실제 code point/block 근거와 경계 분리·혼합/불완전/수동 역할·과거 버전 보존의 로컬 회귀를 통과했다. 새 worker→DB→v2 API FORM 시험은 구현했으며 실제 Linux/PG 실행 통과와 구분한다.
- [x] 전체 로컬 회귀4분6초 성공: root2550=2294통과/256조건부 생략/실패0, QA 패키지20/20. 초기 표적 실행에서 bootJar를 생성했고 마지막 전체 실행의 bootJar/extractor시험은 UP-TO-DATE다. 실제 Linux/공식 파일 시험의 로컬 생략은 통과로 계산하지 않는다.
- [ ] 같은 SHA의 Linux DB/worker와 태백 전체 첨부를 재관측한다. 둘째 파일 FORM 여부는 실행 결과로만 확정한다.
- [ ] 실제 파일 기대값을 검토한다. 한 양식의 역할 인식 개선은 전체 공고 자동 검수 완료/검수 감소율의 증거가 아니다.

새 worker 통합 시험은 합성 HWPX의 실제 격리 parser 출력을 사용한다. 파일명 `공고.hwpx`와 무관하게 FORM3근거를 DB에 저장하고 v2로 조회하며, 본문 없는 상황에서 양식 키워드를 공고 후보의 주된 근거로 승격하지 않아야 한다. 기존 base·원문 link0·요청/추출 횟수·임시 정리를 확인한다. 실제 공식 파일 검증으로 세지 않는다.

첫 전체 회귀는 이전 관측 시험의 `콜론 없는 양식=UNKNOWN` 기대값1건에서 실패했다. 새 명세에 해당하는 FORM/버전 확인으로 수정했고, canary/원문/locator 비노출·signal 위치 검증은 보존했다. 최종 전체 회귀와 새 SHA의 Linux 결과는 [장기 진행 기록](announcement-attachment-end-to-end-progress-2026-09-09.md)을 따른다.

```powershell
.\gradlew.bat :test --tests '*AttachmentDocumentRoleClassifierTest' --tests '*AttachmentFileRoleRulesTest' --tests '*AnnouncementAttachmentClassificationEngineTest' --tests '*AnnouncementAttachmentBbsOfficialObservationContractTest' --tests '*AttachmentProviderQaCaseContractTest' --tests '*AttachmentPolicyValidationSnapshotFactoryTest' --tests '*AnnouncementAttachmentPolicyServiceTest' bootJar --no-daemon --max-workers=1
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa --no-daemon --max-workers=1
```

현재 catalog는 공개 참조18건·실행 기대값0건이다. 이번 규칙 보완만으로 실제 기대값을 승인하거나 운영 정책을 변경하지 않는다.
