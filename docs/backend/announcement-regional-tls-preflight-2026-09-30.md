# 지역별 TLS 실패 서울 서버 사전 진단 준비

## 현재 단계 / Gate

첨부 수집 연결 단계의 환경 차이 진단이다. 실제 서버 진단은 AWS 인증 만료로 실행하지 못했다. 도구 구현·단위 테스트와 외부 수집 성공을 구분한다. 기존 다운로드 집계는 162/223수집원·61잔여(미연결34+등록 미확인27)로 유지한다.

- [x] 성남·속초·의성 파일 호스트만 허용하는 TLS 전용 진단기 구현
- [x] 신규 Python7개, 기존 runner37개·철원8개 회귀 검증
- [x] Node10개 실행 경계 검증,297영수증/238공고 재현 확인
- [x] CI에는 네트워크를 mock한 단위 테스트만 추가
- [!] AWS EC2 RequestExpired / SSM ExpiredTokenException
- [ ] 로그인 갱신 후 대상 태그·리전·SSM Online 재확인
- [ ] 서버 TLS 진단 및 결과에 따른 Java 수집 전용 격리 QA
- [ ] 첨부 추출·정책 QA·운영 E2E

## 구현 범위

`scripts/qa/probe-regional-tls.py`는 아래 고정 대상만 받는다.

| 대상 | 수집원 | 호스트 | 목적 |
|---|---|---|---|
| SEONGNAM | LGS-000089 | eminwon.seongnam.go.kr | 본문·상세 TLS 실패 대조 |
| SOKCHO | LGS-000122 | www.sokcho.go.kr | 상세 TLS 실패 대조 |
| UISEONG_FILE | LGS-000211 | eminwon.uiseong.go.kr | 파일 호스트 TLS 실패 대조 |

IPv4 DNS 결과 전체가 공개 주소인지 확인하고, 사설·loopback·link-local·예약·multicast 주소가 하나라도 있으면 연결하지 않는다. 고정 기관당 주소1개에 TCP 연결1회만 시도하며 인증서·hostname 검증을 켠 기본 SSL context와 SNI를 사용한다. 다른 주소나 HTTP로 fallback하지 않는다.

HTTP 요청·다운로드·본문 파싱·DB 요청·운영 쓰기·원문 파일 저장은 모두0이다. TLS 연결 성공도 `TLS_CONNECTED_NOT_COLLECTION_VERIFIED`이며 수집 성공을 뜻하지 않는다. 출력은 단계·오류 타입·정수 인증서 검증 코드·프로토콜·암호군·소요 시간뿐이다. 인증서·예외 메시지·DNS IP·인증정보를 출력하지 않는다.

Linux 실행 시 CPU1개, 주소 공간128MiB, CPU시간5초, 기관당 alarm10초를 적용한다. 3기관 계획 시간은30초다. 실제 원격 실행에는 별도로 프로세스 전체35초 timeout과5초 강제 종료 한도를 적용해야 한다. 단위 테스트는 자원 제한 호출을 mock하여 인자와 실행 순서를 검증했으며 실제 Linux 커널 집행은 아직 미검증이다.

## 인증 및 운영 경계

직전 회차에서는 기존 공개 CA bundle을 명령 범위에 적용해 AWS 조회가 성공했다. 이번 재확인에서는 같은 방식으로 서울 `ap-northeast-2` EC2·SSM 조회를 시도했으나 각각 RequestExpired·ExpiredTokenException으로 실패했다. 이전 성공을 현재 인증 상태로 간주하지 않는다. 로그인 갱신용 링크 발급 여부를 사용자에게 문의했다.

SSM 명령을 제출하지 않았고 서버 프로세스·설정·DB·정책·배포를 변경하지 않았다. 인증 갱신 후에는 `SanebDeployTarget=true`, 서울 리전, 실행 중인 단일 대상과 SSM Online을 다시 확인해야 한다. 이전 인스턴스 식별자를 검증 없이 실행 대상으로 사용하지 않는다.

외부 진단기는 Linux에서만 명시적으로 실행한다. CI에 추가한 것은 `test_regional_tls_probe.py`의 mock 테스트뿐이며 기본 push로 정부 사이트에 접속하지 않는다. 기존 실파일·worker QA 조건과 예산은 변경하지 않았다.

## 검증 명령 / 결과

로컬 `python` 명령은 Windows Store 실행 별칭으로 연결되어 실행되지 않았다. 설치를 시도하지 않고 Codex 번들 Python을 확인하여 아래 명령을 실행했다. Python bytecode 저장은 `-B`로 막았다.

```powershell
# $qaPython은 workspace dependency 도구가 반환한 Python 실행 파일의 절대 경로
& $qaPython -B -m unittest discover -s scripts/qa -p test_regional_tls_probe.py -v
& $qaPython -B -m unittest discover -s scripts/qa -p test_temporary_bbs_observation.py -v
& $qaPython -B -m unittest discover -s scripts/qa -p test_cheorwon_transport.py -v
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
git -c core.autocrlf=false diff --check
```

Python52개(7+37+8), Node10개 통과. 기존 수집 근거 재현도 통과했다. CI에는 기존 run 블록과 동일한 들여쓰기로 단위 테스트1행만 추가됐음을 정적 검증했다. 별도 YAML parser는 로컬 번들에 없어 전체 YAML 파싱 검증은 미실행이며 의존성을 설치하지 않았다. 서버 진단·실파일 수집은 미실행이다. Java 애플리케이션·migration·API/UI를 수정하지 않아 Java 테스트와 bootJar를 이번에는 재실행하지 않았다. 브라우저 검증은 사용자 정책상 미실행이다. 일회성 Python·Node는 종료됐으며 기존 사용자 프로세스와 미추적 산출물을 유지한다.

연제·구례 조사 HTML7개 정리 차단은 별도 미완료 항목으로 그대로 유지한다. 이 도구의 준비 완료가 해당 정리나 전체 장기 goal 완료를 뜻하지 않는다.
