-- 기존 원문 Provider 코드와 첨부 저장 제약을 맞춘다. 과거 별칭 이력과 원문/정책/작업 데이터는 변경하지 않는다.
ALTER TABLE announcement_attachment_jobs
    DROP CONSTRAINT announcement_attachment_jobs_frozen_provider_code_check,
    ADD CONSTRAINT announcement_attachment_jobs_frozen_provider_code_check
        CHECK (frozen_provider_code IN ('BIZINFO','GOV24','GOV24_PUBLIC_SERVICE','LOCAL_GOV_NOTICE'));

-- 전체 목록 항목은 원문과 같은 실제 코드를 저장한다. 기존 GOV24 이력 재작성/trigger 우회는 하지 않는다.
ALTER TABLE announcement_attachment_backfill_items
    DROP CONSTRAINT announcement_attachment_backfill_items_provider_code_check,
    ADD CONSTRAINT announcement_attachment_backfill_items_provider_code_check
        CHECK (provider_code IN ('BIZINFO','GOV24','GOV24_PUBLIC_SERVICE','LOCAL_GOV_NOTICE'));
