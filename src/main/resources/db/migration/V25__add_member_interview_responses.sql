-- 사용자 기본정보 단계의 기대출 여부와 간단 인터뷰 응답을 저장한다.
ALTER TABLE business_profiles
    ADD COLUMN has_existing_loan boolean;

CREATE INDEX ix_business_profiles_has_existing_loan
    ON business_profiles (has_existing_loan);

CREATE TABLE member_interview_responses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    member_user_id uuid NOT NULL,
    question_code varchar(64) NOT NULL,
    answer_code varchar(16) NOT NULL,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_member_interview_responses_member FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_member_interview_responses_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_member_interview_responses_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_member_interview_responses_question UNIQUE (member_user_id, question_code),
    CONSTRAINT ck_member_interview_responses_question CHECK (
        question_code IN (
            'SAME_BUSINESS_IN_PROGRESS',
            'DUPLICATE_SUPPORT_USAGE',
            'BUSINESS_ACTUALLY_OPERATING',
            'OTHER_RESTRICTION'
        )
    ),
    CONSTRAINT ck_member_interview_responses_answer CHECK (
        answer_code IN ('YES', 'NO', 'UNKNOWN')
    )
);

CREATE INDEX ix_member_interview_responses_member
    ON member_interview_responses (member_user_id);

CREATE INDEX ix_member_interview_responses_question
    ON member_interview_responses (question_code, answer_code);
