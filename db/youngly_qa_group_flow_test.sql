-- Youngly 그룹/라운드/게시물 테스트용 QA 데이터
-- 기준: 2026-08-20 현재 코드 구조
-- 사용 전제: db/data.sql의 user01~user04 / 개인 accounts가 이미 들어가 있어야 합니다.
-- 로그인 예시:
--   user01: login_id=minjun01 / password=user01  (그룹장 테스트)
--   user02: login_id=seoyeon02 / password=user02
--   user03: login_id=jihoon03 / password=user03
--   user04: login_id=yujin04 / password=user04
--
-- 생성 시나리오
-- 1) QA 진행중 그룹        : 현재 라운드 진행중 / 게시물 업로드 / 랭킹 / 그룹수정 테스트
-- 2) QA 시작일 설정 그룹   : 정원 꽉 참 + RECRUITING + 라운드 없음 -> 시작일 설정 바텀시트 테스트
-- 3) QA 가입승인 그룹      : user03이 PENDING_APPROVAL -> 그룹장 승인/거절 UI 테스트
--
-- ★ CURRENT_DATE 기준으로 라운드 기간을 잡아서 실행하는 날에도 "현재 인증 가능 기간"이 되도록 구성함.

USE youngly_db;

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 0. 이전 QA 데이터 정리
-- =========================================================

DELETE pc
FROM post_comments pc
JOIN posts p ON p.post_id = pc.post_id
JOIN rounds r ON r.round_id = p.round_id
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE pr
FROM post_reactions pr
JOIN posts p ON p.post_id = pr.post_id
JOIN rounds r ON r.round_id = p.round_id
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE pa
FROM post_approvals pa
JOIN posts p ON p.post_id = pa.post_id
JOIN rounds r ON r.round_id = p.round_id
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE p
FROM posts p
JOIN rounds r ON r.round_id = p.round_id
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE rh
FROM round_history rh
JOIN rounds r ON r.round_id = rh.round_id
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE FROM rounds
WHERE group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE FROM group_users
WHERE group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE FROM `groups`
WHERE group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
);

DELETE FROM moim_accounts
WHERE moim_account_id IN (
    'moim-qa-flow-01',
    'moim-qa-flow-02',
    'moim-qa-flow-03'
);

DELETE FROM kb_accounts
WHERE kb_account_id IN (
    'kb-moim-qa-flow-01',
    'kb-moim-qa-flow-02',
    'kb-moim-qa-flow-03'
);

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. QA 전용 모임통장 3개 생성
--    user01(김민준)이 모두 그룹장
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
    'kb-moim-qa-flow-01',
    'MOIM',
    '025202-99-910001',
    '국민',
    800000.00,
    2.50,
    '김민준',
    '1998-03-12',
    NOW(),
    NOW()
),
(
    'kb-moim-qa-flow-02',
    'MOIM',
    '025202-99-910002',
    '국민',
    500000.00,
    2.50,
    '김민준',
    '1998-03-12',
    NOW(),
    NOW()
),
(
    'kb-moim-qa-flow-03',
    'MOIM',
    '025202-99-910003',
    '국민',
    300000.00,
    2.50,
    '김민준',
    '1998-03-12',
    NOW(),
    NOW()
);

INSERT INTO moim_accounts (
    moim_account_id,
    user_id,
    kb_account_id,
    account_status,
    created_at,
    synced_at,
    updated_at,
    account_name
) VALUES
(
    'moim-qa-flow-01',
    'user01',
    'kb-moim-qa-flow-01',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    'QA 진행중 그룹 모임통장'
),
(
    'moim-qa-flow-02',
    'user01',
    'kb-moim-qa-flow-02',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    'QA 시작일 설정 그룹 모임통장'
),
(
    'moim-qa-flow-03',
    'user01',
    'kb-moim-qa-flow-03',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    'QA 가입승인 그룹 모임통장'
);

-- =========================================================
-- 2. 시나리오 A: 이미 진행 중인 그룹
--
-- 테스트:
-- - 홈에서 "진행 중" 카드 확인
-- - 그룹 상세 진입
-- - 랭킹 카드 노출 확인
-- - 오늘 인증 게시물 업로드
-- - 날짜 이동
-- - 그룹 내용 수정
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
) VALUES (
    'qa-flow-ongoing',
    'moim-qa-flow-01',
    'user01',
    'QA0001',
    'QA 진행중 운동 챌린지',
    4,
    '지각 인증은 인정하지 않기',
    'EXERCISE',
    '매일 20분 이상 운동 인증',
    '1:25/2:50/3:75/4:100',
    7,
    3,
    28,
    2,
    50000.00,
    'ONGOING',
    NOW(),
    NOW()
);

INSERT INTO group_users (
    group_id,
    user_id,
    group_user_status,
    approved_at,
    current_deposit_amount,
    streak_count,
    created_at,
    updated_at
) VALUES
('qa-flow-ongoing', 'user01', 'ACTIVE', NOW(), 50000.00, 5, NOW(), NOW()),
('qa-flow-ongoing', 'user02', 'ACTIVE', NOW(), 50000.00, 4, NOW(), NOW()),
('qa-flow-ongoing', 'user03', 'ACTIVE', NOW(), 50000.00, 3, NOW(), NOW()),
('qa-flow-ongoing', 'user04', 'ACTIVE', NOW(), 50000.00, 2, NOW(), NOW());

INSERT INTO rounds (
    group_id,
    round_no,
    start_date,
    end_date,
    round_status,
    created_at
) VALUES (
    'qa-flow-ongoing',
    1,
    DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY),
    DATE_ADD(CURRENT_DATE, INTERVAL 24 DAY),
    'ONGOING',
    NOW()
);

SET @qa_ongoing_round_id = LAST_INSERT_ID();

-- 랭킹 API가 바로 보이도록 success_count를 다르게 설정
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
) VALUES
(@qa_ongoing_round_id, 'user01', 'account-user01-pension', 'moim-qa-flow-01', NULL, 5, NULL, 2, NULL, NOW(), NULL),
(@qa_ongoing_round_id, 'user02', 'account-user02-deposit', 'moim-qa-flow-01', NULL, 4, NULL, 2, NULL, NOW(), NULL),
(@qa_ongoing_round_id, 'user03', 'account-user03-pension', 'moim-qa-flow-01', NULL, 3, NULL, 2, NULL, NOW(), NULL),
(@qa_ongoing_round_id, 'user04', 'account-user04-deposit', 'moim-qa-flow-01', NULL, 1, NULL, 2, NULL, NOW(), NULL);

-- 주의:
-- user01의 오늘 posts는 일부러 만들지 않았음.
-- user01로 로그인 후 실제 카메라/이미지 업로드를 해서 createPost()를 테스트하면 됨.

-- =========================================================
-- 3. 시나리오 B: 정원은 다 찼지만 아직 시작 전
--
-- 테스트:
-- - 그룹장 user01로 상세 진입
-- - "시작 날짜 설정" 바텀시트 노출 확인
-- - 날짜 한 번 설정
-- - 설정 후 그룹 상태 ONGOING으로 변경되는지 확인
-- - 두 번째 날짜 설정이 막히는지 확인
--
-- 중요:
-- RoundService가 round_history를 만들 때 수령 계좌가 필요하므로
-- user01~03의 기존 accounts가 있어야 함.
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
) VALUES (
    'qa-flow-ready',
    'moim-qa-flow-02',
    'user01',
    'QA0002',
    'QA 시작일 설정 테스트',
    3,
    '모두 주 3회 이상 인증',
    'HABIT',
    '날짜 설정 기능 테스트용 그룹',
    '1:34/2:67/3:100',
    7,
    3,
    28,
    1,
    30000.00,
    'RECRUITING',
    NOW(),
    NOW()
);

INSERT INTO group_users (
    group_id,
    user_id,
    group_user_status,
    approved_at,
    current_deposit_amount,
    streak_count,
    created_at,
    updated_at
) VALUES
('qa-flow-ready', 'user01', 'ACTIVE', NOW(), 30000.00, 0, NOW(), NOW()),
('qa-flow-ready', 'user02', 'ACTIVE', NOW(), 30000.00, 0, NOW(), NOW()),
('qa-flow-ready', 'user03', 'ACTIVE', NOW(), 30000.00, 0, NOW(), NOW());

-- ★ intentionally no rounds row
-- 상세화면에서 그룹장이 시작일을 직접 설정해야 함.

-- =========================================================
-- 4. 시나리오 C: 그룹장 가입 승인/거절
--
-- 테스트:
-- - user01로 그룹 상세 진입
-- - 참여 요청 목록에 user03 노출
-- - 승인 또는 거절
-- - 승인하면 PENDING_DEPOSIT으로 변경되는지 확인
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
) VALUES (
    'qa-flow-approval',
    'moim-qa-flow-03',
    'user01',
    'QA0003',
    'QA 가입 승인 테스트',
    4,
    '가입 승인 UI 확인용',
    'STUDY',
    '그룹장 승인/거절 흐름 테스트',
    '1:25/2:50/3:75/4:100',
    7,
    4,
    28,
    0,
    20000.00,
    'RECRUITING',
    NOW(),
    NOW()
);

INSERT INTO group_users (
    group_id,
    user_id,
    group_user_status,
    approved_at,
    current_deposit_amount,
    streak_count,
    created_at,
    updated_at
) VALUES
('qa-flow-approval', 'user01', 'ACTIVE', NOW(), 20000.00, 0, NOW(), NOW()),
('qa-flow-approval', 'user02', 'ACTIVE', NOW(), 20000.00, 0, NOW(), NOW()),
('qa-flow-approval', 'user03', 'PENDING_APPROVAL', NULL, 0.00, 0, NOW(), NOW());

-- =========================================================
-- 5. 생성 결과 확인
-- =========================================================

SELECT
    g.group_id,
    g.group_name,
    g.group_status,
    g.group_count,
    g.invite_code,
    COUNT(gu.group_user_id) AS member_rows
FROM `groups` g
LEFT JOIN group_users gu
    ON gu.group_id = g.group_id
WHERE g.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
)
GROUP BY
    g.group_id,
    g.group_name,
    g.group_status,
    g.group_count,
    g.invite_code
ORDER BY g.group_id;

SELECT
    r.round_id,
    r.group_id,
    r.round_no,
    r.start_date,
    r.end_date,
    r.round_status
FROM rounds r
WHERE r.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
)
ORDER BY r.group_id, r.round_no;

SELECT
    gu.group_user_id,
    gu.group_id,
    gu.user_id,
    gu.group_user_status,
    gu.current_deposit_amount
FROM group_users gu
WHERE gu.group_id IN (
    'qa-flow-ongoing',
    'qa-flow-ready',
    'qa-flow-approval'
)
ORDER BY gu.group_id, gu.group_user_id;

-- =========================================================
-- 테스트 순서 추천
-- =========================================================
-- [A] user01 로그인 → "QA 진행중 운동 챌린지"
--     1. 랭킹 노출
--     2. 오늘 인증 게시물 업로드
--     3. 그룹 내용 수정
--     4. 날짜 이전/오늘 이동 확인
--
-- [B] user01 로그인 → "QA 시작일 설정 테스트"
--     1. 시작 날짜 설정 바텀시트
--     2. 오늘 또는 미래 날짜 선택 후 설정
--     3. 새로고침 후 바텀시트 재노출 안 되는지 확인
--
-- [C] user01 로그인 → "QA 가입 승인 테스트"
--     1. 참여 요청 user03 확인
--     2. 승인/거절 버튼 테스트
--
-- [D] 승인/반려 게시물 상호작용 테스트
--     1. user01로 A그룹에 게시물 업로드
--     2. user02 또는 user03 로그인
--     3. user01 게시물 승인/반려
--     4. 투표 직후 상세/댓글/좋아요/싫어요 접근되는지 확인
