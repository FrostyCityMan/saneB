# 구로 첨부 발견·다운로드 연결 검증

## 범위와 결과

사용자의 전 지역 첨부 수집 우선 지시에 따라 구로 `LGS-000018 / SAEOL_GOSI`를 시스템 프로필 `LOCAL_GURO_GOSI_V1`로 추가했다. 운영 DB·정책·worker 설정·기존 migration은 변경하지 않았다. HWP 추출 보완도 실행하지 않았다.

2026-09-28 로컬 실제 Java 수집기로 다음3공고의7파일을 다운로드하고 production `AttachmentFileTypeValidator`로 형식·signature·크기·hash를 확인했다. 수집 전용 JUnit3건 통과, 각 보고서의 원본 정리 확인. 파일 텍스트 추출/첨부 분류/운영 DB/API/E2E 성공은 아니다.

| 공식 고정 공고 | 첨부 | 다운로드 byte 합계 | 결과 |
|---|---|---:|---|
| GURO-49626 | PDF1 | 325822 | 전체 성공 |
| GURO-39520 | HWPX4·PDF1 | 3865501 | 전체 성공 |
| GURO-35870 | HWP1 | 110592 | 전체 성공 |

49626은2026년 융자지원,39520은2023년 고용장려금,35870은2022년 방역물품 지원 공고다. 과거 공고는 현재 게시된 공식 첨부 구조/다운로드의 검증 표본이며 현재 신청 가능한 공고라는 뜻이 아니다. catalog에는 참조3건만 추가하고 `expectation:null`을 유지한다. 후보/초안/운영 공고를 자동 생성하거나 활성화하지 않았다.

## 구조와 안전 경계

- 공식 상세: `www.guro.go.kr/www/selectBbsNttGosiView.do`, 게시판663·메뉴1791·공고번호 고정.
- 공식 파일: `eminwon.guro.go.kr/emwp/jsp/ofr/FileDown.jsp`, user_file_nm/sys_file_nm/file_path3필드 검증.
- `div.p-wrap.bbs.bbs__view > table.p-table.block` 안의 단일 제목·본문 표식·파일 셀만 발견 대상으로 사용한다. 목록의 첨부 아이콘을 전체 파일 목록으로 간주하지 않는다.
- 파일 셀의 다운로드 링크와 표시 파일명, 별도 미리보기 링크의 식별자를 대조한다. 미리보기/임의 JS/다른 host·포트/경로 이동/redirect는 실행하지 않는다.
- 파일 역할은 UNKNOWN이며 지원 형식 외 첨부가 있으면 전체 성공으로 숨기지 않는다. 최대10파일, 개별20MiB, 실제 QA공고별8요청·23MiB로 제한한다.
- 제목1차 규칙은 임시 DB의 DRAFT seed로 검증했다.3건 모두 수집 가능 판정을 확인했다. 운영 활성 규칙의 실제 실행을 검증한 것은 아니다.

## TLS 실패와 복구

첫 Java QA는3건 모두 상세 단계 TLS_FAILED로 종료했다. files=[]였으나 첨부 없음이 아니다. 보수적 예약9요청·6291456byte, 원본 정리true였다.

Windows 기본 인증서 검증을 사용하는 Schannel HTTPS 요청은 같은 공식 상세에서 성공했다. 재실행은 로컬 QA JVM에만 `Windows-ROOT` 신뢰 저장소를 명시했다. 인증서/hostname 검증을 끄거나 서버 설정을 변경하지 않았다. 이 환경 변경 후3건 모두 성공했다. Linux/서울 운영 JVM의 동일 환경 성공은 별도 확인 사항이다.

```powershell
.\gradlew.bat :attachmentRegionalCollectionObservation -PsanebBbsObservationGroup=GURO -PsanebCollectionWindowsTrust=true --no-daemon
```

첫 성공 회차 예약16요청·11945051byte. 이후 첨부 영역의 모호한 문구/활성 속성을 거부하는 안전 경계를 보강하고 변경된 profile hash로 같은3공고7파일을 다시 검증했다. 최종 회차도16요청·11945051byte·3검사 통과다. 로컬 구조 조사7요청(1전송 실패 포함)·2706106byte와 첫 TLS실패 예약을 합쳐 이번 구로 캠페인 누적48요청·32887664byte다. 검증 재시도로 이전 실패량을 초기화하지 않는다. HWP 추출 프로세스0, 운영 쓰기0, browser0이다.

| 성공 보고서 | SHA256 |
|---|---|
| GURO-49626.json | a54ae447bf5405e307e628020662dd99c23ba731339e4d361389991e3081f96f |
| GURO-39520.json | 629a3372fbbc49dbfe7ac4b3428a96616afead25ccb54f87a09db44835a5a693 |
| GURO-35870.json | 703e783c0c3f5ba40777cb0c9d296a192fe1c5aeedbf6ff0c0bd84e07ac91939 |

최종 profile hash: `e343a6066c10ec3d41dedf80babdb6d42efa6b10b7f93821291779468f40f86e`. 첫 성공의 구버전 `2ccb1a99…`를 최신 완료 근거로 재사용하지 않았다.

첫 TLS실패 보고서 hash는 순서대로 `09e7645e0dc1cd60b6e3ed9815c0199d23a3ad972d771b352aea6de7c33c71d3`, `e45fb4d3b3800859283e343b9e6ab6195d96dfd9ea40192eb5b35b492ac17700`, `d561158b900cd1673d5de47217f69d816e14847e6b3bfe68f536b4f7035aedd3`이다. 동일 Gradle 출력 경로는 성공 회차 결과로 교체됐으며 과거 실패 원문 JSON의 재현 검증은 주장하지 않는다.

## 잔여

- [x] 프로필/시스템 binding·고정 참조·부정 경로 회귀·실제3공고7파일 다운로드.
- [x] 전체 지역 대장·로컬/원격 영수증 구분 이관.44영수증/21공고 재현 성공. 활성223지역 중 수집 전용4지역 충족/219잔여, 프로필 미등록204지역.
- [x] 최종 표적 Java199검사 실패/생략0, bootJar 성공, Node18검사 통과. 최초 확대 회귀의 잔여 참조40건 상수 오류1건은 신규3참조에 맞게43건으로 보강하고 재검증했다.
- [x] 각 collector의 임시 파일 정리true, 별도 구조 조사 HTML3개와 소유 임시 디렉터리 삭제 확인.
- [ ] 미등록 지역 확대·기존 CI 근거 이관·Linux/운영 동일 코드 검증.
- [ ] 텍스트 추출/분류·정상 기대값·정책 QA·전체 Gate는 별도 잔여.

브라우저 검증은 현 요청의 사용자 정책에 따라 실행하지 않았다.
