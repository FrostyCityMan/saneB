CREATE TABLE partner_availability_slots (
    id uuid PRIMARY KEY,
    partner_user_id uuid NOT NULL,
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    status_code varchar(20) NOT NULL DEFAULT 'OPEN',
    note varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_partner_availability_slots_partner FOREIGN KEY (partner_user_id) REFERENCES users (id),
    CONSTRAINT fk_partner_availability_slots_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_partner_availability_slots_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT uq_partner_availability_slots_time UNIQUE (partner_user_id, start_at, end_at),
    CONSTRAINT ck_partner_availability_slots_time CHECK (end_at > start_at),
    CONSTRAINT ck_partner_availability_slots_status CHECK (status_code IN ('OPEN', 'HELD', 'CLOSED', 'CANCELED'))
);

CREATE INDEX ix_partner_availability_slots_partner_start ON partner_availability_slots (partner_user_id, start_at);
CREATE INDEX ix_partner_availability_slots_status_start ON partner_availability_slots (status_code, start_at);

CREATE TABLE consultation_reservations (
    id uuid PRIMARY KEY,
    slot_id uuid NOT NULL,
    member_user_id uuid NOT NULL,
    partner_user_id uuid NOT NULL,
    progress_id uuid,
    verification_id uuid,
    status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    request_note varchar(1000),
    status_note varchar(1000),
    confirmed_at timestamptz,
    canceled_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_consultation_reservations_slot FOREIGN KEY (slot_id) REFERENCES partner_availability_slots (id),
    CONSTRAINT fk_consultation_reservations_member FOREIGN KEY (member_user_id) REFERENCES users (id),
    CONSTRAINT fk_consultation_reservations_partner FOREIGN KEY (partner_user_id) REFERENCES users (id),
    CONSTRAINT fk_consultation_reservations_progress FOREIGN KEY (progress_id) REFERENCES application_progresses (id),
    CONSTRAINT fk_consultation_reservations_verification FOREIGN KEY (verification_id) REFERENCES partner_verifications (id),
    CONSTRAINT fk_consultation_reservations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_consultation_reservations_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_consultation_reservations_status CHECK (
        status_code IN ('REQUESTED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    )
);

CREATE UNIQUE INDEX uq_consultation_reservations_active_slot
    ON consultation_reservations (slot_id)
    WHERE status_code IN ('REQUESTED', 'CONFIRMED', 'COMPLETED', 'NO_SHOW');
CREATE INDEX ix_consultation_reservations_member_status ON consultation_reservations (
    member_user_id,
    status_code,
    created_at
);
CREATE INDEX ix_consultation_reservations_partner_status ON consultation_reservations (
    partner_user_id,
    status_code,
    created_at
);
CREATE INDEX ix_consultation_reservations_progress ON consultation_reservations (progress_id);
CREATE INDEX ix_consultation_reservations_verification ON consultation_reservations (verification_id);

CREATE TABLE consultation_histories (
    id uuid PRIMARY KEY,
    reservation_id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    before_status_code varchar(30),
    after_status_code varchar(30) NOT NULL,
    note varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_consultation_histories_reservation FOREIGN KEY (reservation_id) REFERENCES consultation_reservations (id),
    CONSTRAINT fk_consultation_histories_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT ck_consultation_histories_before_status CHECK (
        before_status_code IS NULL
        OR before_status_code IN ('REQUESTED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    ),
    CONSTRAINT ck_consultation_histories_after_status CHECK (
        after_status_code IN ('REQUESTED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')
    )
);

CREATE INDEX ix_consultation_histories_reservation_created_at ON consultation_histories (
    reservation_id,
    created_at
);
CREATE INDEX ix_consultation_histories_actor_created_at ON consultation_histories (
    actor_user_id,
    created_at
);
