# Local Seed

`local` profile에서만 사용하는 Flyway seed 경로입니다.

- 운영 migration과 분리합니다.
- 운영 secret을 기록하지 않습니다.
- 로컬 더미 계정과 샘플 업무 데이터가 필요할 때만 SQL 파일을 추가합니다.

## 기본 로컬 테스트 계정

| loginId | password | 용도 |
|---|---|---|
| `local_user` | `password` | 기본 사용자 로그인 smoke |
| `local_operator` | `password` | 공고/검증/매칭 운영 smoke |
| `local_match_user` | `password` | 테스트 매칭 케이스와 신청 진행 화면 확인 |
