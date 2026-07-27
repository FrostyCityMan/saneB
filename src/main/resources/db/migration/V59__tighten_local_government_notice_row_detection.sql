-- 새올 전자민원 셀 클릭형 목록에서 상위 레이아웃 행을 공고 행으로 중복 인식하지 않도록
-- 실제 제목 셀을 직접 자식으로 가진 행만 선택한다.
UPDATE local_government_notice_parser_profiles
SET list_item_selector = 'tr:has(> td:nth-of-type(3)[onclick*=searchDetail])',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_code = 'SAFE_SAEOL_EMINWON_CELL';
