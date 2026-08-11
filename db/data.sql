-- ============================================================================
-- Youngly - 초기 데이터 스크립트
--
-- [INFO] 실행 순서 : schema.sql -> tables.sql -> data.sql
-- [TODO] 데모/시연용 Mock 데이터를 여기에 작성한다.
--        - Mock 계좌 데이터 (AUTH-04)
--        - 투자 성향 설문 문항 (SURVEY-01)
--        - 캐릭터/아이템 마스터 데이터 (GACHA-01)
--
-- [WARN] 실제 개인정보나 실제 계좌번호는 절대 넣지 않는다.
-- ============================================================================

USE youngly_db;

-- 투자 성향 설문 문항과 선택지 (6문항, 각 1~5점)
INSERT INTO survey_questions (question_id, question_no, question_text, is_multiple) VALUES
    (1, 1, '투자 경험이 있으신가요?', FALSE),
    (2, 2, '금융상품에 대해 얼마나 알고 계신가요?', FALSE),
    (3, 3, '돈을 모으는 목적은 무엇인가요?', FALSE),
    (4, 4, '수익과 안정성 중 무엇이 더 중요한가요?', FALSE),
    (5, 5, '얼마 동안 모을 계획이신가요?', FALSE),
    (6, 6, '손실이 발생하면 어느 정도까지 감내할 수 있나요?', FALSE);

INSERT INTO survey_choices (question_id, choice_text, score, display_order) VALUES
    (1, '투자 경험 없음', 1, 1),
    (1, '1년 미만', 2, 2),
    (1, '1~2년', 3, 3),
    (1, '2~3년', 4, 4),
    (1, '3년 이상', 5, 5),
    (2, '거의 모른다', 1, 1),
    (2, '용어 정도는 안다', 2, 2),
    (2, '기본 상품 구조를 이해한다', 3, 3),
    (2, '수익률과 리스크를 비교할 수 있다', 4, 4),
    (2, '전문적으로 분석할 수 있다', 5, 5),
    (3, '비상금 마련', 1, 1),
    (3, '단기 목돈 마련', 2, 2),
    (3, '주택·결혼 등 중기 목적', 3, 3),
    (3, '자산 증식', 4, 4),
    (3, '공격적인 수익 창출', 5, 5),
    (4, '무조건 안정성', 1, 1),
    (4, '안정성이 더 중요', 2, 2),
    (4, '반반', 3, 3),
    (4, '수익성이 더 중요', 4, 4),
    (4, '무조건 수익성', 5, 5),
    (5, '6개월 이내', 1, 1),
    (5, '6개월~1년', 2, 2),
    (5, '1~3년', 3, 3),
    (5, '3~5년', 4, 4),
    (5, '5년 이상', 5, 5),
    (6, '원금 손실은 원하지 않는다', 1, 1),
    (6, '아주 작은 손실만 감내한다', 2, 2),
    (6, '어느 정도 손실은 감내 가능하다', 3, 3),
    (6, '변동성을 감수하고 수익을 추구한다', 4, 4),
    (6, '큰 손실 가능성도 감수한다', 5, 5);

INSERT INTO `interests` (`interest_id`, `interest_name`, `is_investment`) VALUES -- true: 투자 # false: 관심사
      (1, 'IT/테크', TRUE),
      (2, '금융', TRUE),
      (3, '헬스케어/바이오', TRUE),
      (4, '에너지/친환경', TRUE),
      (5, '소비재/유통', TRUE),
      (6, '부동산/리츠', TRUE),
      (7, '자동차/모빌리티', TRUE),
      (8, '엔터테인먼트/미디어', TRUE),
      (9, '반도체', TRUE),
      (10, '기타', TRUE),
      (11, '여행', FALSE),
      (12, '운동/피트니스', FALSE),
      (13, '게임', FALSE),
      (14, '독서', FALSE),
      (15, '반려동물', FALSE),
      (16, '요리/맛집', FALSE),
      (17, '뷰티/패션', FALSE),
      (18, '자기계발', FALSE),
      (19, '음악/공연', FALSE),
      (20, '재테크/경제', FALSE);
      
      
-- ==================================================
-- MOCK 종합 데이터
-- ============================================================================
USE youngly_db;

-- ============================================================================
-- 1. 사용자
-- ============================================================================
INSERT INTO users (
    user_id,
    name,
    login_id,
    nickname,
    email,
    profile_image_url,
    created_at,
    updated_at,
    password,
    user_status,
    point,
    birthday,
    is_notification_agreement
) VALUES
    ('user01', '김민준', 'minjun01', '민준', 'minjun01@youngly.test', NULL,
     '2026-06-01 09:00:00', '2026-07-31 09:00:00', 'user01', 'ACTIVE', 60, '1998-03-12 00:00:00', TRUE),
    ('user02', '이서연', 'seoyeon02', '서연', 'seoyeon02@youngly.test', NULL,
     '2026-06-02 09:00:00', '2026-07-31 09:00:00', 'user02', 'ACTIVE', 50, '1999-07-21 00:00:00', TRUE),
    ('user03', '박지훈', 'jihoon03', '지훈', 'jihoon03@youngly.test', NULL,
     '2026-06-03 09:00:00', '2026-07-31 09:00:00', 'user03', 'ACTIVE', 80, '1997-11-05 00:00:00', TRUE),
    ('user04', '최유진', 'yujin04', '유진', 'yujin04@youngly.test', NULL,
     '2026-06-04 09:00:00', '2026-07-31 09:00:00', 'user04', 'ACTIVE', 40, '2000-01-18 00:00:00', TRUE),
    ('user05', '정하늘', 'haneul05', '하늘', 'haneul05@youngly.test', NULL,
     '2026-06-05 09:00:00', '2026-07-31 09:00:00', 'user05', 'ACTIVE', 70, '1998-09-30 00:00:00', TRUE),
    ('user06', '강도윤', 'doyun06', '도윤', 'doyun06@youngly.test', NULL,
     '2026-06-06 09:00:00', '2026-07-31 09:00:00', 'user06', 'ACTIVE', 30, '1999-05-09 00:00:00', TRUE),
    ('user07', '송지아', 'jia07', '지아', 'jia07@youngly.test', NULL,
     '2026-06-07 09:00:00', '2026-07-31 09:00:00', 'user07', 'ACTIVE', 90, '2001-02-14 00:00:00', TRUE),
    ('user08', '오현우', 'hyunwoo08', '현우', 'hyunwoo08@youngly.test', NULL,
     '2026-06-08 09:00:00', '2026-07-31 09:00:00', 'user08', 'ACTIVE', 45, '1998-12-03 00:00:00', TRUE),
    ('user09', '임수빈', 'subin09', '수빈', 'subin09@youngly.test', NULL,
     '2026-06-09 09:00:00', '2026-07-31 09:00:00', 'user09', 'ACTIVE', 55, '2000-06-25 00:00:00', TRUE);

-- ============================================================================
-- 2. KB 원장 계좌
--
-- kb-signup-* 계좌는 아직 accounts에 연결하지 않는다.
-- 회원가입 과정에서 실명/생년월일로 보유 계좌를 조회하고 연결하는 시나리오용이다.
-- ============================================================================
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
    -- user01
    ('kb-deposit-01', 'DEPOSIT', '025202-00-005001', '국민', 1500000.00, 0.10, '김민준', '1998-03-12 00:00:00', '2025-01-10 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-01', 'PENSION', '025202-11-220001', '국민', 5000000.00, 2.50, '김민준', '1998-03-12 00:00:00', '2025-02-10 09:00:00', '2026-07-31 09:00:00'),

    -- user02
    ('kb-deposit-02', 'DEPOSIT', '025202-00-005002', '국민', 1200000.00, 0.10, '이서연', '1999-07-21 00:00:00', '2025-01-11 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-02', 'PENSION', '025202-11-220002', '국민', 4200000.00, 2.50, '이서연', '1999-07-21 00:00:00', '2025-02-11 09:00:00', '2026-07-31 09:00:00'),

    -- user03
    ('kb-deposit-03', 'DEPOSIT', '025202-00-005003', '국민', 1800000.00, 0.10, '박지훈', '1997-11-05 00:00:00', '2025-01-12 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-03', 'PENSION', '025202-11-220003', '국민', 6500000.00, 2.50, '박지훈', '1997-11-05 00:00:00', '2025-02-12 09:00:00', '2026-07-31 09:00:00'),

    -- user04
    ('kb-deposit-04', 'DEPOSIT', '025202-00-005004', '국민', 900000.00, 0.10, '최유진', '2000-01-18 00:00:00', '2025-01-13 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-04', 'PENSION', '025202-11-220004', '국민', 3100000.00, 2.50, '최유진', '2000-01-18 00:00:00', '2025-02-13 09:00:00', '2026-07-31 09:00:00'),

    -- user05
    ('kb-deposit-05', 'DEPOSIT', '025202-00-005005', '국민', 2100000.00, 0.10, '정하늘', '1998-09-30 00:00:00', '2025-01-14 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-05', 'PENSION', '025202-11-220005', '국민', 7200000.00, 2.50, '정하늘', '1998-09-30 00:00:00', '2025-02-14 09:00:00', '2026-07-31 09:00:00'),

    -- user06
    ('kb-deposit-06', 'DEPOSIT', '025202-00-005006', '국민', 1350000.00, 0.10, '강도윤', '1999-05-09 00:00:00', '2025-01-15 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-06', 'PENSION', '025202-11-220006', '국민', 4600000.00, 2.50, '강도윤', '1999-05-09 00:00:00', '2025-02-15 09:00:00', '2026-07-31 09:00:00'),

    -- user07
    ('kb-deposit-07', 'DEPOSIT', '025202-00-005007', '국민', 1950000.00, 0.10, '송지아', '2001-02-14 00:00:00', '2025-01-16 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-07', 'PENSION', '025202-11-220007', '국민', 5300000.00, 2.50, '송지아', '2001-02-14 00:00:00', '2025-02-16 09:00:00', '2026-07-31 09:00:00'),

    -- user08
    ('kb-deposit-08', 'DEPOSIT', '025202-00-005008', '국민', 1650000.00, 0.10, '오현우', '1998-12-03 00:00:00', '2025-01-17 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-08', 'PENSION', '025202-11-220008', '국민', 6100000.00, 2.50, '오현우', '1998-12-03 00:00:00', '2025-02-17 09:00:00', '2026-07-31 09:00:00'),

    -- user09
    ('kb-deposit-09', 'DEPOSIT', '025202-00-005009', '국민', 2300000.00, 0.10, '임수빈', '2000-06-25 00:00:00', '2025-01-18 09:00:00', '2026-07-31 09:00:00'),
    ('kb-pension-09', 'PENSION', '025202-11-220009', '국민', 6800000.00, 2.50, '임수빈', '2000-06-25 00:00:00', '2025-02-18 09:00:00', '2026-07-31 09:00:00'),

    -- 모임통장 3개
    ('kb-moim-01', 'MOIM', '025202-22-330001', '국민', 600000.00, 2.50, '김민준', '1998-03-12 00:00:00', '2026-06-20 10:00:00', '2026-07-29 09:00:00'),
    ('kb-moim-02', 'MOIM', '025202-22-330002', '국민', 450000.00, 2.50, '최유진', '2000-01-18 00:00:00', '2026-06-21 10:00:00', '2026-07-29 09:10:00'),
    ('kb-moim-03', 'MOIM', '025202-22-330003', '국민', 300000.00, 2.50, '송지아', '2001-02-14 00:00:00', '2026-06-22 10:00:00', '2026-07-29 09:20:00'),

    -- 회원가입 및 최초 계좌 연결 API 테스트용: 아직 users/accounts에 미연결
    ('kb-signup-deposit-01', 'DEPOSIT', '025202-00-009001', '국민', 2500000.00, 0.10, '윤도현', '2001-04-15 00:00:00', '2025-03-01 09:00:00', '2026-07-31 09:00:00'),
    ('kb-signup-pension-01', 'PENSION', '025202-11-229001', '국민', 3800000.00, 2.50, '윤도현', '2001-04-15 00:00:00', '2025-03-01 09:10:00', '2026-07-31 09:00:00'),
    ('kb-signup-deposit-02', 'DEPOSIT', '025202-00-009002', '국민', 1750000.00, 0.10, '한소희', '2002-08-07 00:00:00', '2025-03-02 09:00:00', '2026-07-31 09:00:00'),
    ('kb-signup-pension-02', 'PENSION', '025202-11-229002', '국민', 2900000.00, 2.50, '한소희', '2002-08-07 00:00:00', '2025-03-02 09:10:00', '2026-07-31 09:00:00');

-- ============================================================================
-- 3. 서비스에 연결된 개인 계좌
--
-- 홀수 사용자: 입출금=OUTCOME / 개인연금=INCOME
-- 짝수 사용자: 입출금=INOUTCOME / 개인연금=NONE
-- ============================================================================
INSERT INTO accounts (
    account_id,
    user_id,
    kb_account_id,
    created_at,
    synced_at,
    account_status,
    account_name,
    updated_at
) VALUES
    ('account-user01-deposit', 'user01', 'kb-deposit-01', '2026-06-01 09:10:00', '2026-06-01 09:10:00', 'OUTCOME', '국민025202-00-005001', '2026-07-31 09:00:00'),
    ('account-user01-pension', 'user01', 'kb-pension-01', '2026-06-01 09:11:00', '2026-06-01 09:11:00', 'INCOME', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user02-deposit', 'user02', 'kb-deposit-02', '2026-06-02 09:10:00', '2026-06-02 09:10:00', 'INOUTCOME', '국민025202-00-005002', '2026-07-31 09:00:00'),
    ('account-user02-pension', 'user02', 'kb-pension-02', '2026-06-02 09:11:00', '2026-06-02 09:11:00', 'NONE', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user03-deposit', 'user03', 'kb-deposit-03', '2026-06-03 09:10:00', '2026-06-03 09:10:00', 'OUTCOME', '국민025202-00-005003', '2026-07-31 09:00:00'),
    ('account-user03-pension', 'user03', 'kb-pension-03', '2026-06-03 09:11:00', '2026-06-03 09:11:00', 'INCOME', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user04-deposit', 'user04', 'kb-deposit-04', '2026-06-04 09:10:00', '2026-06-04 09:10:00', 'INOUTCOME', '국민025202-00-005004', '2026-07-31 09:00:00'),
    ('account-user04-pension', 'user04', 'kb-pension-04', '2026-06-04 09:11:00', '2026-06-04 09:11:00', 'NONE', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user05-deposit', 'user05', 'kb-deposit-05', '2026-06-05 09:10:00', '2026-06-05 09:10:00', 'OUTCOME', '국민025202-00-005005', '2026-07-31 09:00:00'),
    ('account-user05-pension', 'user05', 'kb-pension-05', '2026-06-05 09:11:00', '2026-06-05 09:11:00', 'INCOME', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user06-deposit', 'user06', 'kb-deposit-06', '2026-06-06 09:10:00', '2026-06-06 09:10:00', 'INOUTCOME', '국민025202-00-005006', '2026-07-31 09:00:00'),
    ('account-user06-pension', 'user06', 'kb-pension-06', '2026-06-06 09:11:00', '2026-06-06 09:11:00', 'NONE', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user07-deposit', 'user07', 'kb-deposit-07', '2026-06-07 09:10:00', '2026-06-07 09:10:00', 'OUTCOME', '국민025202-00-005007', '2026-07-31 09:00:00'),
    ('account-user07-pension', 'user07', 'kb-pension-07', '2026-06-07 09:11:00', '2026-06-07 09:11:00', 'INCOME', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user08-deposit', 'user08', 'kb-deposit-08', '2026-06-08 09:10:00', '2026-06-08 09:10:00', 'INOUTCOME', '국민025202-00-005008', '2026-07-31 09:00:00'),
    ('account-user08-pension', 'user08', 'kb-pension-08', '2026-06-08 09:11:00', '2026-06-08 09:11:00', 'NONE', 'KB개인연금', '2026-07-31 09:00:00'),
    ('account-user09-deposit', 'user09', 'kb-deposit-09', '2026-06-09 09:10:00', '2026-06-09 09:10:00', 'OUTCOME', '국민025202-00-005009', '2026-07-31 09:00:00'),
    ('account-user09-pension', 'user09', 'kb-pension-09', '2026-06-09 09:11:00', '2026-06-09 09:11:00', 'INCOME', 'KB개인연금', '2026-07-31 09:00:00');

-- ============================================================================
-- 4. 모임통장 연결 정보
-- ============================================================================
INSERT INTO moim_accounts (
    moim_account_id,
    user_id,
    kb_account_id,
    created_at,
    synced_at,
    updated_at,
    account_name
) VALUES
    ('moim-account-01', 'user01', 'kb-moim-01',
     '2026-06-20 10:05:00', '2026-06-20 10:05:00', '2026-07-29 09:00:00', '국민025202-22-330001'),
    ('moim-account-02', 'user04', 'kb-moim-02',
     '2026-06-21 10:05:00', '2026-06-21 10:05:00', '2026-07-29 09:10:00', '국민025202-22-330002'),
    ('moim-account-03', 'user07', 'kb-moim-03',
     '2026-06-22 10:05:00', '2026-06-22 10:05:00', '2026-07-29 09:20:00', '국민025202-22-330003');

-- ============================================================================
-- 5. 그룹 3개
-- ============================================================================
INSERT INTO `groups` (
    group_id,
    moim_account_id,
    user_id,
    invite_code,
    group_name,
    group_count,
    created_at,
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
    updated_at
) VALUES
    ('group-exercise-01', 'moim-account-01', 'user01',
     '11111111-1111-4111-8111-111111111111', '새벽 러닝 챌린지', 3,
     '2026-06-20 11:00:00', '주 3회 이상 30분 러닝', 'EXERCISE',
     '한 달 동안 함께 달리며 운동 습관을 만드는 모임',
     '1:40/2:60/3:80', 7, 3, 28, 2, 200000.00, 'ONGOING', '2026-07-31 09:00:00'),
    ('group-study-01', 'moim-account-02', 'user04',
     '22222222-2222-4222-8222-222222222222', '매일 코딩 챌린지', 3,
     '2026-06-21 11:00:00', '하루 1커밋 또는 알고리즘 1문제', 'STUDY',
     '매일 꾸준히 개발 공부를 인증하는 모임',
     '1:50/2:70/3:90', 7, 5, 28, 0, 150000.00, 'ONGOING', '2026-07-31 09:00:00'),
    ('group-reading-01', 'moim-account-03', 'user07',
     '33333333-3333-4333-8333-333333333333', '한 달 독서 챌린지', 3,
     '2026-06-22 11:00:00', '주 4회 이상 20분 독서', 'READING',
     '매일 책을 읽고 짧게 인증하는 독서 습관 모임',
     '1:40/2:60/3:80', 7, 4, 28, 0, 100000.00, 'ONGOING', '2026-07-31 09:00:00');

-- ============================================================================
-- 6. 그룹 이력 최초 스냅샷
-- ============================================================================
INSERT INTO group_history (
    moim_account_id,
    group_id,
    group_history_version,
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
    created_at
) VALUES
    ('moim-account-01', 'group-exercise-01', 1,
     '11111111-1111-4111-8111-111111111111', '새벽 러닝 챌린지', 3,
     '주 3회 이상 30분 러닝', 'EXERCISE',
     '한 달 동안 함께 달리며 운동 습관을 만드는 모임',
     '1:40/2:60/3:80', 7, 3, 28, 200000.00, 'ONGOING', '2026-06-20 11:00:00'),
    ('moim-account-02', 'group-study-01', 1,
     '22222222-2222-4222-8222-222222222222', '매일 코딩 챌린지', 3,
     '하루 1커밋 또는 알고리즘 1문제', 'STUDY',
     '매일 꾸준히 개발 공부를 인증하는 모임',
     '1:50/2:70/3:90', 7, 5, 28, 150000.00, 'ONGOING', '2026-06-21 11:00:00'),
    ('moim-account-03', 'group-reading-01', 1,
     '33333333-3333-4333-8333-333333333333', '한 달 독서 챌린지', 3,
     '주 4회 이상 20분 독서', 'READING',
     '매일 책을 읽고 짧게 인증하는 독서 습관 모임',
     '1:40/2:60/3:80', 7, 4, 28, 100000.00, 'ONGOING', '2026-06-22 11:00:00');

-- ============================================================================
-- 7. 그룹 참여자
-- ============================================================================
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
    ('group-exercise-01', 'user01', 'ACTIVE', '2026-06-20 11:00:00', 200000.00, 4, '2026-06-20 11:00:00', '2026-07-31 09:00:00'),
    ('group-exercise-01', 'user02', 'ACTIVE', '2026-06-20 12:00:00', 200000.00, 3, '2026-06-20 11:30:00', '2026-07-31 09:00:00'),
    ('group-exercise-01', 'user03', 'ACTIVE', '2026-06-20 12:10:00', 200000.00, 2, '2026-06-20 11:40:00', '2026-07-31 09:00:00'),
    ('group-study-01', 'user04', 'ACTIVE', '2026-06-21 11:00:00', 150000.00, 4, '2026-06-21 11:00:00', '2026-07-31 09:00:00'),
    ('group-study-01', 'user05', 'ACTIVE', '2026-06-21 12:00:00', 150000.00, 3, '2026-06-21 11:30:00', '2026-07-31 09:00:00'),
    ('group-study-01', 'user06', 'ACTIVE', '2026-06-21 12:10:00', 150000.00, 5, '2026-06-21 11:40:00', '2026-07-31 09:00:00'),
    ('group-reading-01', 'user07', 'ACTIVE', '2026-06-22 11:00:00', 100000.00, 5, '2026-06-22 11:00:00', '2026-07-31 09:00:00'),
    ('group-reading-01', 'user08', 'ACTIVE', '2026-06-22 12:00:00', 100000.00, 4, '2026-06-22 11:30:00', '2026-07-31 09:00:00'),
    ('group-reading-01', 'user09', 'ACTIVE', '2026-06-22 12:10:00', 100000.00, 3, '2026-06-22 11:40:00', '2026-07-31 09:00:00');

-- ============================================================================
-- 8. 라운드
-- ============================================================================
INSERT INTO rounds (
    group_id,
    round_no,
    start_date,
    end_date,
    round_status,
    created_at
) VALUES
    ('group-exercise-01', 1, '2026-06-28', '2026-07-25', 'SETTLED', '2026-06-27 23:00:00'),
    ('group-exercise-01', 2, '2026-07-26', '2026-08-22', 'ONGOING', '2026-07-25 23:00:00'),
    ('group-study-01', 1, '2026-07-05', '2026-08-01', 'ONGOING', '2026-07-04 23:00:00'),
    ('group-reading-01', 1, '2026-07-12', '2026-08-08', 'ONGOING', '2026-07-11 23:00:00');

-- ==========================================================================
-- round_id=3(group-study-01 1라운드) 참여자별 round_history 테스트 데이터
-- data.sql 전체를 다시 실행하지 않고 이 구간만 반복 실행하면 이력이 중복될 수 있다.
-- ==========================================================================
INSERT INTO round_history (
    round_id,
    user_id,
    account_id,
    moim_account_id,
    success_count,
    remaining_fail_pass_count,
    created_at
) VALUES
    (3, 'user04', 'account-user04-deposit', 'moim-account-02', 0, 0, '2026-07-04 23:00:00'),
    (3, 'user05', 'account-user05-pension', 'moim-account-02', 0, 0, '2026-07-04 23:00:00'),
    (3, 'user06', 'account-user06-deposit', 'moim-account-02', 0, 0, '2026-07-04 23:00:00');

-- ============================================================================
-- 9. 통합 모임통장 거래내역
--
-- transaction_category ENUM:
--   CHARGE     : 최초 예치 및 재충전
--   SETTLEMENT : 미래 적립금 정산
--   REFUND     : 예치금 환불
--
-- group_user_id와 round_id는 AUTO_INCREMENT 값을 직접 가정하지 않고
-- UNIQUE 키(group_id, user_id), (group_id, round_no)로 조회한다.
-- ============================================================================
INSERT INTO account_transactions (
    kb_account_id,
    group_user_id,
    round_id,
    transaction_type,
    amount,
    balance_after,
    description,
    created_at,
    transaction_category,
    idempotency_key,
    another_account_number,
    another_name,
    another_bank_name
) VALUES
    -- 새벽 러닝 챌린지 최초 예치: 3명 x 200,000원
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user01'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 1),
     'DEPOSIT', 200000.00, 200000.00, '김민준 최초 예치금 납입',
     '2026-06-27 10:00:00', 'CHARGE', 'GROUP01-R01-USER01-INITIAL',
     '025202-00-005001', '김민준', '국민'),
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user02'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 1),
     'DEPOSIT', 200000.00, 400000.00, '이서연 최초 예치금 납입',
     '2026-06-27 10:10:00', 'CHARGE', 'GROUP01-R01-USER02-INITIAL',
     '025202-00-005002', '이서연', '국민'),
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user03'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 1),
     'DEPOSIT', 200000.00, 600000.00, '박지훈 최초 예치금 납입',
     '2026-06-27 10:20:00', 'CHARGE', 'GROUP01-R01-USER03-INITIAL',
     '025202-00-005003', '박지훈', '국민'),

    -- 1라운드 정산 지급 후 2라운드 재충전
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user01'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 1),
     'WITHDRAW', 120000.00, 480000.00, '1라운드 미래 적립금 정산',
     '2026-07-28 09:00:00', 'SETTLEMENT', 'GROUP01-R01-USER01-SETTLEMENT',
     '025202-11-220001', '김민준', '국민'),
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user02'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 1),
     'WITHDRAW', 80000.00, 400000.00, '1라운드 미래 적립금 정산',
     '2026-07-28 09:01:00', 'SETTLEMENT', 'GROUP01-R01-USER02-SETTLEMENT',
     '025202-00-005002', '이서연', '국민'),
    ('kb-moim-01',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-exercise-01' AND user_id = 'user03'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'DEPOSIT', 200000.00, 600000.00, '2라운드 예치금 재충전',
     '2026-07-29 09:00:00', 'CHARGE', 'GROUP01-R02-USER03-RECHARGE',
     '025202-00-005003', '박지훈', '국민'),

    -- 매일 코딩 챌린지 최초 예치: 3명 x 150,000원
    ('kb-moim-02',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-study-01' AND user_id = 'user04'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-study-01' AND round_no = 1),
     'DEPOSIT', 150000.00, 150000.00, '최유진 최초 예치금 납입',
     '2026-07-04 10:00:00', 'CHARGE', 'GROUP02-R01-USER04-INITIAL',
     '025202-00-005004', '최유진', '국민'),
    ('kb-moim-02',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-study-01' AND user_id = 'user05'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-study-01' AND round_no = 1),
     'DEPOSIT', 150000.00, 300000.00, '정하늘 최초 예치금 납입',
     '2026-07-04 10:10:00', 'CHARGE', 'GROUP02-R01-USER05-INITIAL',
     '025202-00-005005', '정하늘', '국민'),
    ('kb-moim-02',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-study-01' AND user_id = 'user06'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-study-01' AND round_no = 1),
     'DEPOSIT', 150000.00, 450000.00, '강도윤 최초 예치금 납입',
     '2026-07-04 10:20:00', 'CHARGE', 'GROUP02-R01-USER06-INITIAL',
     '025202-00-005006', '강도윤', '국민'),

    -- 한 달 독서 챌린지 최초 예치: 3명 x 100,000원
    ('kb-moim-03',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-reading-01' AND user_id = 'user07'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-reading-01' AND round_no = 1),
     'DEPOSIT', 100000.00, 100000.00, '송지아 최초 예치금 납입',
     '2026-07-11 10:00:00', 'CHARGE', 'GROUP03-R01-USER07-INITIAL',
     '025202-00-005007', '송지아', '국민'),
    ('kb-moim-03',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-reading-01' AND user_id = 'user08'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-reading-01' AND round_no = 1),
     'DEPOSIT', 100000.00, 200000.00, '오현우 최초 예치금 납입',
     '2026-07-11 10:10:00', 'CHARGE', 'GROUP03-R01-USER08-INITIAL',
     '025202-00-005008', '오현우', '국민'),
    ('kb-moim-03',
     (SELECT group_user_id FROM group_users WHERE group_id = 'group-reading-01' AND user_id = 'user09'),
     (SELECT round_id FROM rounds WHERE group_id = 'group-reading-01' AND round_no = 1),
     'DEPOSIT', 100000.00, 300000.00, '임수빈 최초 예치금 납입',
     '2026-07-11 10:20:00', 'CHARGE', 'GROUP03-R01-USER09-INITIAL',
     '025202-00-005009', '임수빈', '국민');

-- ==========================================================================
-- 주간 결산 테스트 데이터 시작
-- POST /api/dev/weekly-settlements?date=2026-08-03
-- group-exercise-01의 2라운드 1주차(2026-07-26~2026-08-01)를 결산한다.
-- user01: 승인 3개, 패스 2개 -> 정상 성공, 패스 유지
-- user02: 승인 2개, 패스 1개 -> 부족분 1개를 패스로 충당하여 성공, 패스 0
-- user03: 승인 1개, 패스 1개 -> 부족분 2개를 충당하지 못해 실패, 패스 유지
-- ==========================================================================

-- 라운드 시작 당시 결산 대상이 되는 참여자별 이력을 생성한다.
INSERT INTO round_history (
    round_id,
    user_id,
    account_id,
    moim_account_id,
    success_count,
    remaining_fail_pass_count,
    created_at
) VALUES
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user01', 'account-user01-pension', 'moim-account-01', 0, 2, '2026-07-25 23:00:00'),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user02', 'account-user02-deposit', 'moim-account-01', 0, 1, '2026-07-25 23:00:00'),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user03', 'account-user03-pension', 'moim-account-01', 0, 1, '2026-07-25 23:00:00');

-- 1주차 게시물을 생성한다. APPROVED 상태인 게시물만 주간 결산에서 집계된다.
INSERT INTO posts (
    round_id,
    user_id,
    photo_url,
    content,
    post_status,
    created_at,
    posted_at,
    status_changed_at,
    approve_count,
    reject_count
) VALUES
    -- user01: 승인 3개
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user01', '/test/weekly-settlement/user01-day1.jpg', 'user01 1일차 러닝',
     'APPROVED', '2026-07-26 07:00:00', '2026-07-26 07:00:00', '2026-07-26 09:00:00', 2, 0),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user01', '/test/weekly-settlement/user01-day3.jpg', 'user01 3일차 러닝',
     'APPROVED', '2026-07-28 07:00:00', '2026-07-28 07:00:00', '2026-07-28 09:00:00', 2, 0),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user01', '/test/weekly-settlement/user01-day5.jpg', 'user01 5일차 러닝',
     'APPROVED', '2026-07-30 07:00:00', '2026-07-30 07:00:00', '2026-07-30 09:00:00', 2, 0),

    -- user02: 승인 2개와 반려 1개(최소 횟수 미달)
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user02', '/test/weekly-settlement/user02-day2.jpg', 'user02 2일차 러닝',
     'APPROVED', '2026-07-27 07:00:00', '2026-07-27 07:00:00', '2026-07-27 09:00:00', 2, 0),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user02', '/test/weekly-settlement/user02-day4.jpg', 'user02 4일차 러닝',
     'APPROVED', '2026-07-29 07:00:00', '2026-07-29 07:00:00', '2026-07-29 09:00:00', 2, 0),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user02', '/test/weekly-settlement/user02-day6-rejected.jpg', 'user02 6일차 반려 게시물',
     'REJECTED', '2026-07-31 07:00:00', '2026-07-31 07:00:00', '2026-07-31 09:00:00', 0, 2),

    -- user03: 승인 1개와 반려 3개
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user03', '/test/weekly-settlement/user03-day1.jpg', 'user03 1일차 러닝',
     'APPROVED', '2026-07-26 08:00:00', '2026-07-26 08:00:00', '2026-07-26 10:00:00', 2, 0),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user03', '/test/weekly-settlement/user03-day2.jpg', 'user03 2일차 러닝',
     'REJECTED', '2026-07-27 08:00:00', '2026-07-27 08:00:00', '2026-07-27 10:00:00', 0, 2),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user03', '/test/weekly-settlement/user03-day4.jpg', 'user03 4일차 러닝',
     'REJECTED', '2026-07-29 08:00:00', '2026-07-29 08:00:00', '2026-07-29 10:00:00', 0, 2),
    ((SELECT round_id FROM rounds WHERE group_id = 'group-exercise-01' AND round_no = 2),
     'user03', '/test/weekly-settlement/user03-day7.jpg', 'user03 7일차 러닝',
     'REJECTED', '2026-08-01 08:00:00', '2026-08-01 08:00:00', '2026-08-01 10:00:00', 0, 2);

-- 각 테스트 게시물의 최초 최종 상태를 게시물 변경 이력으로 저장한다.
INSERT INTO post_history (
    post_id,
    post_history_version,
    photo_url,
    content,
    post_status,
    created_at
)
SELECT p.post_id,
       1,
       p.photo_url,
       p.content,
       p.post_status,
       p.status_changed_at
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%';

-- 작성자를 제외한 나머지 두 사용자의 승인 또는 반려 평가 이력을 저장한다.
INSERT INTO post_approvals (
    user_id,
    post_id,
    approval_status,
    reject_reason,
    created_at,
    updated_at
)
SELECT CASE p.user_id
           WHEN 'user01' THEN 'user02'
           WHEN 'user02' THEN 'user01'
           ELSE 'user01'
       END,
       p.post_id,
       CASE p.post_status WHEN 'APPROVED' THEN 'APPROVE' ELSE 'REJECT' END,
       CASE p.post_status WHEN 'REJECTED' THEN '인증 사진에서 수행 여부를 확인할 수 없습니다.' ELSE NULL END,
       p.status_changed_at,
       p.status_changed_at
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%';

INSERT INTO post_approvals (
    user_id,
    post_id,
    approval_status,
    reject_reason,
    created_at,
    updated_at
)
SELECT CASE p.user_id
           WHEN 'user01' THEN 'user03'
           WHEN 'user02' THEN 'user03'
           ELSE 'user02'
       END,
       p.post_id,
       CASE p.post_status WHEN 'APPROVED' THEN 'APPROVE' ELSE 'REJECT' END,
       CASE p.post_status WHEN 'REJECTED' THEN '인증 사진에서 수행 여부를 확인할 수 없습니다.' ELSE NULL END,
       p.status_changed_at,
       p.status_changed_at
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%';

-- ==========================================================================
-- 주간 결산 테스트 데이터 끝
-- ==========================================================================

-- ==========================================================================
-- round_id=3 주간 결산 게시물 테스트 데이터 시작
-- POST /api/dev/weekly-settlements?date=2026-08-03
-- 4주차(2026-07-26~2026-08-01) 승인 수: user04=5, user05=4, user06=5
-- group-study-01의 min_count가 5이므로 user04와 user06만 성공한다.
-- ==========================================================================

INSERT INTO posts (
    round_id,
    user_id,
    photo_url,
    content,
    post_status,
    created_at,
    posted_at,
    status_changed_at,
    approve_count,
    reject_count
) VALUES
    -- user04: 승인 5개
    (3, 'user04', '/test/weekly-settlement-round3/user04-day1.jpg', 'user04 1일차 코딩 인증',
     'APPROVED', '2026-07-26 20:00:00', '2026-07-26 20:00:00', '2026-07-26 22:00:00', 2, 0),
    (3, 'user04', '/test/weekly-settlement-round3/user04-day2.jpg', 'user04 2일차 코딩 인증',
     'APPROVED', '2026-07-27 20:00:00', '2026-07-27 20:00:00', '2026-07-27 22:00:00', 2, 0),
    (3, 'user04', '/test/weekly-settlement-round3/user04-day3.jpg', 'user04 3일차 코딩 인증',
     'APPROVED', '2026-07-28 20:00:00', '2026-07-28 20:00:00', '2026-07-28 22:00:00', 2, 0),
    (3, 'user04', '/test/weekly-settlement-round3/user04-day5.jpg', 'user04 5일차 코딩 인증',
     'APPROVED', '2026-07-30 20:00:00', '2026-07-30 20:00:00', '2026-07-30 22:00:00', 2, 0),
    (3, 'user04', '/test/weekly-settlement-round3/user04-day7.jpg', 'user04 7일차 코딩 인증',
     'APPROVED', '2026-08-01 20:00:00', '2026-08-01 20:00:00', '2026-08-01 22:00:00', 2, 0),

    -- user05: 승인 4개와 반려 1개
    (3, 'user05', '/test/weekly-settlement-round3/user05-day1.jpg', 'user05 1일차 코딩 인증',
     'APPROVED', '2026-07-26 20:10:00', '2026-07-26 20:10:00', '2026-07-26 22:10:00', 2, 0),
    (3, 'user05', '/test/weekly-settlement-round3/user05-day2.jpg', 'user05 2일차 코딩 인증',
     'APPROVED', '2026-07-27 20:10:00', '2026-07-27 20:10:00', '2026-07-27 22:10:00', 2, 0),
    (3, 'user05', '/test/weekly-settlement-round3/user05-day3.jpg', 'user05 3일차 코딩 인증',
     'APPROVED', '2026-07-28 20:10:00', '2026-07-28 20:10:00', '2026-07-28 22:10:00', 2, 0),
    (3, 'user05', '/test/weekly-settlement-round3/user05-day4.jpg', 'user05 4일차 코딩 인증',
     'APPROVED', '2026-07-29 20:10:00', '2026-07-29 20:10:00', '2026-07-29 22:10:00', 2, 0),
    (3, 'user05', '/test/weekly-settlement-round3/user05-day5-rejected.jpg', 'user05 반려 대상 인증',
     'REJECTED', '2026-07-30 20:10:00', '2026-07-30 20:10:00', '2026-07-30 22:10:00', 0, 2),

    -- user06: 승인 5개
    (3, 'user06', '/test/weekly-settlement-round3/user06-day1.jpg', 'user06 1일차 코딩 인증',
     'APPROVED', '2026-07-26 20:20:00', '2026-07-26 20:20:00', '2026-07-26 22:20:00', 2, 0),
    (3, 'user06', '/test/weekly-settlement-round3/user06-day2.jpg', 'user06 2일차 코딩 인증',
     'APPROVED', '2026-07-27 20:20:00', '2026-07-27 20:20:00', '2026-07-27 22:20:00', 2, 0),
    (3, 'user06', '/test/weekly-settlement-round3/user06-day4.jpg', 'user06 4일차 코딩 인증',
     'APPROVED', '2026-07-29 20:20:00', '2026-07-29 20:20:00', '2026-07-29 22:20:00', 2, 0),
    (3, 'user06', '/test/weekly-settlement-round3/user06-day6.jpg', 'user06 6일차 코딩 인증',
     'APPROVED', '2026-07-31 20:20:00', '2026-07-31 20:20:00', '2026-07-31 22:20:00', 2, 0),
    (3, 'user06', '/test/weekly-settlement-round3/user06-day7.jpg', 'user06 7일차 코딩 인증',
     'APPROVED', '2026-08-01 20:20:00', '2026-08-01 20:20:00', '2026-08-01 22:20:00', 2, 0);

-- round_id=3 테스트 게시물의 최종 상태를 변경 이력으로 저장한다.
INSERT INTO post_history (
    post_id,
    post_history_version,
    photo_url,
    content,
    post_status,
    created_at
)
SELECT p.post_id,
       1,
       p.photo_url,
       p.content,
       p.post_status,
       p.status_changed_at
FROM posts p
WHERE p.round_id = 3
  AND p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- 첫 번째 평가자: 게시물 작성자를 제외한 그룹 참여자의 승인 또는 반려 내역.
INSERT INTO post_approvals (
    user_id,
    post_id,
    approval_status,
    reject_reason,
    created_at,
    updated_at
)
SELECT CASE p.user_id
           WHEN 'user04' THEN 'user05'
           WHEN 'user05' THEN 'user04'
           ELSE 'user04'
       END,
       p.post_id,
       CASE p.post_status WHEN 'APPROVED' THEN 'APPROVE' ELSE 'REJECT' END,
       CASE p.post_status WHEN 'REJECTED' THEN '인증 내용이 챌린지 기준을 충족하지 않습니다.' ELSE NULL END,
       p.status_changed_at,
       p.status_changed_at
FROM posts p
WHERE p.round_id = 3
  AND p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- 두 번째 평가자: 과반수 판정을 위한 나머지 그룹 참여자의 평가 내역.
INSERT INTO post_approvals (
    user_id,
    post_id,
    approval_status,
    reject_reason,
    created_at,
    updated_at
)
SELECT CASE p.user_id
           WHEN 'user04' THEN 'user06'
           WHEN 'user05' THEN 'user06'
           ELSE 'user05'
       END,
       p.post_id,
       CASE p.post_status WHEN 'APPROVED' THEN 'APPROVE' ELSE 'REJECT' END,
       CASE p.post_status WHEN 'REJECTED' THEN '인증 내용이 챌린지 기준을 충족하지 않습니다.' ELSE NULL END,
       p.status_changed_at,
       p.status_changed_at
FROM posts p
WHERE p.round_id = 3
  AND p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- ==========================================================================
-- round_id=3 주간 결산 게시물 테스트 데이터 끝
-- ==========================================================================

-- ==========================================================================
-- 주간 결산 테스트 게시물 reactions/comments 데이터 시작
-- 승인 게시물에는 LIKE 2개, 반려 게시물에는 DISLIKE 2개를 생성한다.
-- 모든 테스트 게시물에는 작성자가 아닌 그룹 참여자의 댓글 1개를 생성한다.
-- ==========================================================================

-- 첫 번째 참여자의 리액션.
INSERT INTO post_reactions (
    post_id,
    user_id,
    reaction_type,
    created_at,
    updated_at
)
SELECT p.post_id,
       CASE p.user_id
           WHEN 'user01' THEN 'user02'
           WHEN 'user02' THEN 'user01'
           WHEN 'user03' THEN 'user01'
           WHEN 'user04' THEN 'user05'
           WHEN 'user05' THEN 'user04'
           ELSE 'user04'
       END,
       CASE p.post_status WHEN 'APPROVED' THEN 'LIKE' ELSE 'DISLIKE' END,
       DATE_ADD(p.posted_at, INTERVAL 30 MINUTE),
       DATE_ADD(p.posted_at, INTERVAL 30 MINUTE)
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%'
   OR p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- 두 번째 참여자의 리액션.
INSERT INTO post_reactions (
    post_id,
    user_id,
    reaction_type,
    created_at,
    updated_at
)
SELECT p.post_id,
       CASE p.user_id
           WHEN 'user01' THEN 'user03'
           WHEN 'user02' THEN 'user03'
           WHEN 'user03' THEN 'user02'
           WHEN 'user04' THEN 'user06'
           WHEN 'user05' THEN 'user06'
           ELSE 'user05'
       END,
       CASE p.post_status WHEN 'APPROVED' THEN 'LIKE' ELSE 'DISLIKE' END,
       DATE_ADD(p.posted_at, INTERVAL 45 MINUTE),
       DATE_ADD(p.posted_at, INTERVAL 45 MINUTE)
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%'
   OR p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- 작성자가 아닌 참여자의 피드 댓글.
INSERT INTO post_comments (
    user_id,
    post_id,
    content,
    created_at,
    updated_at
)
SELECT CASE p.user_id
           WHEN 'user01' THEN 'user02'
           WHEN 'user02' THEN 'user01'
           WHEN 'user03' THEN 'user01'
           WHEN 'user04' THEN 'user05'
           WHEN 'user05' THEN 'user04'
           ELSE 'user04'
       END,
       p.post_id,
       CASE p.post_status
           WHEN 'APPROVED' THEN '오늘도 목표 달성 수고했어요!'
           ELSE '다음 인증에서는 수행 내용을 조금 더 잘 보여주세요.'
       END,
       DATE_ADD(p.posted_at, INTERVAL 60 MINUTE),
       DATE_ADD(p.posted_at, INTERVAL 60 MINUTE)
FROM posts p
WHERE p.photo_url LIKE '/test/weekly-settlement/%'
   OR p.photo_url LIKE '/test/weekly-settlement-round3/%';

-- posts의 비정규화된 리액션/댓글 카운트를 실제 생성된 데이터와 일치시킨다.
SET SQL_SAFE_UPDATES = 0;

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
    )
WHERE p.photo_url LIKE '/test/weekly-settlement/%'
   OR p.photo_url LIKE '/test/weekly-settlement-round3/%';

SET SQL_SAFE_UPDATES = 1;
-- ==========================================================================
-- 주간 결산 테스트 게시물 reactions/comments 데이터 끝
-- ==========================================================================


-- ai mock
INSERT INTO users (
    user_id,
    name,
    login_id,
    nickname,
    email,
    profile_image_url,
    created_at,
    updated_at,
    password,
    user_status,
    point,
    birthday,
    is_notification_agreement
) VALUES (
             'aitest01',          -- user_id
             'AI 테스트',         -- name
             'aitest01',          -- login_id
             'AI테스트',          -- nickname
             'aitest01@youngly.test',
             NULL,
             '2026-08-06 09:00:00',
             '2026-08-06 09:00:00',
             '$2a$10$JG6aEjTMBlkUc/ZPV0THRe/xEY1VN5VNsFAP7JUm/cv6kGMPZoF4e', -- 여기 bcrypt로 생성한 해시 문자열 그대로
             'ACTIVE',
             0,
             '1998-01-01 00:00:00', -- 예시 생일 (20대 후반)
             TRUE);

INSERT INTO survey_results (
    survey_result_id,
    user_id,
    answers_json,
    total_score,
    baseline,
    submitted_at,
    calculated_at,
    created_at
) VALUES (
             1001, -- 다른 값과 안 겹치도록 충분히 큰 PK
             'aitest01',
             '{"Q1":4,"Q2":4,"Q3":4,"Q4":4,"Q5":3,"Q6":4}', -- 예시 응답
             23,                                             -- AGGRESSIVE 범위(21~25)
             'AGGRESSIVE',
             '2026-08-06 09:05:00',
             '2026-08-06 09:05:10',
             '2026-08-06 09:05:10'
         );



INSERT INTO interest_users (interest_id, user_id, created_at)
VALUES (1, 'aitest01', '2026-08-06 09:06:00'),  -- IT/테크 (투자)
       (9, 'aitest01', '2026-08-06 09:06:00'),  -- 반도체 (투자)
       (12, 'aitest01', '2026-08-06 09:06:00'), -- 운동/피트니스 (취미)
       (14, 'aitest01', '2026-08-06 09:06:00');  -- 독서 (취미)


INSERT INTO `users`
(`user_id`, `name`, `login_id`, `nickname`, `email`, `profile_image_url`,
 `created_at`, `updated_at`, `password`, `user_status`, `point`, `birthday`)
VALUES
    ('aiuser02', '테스트유저', 'aiuser02', 'AI테스트02', 'aiuser02@youngly.test', NULL,
     '2026-06-15 09:00:00', '2026-08-10 09:00:00', '$2a$10$AFf3cAQ7PrvLZImN6rwaz.JdxNuVNK0ty/vkq/8A3o0ahid8naerS', 'ACTIVE', 60, '1999-05-20 00:00:00');

INSERT INTO `survey_results`
(`survey_result_id`, `user_id`, `answers_json`, `total_score`, `baseline`,
 `submitted_at`, `calculated_at`, `created_at`)
VALUES
    (2001, 'aiuser02', '{"Q1":4,"Q2":4,"Q3":3,"Q4":4,"Q5":3,"Q6":4}', 22, 'AGGRESSIVE',
     '2026-08-01 10:00:00', '2026-08-01 10:00:10', '2026-08-01 10:00:10');

-- 아래 interest_id는 실제 값 확인 후 필요시 수정: SELECT interest_id, interest_name FROM interests;
INSERT INTO `interest_users` (`interest_id`, `user_id`, `created_at`)
VALUES
    (1, 'aiuser02', '2026-08-01 10:05:00'),
    (9, 'aiuser02', '2026-08-01 10:05:00'),
    (12, 'aiuser02', '2026-08-01 10:05:00');

INSERT INTO `kb_accounts`
(`kb_account_id`, `account_type`, `account_number`, `bank_name`, `balance`,
 `interest_rate`, `name`, `birthday`, `created_at`, `updated_at`)
VALUES
    ('kb-deposit-aiuser02', 'DEPOSIT', '025202-00-009902', '국민', 1000000.00, 0.10,
     '테스트유저', '1999-05-20 00:00:00', '2026-06-15 09:10:00', '2026-08-10 09:00:00'),
    ('kb-pension-aiuser02', 'PENSION', '025202-11-229902', '국민', 5030000.00, 2.50,
     '테스트유저', '1999-05-20 00:00:00', '2026-06-15 09:11:00', '2026-08-10 09:00:00'),
    ('kb-moim-aiuser02-ex', 'MOIM', '025202-22-339901', '국민', 400000.00, 2.50,
     '테스트유저', '1999-05-20 00:00:00', '2026-07-01 09:50:00', '2026-08-10 09:00:00'),
    ('kb-moim-aiuser02-rd', 'MOIM', '025202-22-339902', '국민', 200000.00, 2.50,
     '테스트유저', '1999-05-20 00:00:00', '2026-07-01 09:50:00', '2026-08-10 09:00:00');

INSERT INTO `accounts`
(`account_id`, `user_id`, `kb_account_id`, `created_at`, `account_status`,
 `account_name`, `updated_at`)
VALUES
    ('account-aiuser02-deposit', 'aiuser02', 'kb-deposit-aiuser02', '2026-06-15 09:12:00',
     'OUTCOME', '025202-00-009902', '2026-08-10 09:00:00'),
    ('account-aiuser02-pension', 'aiuser02', 'kb-pension-aiuser02', '2026-06-15 09:13:00',
     'INCOME', 'KB', '2026-08-10 09:00:00');

INSERT INTO `moim_accounts`
(`moim_account_id`, `user_id`, `kb_account_id`, `account_status`, `created_at`, `updated_at`, `account_name`)
VALUES
    ('kb-moim-aiuser02-ex', 'aiuser02', 'kb-moim-aiuser02-ex', 'ACTIVE', '2026-07-01 10:00:00', '2026-08-10 09:00:00', '국민-모임통장-운동'),
    ('kb-moim-aiuser02-rd', 'aiuser02', 'kb-moim-aiuser02-rd', 'ACTIVE', '2026-07-01 10:00:00', '2026-08-10 09:00:00', '국민-모임통장-독서');

INSERT INTO `groups`
(`group_id`, `moim_account_id`, `user_id`, `invite_code`, `group_name`, `group_count`,
 `created_at`, `custom_rule`, `challenge_type`, `content`, `future_deposit_ratio_rule`,
 `duration_days`, `min_count`, `round_cycle_days`, `base_deposit_amount`, `group_status`, `updated_at`)
VALUES
    ('group-aiuser02-exercise', 'kb-moim-aiuser02-ex', 'aiuser02',
     'aaaaaaaa-0002-4aaa-8aaa-aaaaaaaaaaaa', '아침 운동 챌린지(AI테스트)', 3,
     '2026-07-01 10:00:00', '주 3회 이상 인증', 'EXERCISE', '아침 운동 인증 챌린지',
     '1:40/2:60/3:80', 7, 3, 28, 200000.00, 'ONGOING', '2026-08-10 09:00:00'),
    ('group-aiuser02-reading', 'kb-moim-aiuser02-rd', 'aiuser02',
     'bbbbbbbb-0002-4bbb-8bbb-bbbbbbbbbbbb', '독서 습관 챌린지(AI테스트)', 3,
     '2026-07-05 10:00:00', '주 4회 이상 인증', 'READING', '매일 20분 독서 인증',
     '1:50/2:70/3:90', 7, 4, 28, 100000.00, 'ONGOING', '2026-08-10 09:00:00');

-- 순위 비교용 참가자 (독서 1위 success_count=4, 3위 success_count=1)
INSERT INTO `users`
(`user_id`, `name`, `login_id`, `nickname`, `email`, `profile_image_url`,
 `created_at`, `updated_at`, `password`, `user_status`, `point`, `birthday`)
VALUES
    ('aipeer01', '비교유저1', 'aipeer01', 'AI피어01', 'aipeer01@youngly.test', NULL,
     '2026-06-15 09:00:00', '2026-08-10 09:00:00', '$2a$10$AFf3cAQ7PrvLZImN6rwaz.JdxNuVNK0ty/vkq/8A3o0ahid8naerS', 'ACTIVE', 0, '1998-03-01 00:00:00'),
    ('aipeer02', '비교유저2', 'aipeer02', 'AI피어02', 'aipeer02@youngly.test', NULL,
     '2026-06-15 09:00:00', '2026-08-10 09:00:00', '$2a$10$AFf3cAQ7PrvLZImN6rwaz.JdxNuVNK0ty/vkq/8A3o0ahid8naerS', 'ACTIVE', 0, '1997-11-11 00:00:00');

INSERT INTO `kb_accounts`
(`kb_account_id`, `account_type`, `account_number`, `bank_name`, `balance`,
 `interest_rate`, `name`, `birthday`, `created_at`, `updated_at`)
VALUES
    ('kb-pension-aipeer01', 'PENSION', '025202-11-229911', '국민', 1000000.00, 2.50,
     '비교유저1', '1998-03-01 00:00:00', '2026-06-15 09:11:00', '2026-08-10 09:00:00'),
    ('kb-pension-aipeer02', 'PENSION', '025202-11-229912', '국민', 1000000.00, 2.50,
     '비교유저2', '1997-11-11 00:00:00', '2026-06-15 09:11:00', '2026-08-10 09:00:00');

INSERT INTO `accounts`
(`account_id`, `user_id`, `kb_account_id`, `created_at`, `account_status`,
 `account_name`, `updated_at`)
VALUES
    ('account-aipeer01-pension', 'aipeer01', 'kb-pension-aipeer01', '2026-06-15 09:13:00',
     'INCOME', 'KB', '2026-08-10 09:00:00'),
    ('account-aipeer02-pension', 'aipeer02', 'kb-pension-aipeer02', '2026-06-15 09:13:00',
     'INCOME', 'KB', '2026-08-10 09:00:00');

INSERT INTO `group_users`
(`group_user_id`, `group_id`, `user_id`, `group_user_status`, `approved_at`,
 `current_deposit_amount`, `streak_count`, `created_at`, `updated_at`)
VALUES
    (9001, 'group-aiuser02-exercise', 'aiuser02', 'ACTIVE', '2026-07-01 10:05:00',
     200000.00, 1, '2026-07-01 10:05:00', '2026-08-10 09:00:00'),
    (9002, 'group-aiuser02-reading', 'aiuser02', 'ACTIVE', '2026-07-05 10:05:00',
     100000.00, 3, '2026-07-05 10:05:00', '2026-08-10 09:00:00'),
    (9003, 'group-aiuser02-exercise', 'aipeer01', 'ACTIVE', '2026-07-01 10:06:00',
     200000.00, 0, '2026-07-01 10:06:00', '2026-08-10 09:00:00'),
    (9004, 'group-aiuser02-exercise', 'aipeer02', 'ACTIVE', '2026-07-01 10:07:00',
     200000.00, 0, '2026-07-01 10:07:00', '2026-08-10 09:00:00'),
    (9005, 'group-aiuser02-reading', 'aipeer01', 'ACTIVE', '2026-07-05 10:06:00',
     100000.00, 4, '2026-07-05 10:06:00', '2026-08-10 09:00:00'),
    (9006, 'group-aiuser02-reading', 'aipeer02', 'ACTIVE', '2026-07-05 10:07:00',
     100000.00, 1, '2026-07-05 10:07:00', '2026-08-10 09:00:00');

INSERT INTO `rounds`
(`round_id`, `group_id`, `round_no`, `start_date`, `end_date`, `round_status`, `created_at`)
VALUES
    (9101, 'group-aiuser02-exercise', 1, '2026-07-01', '2026-07-28', 'SETTLED', '2026-06-30 23:00:00'),
    (9102, 'group-aiuser02-exercise', 2, '2026-07-29', '2026-08-25', 'ONGOING', '2026-07-28 23:00:00'),
    -- 종료일 경과 후 정산 대기: 예상 적립 대상에 반드시 포함되어야 한다 (ONGOING만 보면 0원이 됨)
    (9103, 'group-aiuser02-reading', 1, '2026-07-05', '2026-08-10', 'WAITING_SETTLEMENT', '2026-07-04 23:00:00');

INSERT INTO `round_history`
(`round_history_id`, `round_id`, `user_id`, `account_id`, `moim_account_id`,
 `rank_no`, `success_count`, `settlement_amount`, `remaining_fail_pass_count`,
 `prior_failure_response`, `created_at`, `settlement_at`)
VALUES
    (9201, 9101, 'aiuser02', 'account-aiuser02-pension', 'kb-moim-aiuser02-ex',
     1, 3, 30000.00, 0, NULL, '2026-06-30 23:00:00', '2026-08-01 09:00:00'),
    -- 2026-08-11 기준 완료 주차는 week1(8/05)만. success_count는 완료 주차와 일치해야 함.
    -- 운동 9102: aiuser02(1)=1위, aipeer01(0)=2위(동률), aipeer02(0)=2위(동률) / rank_no NULL 유지
    (9202, 9102, 'aiuser02', 'account-aiuser02-pension', 'kb-moim-aiuser02-ex',
     NULL, 1, NULL, 0, NULL, '2026-07-28 23:00:00', NULL),
    (9204, 9102, 'aipeer01', 'account-aipeer01-pension', 'kb-moim-aiuser02-ex',
     NULL, 0, NULL, 0, NULL, '2026-07-28 23:00:00', NULL),
    (9205, 9102, 'aipeer02', 'account-aipeer02-pension', 'kb-moim-aiuser02-ex',
     NULL, 0, NULL, 0, NULL, '2026-07-28 23:00:00', NULL),
    -- 독서 9103: aipeer01(4)=1위, aiuser02(3)=2위, aipeer02(1)=3위 → 현재순위 추가분 15만
    (9203, 9103, 'aiuser02', 'account-aiuser02-pension', 'kb-moim-aiuser02-rd',
     NULL, 3, NULL, 0, NULL, '2026-07-04 23:00:00', NULL),
    (9206, 9103, 'aipeer01', 'account-aipeer01-pension', 'kb-moim-aiuser02-rd',
     NULL, 4, NULL, 0, NULL, '2026-07-04 23:00:00', NULL),
    (9207, 9103, 'aipeer02', 'account-aipeer02-pension', 'kb-moim-aiuser02-rd',
     NULL, 1, NULL, 0, NULL, '2026-07-04 23:00:00', NULL);

-- weeklySuccessRate 분모 = created_at <= NOW() 인 DISTINCT week_no 만
-- 2026-08-11: 운동 week1만 완료(1), week2(8/12)·week3(8/19)는 미래 fixture로 분모 제외
-- 독서 week1~4 전부 과거 → completed=4, aiuser02 성공률 3/4
INSERT INTO `weekly_settlements`
(`weekly_settlement_id`, `round_id`, `week_no`, `user_id`, `approved_post_count`, `created_at`)
VALUES
    (9401, 9102, 1, 'aiuser02', 3, '2026-08-05 23:59:00'),
    (9402, 9102, 2, 'aiuser02', 3, '2026-08-12 23:59:00'),
    (9403, 9102, 3, 'aiuser02', 3, '2026-08-19 23:59:00'),
    (9404, 9102, 1, 'aipeer01', 1, '2026-08-05 23:59:00'),
    (9405, 9102, 2, 'aipeer01', 3, '2026-08-12 23:59:00'),
    (9406, 9102, 3, 'aipeer01', 1, '2026-08-19 23:59:00'),
    (9407, 9102, 1, 'aipeer02', 0, '2026-08-05 23:59:00'),
    (9408, 9102, 2, 'aipeer02', 1, '2026-08-12 23:59:00'),
    (9409, 9102, 3, 'aipeer02', 0, '2026-08-19 23:59:00'),
    (9410, 9103, 1, 'aiuser02', 4, '2026-07-12 23:59:00'),
    (9411, 9103, 2, 'aiuser02', 4, '2026-07-19 23:59:00'),
    (9412, 9103, 3, 'aiuser02', 4, '2026-07-26 23:59:00'),
    (9413, 9103, 4, 'aiuser02', 2, '2026-08-02 23:59:00'),
    (9414, 9103, 1, 'aipeer01', 4, '2026-07-12 23:59:00'),
    (9415, 9103, 2, 'aipeer01', 4, '2026-07-19 23:59:00'),
    (9416, 9103, 3, 'aipeer01', 4, '2026-07-26 23:59:00'),
    (9417, 9103, 4, 'aipeer01', 4, '2026-08-02 23:59:00'),
    (9418, 9103, 1, 'aipeer02', 4, '2026-07-12 23:59:00'),
    (9419, 9103, 2, 'aipeer02', 0, '2026-07-19 23:59:00'),
    (9420, 9103, 3, 'aipeer02', 1, '2026-07-26 23:59:00'),
    (9421, 9103, 4, 'aipeer02', 0, '2026-08-02 23:59:00');

INSERT INTO `account_transactions`
(`account_transaction_id`, `kb_account_id`, `group_user_id`, `round_id`,
 `transaction_type`, `transaction_category`, `amount`, `balance_after`,
 `idempotency_key`, `description`, `another_account_number`, `another_bank_name`,
 `another_name`, `created_at`)
VALUES
    (9301, 'kb-moim-aiuser02-ex', 9001, 9101, 'DEPOSIT', 'CHARGE', 200000.00, 200000.00,
     'AIUSER02-EX-R1-INITIAL', '운동 챌린지 1라운드 예치금', 'kb-deposit-aiuser02', '국민', '테스트유저',
     '2026-07-01 10:10:00'),
    -- 이번 달(8월) 확정 적립금: settledAmountThisMonth 에만 반영, 예상치(ONGOING/WAITING)에는 미포함
    (9302, 'kb-pension-aiuser02', 9001, 9101, 'DEPOSIT', 'SETTLEMENT', 30000.00, 5030000.00,
     'AIUSER02-EX-R1-SETTLEMENT', '운동 챌린지 1라운드 정산 - 개인연금 적립', 'kb-moim-aiuser02-ex', '국민', '테스트유저',
     '2026-08-01 09:00:00'),
    (9303, 'kb-moim-aiuser02-rd', 9002, 9103, 'DEPOSIT', 'CHARGE', 100000.00, 100000.00,
     'AIUSER02-RD-R1-INITIAL', '독서 챌린지 1라운드 예치금', 'kb-deposit-aiuser02', '국민', '테스트유저',
     '2026-07-05 10:10:00');


-- ============================================================================
-- 추가 통합 테스트 데이터
-- ============================================================================

USE youngly_db;

START TRANSACTION;

-- =========================================================
-- 1. 테스트 사용자
-- 로그인 비밀번호: test
-- =========================================================
INSERT INTO users (
    user_id,
    name,
    login_id,
    nickname,
    email,
    profile_image_url,
    password,
    user_status,
    point,
    birthday,
    created_at,
    updated_at,
    is_notification_agreement
) VALUES
(
    'test_user01', '테스트일', 'test_user01', '테스트일',
    'test_user01@youngly.test', NULL,
    '$2a$10$0MW4k3X6g./yliQpJO7mxe27eIMc6mGU0BOhPL8dQUHb8.h1FsLMW',
    'ACTIVE', 0, '1998-01-01',
    '2026-07-01 09:00:00', '2026-08-01 09:00:00', TRUE),
(
    'test_user02', '테스트이', 'test_user02', '테스트이',
    'test_user02@youngly.test', NULL,
    '$2a$10$0MW4k3X6g./yliQpJO7mxe27eIMc6mGU0BOhPL8dQUHb8.h1FsLMW',
    'ACTIVE', 0, '1998-02-02',
    '2026-07-01 09:01:00', '2026-08-01 09:00:00', TRUE),
(
    'test_user03', '테스트삼', 'test_user03', '테스트삼',
    'test_user03@youngly.test', NULL,
    '$2a$10$0MW4k3X6g./yliQpJO7mxe27eIMc6mGU0BOhPL8dQUHb8.h1FsLMW',
    'ACTIVE', 0, '1998-03-03',
    '2026-07-01 09:02:00', '2026-08-01 09:00:00', TRUE),
(
    'test_user04', '테스트사', 'test_user04', '테스트사',
    'test_user04@youngly.test', NULL,
    '$2a$10$0MW4k3X6g./yliQpJO7mxe27eIMc6mGU0BOhPL8dQUHb8.h1FsLMW',
    'ACTIVE', 0, '1998-04-04',
    '2026-07-01 09:03:00', '2026-08-01 09:00:00', TRUE);

-- =========================================================
-- 2. KB 입출금·개인연금·모임통장
-- 잔액은 아래 거래가 모두 끝난 최종 잔액
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
-- test_user01
(
    'kb-test-deposit-01', 'DEPOSIT', '025202-90-100001',
    '국민', 1800000.00, 0.10, '테스트일', '1998-01-01',
    '2026-07-01 09:10:00', '2026-08-01 09:00:00'
),
(
    'kb-test-pension-01', 'PENSION', '025202-91-100001',
    '국민', 3040000.00, 2.50, '테스트일', '1998-01-01',
    '2026-07-01 09:11:00', '2026-08-01 09:00:00'
),

-- test_user02
(
    'kb-test-deposit-02', 'DEPOSIT', '025202-90-100002',
    '국민', 1850000.00, 0.10, '테스트이', '1998-02-02',
    '2026-07-01 09:12:00', '2026-08-01 09:00:00'
),
(
    'kb-test-pension-02', 'PENSION', '025202-91-100002',
    '국민', 3030000.00, 2.50, '테스트이', '1998-02-02',
    '2026-07-01 09:13:00', '2026-08-01 09:00:00'
),

-- test_user03
(
    'kb-test-deposit-03', 'DEPOSIT', '025202-90-100003',
    '국민', 1900000.00, 0.10, '테스트삼', '1998-03-03',
    '2026-07-01 09:14:00', '2026-08-01 09:00:00'
),
(
    'kb-test-pension-03', 'PENSION', '025202-91-100003',
    '국민', 3020000.00, 2.50, '테스트삼', '1998-03-03',
    '2026-07-01 09:15:00', '2026-08-01 09:00:00'
),

-- test_user04
(
    'kb-test-deposit-04', 'DEPOSIT', '025202-90-100004',
    '국민', 1950000.00, 0.10, '테스트사', '1998-04-04',
    '2026-07-01 09:16:00', '2026-08-01 09:00:00'
),
(
    'kb-test-pension-04', 'PENSION', '025202-91-100004',
    '국민', 3010000.00, 2.50, '테스트사', '1998-04-04',
    '2026-07-01 09:17:00', '2026-08-01 09:00:00'
),

-- 공용 모임통장
(
    'kb-test-moim-01', 'MOIM', '025202-92-200001',
    '국민', 400000.00, 2.50, '테스트일', '1998-01-01',
    '2026-07-01 10:00:00', '2026-08-01 09:00:00'
);

-- =========================================================
-- 3. 서비스에 연동된 개인 계좌
-- =========================================================
INSERT INTO accounts (
    account_id,
    user_id,
    kb_account_id,
    account_status,
    account_name,
    created_at,
    updated_at
) VALUES
(
    'account-test-user01-deposit', 'test_user01',
    'kb-test-deposit-01', 'OUTCOME',
    '국민 025202-90-100001',
    '2026-07-01 09:20:00', '2026-08-01 09:00:00'
),
(
    'account-test-user01-pension', 'test_user01',
    'kb-test-pension-01', 'INCOME',
    'KB 개인연금',
    '2026-07-01 09:21:00', '2026-08-01 09:00:00'
),
(
    'account-test-user02-deposit', 'test_user02',
    'kb-test-deposit-02', 'OUTCOME',
    '국민 025202-90-100002',
    '2026-07-01 09:22:00', '2026-08-01 09:00:00'
),
(
    'account-test-user02-pension', 'test_user02',
    'kb-test-pension-02', 'INCOME',
    'KB 개인연금',
    '2026-07-01 09:23:00', '2026-08-01 09:00:00'
),
(
    'account-test-user03-deposit', 'test_user03',
    'kb-test-deposit-03', 'OUTCOME',
    '국민 025202-90-100003',
    '2026-07-01 09:24:00', '2026-08-01 09:00:00'
),
(
    'account-test-user03-pension', 'test_user03',
    'kb-test-pension-03', 'INCOME',
    'KB 개인연금',
    '2026-07-01 09:25:00', '2026-08-01 09:00:00'
),
(
    'account-test-user04-deposit', 'test_user04',
    'kb-test-deposit-04', 'OUTCOME',
    '국민 025202-90-100004',
    '2026-07-01 09:26:00', '2026-08-01 09:00:00'
),
(
    'account-test-user04-pension', 'test_user04',
    'kb-test-pension-04', 'INCOME',
    'KB 개인연금',
    '2026-07-01 09:27:00', '2026-08-01 09:00:00'
);

-- =========================================================
-- 4. 모임통장 연동
-- test_user01이 모임통장 연동 사용자
-- =========================================================
INSERT INTO moim_accounts (
    moim_account_id,
    user_id,
    kb_account_id,
    account_status,
    account_name,
    created_at,
    updated_at
) VALUES (
    'moim-account-test-01',
    'test_user01',
    'kb-test-moim-01',
    'ACTIVE',
    '테스트 공동 저축통장',
    '2026-07-01 10:05:00',
    '2026-08-01 09:00:00'
);

-- =========================================================
-- 5. 네 사용자가 참여하는 동일 그룹
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
    'group-test-savings-01',
    'moim-account-test-01',
    'test_user01',
    '44444444-4444-4444-8444-444444444444',
    '테스트 공동 저축 챌린지',
    4,
    '매주 3회 이상 목표 인증',
    'HABIT',
    '서로 다른 예치금과 거래내역을 확인하기 위한 테스트 그룹',
    '1:40/2:60/3:80/4:100',
    7,
    3,
    28,
    200000.00,
    'ONGOING',
    '2026-07-01 10:10:00',
    '2026-08-01 09:00:00'
);

-- =========================================================
-- 6. 그룹 참여자
-- user01: 200,000원 / 납부 완료
-- user02: 150,000원 / 50,000원 부족
-- user03: 100,000원 / 100,000원 부족
-- user04:  50,000원 / 150,000원 부족
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
) VALUES
(
    'group-test-savings-01', 'test_user01',
    'ACTIVE', '2026-07-01 11:00:00',
    200000.00, 4,
    '2026-07-01 10:20:00', '2026-08-01 09:00:00'
),
(
    'group-test-savings-01', 'test_user02',
    'PENDING_DEPOSIT', '2026-07-01 11:01:00',
    150000.00, 3,
    '2026-07-01 10:21:00', '2026-08-01 09:00:00'
),
(
    'group-test-savings-01', 'test_user03',
    'PENDING_DEPOSIT', '2026-07-01 11:02:00',
    100000.00, 2,
    '2026-07-01 10:22:00', '2026-08-01 09:00:00'
),
(
    'group-test-savings-01', 'test_user04',
    'PENDING_DEPOSIT', '2026-07-01 11:03:00',
    50000.00, 1,
    '2026-07-01 10:23:00', '2026-08-01 09:00:00'
);

-- =========================================================
-- 7. 테스트 라운드
-- =========================================================
INSERT INTO rounds (
    group_id,
    round_no,
    start_date,
    end_date,
    round_status,
    created_at
) VALUES (
    'group-test-savings-01',
    1,
    '2026-07-01',
    '2026-07-28',
    'SETTLED',
    '2026-07-01 12:00:00'
);

SET @test_round_id = (
    SELECT round_id
    FROM rounds
    WHERE group_id = 'group-test-savings-01'
      AND round_no = 1
);

SET @test_gu01 = (
    SELECT group_user_id
    FROM group_users
    WHERE group_id = 'group-test-savings-01'
      AND user_id = 'test_user01'
);

SET @test_gu02 = (
    SELECT group_user_id
    FROM group_users
    WHERE group_id = 'group-test-savings-01'
      AND user_id = 'test_user02'
);

SET @test_gu03 = (
    SELECT group_user_id
    FROM group_users
    WHERE group_id = 'group-test-savings-01'
      AND user_id = 'test_user03'
);

SET @test_gu04 = (
    SELECT group_user_id
    FROM group_users
    WHERE group_id = 'group-test-savings-01'
      AND user_id = 'test_user04'
);

-- =========================================================
-- 8. 개인계좌 → 모임통장 예치 거래
-- 각각 2회씩 나누어 납부
-- =========================================================
INSERT INTO account_transactions (
    kb_account_id,
    group_user_id,
    round_id,
    transaction_type,
    transaction_category,
    amount,
    balance_after,
    idempotency_key,
    description,
    another_account_number,
    another_bank_name,
    another_name,
    created_at
) VALUES
-- test_user01: 80,000 + 120,000 = 200,000
(
    'kb-test-deposit-01', @test_gu01, @test_round_id,
    'WITHDRAW', 'CHARGE', 80000.00, 1920000.00,
    'TEST-U01-CHARGE-01-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-02 10:00:00'
),
(
    'kb-test-moim-01', @test_gu01, @test_round_id,
    'DEPOSIT', 'CHARGE', 80000.00, 80000.00,
    'TEST-U01-CHARGE-01-IN',
    '테스트일 예치금 입금',
    '025202-90-100001', '국민', '테스트일',
    '2026-07-02 10:00:01'
),
(
    'kb-test-deposit-01', @test_gu01, @test_round_id,
    'WITHDRAW', 'CHARGE', 120000.00, 1800000.00,
    'TEST-U01-CHARGE-02-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-05 10:00:00'
),
(
    'kb-test-moim-01', @test_gu01, @test_round_id,
    'DEPOSIT', 'CHARGE', 120000.00, 200000.00,
    'TEST-U01-CHARGE-02-IN',
    '테스트일 예치금 입금',
    '025202-90-100001', '국민', '테스트일',
    '2026-07-05 10:00:01'
),

-- test_user02: 50,000 + 100,000 = 150,000
(
    'kb-test-deposit-02', @test_gu02, @test_round_id,
    'WITHDRAW', 'CHARGE', 50000.00, 1950000.00,
    'TEST-U02-CHARGE-01-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-06 10:00:00'
),
(
    'kb-test-moim-01', @test_gu02, @test_round_id,
    'DEPOSIT', 'CHARGE', 50000.00, 250000.00,
    'TEST-U02-CHARGE-01-IN',
    '테스트이 예치금 입금',
    '025202-90-100002', '국민', '테스트이',
    '2026-07-06 10:00:01'
),
(
    'kb-test-deposit-02', @test_gu02, @test_round_id,
    'WITHDRAW', 'CHARGE', 100000.00, 1850000.00,
    'TEST-U02-CHARGE-02-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-09 10:00:00'
),
(
    'kb-test-moim-01', @test_gu02, @test_round_id,
    'DEPOSIT', 'CHARGE', 100000.00, 350000.00,
    'TEST-U02-CHARGE-02-IN',
    '테스트이 예치금 입금',
    '025202-90-100002', '국민', '테스트이',
    '2026-07-09 10:00:01'
),

-- test_user03: 40,000 + 60,000 = 100,000
(
    'kb-test-deposit-03', @test_gu03, @test_round_id,
    'WITHDRAW', 'CHARGE', 40000.00, 1960000.00,
    'TEST-U03-CHARGE-01-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-10 10:00:00'
),
(
    'kb-test-moim-01', @test_gu03, @test_round_id,
    'DEPOSIT', 'CHARGE', 40000.00, 390000.00,
    'TEST-U03-CHARGE-01-IN',
    '테스트삼 예치금 입금',
    '025202-90-100003', '국민', '테스트삼',
    '2026-07-10 10:00:01'
),
(
    'kb-test-deposit-03', @test_gu03, @test_round_id,
    'WITHDRAW', 'CHARGE', 60000.00, 1900000.00,
    'TEST-U03-CHARGE-02-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-13 10:00:00'
),
(
    'kb-test-moim-01', @test_gu03, @test_round_id,
    'DEPOSIT', 'CHARGE', 60000.00, 450000.00,
    'TEST-U03-CHARGE-02-IN',
    '테스트삼 예치금 입금',
    '025202-90-100003', '국민', '테스트삼',
    '2026-07-13 10:00:01'
),

-- test_user04: 20,000 + 30,000 = 50,000
(
    'kb-test-deposit-04', @test_gu04, @test_round_id,
    'WITHDRAW', 'CHARGE', 20000.00, 1980000.00,
    'TEST-U04-CHARGE-01-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-14 10:00:00'
),
(
    'kb-test-moim-01', @test_gu04, @test_round_id,
    'DEPOSIT', 'CHARGE', 20000.00, 470000.00,
    'TEST-U04-CHARGE-01-IN',
    '테스트사 예치금 입금',
    '025202-90-100004', '국민', '테스트사',
    '2026-07-14 10:00:01'
),
(
    'kb-test-deposit-04', @test_gu04, @test_round_id,
    'WITHDRAW', 'CHARGE', 30000.00, 1950000.00,
    'TEST-U04-CHARGE-02-OUT',
    '모임 예치금 출금',
    '025202-92-200001', '국민', '테스트 공동 저축통장',
    '2026-07-17 10:00:00'
),
(
    'kb-test-moim-01', @test_gu04, @test_round_id,
    'DEPOSIT', 'CHARGE', 30000.00, 500000.00,
    'TEST-U04-CHARGE-02-IN',
    '테스트사 예치금 입금',
    '025202-90-100004', '국민', '테스트사',
    '2026-07-17 10:00:01'
);

-- =========================================================
-- 9. 모임통장 → 개인연금 정산
-- 모임통장 500,000원 → 정산 후 400,000원
-- =========================================================
INSERT INTO account_transactions (
    kb_account_id,
    group_user_id,
    round_id,
    transaction_type,
    transaction_category,
    amount,
    balance_after,
    idempotency_key,
    description,
    another_account_number,
    another_bank_name,
    another_name,
    created_at
) VALUES
-- test_user01: 40,000원 정산
(
    'kb-test-moim-01', @test_gu01, @test_round_id,
    'WITHDRAW', 'SETTLEMENT', 40000.00, 460000.00,
    'TEST-U01-SETTLEMENT-OUT',
    '테스트일 미래 적립금 정산',
    '025202-91-100001', '국민', '테스트일 개인연금',
    '2026-07-29 09:00:00'
),
(
    'kb-test-pension-01', @test_gu01, @test_round_id,
    'DEPOSIT', 'SETTLEMENT', 40000.00, 3040000.00,
    'TEST-U01-SETTLEMENT-IN',
    '챌린지 미래 적립금 입금',
    '025202-92-200001', '국민', '테스트 공동 저축 챌린지',
    '2026-07-29 09:00:01'
),

-- test_user02: 30,000원 정산
(
    'kb-test-moim-01', @test_gu02, @test_round_id,
    'WITHDRAW', 'SETTLEMENT', 30000.00, 430000.00,
    'TEST-U02-SETTLEMENT-OUT',
    '테스트이 미래 적립금 정산',
    '025202-91-100002', '국민', '테스트이 개인연금',
    '2026-07-29 09:01:00'
),
(
    'kb-test-pension-02', @test_gu02, @test_round_id,
    'DEPOSIT', 'SETTLEMENT', 30000.00, 3030000.00,
    'TEST-U02-SETTLEMENT-IN',
    '챌린지 미래 적립금 입금',
    '025202-92-200001', '국민', '테스트 공동 저축 챌린지',
    '2026-07-29 09:01:01'
),

-- test_user03: 20,000원 정산
(
    'kb-test-moim-01', @test_gu03, @test_round_id,
    'WITHDRAW', 'SETTLEMENT', 20000.00, 410000.00,
    'TEST-U03-SETTLEMENT-OUT',
    '테스트삼 미래 적립금 정산',
    '025202-91-100003', '국민', '테스트삼 개인연금',
    '2026-07-29 09:02:00'
),
(
    'kb-test-pension-03', @test_gu03, @test_round_id,
    'DEPOSIT', 'SETTLEMENT', 20000.00, 3020000.00,
    'TEST-U03-SETTLEMENT-IN',
    '챌린지 미래 적립금 입금',
    '025202-92-200001', '국민', '테스트 공동 저축 챌린지',
    '2026-07-29 09:02:01'
),

-- test_user04: 10,000원 정산
(
    'kb-test-moim-01', @test_gu04, @test_round_id,
    'WITHDRAW', 'SETTLEMENT', 10000.00, 400000.00,
    'TEST-U04-SETTLEMENT-OUT',
    '테스트사 미래 적립금 정산',
    '025202-91-100004', '국민', '테스트사 개인연금',
    '2026-07-29 09:03:00'
),
(
    'kb-test-pension-04', @test_gu04, @test_round_id,
    'DEPOSIT', 'SETTLEMENT', 10000.00, 3010000.00,
    'TEST-U04-SETTLEMENT-IN',
    '챌린지 미래 적립금 입금',
    '025202-92-200001', '국민', '테스트 공동 저축 챌린지',
    '2026-07-29 09:03:01'
);

COMMIT;


-- ==========================================================================
-- 데일리 통합 배치 API 테스트 데이터
--
-- 실행 API: POST /api/dev/daily-batches?date=2026-08-14
--
-- 한 번의 호출로 다음 작업을 모두 확인한다.
--   1. 2026-08-09에 등록된 PENDING 게시글 1건 자동 승인
--   2. group-batch-weekly-01의 1주차 결산
--   3. 2026-08-13 종료된 group-batch-transition-01 라운드 전환
--   4. 2026-08-12 종료된 group-batch-settlement-01 라운드 최종 정산
--
-- 이 데이터는 배치 실행 시 계좌 잔액과 상태가 변경되므로 반복 테스트 전에는
-- tables.sql과 data.sql을 다시 실행해 초기 상태로 복원한다.
-- ==========================================================================

START TRANSACTION;

-- 세 배치 대상 그룹이 기존 모임통장 잔액에 영향을 주지 않도록 전용 통장을 생성한다.
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
    ('kb-batch-moim-weekly', 'MOIM', '025202-22-339971', '국민',
     300000.00, 0.10, '김민준', '1998-03-12', '2026-08-01 09:00:00', '2026-08-12 23:00:00'),
    ('kb-batch-moim-transition', 'MOIM', '025202-22-339972', '국민',
     450000.00, 0.10, '최유진', '2000-01-18', '2026-07-15 09:00:00', '2026-08-12 23:00:00'),
    ('kb-batch-moim-settlement', 'MOIM', '025202-22-339973', '국민',
     300000.00, 0.10, '송지아', '2001-02-14', '2026-07-14 09:00:00', '2026-08-12 23:00:00');

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
    ('moim-batch-weekly', 'user01', 'kb-batch-moim-weekly', 'ACTIVE',
     '통합배치 주간결산 통장', '2026-08-01 09:00:00', '2026-08-01 09:00:00', '2026-08-12 23:00:00'),
    ('moim-batch-transition', 'user04', 'kb-batch-moim-transition', 'ACTIVE',
     '통합배치 라운드전환 통장', '2026-07-15 09:00:00', '2026-07-15 09:00:00', '2026-08-12 23:00:00'),
    ('moim-batch-settlement', 'user07', 'kb-batch-moim-settlement', 'ACTIVE',
     '통합배치 최종정산 통장', '2026-07-14 09:00:00', '2026-07-14 09:00:00', '2026-08-12 23:00:00');

-- 단계별 대상 조건이 겹치지 않도록 주간 결산, 전환, 최종 정산 그룹을 분리한다.
INSERT INTO `groups` (
    group_id,
    moim_account_id,
    user_id,
    invite_code,
    group_name,
    group_count,
    created_at,
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
    updated_at
) VALUES
    ('group-batch-weekly-01', 'moim-batch-weekly', 'user01',
     '97111111-1111-4111-8111-111111111111', '통합배치 주간결산 그룹', 3,
     '2026-08-01 09:00:00', '주 3회 인증', 'EXERCISE',
     '데일리 배치 주간 결산 단계 테스트', '1:30/2:50/3:70',
     7, 3, 28, 1, 100000.00, 'ONGOING', '2026-08-12 23:00:00'),
    ('group-batch-transition-01', 'moim-batch-transition', 'user04',
     '97222222-2222-4222-8222-222222222222', '통합배치 라운드전환 그룹', 3,
     '2026-07-15 09:00:00', '주 3회 인증', 'STUDY',
     '데일리 배치 라운드 전환 단계 테스트', '1:30/2:50/3:70',
     7, 3, 28, 1, 150000.00, 'ONGOING', '2026-08-12 23:00:00'),
    ('group-batch-settlement-01', 'moim-batch-settlement', 'user07',
     '97333333-3333-4333-8333-333333333333', '통합배치 최종정산 그룹', 3,
     '2026-07-14 09:00:00', '주 3회 인증', 'READING',
     '데일리 배치 최종 정산 단계 테스트', '1:30/2:50/3:70',
     7, 3, 28, 1, 100000.00, 'ONGOING', '2026-08-12 23:00:00');

-- 기존 사용자의 수령 계좌를 재사용하되 그룹별 예치금은 독립된 group_users로 관리한다.
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
    ('group-batch-weekly-01', 'user01', 'ACTIVE', '2026-08-01 10:00:00', 100000.00, 0, '2026-08-01 10:00:00', '2026-08-12 23:00:00'),
    ('group-batch-weekly-01', 'user02', 'ACTIVE', '2026-08-01 10:01:00', 100000.00, 0, '2026-08-01 10:01:00', '2026-08-12 23:00:00'),
    ('group-batch-weekly-01', 'user03', 'ACTIVE', '2026-08-01 10:02:00', 100000.00, 0, '2026-08-01 10:02:00', '2026-08-12 23:00:00'),
    ('group-batch-transition-01', 'user04', 'ACTIVE', '2026-07-15 10:00:00', 150000.00, 0, '2026-07-15 10:00:00', '2026-08-12 23:00:00'),
    ('group-batch-transition-01', 'user05', 'ACTIVE', '2026-07-15 10:01:00', 150000.00, 0, '2026-07-15 10:01:00', '2026-08-12 23:00:00'),
    ('group-batch-transition-01', 'user06', 'ACTIVE', '2026-07-15 10:02:00', 150000.00, 0, '2026-07-15 10:02:00', '2026-08-12 23:00:00'),
    ('group-batch-settlement-01', 'user07', 'ACTIVE', '2026-07-14 10:00:00', 100000.00, 4, '2026-07-14 10:00:00', '2026-08-12 23:00:00'),
    ('group-batch-settlement-01', 'user08', 'ACTIVE', '2026-07-14 10:01:00', 100000.00, 3, '2026-07-14 10:01:00', '2026-08-12 23:00:00'),
    ('group-batch-settlement-01', 'user09', 'ACTIVE', '2026-07-14 10:02:00', 100000.00, 3, '2026-07-14 10:02:00', '2026-08-12 23:00:00');

-- 8월 14일 배치 기준: 주간 결산일 D, 전환 종료일 D-1, 최종 정산 종료일 D-2.
INSERT INTO rounds (
    round_id,
    group_id,
    round_no,
    start_date,
    end_date,
    round_status,
    created_at
) VALUES
    (9801, 'group-batch-weekly-01', 1, '2026-08-06', '2026-09-02', 'ONGOING', '2026-08-05 23:00:00'),
    (9802, 'group-batch-transition-01', 1, '2026-07-17', '2026-08-13', 'ONGOING', '2026-07-16 23:00:00'),
    (9803, 'group-batch-settlement-01', 1, '2026-07-16', '2026-08-12', 'WAITING_SETTLEMENT', '2026-07-15 23:00:00');

-- 주간 결산 그룹은 user01과 user02가 성공하고 user03은 실패하도록 구성한다.
-- user02는 승인 2건과 실패 패스 1개로 부족한 1회를 충당한다.
INSERT INTO round_history (
    round_history_id,
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
    (98001, 9801, 'user01', 'account-user01-pension', 'moim-batch-weekly', NULL, 0, NULL, 0, NULL, '2026-08-05 23:00:00', NULL),
    (98002, 9801, 'user02', 'account-user02-deposit', 'moim-batch-weekly', NULL, 0, NULL, 1, NULL, '2026-08-05 23:00:00', NULL),
    (98003, 9801, 'user03', 'account-user03-pension', 'moim-batch-weekly', NULL, 0, NULL, 0, NULL, '2026-08-05 23:00:00', NULL),

    -- 전환 대상 라운드의 기존 참여 이력. 다음 라운드 이력은 배치가 자동 생성한다.
    (98004, 9802, 'user04', 'account-user04-deposit', 'moim-batch-transition', NULL, 4, NULL, 1, NULL, '2026-07-16 23:00:00', NULL),
    (98005, 9802, 'user05', 'account-user05-pension', 'moim-batch-transition', NULL, 3, NULL, 1, NULL, '2026-07-16 23:00:00', NULL),
    (98006, 9802, 'user06', 'account-user06-deposit', 'moim-batch-transition', NULL, 2, NULL, 1, NULL, '2026-07-16 23:00:00', NULL),

    -- 최종 정산은 success_count 5, 3, 3으로 공동 2등을 검증한다.
    (98007, 9803, 'user07', 'account-user07-pension', 'moim-batch-settlement', NULL, 5, NULL, 1, 'RETRY', '2026-07-15 23:00:00', NULL),
    (98008, 9803, 'user08', 'account-user08-deposit', 'moim-batch-settlement', NULL, 3, NULL, 1, 'EASE', '2026-07-15 23:00:00', NULL),
    (98009, 9803, 'user09', 'account-user09-pension', 'moim-batch-settlement', NULL, 3, NULL, 1, 'GIVE_UP', '2026-07-15 23:00:00', NULL);

-- 8월 14일 주간 결산 범위는 8월 6일~12일이다.
-- user01의 PENDING 게시글은 자동 승인된 뒤 세 번째 승인 건으로 집계된다.
INSERT INTO posts (
    round_id,
    user_id,
    photo_url,
    content,
    post_status,
    created_at,
    posted_at,
    status_changed_at,
    approve_count,
    reject_count
) VALUES
    (9801, 'user01', '/test/daily-batch/user01-approved-1.jpg', '통합배치 user01 승인 1',
     'APPROVED', '2026-08-06 07:00:00', '2026-08-06 07:00:00', '2026-08-06 09:00:00', 2, 0),
    (9801, 'user01', '/test/daily-batch/user01-approved-2.jpg', '통합배치 user01 승인 2',
     'APPROVED', '2026-08-07 07:00:00', '2026-08-07 07:00:00', '2026-08-07 09:00:00', 2, 0),
    (9801, 'user01', '/test/daily-batch/user01-auto-approve.jpg', '통합배치 user01 자동 승인 대상',
     'PENDING', '2026-08-09 07:00:00', '2026-08-09 07:00:00', NULL, 0, 0),
    (9801, 'user02', '/test/daily-batch/user02-approved-1.jpg', '통합배치 user02 승인 1',
     'APPROVED', '2026-08-06 08:00:00', '2026-08-06 08:00:00', '2026-08-06 10:00:00', 2, 0),
    (9801, 'user02', '/test/daily-batch/user02-approved-2.jpg', '통합배치 user02 승인 2',
     'APPROVED', '2026-08-08 08:00:00', '2026-08-08 08:00:00', '2026-08-08 10:00:00', 2, 0),
    (9801, 'user03', '/test/daily-batch/user03-approved-1.jpg', '통합배치 user03 승인 1',
     'APPROVED', '2026-08-07 08:30:00', '2026-08-07 08:30:00', '2026-08-07 10:30:00', 2, 0);

COMMIT;
