# 미수집 5개 수집원 전송 단계 진단

## 현재 단계 / Gate

지역별 첨부 수집을 우선하는 장기 goal의 후속 진단이다. 직전 회차의 평창 첫 다운로드는 검증 진전이며, 이번에는 기존 `NETWORK_ERROR`를 더 세분화했다. 다운로드 성공이나 전체 goal 완료를 선언하지 않는다.

- [x] Windows DNS·TCP·인증서/호스트 검증을 포함한 TLS 5호스트 연결 확인
- [x] Java 실제 pinned 전송 경로의 고정 상세 5건 실패 유형 확인
- [x] 포천의 Java TLS 1.2·1.3 직접 handshake 성공 확인
- [x] 포천의 Java·Windows HTTP 대조에서 응답 이전 소켓 오류 확인
- [x] 명시적 opt-in 진단 테스트 추가, 일반 실행 시 외부 진단 생략 확인
- [!] 사이트·네트워크·중간 장비 중 연결 종료 주체와 세부 원인은 미확정
- [ ] 다운로드 미확인 20개, 최신 코드 확인·재검증 186개, 운영 E2E

기준 HEAD: `37decf2ce93327a9001cc08cadbdd1335cc7dab1`. 작업 경로 `C:\PersonalProject\saneB`와 루트 AGENTS를 준수하며 `long-goal-operating-protocol`에 따라 가설·실측·수정 여부를 구분한다.

응용 전송 코드·프로필 지문·URL·정책·DB/API·Flyway·운영 설정은 변경하지 않았다. 진단 테스트와 기록만 추가했다. 외부 성공을 만들기 위한 TLS 검증 해제·인증서 신뢰 확대·사용자 에이전트 위장·프록시 우회는 하지 않았다.

## 대상과 관측

2026-10-01 01시대 로컬 Windows 관측이다. 고정 공식 상세는 기존 QA 참조와 같은 식별자를 사용한다.

| 수집원 | 고정 공고 | Windows TLS | Java pinned 상세 전송 |
|---|---:|---|---|
| 포천 LGS-000108 | 64129 | TLS 1.3 성공 | SocketException, HTTP 응답 상태 미확보 |
| 강릉 LGS-000119 | 60798 | TLS 1.2 성공 | ConnectTimeoutException, HTTP 응답 상태 미확보 |
| 충북도 LGS-000135 | 67302 | TLS 1.2 성공 | SocketException, HTTP 응답 상태 미확보 |
| 공주 LGS-000149 | 59971 | TLS 1.3 성공 | SocketException, HTTP 응답 상태 미확보 |
| 평택 LGS-000093 | 95902 | TLS 1.3 성공 | SocketException, HTTP 응답 상태 미확보 |

Java 5건에서 보고된 예외 원인 체인에는 PKIX 인증 경로 오류나 SSLHandshakeException이 없었다. 모든 TLS 문제를 배제한 것은 아니며, 위 예외 종류만으로 연결 초기화·방화벽·서버 장애를 확정하지 않는다.

### 포천 단계별 대조

1. Windows SslStream: 공개 IPv4 주소에 직접 연결하고 원래 호스트의 SNI·인증서 검증을 유지한 TLS 성공. HTTP0.
2. Java SSLSocket: 같은 공식 호스트에 TLS 1.2·1.3을 각각 명시하고 `HTTPS` endpoint identification·SNI·기본 신뢰 저장소를 유지했다. 둘 다 handshake 성공, HTTP0.
3. Java SSLSocket 위 HTTP/1.1 GET: 같은 상세 주소, 기존 `saneB-attachment-collector/1.0`, `Accept-Encoding: identity`, `Connection: close`. handshake 이후 응답 상태 행을 받기 전에 SocketException이며 `connectionReset=true`였다. 본문은 읽지 않았다.
4. Windows HttpClient: 동일 URL·User-Agent·identity·HTTP/1.1·Connection close로 요청했으나 HttpRequestException→IOException→SocketException이었다. HTTP 상태 미확보·본문 읽기0.

따라서 포천을 TLS 버전 미지원이나 Apache HttpClient만의 문제로 단정할 근거는 없다. HTTP 요청 이후 연결 종료가 재현됐지만 사이트 자체·중간 보안 장비·네트워크 중 어느 계층이 원인인지는 미확정이다. 공통 수집기를 추측으로 수정하지 않았다. 다른 4곳을 포천과 같은 원인으로 일반화하지 않는다.

Apache의 [TLS 전략 공식 API](https://hc.apache.org/components/httpcomponents-client-5.5.x/5.5.2/httpclient5/apidocs/org/apache/hc/client5/http/ssl/DefaultClientTlsStrategy.html)와 로컬 5.4.3 소스 JAR의 handshake 경로를 참고했다. [HTTP upgrade 이슈](https://issues.apache.org/jira/browse/HTTPCLIENT-2344)는 별도 HTTP 동작에 관한 자료이며 이번 HTTPS 오류의 원인으로 채택하지 않았다.

## 구현한 진단 경계

`RegionalTransportDiagnosticTest`는 `SANEB_REGIONAL_TRANSPORT_DIAGNOSTIC=true`에서만 실행한다.

- 대상 주소는 고정 5개이며 임의 URL 입력을 받지 않는다.
- 기존 공개 주소 검증과 pinned DNS 전송을 사용한다. redirect·다른 호스트·다른 요청은 거부한다.
- 상세 전송은 각 최대1회·1MiB·총15초, 전체5회·5MiB다. 실패 재시도는 없다.
- 포천 TLS 대조는 2연결·HTTP0이다. 별도 raw HTTP 대조는 최대1회·상태 행1KiB만 읽는다.
- Java opt-in 전체 실행의 상한은 HTTP 의도 요청6회·상세5MiB+상태 행1KiB다. 실제 서버 도달·wire 요청 수와 transport 호출 수를 동일시하지 않는다.
- 이번 별도 Windows HTTP 대조1회를 더하면 의도 HTTP 요청 상한7회다. Windows TLS 대조5연결은 HTTP 요청이 아니다.
- Windows DNS는 각3초, TCP3초, TLS5초; Java 직접 TCP3초·TLS/응답5초다. 단위 실행의 상위 제한도 둔다.
- Windows ROOT 지정은 해당 JVM 실행에만 적용하고 이전 설정을 복원한다. 원문·URL·헤더·예외 메시지는 출력하지 않고 고정 식별자·단계·예외 클래스·제한된 불리언만 출력한다.
- 생성 가능한 파일은 JUnit 임시 디렉터리의 `detail.bin` 한 개뿐이며 finally에서 정리한다. 원본 정리 여부를 확인한다.

## 실행 명령과 결과

```powershell
# 첫 세 명령에만 명시적 환경변수 true를 적용한 후 이전 값 복원
.\gradlew.bat --no-daemon :test --tests '*RegionalTransportDiagnosticTest'
.\gradlew.bat --no-daemon :test --tests '*RegionalTransportDiagnosticTest.selectPocheonTlsControlWithoutHttp'
.\gradlew.bat --no-daemon :test --tests '*RegionalTransportDiagnosticTest.selectPocheonRawHttpControl'

# 최종 정리 후 외부 진단 비활성 상태에서 로컬 회귀
.\gradlew.bat --no-daemon :test --tests '*RegionalTransportDiagnosticTest' --tests '*AttachmentPinnedDownloadClientTest' --tests '*PinnedProviderContentHttpTransportTest' --tests '*ProviderContentUrlValidatorTest'
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
git diff --check
```

진단을 순차 추가하여 첫 실행은 고정 상세 5개, 두 번째는 TLS2개, 세 번째는 raw HTTP1개였다. 각 진단 테스트는 제한 내 실행·오류 분리·원본 정리를 검증하여 통과했다. **진단 테스트 통과는 전송 성공이 아니다.** 최종 중복 코드 정리 후 로컬 회귀는 **20개 통과·외부 opt-in 3개 조건부 생략**, 실패0이다. 최종 일반 실행에서 외부 요청을 다시 발생시키지 않았다.

전체 Java suite·bootJar·운영 AWS·DB/API·worker·정책 게시·재분류·배포는 이번에 실행하지 않았다. 브라우저는 현재 사용자 명시 지시가 없어 정책상 미실행이다. 기존 임시 공개 CA·조사 HTML 정리 차단과 AWS 인증 대기는 별도 보류다.

## 수량과 다음 조치

새 다운로드 근거가 없으므로 수집 대장은 변경하지 않았다. **과거 다운로드 경험203/223·미확인20개**, **현재 코드37/223·신규 확인/재검증186개**, 영수증417개·최신 공고281개를 유지한다.

후속은 다른 미확인 수집원의 공식 파일 제공 경로 검토와 현재 코드 재검증이다. 이 5개 대상에는 동일한 실패 요청을 반복하지 않는다. 사이트의 정상 응답, 공식 계약 변경, 승인된 다른 실행 환경 등 새로운 근거가 생기면 단계별로 재개한다. 오류는 별도 유지하고 수집 가능한 다른 공고·파일 처리를 중단하지 않는다.
