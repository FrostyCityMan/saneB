# 전체 Provider QA 실행 증거 판정

## 목표와 범위

Gate3/6 진행이다. V79/V80 분할 원장의 실제 case 결과를 현재 전체 catalog/정책 snapshot과 대조해 PROVIDER_PROFILES를 판정한다. 제목/본문·파일 텍스트를 감사 근거에 복사하지 않는다. 고정 공고 기대값, 실제 실행 원장, 정책 전체 QA, 게시 승인은 서로 다른 단계다.

- [~] 전체 현재 계획·분할·항목 원장 조회 및 순서/분모 검증 구현. 실제 PostgreSQL 검증은 미실행.
- [~] 결과 JSON/지문·형식별 품질·원본 정리·요청/byte/시각 대조 구현 및 표적 단위 검증 통과.
- [~] 정책 QA 단계 연결과 게시 시 같은 불변 근거·최신 분할 시도 재검증 구현. V81 실제 DB 검증은 미실행.
- [ ] 단위/Mapper/실제 PostgreSQL 시험과 운영 Linux 전체 표본 실행.

## 판정 계약

현재 snapshot6의 전체 요구 목록과 classpath catalog를 다시 계산한다. 전체 target에 시스템 binding, 정상 공고3건 이상, PDF/HWP/HWPX 기대값 coverage가 있어야 한다. 모든 catalog case가 실행 준비 상태여야 하며 일부 분할/참조만 있으면 MISSING이다. 기대값 준비 여부와 실제 실행 성공을 혼동하지 않는다.

같은 정책/snapshot/catalog/plan의 분할별 최신 시도를 전체 상태에서 선택한다. 최신 실행이 실패·취소·대기이면 과거 COMPLETED를 대신 사용하지 않는다. 모든 분할 번호가 한 번씩 있어야 하며 각 run의 전체 분모/고정 버전·코드/runtime/분할 상한과 case 순서·입력/profile/예상 파일 수가 현재 계획과 같아야 한다.

DB 결과는 고정 크기로 페이지 조회하되 count와 전체 순번을 모두 대조한다. LIMIT으로 검증 분모를 줄이지 않는다. 각 case는 PASSED·증거 hash 일치·원본 정리가 필요하다. JSON의 필드/자료형/최대 크기를 검사하고, 실제 파일별 locator/binary/format/quality·textHash/문자·블록 수를 고정 기대값과 대조한다. 파일 목록의 순서/개수·미실행·부분 품질을 보존한다. 음성 사례 기대 동작의 PASSED와 정상 텍스트 coverage는 구분한다.

case/run의 실제 누적 요청/byte를 결과와 재대조하며 합계를 정확히 비교한다. 시작/완료는 원장 소유 실행 시간과 부모 유효기간 안이어야 하고 미래 시각은 거부한다. 제목 제외는 상세/첨부 요청·다운로드0을 요구한다. 긍정 사례의 최소 문구 내용은 실제 CaseExecutor가 검사하고, 재검증은 불변 입력 지문·실제 출력 textHash 및 최소 분량으로 그 실행 증거를 결합한다.

완료 근거는 정책 QA run/snapshotHash, catalog/plan/code/runtime 지문, 전체 target/case/file 수, 정렬된 분할별 run ID와 전체 항목 근거 digest로 구성한다. 원문/파일명/URL/기대 문구를 넣지 않으며32KiB 이내다. 저장된 전체 근거도 현재 DB 원장과 재대조해야 한다. 외부 제출 JSON으로 성공을 만들 수 없다.

## 동시성·게시 경계

전체 파일/설치 검증은 게시 transaction 밖에서 수행한다. 게시 직전에는 기존 잠금과 함께 Provider run/case/승인 계획도 잠그고, 각 분할의 최신 시도 ID/상태·불변 metadata가 검증 당시와 같음을 확인해야 한다. 새 실패/대기 실행이 발생했다면 이전 근거로 게시하지 않는다. 이 연결이 완성되기 전에는 정책 전체 VERIFIED로 전환하지 않는다.

실제 Linux/Provider 실행·전체 운영 대상·현재 외부 환경은 별도 검증이다. 테스트 대역/합성 DB 근거로 운영 PASSED나 정책 게시를 주장하지 않는다.

## 현재 구현 경계

`AttachmentProviderQaStoredResultVerifier`는 현재 규칙의 제목 판정, 전체 입력 hash, DB case 소속·상태·버전, 원본 정리, 결과 JSON의 정확한 필드/자료형/hash, 전체 파일별 metadata·품질/분량과 누적 예산·실행 시간을 검증한다. 제목 제외의 discoveredFileCount=-1은 미요청을 뜻하며0개 발견으로 바꾸지 않는다. 음성 품질의 기대 일치는 정상 텍스트 coverage가 아니다.

`AttachmentProviderQaEvidenceGate`는 snapshot6/catalog를 다시 준비하고, 전체 기대 coverage가 없으면 DB 성공 조회 없이 MISSING을 반환한다. 완전한 계획에 대해서만 REPEATABLE READ/10초·페이지100개 단위로 분할별 최신 시도와 전체 case를 읽는다. 같은 count의 항목 교체/중복·누락·순서·원장의 required_scope_json도 대조한다. 결과는 전체 분모와 정렬된 분할 ID·case metadata digest이며 원문/URL/기대 문구는 없다.

각 case는 해당 정책 QA 시작 전에 완료된 원장이어야 한다. 분할 번호순 실행을 강제하지 않으며 관리자의 일부 분할 재검증/역순 실행을 허용한다. 전역 중첩 검사는 실제 생성 시각순으로 수행한다. 동일 분할의 최신 시도가 실패/취소/대기이면 이전 성공을 사용하지 않는다.

2026-09-14 정책 QA worker와 게시 추가 검증기에 전체 근거 수집기를 연결했다. Provider PASSED 근거와 나머지 세 단계가 저장되어 모두 PASSED일 때만 VERIFIED를 저장한다. 기대값/분할이 누락되면 MISSING/INCOMPLETE, 잘못된 실제 근거는 FAILED, 취소는 CANCELLED다. 정책 QA 완료는 게시/ENFORCE/기존 데이터 실행이 아니다.

게시 잠금 밖에서 전체 catalog·case·파일 근거를 재검증하고 저장된 JSON·지문과 비교한다. V81은 기존18테이블 잠금 순서를 보존하고 Provider run/case/plan3테이블을 EXCLUSIVE NOWAIT에 추가한다. 잠금 안에서는 새 transaction·파일·네트워크 없이 최신 분할 run ID/상태/버전/현재성·건수/예산/완료 시각을 다시 비교한다. 완료 run/case/plan의 DB 불변 계약과 함께 재검증 이후 최신 시도 변경을 차단한다. 충돌 시 게시 영수증/정책/감사 쓰기 전 전체 rollback한다. 같은 멱등 키로 이미 게시한 요청은 기존 영수증을 반환한다.

V1~V80은 수정하지 않았다. V81은 데이터 변경/정책 활성화가 없는 잠금 함수 확장이다. 실제 DB 경합 시험은 작성했으나 아직 실행하지 못했다. 전체 근거 수집기의 합성 테스트 PASSED를 실제 정책/운영 성공으로 표시하지 않는다.

남은 요구 정합성: 현재 catalog는 모든 대상에 세 형식 기대값을 요구하지만 원래 QA 계획은 실제 출처별 해당 형식/미확인 사유를 구분한다. 이 적용성 계약과 전체 표본·기대값, 미지원 Provider 구현, 관리자 QA 화면은 별도 필수 잔여다. 기대값 없는 현 catalog를 통과시키거나 미확인을 성공으로 바꾸지 않았다.

최초 컴파일의 괄호 오류와 테스트 오버로드 인자 모호성을 수정했다. 이어진 네 표적 실패는 Mockito 대역 재설정 시 기존 Answer가 먼저 호출된 fixture 문제였으며 reset/doReturn/doAnswer로 수정했고 실제 실행 시 transaction assertion은 유지했다. 최종 회귀/산출물 수치는 최신 진행 기록을 따른다.
