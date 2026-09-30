# 전남7곳·김제 첨부 재검증과 AWS 인증 사전 점검

## 현재 단계 / Gate

- [x] 전남7수집원·전북 김제1수집원의 현재 코드 실파일 재검증
- [x] 정상 파일8개 보존 근거와 김제 다른 첨부의 개별 오류 분리
- [x] 414영수증/280최신 공고 재현, Node33·Python7 통과
- [~] 최신 코드 다운로드 관측35/223, 신규 확인·재검증188개
- [~] 과거 관측 포함201/223(90.1%), 다운로드 미확인22개 유지
- [!] AWS 로그인 갱신 필요, 서울 서버 SSM 진단 미실행
- [!] 임시 공개 CA PEM2개 삭제 요청이 도구 정책으로 차단됨
- [ ] 상시 유입·추출·DB/API/UI·운영 E2E 및 전체 장기 goal 완료

분모223은 2026-09-28T15:56:12.116778+09:00 활성 지역 수집원 스냅샷이다. 고유 지자체 수나 현재 운영 조회 결과가 아니다. 90.1%는 과거에 한 번 이상 유효 파일을 관측한 비율이며 전체 개발 완료율이 아니다. 엄격3표본 전체 첨부 집합 Gate는 현재 코드 기준0/223으로 별도 유지한다.

## 실제 수집 결과

기준 HEAD `73a122e18618c5583a9492c49ee46663c71caa03`. 응용 코드·관측 클래스·카탈로그·프로필을 수정하지 않고 기존 고정 표본을 재검증했다. 모든 표본에서 본문 AVAILABLE 및 공식 첨부 발견 완료를 확인했다.

| 공고 | 본문 글자 수 | 유효 파일 / 바이트 | 별도 오류 |
| --- | ---: | --- | --- |
| 강진28097 | 3494 | HWP / 109056 | 없음 |
| 김제310426 | 620 | HWPX / 103215 | 다른 HWP: FILE_DOWNLOAD / ATTACHMENT_PATH_NOT_APPROVED |
| 장흥27081 | 483 | HWP / 82432 | 없음 |
| 목포54011 | 9145 | HWP / 155136 | 없음 |
| 무안33617 | 3573 | HWP / 165376 | 없음 |
| 신안38211 | 150 | HWP / 123392 | 없음 |
| 완도30322 | 566 | HWP / 176640 | 없음 |
| 여수79153 | 9778 | HWPX / 102353 | 없음 |

유효 파일8개, 총1,017,600byte다. 김제는 부분 성공이며 모든 파일을 성공했다고 표시하지 않는다. 허용 경로 검증은 완화하지 않았다. 요청 예약 상한48회·192,937,984byte(184MiB), 본문 포함 예약34회·18,876,160byte였다. 예약은 실제 wire 요청 수·전송량과 다르다. 8개 보고서 모두 originalFilesRemoved=true·productionWriteCount=0이며 추출·정책·기대값 승인·운영 E2E는 false다.

보고서: `build/reports/attachment-regional-collection/*-JEONNAM-RECHECK-20261001.json`. 성공과 오류는 수집원 수준에서 중복 가능하다. 현재 오류 포함 수집원은8→9, 다운로드 관측은27→35다. 과거 성공 이력201개와 미확인22개는 변하지 않았다.

- inventory SHA-256: `7f6d6881f8b2f70781a955d5ab9e31174735bd1455400c43b6742bf7e89b419c`
- 관측 producer SHA-256: `1df3a2ac93ee2d0e73aa1894eb42df316589e7a3209c1d9fa4e920415ae2a7cc`

## AWS·로컬 Linux 사전 점검

서울 ap-northeast-2의 default 프로필로 STS 읽기 전용 인증 확인을 시도했다. 기본 CLI는 TLS_VALIDATION_FAILED였다. 기존 CA 묶음72개 중 현재 Windows 신뢰 루트에 없는12개와 만료12개를 발견하여 전체 묶음을 사용하지 않았다. 두 집합이 같다고 단정하지 않는다. 현재도 신뢰·유효한 기존 묶음의60개만 사용한 시도 역시 TLS_VALIDATION_FAILED였다.

현재 Windows Root의 유효·중복 제거 공개 인증서61개를 명령 단위 AWS_CA_BUNDLE로 지정한 시도는 AUTHENTICATION_REQUIRED였다. TLS 검증은 해제하지 않았고 전역 신뢰·환경설정은 변경하지 않았다. 계정 식별 성공·로그인 갱신 성공으로 표현하지 않는다. 사용자에게 로그인 갱신 여부를 질문했으며 이 기록 시점에는 답변 대기다. EC2 대상 확인·SSM·운영 DB 및 health 조회는 실행하지 않았다.

`docker info`는 로컬 Linux 컨테이너 런타임 사용 불가로 종료했다. Docker 설치·시작·설정 변경은 없으며 수집 관측의 임시 PostgreSQL 검증과 Docker 가용성은 별개다. TLS 고정3대상 진단 스크립트의 Python 모의 테스트7개는 통과했으나 실제 서버 TLS3대상 관측은 미실행이다.

## 실행 명령 / 결과

```powershell
.\gradlew.bat --no-daemon :attachmentRegionalCollectionObservation '-PsanebBbsObservationGroup=WANDO,SHINAN,JANGHEUNG,GIMJE,YEOSU,MOKPO,MUAN,GANGJIN' -PsanebCollectionReportLabel=JEONNAM-RECHECK-20261001 -PsanebCollectionWindowsTrust=true
node --test scripts/qa/attachment-bbs-observation-probe.test.mjs
node --test scripts/qa/attachment-collection-stage.test.mjs scripts/qa/attachment-collection-receipts.test.mjs scripts/qa/attachment-collection-availability.test.mjs
node scripts/qa/verify-collection-receipt-index.mjs
node scripts/qa/report-collection-availability.mjs
# 번들 Python으로 실행, 기본 WindowsStore python 별칭은 사용 불가
& 'C:\Users\valen\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -B -m unittest discover -s scripts/qa -p test_regional_tls_probe.py
aws sts get-caller-identity --profile default --region ap-northeast-2 --cli-connect-timeout 5 --cli-read-timeout 10 --output json
docker info --format '{{.OSType}} {{.ServerVersion}}'
git diff --check
```

수집 Gradle 종료0, Node10+23·Python7 통과. 영수증414개/최신 공고280개·현재 다운로드35개/오류9개/엄격 Gate0개를 재현했다. 과거 영수증의 producer 지문을 새 값으로 덮지 않는다. 응용 코드 변경이 없어 Java 단위 전체 suite·bootJar는 이번 묶음에서 다시 실행하지 않았으며 과거 통과 결과를 이번 결과로 대신하지 않는다.

## 자원 정리 / 미완료

수집한 정부 사이트 원본 파일의 정리와 CA 진단 파일 정리는 별개다. 다음 두 파일은 공개 인증서만 포함하며 개인키·인증 토큰은 없다. 정확한 소유 파일 경로를 검증한 삭제 요청이 도구 정책으로 차단되어 남아 있다. 다른 도구를 이용한 삭제 우회는 하지 않는다.

- `build/qa-results/aws-trust-20261001.pem`: 78,522byte
- `build/qa-results/aws-current-roots-20261001.pem`: 93,445byte

기존 `build/qa-yeonje-gurye-20260930` 임시 HTML 정리 차단도 미해결이다. 기존 미추적 output·scripts/qa/__pycache__는 보존한다. 작업 소유 Java·Node·Python 실행 프로세스는0개로 확인했다.

HWP 추출 고도화1.0.16 보류, Flyway V85 보존, 운영 DB·정책·worker·ENFORCE·배포 변경 없음. 브라우저는 현재 명시 요청이 없어 정책상 미실행이다. 이 묶음은 승인된 작업 브랜치의 기록 커밋·푸시 범위이며 전체 goal 완료가 아니다. AWS 진단은 로그인 갱신 후 서울 대상의 프로젝트 소유·SSM 상태를 다시 확인해야 한다. 그 전에도 나머지 지역의 로컬 수집 구현·오류 분리·재검증을 진행할 수 있다.
