# 웹 JAR와 기본 worker 추출기의 불변 release 연결

## 목적과 범위

제목→본문→실제 첨부→관리자 검증의 상시 worker를 배포·복구할 때 웹 코드와 추출기 버전이 어긋나지 않도록 한다. 기존 `start.sh`/`validate.sh`의 실패 복구는 `app.jar.previous`만 복원한다. 기존 launch는 QA 패키지만 현재 JAR 해시에 연결하고 worker 추출기는 공용 `/opt/saneb/attachment-extractor`를 기본값으로 사용하므로, JAR 복구 뒤에도 새 추출기를 선택할 수 있었다.

이미 설치하는 JAR별 불변 QA release 안에 같은 추출기 배포본이 포함돼 있다. 이를 worker의 기본 경로로 사용한다. 공용 설치를 삭제·덮어쓰거나 새 서비스/의존성을 도입하지 않는다. DB/API/화면/Flyway V1~V83과 정책·worker 실행 플래그는 변경하지 않는다.

## 계약

1. `attachment-contract-release.sh`의 `select_attachment_extractor_release`는 실제 웹 JAR SHA256에 대응하는 release의 `extractor`만 반환한다. release/하위 디렉터리·launcher가 존재해야 하며 심볼릭 링크·특수 파일·추출기 실행 JAR 누락/복수 버전을 거부한다.
2. `configure_attachment_release_environment`는 QA 경로와 추출기 경로를 모두 계산한 다음에만 실행 프로세스에 export한다. 경로 준비 실패 시 일부 환경만 바꾸지 않으며 공용 설치로 조용히 대체하지 않는다.
3. 비어 있지 않은 `SANEB_ANNOUNCEMENT_ATTACHMENT_CONTRACT_QA_ROOT`와 `SANEB_ANNOUNCEMENT_ATTACHMENT_EXTRACTOR_ROOT`는 각각 보존한다. QA 경로만 별도 지정해도 기본 worker 추출기는 그 임의 경로가 아니라 실제 웹 JAR의 불변 release를 따른다.
4. `start.sh`가 생성하는 launch는 Java 실행 직전에 이 helper를 호출한다. 새 JAR는 새 release, 이전 JAR 복구 후 재시작은 이전 release를 선택한다. `app.env`에 새 값을 기록하거나 정책/ENFORCE/worker를 켜지 않는다.
5. 명시한 사용자 정의 추출기 경로의 내용·버전 일치 여부는 기존 runtime/정책 QA가 검증한다. 이 변경은 명시 경로를 자동 복구하지 않는다.

## 검증 및 완료 경계

- [x] 실제 Git Bash에서 새 release→새 JAR→이전 JAR의 두 경로 선택, QA/추출기 독립 override, 기본 release 누락 시 실패·환경 불변, 실행 JAR 누락·중복 거부 검증.
- [x] 설치·불변성·복사 실패 정리의 기존 시험 보존. `node --test scripts/qa/attachment-contract-release.test.mjs`:17건 중14통과/실패0, Linux 심볼릭 링크3건 생략.
- [x] hook/helper Bash 구문과 실제 launch 호출 연결 검사. 테스트는 직접 생성한 임시 디렉터리만 사용하고 정리한다.
- [x] catalog 갱신과 함께 표적 Java/패키지/bootJar1분23초 성공. 전체 회귀4분36초는 root2677=2413통과/264조건부 생략/실패0이며 패키지20·추출기88·bootJar/probe는 선행 결과 재사용이다. 웹 JAR SHA256은 `917d743ee8634d8af6cf736102f9fa198cf71aa29fb205d7787abe5fe8b09905`다.
- [x] `f7b7396`의 [Linux34944248951](https://github.com/FrostyCityMan/saneB/actions/runs/34944248951) 성공을09-16 재확인했다. 새/이전 JAR의 worker 추출기 선택과 Linux 심볼릭 링크 거부를 포함한 배포 QA 단계·전체 계약·태백 HWPX 고정2파일 비교·HWP worker 시험이 성공했다. 실제 HWP는 부분 추출이며 운영 설치/원복 성공을 뜻하지 않는다.
- [ ] 승인된 새 코드 운영 설치 후 프로세스의 실제 선택 경로·추출기 지문·QA 지문 일치 확인.
- [ ] 운영 실패를 발생시킨 실제 원복 E2E. 이번 경로 선택 시험은 서비스 재시작·CodeDeploy 전체 원복·DB 복원 시험이 아니다.

배포용 공용 추출기 설치 경로는 유지한다. 이 변경이 해당 디렉터리 자체를 과거 상태로 되돌리는 것은 아니다. 기본 worker/기본 QA가 이전 웹 JAR의 불변 묶음을 함께 사용하도록 하는 범위다. 실제 운영 rollout은 취소된 배포 재개 승인 및 새 SHA 검증 후 진행하며, 기존cc79d59 대상 승인에 이 미배포 변경을 포함됐다고 가정하지 않는다.
