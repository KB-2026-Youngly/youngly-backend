USE youngly_db;

START TRANSACTION;

-- =========================================================
-- 1. 테스트 그룹 생성
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
    default_fail_pass_count,
    base_deposit_amount,
    group_status,
    created_at,
    updated_at
)
VALUES (
    'group-feed-api-test-01',
    'moim-account-test-01',
    'test_user01',
    'feed-api-test-2026-0000-000000000001',
    '4인 운동 API 테스트 그룹',
    4,
    '모든 참여자가 서로 응원 댓글 남기기',
    'EXERCISE',
    '매일 20분 이상 운동 인증',
    '1:25/2:50/3:75/4:100',
    7,
    5,
    28,
    2,
    50000.00,
    'ONGOING',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    group_count = 4,
    content = VALUES(content),
    group_status = 'ONGOING',
    updated_at = NOW();

-- =========================================================
-- 2. 그룹 참여자 4명
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
)
VALUES
(
    'group-feed-api-test-01',
    'test_user01',
    'ACTIVE',
    NOW(),
    50000.00,
    5,
    NOW(),
    NOW()
),
(
    'group-feed-api-test-01',
    'test_user02',
    'ACTIVE',
    NOW(),
    50000.00,
    4,
    NOW(),
    NOW()
),
(
    'group-feed-api-test-01',
    'test_user03',
    'ACTIVE',
    NOW(),
    50000.00,
    3,
    NOW(),
    NOW()
),
(
    'group-feed-api-test-01',
    'test_user04',
    'ACTIVE',
    NOW(),
    50000.00,
    2,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    group_user_status = 'ACTIVE',
    approved_at = NOW(),
    current_deposit_amount = 50000.00,
    updated_at = NOW();

-- =========================================================
-- 3. 현재 진행 라운드
-- =========================================================
INSERT INTO rounds (
    group_id,
    round_no,
    start_date,
    end_date,
    round_status,
    created_at
)
VALUES (
    'group-feed-api-test-01',
    1,
    DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    DATE_ADD(CURRENT_DATE, INTERVAL 22 DAY),
    'ONGOING',
    NOW()
)
ON DUPLICATE KEY UPDATE
    round_id = LAST_INSERT_ID(round_id),
    start_date = DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    end_date = DATE_ADD(CURRENT_DATE, INTERVAL 22 DAY),
    round_status = 'ONGOING';

SET @feed_test_round_id = (
    SELECT round_id
    FROM rounds
    WHERE group_id = 'group-feed-api-test-01'
      AND round_no = 1
    LIMIT 1
);

-- =========================================================
-- 4. 라운드 참여 이력
-- checkGroupMembership() 통과에 필요
-- =========================================================
INSERT INTO round_history (
    round_id,
    user_id,
    account_id,
    moim_account_id,
    rank_no,
    success_count,
    settlement_amount,
    remaining_fail_pass_count,
    prior_failure_response,
    created_at,
    settlement_at
)
SELECT
    @feed_test_round_id,
    'test_user01',
    'account-test-user01-deposit',
    'moim-account-test-01',
    NULL,
    5,
    NULL,
    2,
    NULL,
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM round_history
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user01'
);

INSERT INTO round_history (
    round_id, user_id, account_id, moim_account_id,
    rank_no, success_count, settlement_amount,
    remaining_fail_pass_count, prior_failure_response,
    created_at, settlement_at
)
SELECT
    @feed_test_round_id,
    'test_user02',
    'account-test-user02-deposit',
    'moim-account-test-01',
    NULL,
    4,
    NULL,
    2,
    NULL,
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM round_history
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user02'
);

INSERT INTO round_history (
    round_id, user_id, account_id, moim_account_id,
    rank_no, success_count, settlement_amount,
    remaining_fail_pass_count, prior_failure_response,
    created_at, settlement_at
)
SELECT
    @feed_test_round_id,
    'test_user03',
    'account-test-user03-deposit',
    'moim-account-test-01',
    NULL,
    3,
    NULL,
    2,
    NULL,
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM round_history
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user03'
);

INSERT INTO round_history (
    round_id, user_id, account_id, moim_account_id,
    rank_no, success_count, settlement_amount,
    remaining_fail_pass_count, prior_failure_response,
    created_at, settlement_at
)
SELECT
    @feed_test_round_id,
    'test_user04',
    'account-test-user04-deposit',
    'moim-account-test-01',
    NULL,
    2,
    NULL,
    2,
    NULL,
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM round_history
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user04'
);

-- =========================================================
-- 5. test_user01: 최종 승인 게시글
-- =========================================================
SET @post_user01 = (
    SELECT post_id
    FROM posts
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user01'
      AND DATE(created_at) = CURRENT_DATE
    ORDER BY post_id DESC
    LIMIT 1
);

UPDATE posts
SET photo_url = '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-50-24.jpeg',
    content = '오늘도 운동 완료',
    post_status = 'APPROVED',
    posted_at = NOW(),
    status_changed_at = NOW(),
    updated_at = NOW()
WHERE post_id = @post_user01;

INSERT INTO posts (
    round_id, user_id, photo_url, content, post_status,
    created_at, updated_at, posted_at, status_changed_at,
    like_count, dislike_count, comment_count,
    reject_count, approve_count
)
SELECT
    @feed_test_round_id,
    'test_user01',
    '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-50-24.jpeg',
    '오늘도 운동 완료',
    'APPROVED',
    NOW(), NOW(), NOW(), NOW(),
    0, 0, 0, 0, 0
WHERE @post_user01 IS NULL;

SET @post_user01 = COALESCE(
    @post_user01,
    LAST_INSERT_ID()
);

-- =========================================================
-- 6. test_user02: 투표 진행 중 게시글
-- =========================================================
SET @post_user02 = (
    SELECT post_id
    FROM posts
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user02'
      AND DATE(created_at) = CURRENT_DATE
    ORDER BY post_id DESC
    LIMIT 1
);

UPDATE posts
SET photo_url = '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-49-38.jpeg',
    content = '헬스 인증 완료',
    post_status = 'PENDING',
    posted_at = NOW(),
    status_changed_at = NULL,
    updated_at = NOW()
WHERE post_id = @post_user02;

INSERT INTO posts (
    round_id, user_id, photo_url, content, post_status,
    created_at, updated_at, posted_at, status_changed_at,
    like_count, dislike_count, comment_count,
    reject_count, approve_count
)
SELECT
    @feed_test_round_id,
    'test_user02',
    '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-49-38.jpeg',
    '헬스 인증 완료',
    'PENDING',
    NOW(), NOW(), NOW(), NULL,
    0, 0, 0, 0, 0
WHERE @post_user02 IS NULL;

SET @post_user02 = COALESCE(
    @post_user02,
    LAST_INSERT_ID()
);

-- =========================================================
-- 7. test_user03: 최종 반려 게시글
-- =========================================================
SET @post_user03 = (
    SELECT post_id
    FROM posts
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user03'
      AND DATE(created_at) = CURRENT_DATE
    ORDER BY post_id DESC
    LIMIT 1
);

UPDATE posts
SET photo_url = '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-49-11.jpeg',
    content = '오늘 운동했습니다',
    post_status = 'REJECTED',
    posted_at = NOW(),
    status_changed_at = NOW(),
    updated_at = NOW()
WHERE post_id = @post_user03;

INSERT INTO posts (
    round_id, user_id, photo_url, content, post_status,
    created_at, updated_at, posted_at, status_changed_at,
    like_count, dislike_count, comment_count,
    reject_count, approve_count
)
SELECT
    @feed_test_round_id,
    'test_user03',
    '/src/assets/photos/excercise/KakaoTalk_Photo_2026-08-08-01-49-11.jpeg',
    '오늘 운동했습니다',
    'REJECTED',
    NOW(), NOW(), NOW(), NOW(),
    0, 0, 0, 0, 0
WHERE @post_user03 IS NULL;

SET @post_user03 = COALESCE(
    @post_user03,
    LAST_INSERT_ID()
);

-- =========================================================
-- 8. test_user04: 미인증 ZZZ 게시글
-- =========================================================
SET @post_user04 = (
    SELECT post_id
    FROM posts
    WHERE round_id = @feed_test_round_id
      AND user_id = 'test_user04'
      AND DATE(created_at) = CURRENT_DATE
    ORDER BY post_id DESC
    LIMIT 1
);

UPDATE posts
SET photo_url = NULL,
    content = NULL,
    post_status = 'NONE',
    posted_at = NULL,
    status_changed_at = NULL,
    updated_at = NOW(),
    like_count = 0,
    dislike_count = 0,
    comment_count = 0,
    reject_count = 0,
    approve_count = 0
WHERE post_id = @post_user04;

INSERT INTO posts (
    round_id, user_id, photo_url, content, post_status,
    created_at, updated_at, posted_at, status_changed_at,
    like_count, dislike_count, comment_count,
    reject_count, approve_count
)
SELECT
    @feed_test_round_id,
    'test_user04',
    NULL,
    NULL,
    'NONE',
    NOW(), NOW(), NULL, NULL,
    0, 0, 0, 0, 0
WHERE @post_user04 IS NULL;

SET @post_user04 = COALESCE(
    @post_user04,
    LAST_INSERT_ID()
);

-- =========================================================
-- 9. 승인/반려 투표 데이터
-- =========================================================

-- user01 게시글 최종 승인: 3명 승인
INSERT INTO post_approvals (
    user_id, post_id, approval_status,
    reject_reason, created_at, updated_at
)
VALUES
    ('test_user02', @post_user01, 'APPROVE', NULL, NOW(), NOW()),
    ('test_user03', @post_user01, 'APPROVE', NULL, NOW(), NOW()),
    ('test_user04', @post_user01, 'APPROVE', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    approval_status = 'APPROVE',
    reject_reason = NULL,
    updated_at = NOW();

-- user02 게시글: 방장 한 명만 승인
INSERT INTO post_approvals (
    user_id, post_id, approval_status,
    reject_reason, created_at, updated_at
)
VALUES (
    'test_user01',
    @post_user02,
    'APPROVE',
    NULL,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    approval_status = 'APPROVE',
    reject_reason = NULL,
    updated_at = NOW();

-- user03 게시글 최종 반려: 3명 반려
INSERT INTO post_approvals (
    user_id, post_id, approval_status,
    reject_reason, created_at, updated_at
)
VALUES
    (
        'test_user01',
        @post_user03,
        'REJECT',
        '운동 장면을 확인하기 어려워요.',
        NOW(),
        NOW()
    ),
    (
        'test_user02',
        @post_user03,
        'REJECT',
        '인증 기준에 맞는 사진이 필요해요.',
        NOW(),
        NOW()
    ),
    (
        'test_user04',
        @post_user03,
        'REJECT',
        '챌린지 수행 여부가 보이지 않아요.',
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    approval_status = 'REJECT',
    reject_reason = VALUES(reject_reason),
    updated_at = NOW();

-- =========================================================
-- 10. 좋아요·싫어요
-- =========================================================

INSERT INTO post_reactions (
    post_id, user_id, reaction_type,
    created_at, updated_at
)
VALUES
    (@post_user01, 'test_user02', 'LIKE', NOW(), NOW()),
    (@post_user01, 'test_user03', 'LIKE', NOW(), NOW()),
    (@post_user01, 'test_user04', 'LIKE', NOW(), NOW()),

    (@post_user02, 'test_user01', 'LIKE', NOW(), NOW()),
    (@post_user02, 'test_user03', 'LIKE', NOW(), NOW()),

    (@post_user03, 'test_user01', 'DISLIKE', NOW(), NOW()),
    (@post_user03, 'test_user02', 'DISLIKE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    reaction_type = VALUES(reaction_type),
    updated_at = NOW();

-- =========================================================
-- 11. 댓글
-- 중복 실행 시 같은 댓글은 다시 생성하지 않음
-- =========================================================

INSERT INTO post_comments (
    user_id, post_id, content,
    created_at, updated_at
)
SELECT
    'test_user02',
    @post_user01,
    '꾸준함이 최고예요!',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM post_comments
    WHERE post_id = @post_user01
      AND user_id = 'test_user02'
      AND content = '꾸준함이 최고예요!'
);

INSERT INTO post_comments (
    user_id, post_id, content,
    created_at, updated_at
)
SELECT
    'test_user03',
    @post_user01,
    '오늘도 수고했어요!',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM post_comments
    WHERE post_id = @post_user01
      AND user_id = 'test_user03'
      AND content = '오늘도 수고했어요!'
);

INSERT INTO post_comments (
    user_id, post_id, content,
    created_at, updated_at
)
SELECT
    'test_user01',
    @post_user02,
    '운동 인증 멋져요!',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM post_comments
    WHERE post_id = @post_user02
      AND user_id = 'test_user01'
      AND content = '운동 인증 멋져요!'
);

INSERT INTO post_comments (
    user_id, post_id, content,
    created_at, updated_at
)
SELECT
    'test_user04',
    @post_user03,
    '다음에는 운동 장면도 보여주세요.',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM post_comments
    WHERE post_id = @post_user03
      AND user_id = 'test_user04'
      AND content = '다음에는 운동 장면도 보여주세요.'
);

-- =========================================================
-- 12. 게시글 카운트 실제 데이터와 동기화
-- =========================================================
UPDATE posts p
SET like_count = (
        SELECT COUNT(*)
        FROM post_reactions pr
        WHERE pr.post_id = p.post_id
          AND pr.reaction_type = 'LIKE'
    ),
    dislike_count = (
        SELECT COUNT(*)
        FROM post_reactions pr
        WHERE pr.post_id = p.post_id
          AND pr.reaction_type = 'DISLIKE'
    ),
    comment_count = (
        SELECT COUNT(*)
        FROM post_comments pc
        WHERE pc.post_id = p.post_id
    ),
    approve_count = (
        SELECT COUNT(*)
        FROM post_approvals pa
        WHERE pa.post_id = p.post_id
          AND pa.approval_status IN ('APPROVE', 'AUTO_APPROVE')
    ),
    reject_count = (
        SELECT COUNT(*)
        FROM post_approvals pa
        WHERE pa.post_id = p.post_id
          AND pa.approval_status = 'REJECT'
    )
WHERE p.round_id = @feed_test_round_id;

COMMIT;

-- =========================================================
-- 13. 생성 결과 확인
-- =========================================================
SELECT
    g.group_id,
    g.group_name,
    g.group_status,
    r.round_id,
    r.round_status
FROM `groups` g
JOIN rounds r
    ON r.group_id = g.group_id
WHERE g.group_id = 'group-feed-api-test-01';

SELECT
    gu.user_id,
    u.nickname,
    gu.group_user_status,
    gu.current_deposit_amount
FROM group_users gu
JOIN users u
    ON u.user_id = gu.user_id
WHERE gu.group_id = 'group-feed-api-test-01'
ORDER BY gu.user_id;

SELECT
    p.post_id,
    p.user_id,
    p.content,
    p.post_status,
    p.like_count,
    p.dislike_count,
    p.comment_count,
    p.approve_count,
    p.reject_count,
    p.photo_url
FROM posts p
WHERE p.round_id = @feed_test_round_id
ORDER BY p.user_id;