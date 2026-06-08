CREATE TABLE subscription_plans (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_code varchar(50) NOT NULL,
    plan_name varchar(100) NOT NULL,
    billing_cycle_code varchar(30) NOT NULL,
    price_amount numeric(18, 2) NOT NULL,
    currency_code varchar(3) NOT NULL DEFAULT 'KRW',
    is_active boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    description text,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT uq_subscription_plans_code UNIQUE (plan_code),
    CONSTRAINT fk_subscription_plans_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_subscription_plans_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_subscription_plans_cycle CHECK (billing_cycle_code IN ('ONE_TIME', 'MONTHLY', 'YEARLY')),
    CONSTRAINT ck_subscription_plans_price CHECK (price_amount >= 0),
    CONSTRAINT ck_subscription_plans_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_subscription_plans_active_sort ON subscription_plans (is_active, sort_order, plan_code);

CREATE TABLE user_subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    status_code varchar(30) NOT NULL DEFAULT 'PENDING',
    current_period_start timestamptz,
    current_period_end timestamptz,
    canceled_at timestamptz,
    cancel_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_user_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    CONSTRAINT fk_user_subscriptions_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_user_subscriptions_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_user_subscriptions_status CHECK (status_code IN ('PENDING', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED')),
    CONSTRAINT ck_user_subscriptions_period CHECK (
        current_period_start IS NULL
        OR current_period_end IS NULL
        OR current_period_end > current_period_start
    )
);

CREATE UNIQUE INDEX uq_user_subscriptions_current
    ON user_subscriptions (user_id)
    WHERE status_code IN ('PENDING', 'ACTIVE', 'PAST_DUE');
CREATE INDEX ix_user_subscriptions_user_status ON user_subscriptions (user_id, status_code);
CREATE INDEX ix_user_subscriptions_plan_status ON user_subscriptions (plan_id, status_code);

CREATE TABLE payment_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id uuid NOT NULL,
    user_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    provider_code varchar(30) NOT NULL,
    merchant_uid varchar(120) NOT NULL,
    provider_payment_key varchar(200),
    status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    amount numeric(18, 2) NOT NULL,
    currency_code varchar(3) NOT NULL DEFAULT 'KRW',
    requested_at timestamptz NOT NULL DEFAULT now(),
    approved_at timestamptz,
    failed_at timestamptz,
    failure_code varchar(100),
    failure_message varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT uq_payment_transactions_merchant_uid UNIQUE (merchant_uid),
    CONSTRAINT fk_payment_transactions_subscription FOREIGN KEY (subscription_id) REFERENCES user_subscriptions (id),
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_payment_transactions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    CONSTRAINT fk_payment_transactions_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_payment_transactions_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_payment_transactions_provider CHECK (provider_code IN ('MANUAL', 'TOSS', 'NICEPAY', 'KCP', 'STRIPE')),
    CONSTRAINT ck_payment_transactions_status CHECK (status_code IN ('REQUESTED', 'APPROVED', 'FAILED', 'CANCELED', 'REFUNDED')),
    CONSTRAINT ck_payment_transactions_amount CHECK (amount >= 0)
);

CREATE UNIQUE INDEX uq_payment_transactions_provider_key
    ON payment_transactions (provider_code, provider_payment_key)
    WHERE provider_payment_key IS NOT NULL;
CREATE INDEX ix_payment_transactions_user_status ON payment_transactions (user_id, status_code);
CREATE INDEX ix_payment_transactions_subscription_status ON payment_transactions (subscription_id, status_code);

CREATE TABLE refund_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id uuid NOT NULL,
    user_id uuid NOT NULL,
    provider_code varchar(30) NOT NULL,
    provider_refund_key varchar(200),
    status_code varchar(30) NOT NULL DEFAULT 'REQUESTED',
    refund_amount numeric(18, 2) NOT NULL,
    reason varchar(500),
    requested_by uuid NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    failure_code varchar(100),
    failure_message varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    CONSTRAINT fk_refund_transactions_payment FOREIGN KEY (payment_id) REFERENCES payment_transactions (id),
    CONSTRAINT fk_refund_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refund_transactions_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_refund_transactions_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    CONSTRAINT ck_refund_transactions_provider CHECK (provider_code IN ('MANUAL', 'TOSS', 'NICEPAY', 'KCP', 'STRIPE')),
    CONSTRAINT ck_refund_transactions_status CHECK (status_code IN ('REQUESTED', 'APPROVED', 'FAILED')),
    CONSTRAINT ck_refund_transactions_amount CHECK (refund_amount > 0)
);

CREATE UNIQUE INDEX uq_refund_transactions_provider_key
    ON refund_transactions (provider_code, provider_refund_key)
    WHERE provider_refund_key IS NOT NULL;
CREATE INDEX ix_refund_transactions_payment_status ON refund_transactions (payment_id, status_code);
CREATE INDEX ix_refund_transactions_user_status ON refund_transactions (user_id, status_code);

CREATE TABLE payment_provider_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code varchar(30) NOT NULL,
    provider_event_id varchar(200) NOT NULL,
    event_type_code varchar(50) NOT NULL,
    payment_id uuid,
    refund_id uuid,
    result_code varchar(30) NOT NULL DEFAULT 'RECEIVED',
    metadata_json jsonb,
    received_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_provider_events_key UNIQUE (provider_code, provider_event_id),
    CONSTRAINT fk_payment_provider_events_payment FOREIGN KEY (payment_id) REFERENCES payment_transactions (id),
    CONSTRAINT fk_payment_provider_events_refund FOREIGN KEY (refund_id) REFERENCES refund_transactions (id),
    CONSTRAINT ck_payment_provider_events_provider CHECK (provider_code IN ('MANUAL', 'TOSS', 'NICEPAY', 'KCP', 'STRIPE')),
    CONSTRAINT ck_payment_provider_events_type CHECK (
        event_type_code IN ('PAYMENT_APPROVED', 'PAYMENT_FAILED', 'PAYMENT_CANCELED', 'REFUND_APPROVED', 'REFUND_FAILED')
    ),
    CONSTRAINT ck_payment_provider_events_result CHECK (result_code IN ('RECEIVED', 'DUPLICATE', 'IGNORED'))
);

CREATE INDEX ix_payment_provider_events_payment ON payment_provider_events (payment_id, received_at);
CREATE INDEX ix_payment_provider_events_refund ON payment_provider_events (refund_id, received_at);
