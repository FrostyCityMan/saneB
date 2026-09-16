# 첨부 DNS 조회 실패와 보안 차단의 분리

## 원인과 범위

보은의 이전 Linux 관측은 BODY `DNS_LOOKUP_FAILED`였으며 첨부 발견의 별도 원인 코드는 없었다. [보은 관측 기록](announcement-boeun-worker-db-api-qa-2026-09-16.md)을 따른다. 이번 수정은 그 실패의 근본 원인을 확정한 것이 아니라, 실패 경로를 조사하다 코드와 합성 회귀로 확인한 별도 결함을 해결한다.

`AttachmentPinnedDownloadClient.selectTarget`은 비동기 URL/DNS 검증의 모든 `ExecutionException`을 `ATTACHMENT_URL_BLOCKED`로 바꿨다. 따라서 JDK `UnknownHostException` 또는 DNS 주소 없음도 worker의 재시도 불가 `DOWNLOAD_BLOCKED`로 합쳐졌다. 일시적인 조회 실패가 보안 정책 위반으로 잘못 표시되고 기존 제한 재시도를 사용하지 못하는 문제다.

## 최소 변경과 불변조건

- 원인이 검증기의 고정 `DNS_LOOKUP_FAILED`일 때만 `ATTACHMENT_DNS_LOOKUP_FAILED`를 반환한다. worker는 이를 기존 `NETWORK_UNAVAILABLE`로 처리한다. 오류 원문/원인 객체는 전달하지 않는다.
- 사설/loopback 주소·미승인 host/path·잘못된 URL·예상 밖 resolver 오류는 계속 차단한다. 사설 주소를 일시 오류로 승격하거나 HTTP로 진행하지 않는다.
- DNS3초/논리 다운로드30초 deadline, DNS thread2개·대기열 없음, IP 고정·TLS·redirect·다운로드 승인 범위는 변경하지 않는다. DNS 서버·JVM hosts/캐시 설정도 변경하지 않는다.
- 기존 작업 재시도 최대3회를 사용한다. 저장 계층의 첫 실패60초·둘째300초와0~10초 jitter, lease/CAS·누적 예산 비환급을 유지한다. 새 즉시 재시도 루프를 추가하지 않는다.
- 마지막 발견 실패는 발견 불완전·파일0·네트워크 실패 근거를 남기며 정상 후보로 바꾸지 않는다. worker의 EVALUATED가 완전 수집을 뜻하지 않는다.
- 본문 수집기의 초기 DNS/시도 횟수 계약은 이번에 변경하지 않는다. 제목→본문→첨부→관리자 순서, v1 API·기존 DB 실패 enum·migration·정책/키워드도 그대로다. 운영 배포·worker 활성화·기존 데이터 실행은 별도다.

## 검증과 결과

- 변경 전: UnknownHostException·빈 주소 배열·null 주소 배열의3사례가 기대한 DNS 오류 대신 URL 차단을 반환하여 실패했다. 당시 표적40건 중3실패이며 네트워크는 합성이다.
- 변경 후: 다운로드7·worker33·URL 검증5·관측 오류 보호9의 총54건 실패/생략0,43초 성공이다. HTTP 호출0·원본 파일0, 예상 밖 오류의 차단/원문 비노출, worker1/2회 재시도와3회째 불완전 근거 보존을 확인했다.
- 첫 전체 회귀는6분12초 실패다. XML2729=2463통과/265조건부 생략/실패1이며, 실패는 `AttachmentProviderQaCatalogTest.packagedRevalidatedExpectationKeepsWholeSetAndDoesNotCompleteCoverage`의 고정 기대값/현행 프로필 지문 대조다. 보관 기대값8a93cf41…와 전송 코드 변경 후627d3f60…가 다르다. 해당 assertion과 배포 catalog JSON은 수정하지 않았다. 실패 XML을 `build/qa-results/local-dns-retry-catalog-failure-20260916/`에 보존했다.
- 기존 태백 기대값1건은 보관하되 현행 실행 입력은 PROFILE_CHANGED/0건이다. 성공률·정상 공고 수를 유지하려고 기대값 지문을 자동 치환하지 않는다. 새 SHA의 실제 태백184816/HWPX2개 관측→기존 binary/text/역할/위치 기대값 대조→검토된 입력 갱신→고정 기대값 재실행과 전체 회귀가 필요하다.
- 이를 위해 QA 브랜치의 명시적 `[taebaek-revalidation-observation]` 표식만 고정1공고를 전체 계약 검사 전에 관측한다. 기존 44요청/80MiB 및 추출 격리를 사용하고, 원문 없는 metadata/JUnit만 보관한다. 전체 계약 테스트/실패 판정은 그대로 실행하며 관측 성공으로 catalog 회귀 실패를 무시하지 않는다. 테스트를 제외하거나 정상으로 바꾸는 경로가 아니다.
- Node 전체396건 중393통과/Windows에서 Linux 전용3생략/실패0이다. 후속 표적99건·패키징20건은 실패/생략0이며1분35초 빌드 성공을 확인했다. 웹 JAR/probe/추출기 설치 task는 앞선 동일 소스 산출물을 재사용(UP-TO-DATE)했다. 이 표적 성공으로 전체 회귀의 catalog1실패를 덮지 않는다. 새 SHA Linux 결과는 별도로 판정한다. 수정 전 SHA8589016의 Linux35055559668 성공과 보은35055669009 실패는 이 수정본의 검증 근거가 아니다.

```powershell
.\gradlew.bat :test :attachment-extractor:test attachmentContractQaTest bootJar installAttachmentContractQa attachmentOfficialWorkerProbeJar --no-daemon --console=plain --max-workers=1
```

전체 ATT-001~062·Gate0~8 및 출처별 정상 표본/형식/운영/브라우저 요구는 유지한다. DNS 오류 분리만으로 실파일·정책 QA 또는 출시 Gate를 통과시키지 않는다.
