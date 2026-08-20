USE youngly_db;

-- =========================================================
-- 1. KB 원장 모임통장 추가 (test_user02 소유)
-- =========================================================
INSERT INTO kb_accounts (
    kb_account_id,
    account_type,
    account_number,
    bank_name,
    balance,
    interest_rate,
    name,
    birthday,
    created_at,
    updated_at
) VALUES 
(
    'kb-test-moim-02', 'MOIM', '025202-92-200002',
    '국민', 500000.00, 2.50, '테스트이', '1998-02-02',
    '2026-08-01 10:00:00', '2026-08-15 09:00:00'
),
(
    'kb-test-moim-03', 'MOIM', '025202-92-200003',
    '국민', 300000.00, 2.50, '테스트이', '1998-02-02',
    '2026-08-05 10:00:00', '2026-08-15 09:00:00'
);

-- =========================================================
-- 2. Youngly 서비스 모임통장 연동 데이터 추가
-- =========================================================
INSERT INTO moim_accounts (
    moim_account_id,
    user_id,
    kb_account_id,
    account_status,
    account_name,
    created_at,
    synced_at,
    updated_at
) VALUES 
(
    'moim-account-test-02',
    'test_user02',
    'kb-test-moim-02',
    'ACTIVE',
    '테스트이의 러닝 모임통장',
    '2026-08-01 10:05:00',
    '2026-08-01 10:05:00',
    '2026-08-15 09:00:00'
),
(
    'moim-account-test-03',
    'test_user02',
    'kb-test-moim-03',
    'ACTIVE',
    '테스트이의 독서 모임통장',
    '2026-08-05 10:05:00',
    '2026-08-05 10:05:00',
    '2026-08-15 09:00:00'
);


USE youngly_db;

-- 외래키 제약조건 때문에 group_users 먼저 삭제 후 groups 삭제
DELETE FROM group_users WHERE group_id = 'group-test-join-01';
DELETE FROM `groups` WHERE group_id = 'group-test-join-01';

USE youngly_db;

-- =========================================================
-- 가입 테스트용 '모집 중(RECRUITING)' 그룹 생성 (방장: test_user01)
-- =========================================================
INSERT INTO `groups` (
    group_id,
    moim_account_id,
    user_id,
    invite_code,
    group_name,
    group_count,
    custom_rule,
    challenge_type,
    content,
    future_deposit_ratio_rule,
    duration_days,
    min_count,
    round_cycle_days,
    base_deposit_amount,
    group_status,
    created_at,
    updated_at
) VALUES (
    'group-test-join-01',
    'moim-account-test-01',
    'test_user01',
    'X9Y8Z7', -- 영문+숫자 6자리 초대코드 적용!
    '초대코드 가입 테스트 그룹',
    5,
    '테스트 인증하기',
    'HABIT',
    '초대코드로 그룹에 들어가는 기능을 테스트하기 위한 모집 중인 방입니다.',
    '1:20/2:40/3:60/4:80/5:100',
    7,
    3,
    28,
    10000.00,
    'RECRUITING',
    NOW(),
    NOW()
);

-- =========================================================
-- 방장(test_user01)은 이미 가입(ACTIVE)된 상태로 추가
-- =========================================================
INSERT INTO group_users (
    group_id,
    user_id,
    group_user_status,
    approved_at,
    current_deposit_amount,
    streak_count,
    created_at,
    updated_at
) VALUES (
    'group-test-join-01', 
    'test_user01',
    'ACTIVE', 
    NOW(),
    10000.00, 
    0,
    NOW(), 
    NOW()
);