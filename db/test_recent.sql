USE youngly_db;

-- =========================================================
-- 1. 게시글 업로드 테스트 전용 그룹 생성 (방장: test_user02)
-- =========================================================
INSERT INTO `groups` (
    group_id, moim_account_id, user_id, invite_code, group_name,
    group_count, custom_rule, challenge_type, content, future_deposit_ratio_rule,
    duration_days, min_count, round_cycle_days, base_deposit_amount, group_status,
    created_at, updated_at
) VALUES (
    'group-test-post-02', 'moim-account-test-02', 'test_user02', 'POST02', 'test_user02 게시글 테스트 방',
    1, '매일 사진 인증하기', 'HABIT', 'test_user02 게시글 작성 API 테스트용 방입니다.', '1:100',
    7, 3, 7, 10000.00, 'ONGOING', NOW(), NOW()
);

-- =========================================================
-- 2. 그룹 참여자 (test_user02) 활성 상태로 추가
-- =========================================================
INSERT INTO group_users (
    group_id, user_id, group_user_status, approved_at,
    current_deposit_amount, streak_count, created_at, updated_at
) VALUES (
    'group-test-post-02', 'test_user02', 'ACTIVE', NOW(),
    10000.00, 0, NOW(), NOW()
);

-- =========================================================
-- 3. 현재 진행 중(ONGOING)인 라운드 생성 
-- (💡 round_id를 14002로 고정!)
-- =========================================================
INSERT INTO rounds (
    round_id, group_id, round_no, start_date, end_date, round_status, created_at
) VALUES (
    14002, 'group-test-post-02', 1, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'ONGOING', NOW()
);

-- =========================================================
-- 4. 라운드 히스토리 추가
-- =========================================================
INSERT INTO round_history (
    round_id, user_id, account_id, moim_account_id,
    success_count, remaining_fail_pass_count, created_at
) VALUES (
    14002, 'test_user02', 'account-test-user02-deposit', 'moim-account-test-02',
    0, 1, NOW()
);

SELECT
    post_id,
    round_id,
    user_id,
    photo_url,
    content,
    post_status,
    posted_at
FROM posts
ORDER BY post_id DESC
LIMIT 10;