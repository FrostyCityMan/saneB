INSERT INTO subscription_plans (
    plan_code,
    plan_name,
    billing_cycle_code,
    price_amount,
    currency_code,
    is_active,
    sort_order,
    description
) VALUES (
    'SANEB_MONTHLY_MOCK',
    '사내비 월 구독',
    'MONTHLY',
    12900.00,
    'KRW',
    true,
    10,
    'TossPayments 실연동 전 운영 테스트용 월 구독 요금제입니다.'
)
ON CONFLICT (plan_code) DO UPDATE SET
    plan_name = EXCLUDED.plan_name,
    billing_cycle_code = EXCLUDED.billing_cycle_code,
    price_amount = EXCLUDED.price_amount,
    currency_code = EXCLUDED.currency_code,
    is_active = EXCLUDED.is_active,
    sort_order = EXCLUDED.sort_order,
    description = EXCLUDED.description,
    updated_at = now();
