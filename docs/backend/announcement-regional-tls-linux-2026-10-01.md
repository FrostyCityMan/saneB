# 성남·속초·의성 Linux TLS 원인 구분

## 현재 단계 / Gate

- [x] 기존 공개 HTTP 관측·Java 실패·미실행 서울 TLS 진단의 근거 대조
- [x] 기존 3호스트 TLS 전용 진단기에 제한된 원인 코드 추가
- [x] 로컬 mock·workflow 계약 검증
- [x] GitHub Linux 기본 TLS 진단 1회, 세 호스트 handshake alert 확인
- [x] 기본 신뢰·암호군을 유지한 TLS 1.2 비교 1회, 세 호스트 동일 실패
- [!] 실파일204/223·잔여19개 유지, 운영 E2E 미완료

기준 HEAD `5b3b93a3ace6c769159168082cb5f65f9f94511d`. 직전 회차는 본문 TIMEOUT 진단 보정과 울산 남구 검색 시간 초과 근거를 확보한 진전이다. 전 지역 첨부 발견·다운로드를 우선하고 HWP 추출 고도화는 보류한다.

## 필요한 이유와 범위

기존 성남·속초 조사에서는 공개 HTTPS 응답을 확보했으나 Java 관측은TLS_FAILED였다. 의성은 본문·첨부 발견 후 새올 파일 호스트에서TLS_FAILED였다. GitHub Linux 실제 관측에서도 이 세 실패가 재현됐다. 이천의 Windows/Java 비교와 포천의 TLS/HTTP 비교를 이 세 기관의 원인으로 일반화하지 않는다.

서울 서버에서 실행하지 못한 `probe-regional-tls.py`를 GitHub 임시 Linux runner에서 사용한다. 고정 성남 새올·속초 대표 홈페이지·의성 새올 세 호스트에 각1연결만 시도한다. 공개 IPv4 검증·주소 고정·기본 인증서 검증·hostname/SNI 검증을 유지한다. HTTP 요청·파일 다운로드·DB·정책·worker·배포·운영 쓰기는0이다. HTTP 강등·TLS 검증 해제·신뢰 저장소 수정·다른 주소 fallback은 하지 않는다.

각 기관10초, 전체 계획30초, 상위35초 timeout과5초 강제 종료 한도, CPU1개·주소공간128MiB·CPU시간5초다. 기존 진단기의 범위를 늘리지 않는다. 새로운 `[regional-tls-only-01]` 최초 push에서만 실행하고 일반 push·동일 run 재실행에서는 접속하지 않는다. 기존 전체 계약 검증의 실패는 보존한다.

기존 단계·오류 타입·정수 인증서 검증 코드에 `tlsReasonCode`를 추가한다. 알려진 OpenSSL 원인만 허용하고 미지 문자열은`TLS_REASON_UNCLASSIFIED`로 바꾼다. 인증서·IP·예외 메시지·URL·공고 원문은 저장하지 않는다. TLS 성공은 파일 수집 성공이나 운영 성공으로 계수하지 않는다. 원인에 따라 공식 경로·서버 인증서·런타임 협상을 구분하되 결과 전에 응용 코드를 수정하지 않는다.

## 검증 계획

```powershell
# $qaPython은 workspace dependency 도구에서 확인한 번들 Python
& $qaPython -B -m unittest discover -s scripts/qa -p test_regional_tls_probe.py -v
.\gradlew.bat --no-daemon :test --tests '*AttachmentContractWorkflowTest' --rerun
node --test scripts/qa/attachment-regional-linux-workflow.test.mjs scripts/qa/attachment-ulsan-namgu-survey.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.safecrlf=false diff --check
```

브라우저는 사용자 정책상 미실행이다. 신규 진단 metadata만 artifact로 보관하며 기존 영수증·프로필·Flyway는 변경하지 않는다.

로컬 Python mock8개·관련 Node15개 통과, Gradle workflow 계약20개를 실제 재실행하여20초 종료0으로 확인했다. TLS 실접속은 로컬에서 실행하지 않았다. 원격 metadata에는 UTC 관측 시각과 OpenSSL 버전도 포함하여 실행 환경을 구분한다.

## 기본 TLS 실제 결과와 추가 비교

[run36787477542](https://github.com/FrostyCityMan/saneB/actions/runs/36787477542), HEAD `ac3ab91501182801e2a5c9927c5b233143e8749e`, job `110132724674`, artifact `11129978554`의 보고서를 확인했다. `2026-09-30T22:48:34.829001+00:00`, OpenSSL3.0.2에서 세 호스트 모두 DNS/TCP 이후 TLS 단계의`SSLV3_ALERT_HANDSHAKE_FAILURE`였다. 이 OpenSSL 오류명에 포함된SSLV3는SSL3 프로토콜로 접속했다는 뜻으로 해석하지 않는다. 인증서 검증 오류로 확정하지 않으며 HTTP 요청 전 실패로 구분한다.

실제TCP3연결, HTTP·파일·운영쓰기0이다. 성남0.944초·속초0.795초·의성0.699초, 다운로드 수치204/223·잔여19개 유지. 기본 TLS 진단의 전체 계약 job은 metadata 확보 당시 실행 중이다. 직전 남구 조회 SHA의 전체 계약은 기존 화천 worker1실패(4,419개 중4,098통과·320생략)로 종료됐다.

다음은 같은3호스트에 TLS1.2의 최소·최대 버전만 고정하는 비교1회다. 기본 신뢰 저장소·인증서/hostname 검증·암호군·공개 주소 고정은 그대로다. 보안 수준 낮추기, TLS1.0/1.1, 약한 암호군 추가, 인증서 예외, HTTP 강등은 없다. 추가3연결·HTTP0, 두 진단의 누적 상한6연결이다. 실패 시 자동 fallback이나 재시도하지 않는다.

이 비교는 `.github/workflows/attachment-tls-diagnostic.yml`의 독립2분 job과 `[regional-tls12-only-01]` 최초 push에서 실행한다. CPU/메모리/35초 종료 한도를 가진 짧은 TLS 진단이 전체 DB 계약 검증의 대기열에 묶이지 않게 한다. 기존 전체 계약 workflow는 삭제·완화·취소하지 않으며 동일 SHA의 전체 검증은 별도로 남는다. TLS 결과를 수집/운영 완료로 바꾸지 않는다.

추가 비교 구현은 Python10개·workflow21개 실제 재실행(21초 종료0)을 통과했다. 기본 context·인증서 검증과 최소/최대 버전 API 의미는 [Python 공식 ssl 문서](https://docs.python.org/3/library/ssl.html#ssl.SSLContext.minimum_version)를 참고하며, 실제 런타임 버전은 관측 보고서의 값만 사용한다.

## TLS 1.2 실제 결과 및 후속 경계

[run36788064660](https://github.com/FrostyCityMan/saneB/actions/runs/36788064660), HEAD `74fa4a2815b772489048bf673f7ce82fb926f2db`, job `110134134760`, artifact `11130837257`의 결과를 확인했다. `2026-09-30T22:52:59.531784+00:00`, OpenSSL3.0.2, TLS12 mode에서 성남·속초·의성 모두`SSLV3_ALERT_HANDSHAKE_FAILURE`였다. 각각0.614초·1.233초·0.620초, TCP3연결·HTTP0·다운로드0·운영쓰기0이다.

workflow의success는 제한된 진단과 metadata 보존 성공이며 세 사이트의 TLS 성공이 아니다. 두 보고서의고정3대상·각3연결·요청0·수집 미검증 값을 다시 대조했다. 기본 협상과TLS1.2 모두 실패하여 TLS 버전 고정만으로 복구된다는 가설은 지지되지 않는다. 인증서 만료·특정 암호군 부재·방화벽 차단·원 서버 장애를 이 결과만으로 확정할 수 없다. 인증서 검증 해제·신뢰 추가·약한 암호군 활성화·HTTP 강등 없이 여기서 반복을 멈춘다.

보관 JSON:

- 기본: `build/qa-github-runs/36787477542/result.json`, SHA-256 `910867c5038b04724f0b4197ca676dec26842634fc0d45b7ac6d18dcd112f940`.
- TLS12: `build/qa-github-runs/36788064660/result.json`, SHA-256 `c6d8ae3027bc691566acb71a9d28cd3e56aba87c889148f5b1e6064a9642300b`.

누적6연결·HTTP0이다. 수집 대장686영수증/289표본·실파일204/223·잔여19개는 변경하지 않는다. 응용 전송 코드·프로필·Flyway·추출기는 그대로다. 이천이나 다른TLS 오류 지역을 이번3호스트와 동일 원인으로 묶지 않는다.

후속은 정상 공식 응답·서버/기관의 접속 조건·인증 갱신 후 서울 환경 대조 같은 새로운 근거가 있을 때 재개한다. AWS 로그인·신뢰 저장소·운영 설정을 이 진단으로 변경하지 않았다. 이번 회귀는 Python55개(10+37+8)·Node64개·workflow21개 통과이며 Java 전체/bootJar/운영/브라우저 통과로 확대하지 않는다. 기본 진단 SHA의 전체 계약 job과TLS12 SHA의 별도 전체 계약 job은 결과 기록 시점에 각각실행/대기 중이다.
