-- ============================================================================
-- Youngly - 테이블 생성 스크립트
--
-- [INFO] 담당 : BE / ERD 기준 (v4 ERD 이미지 참조)
-- [TODO] ERD 확정본에 따라 아래 테이블을 채운다.
--        users, groups, group_users, rounds, round_users, posts, post_approvals,
--        comments, notifications, accounts, group_accounts, point_logs,
--        survey_results, recommendations, collectibles, user_items 등
--
-- [WARN] 작성 규칙
--   - 엔진 : InnoDB, 문자셋 : utf8mb4
--   - 컬럼명은 snake_case (MyBatis mapUnderscoreToCamelCase 설정과 연동됨)
--   - 모든 테이블에 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 포함
--   - 금액 컬럼은 DECIMAL 사용. FLOAT/DOUBLE 금지 (정산 오차 방지)
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

USE youngly_db;

-- FK 의존 관계의 자식 테이블부터 제거한다. FOREIGN_KEY_CHECKS는 순서 실수 방지용이다.
# weekly_settlements 테이블 추가 260805
DROP TABLE IF EXISTS `survey_result_interest`;
DROP TABLE IF EXISTS `survey_results`;
DROP TABLE IF EXISTS `survey_choices`;
DROP TABLE IF EXISTS `survey_questions`;
DROP TABLE IF EXISTS `interest_industry`;
DROP TABLE IF EXISTS `post_comments`;
DROP TABLE IF EXISTS `post_reactions`;
DROP TABLE IF EXISTS `post_history`;
DROP TABLE IF EXISTS `post_approvals`;
DROP TABLE IF EXISTS `posts`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `point_history`;
DROP TABLE IF EXISTS `user_items`;
DROP TABLE IF EXISTS `recommendations`;
DROP TABLE IF EXISTS `weekly_settlements`;
DROP TABLE IF EXISTS `round_history`;
DROP TABLE IF EXISTS `group_history`;
DROP TABLE IF EXISTS `account_transactions`;
DROP TABLE IF EXISTS `rounds`;
DROP TABLE IF EXISTS `group_users`;
DROP TABLE IF EXISTS `accounts`;
DROP TABLE IF EXISTS `groups`;
DROP TABLE IF EXISTS `moim_accounts`;
DROP TABLE IF EXISTS `kb_accounts`;
DROP TABLE IF EXISTS `interest_users`;
DROP TABLE IF EXISTS `collectible_items`;
DROP TABLE IF EXISTS `interests`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `market_daily_snapshot`;

-- 예시 (실제 컬럼은 ERD 확정 후 교체)
-- CREATE TABLE users (
--     user_id         BIGINT       NOT NULL AUTO_INCREMENT,
--     login_id        VARCHAR(20)  NOT NULL,
--     password        VARCHAR(255) NOT NULL,
--     name            VARCHAR(50)  NOT NULL,
--     nickname        VARCHAR(15)  NOT NULL,
--     email           VARCHAR(100) NOT NULL,
--     profile_image_url VARCHAR(255) NULL,
--     created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
--     PRIMARY KEY (user_id),
--     UNIQUE KEY uk_users_login_id (login_id),
--     UNIQUE KEY uk_users_nickname (nickname)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `account_transactions` (
                                        `account_transaction_id`	BIGINT	NOT NULL,
                                        `kb_account_id`	VARCHAR(50)	NOT NULL,
                                        `group_user_id`               BIGINT NOT NULL,
                                        `round_id`                    BIGINT NULL,
                                        `transaction_type`	ENUM('DEPOSIT','WITHDRAW')	NOT NULL,
                                        `transaction_category` ENUM(
                                            'CHARGE',
                                            'SETTLEMENT',
                                            'REFUND'
                                            ) NOT NULL,
                                        `amount`	DECIMAL(19,2)	NOT NULL,
                                        `balance_after`	DECIMAL(19,2)	NOT NULL,
                                        `idempotency_key`             VARCHAR(100) NULL,
                                        `description`	VARCHAR(255)	NULL,
                                        `another_account_number` VARCHAR(50) NULL,
                                        `another_bank_name` VARCHAR(50) NULL DEFAULT '국민',
                                        `another_name` VARCHAR(50) NULL,
                                        `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `recommendations` (
                                   `recommendation_id`	BIGINT	NOT NULL,
                                   `user_id`	VARCHAR(50)	NOT NULL,
                                   `survey_result_id` BIGINT NULL COMMENT '이 추천 생성에 사용한 최신 설문 결과 ID',
                                   `baseline`	ENUM( 'STABLE', 'CONSERVATIVE', 'NEUTRAL', 'AGGRESSIVE', 'VERY_AGGRESSIVE' )	NOT NULL,
                                   `period_start_date` DATE NULL COMMENT '예상 산정 기간 시작일 (이번 달 1일)',
                                   `period_end_date` DATE NULL COMMENT '예상 산정 기간 종료일 (이번 달 말일)',
                                   `settled_amount_this_month` DECIMAL(19,2) NULL COMMENT '이번 달 정산 완료 확정 적립금 (백엔드 계산)',
                                   `expected_additional_amount_current_rank` DECIMAL(19,2) NULL COMMENT '진행 중 챌린지 현재 순위 기준 추가 예상분 (백엔드 계산)',
                                   `expected_additional_amount_best_case` DECIMAL(19,2) NULL COMMENT '진행 중 챌린지 최고 순위 기준 추가 예상분 (백엔드 계산)',
                                   `expected_total_amount_this_month` DECIMAL(19,2) NULL COMMENT '이번 달 예상 총액 (확정 + 현재 순위) (백엔드 계산)',
                                   `expected_max_total_amount_this_month` DECIMAL(19,2) NULL COMMENT '이번 달 예상 최대 총액 (확정 + 최고 순위) (백엔드 계산)',
                                   `current_pension_balance` DECIMAL(19,2) NULL COMMENT '개인연금 현재 잔액 (백엔드 계산)',
                                   `has_ongoing_round_this_month` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이번 달 종료 예정 진행 라운드 존재 여부 (백엔드 계산)',
                                   `next_deposit_date` DATE NULL COMMENT '다음 적립(라운드 종료) 예정일 (백엔드 계산)',
                                   `forecast_basis_json` JSON NULL COMMENT '예상 구간 산정 근거 스냅샷 (챌린지별 순위/인증률/기여도)',
                                   `market_highlights_json` JSON NULL COMMENT 'LLM 생성 - 최근 2주 시장 개조식 요약',
                                   `market_detail` VARCHAR(400) NULL COMMENT 'LLM 생성 - 시장 동향 상세 문단',
                                   `pension_insight_intro` VARCHAR(500) NULL COMMENT 'LLM 생성 - 투자성향·관심분야 기반 개인연금 인사이트',
                                   `pension_insight_strategy` VARCHAR(200) NULL COMMENT 'LLM 생성 - 장기 전략 제안',
                                   `guardrail_status` ENUM('PASSED','FAILED_FALLBACK') NOT NULL DEFAULT 'PASSED' COMMENT '숫자 검증 가드레일 통과 여부',
                                   `generation_mode` ENUM('LIVE','CACHED','SAFE_DEFAULT') NOT NULL DEFAULT 'SAFE_DEFAULT' COMMENT '이 레코드가 생성된 방식',
                                   `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_items` (
                              `user_item_id`	BIGINT	NOT NULL,
                              `item_id`	BIGINT	NOT NULL,
                              `user_id`	VARCHAR(50)	NOT NULL,
                              `is_equipped`	BOOLEAN	NOT NULL	DEFAULT FALSE,
                              `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `group_users` (
                               `group_user_id`	BIGINT	NOT NULL,
                               `group_id`	VARCHAR(50)	NOT NULL,
                               `user_id`	VARCHAR(50)	NOT NULL,
                               `group_user_status`	ENUM( 'PENDING_DEPOSIT', 'ACTIVE', 'WITHDRAWN', 'REJECTED', 'PENDING_APPROVAL' )	NOT NULL,
                               `approved_at`	DATETIME	NULL,
                               `current_deposit_amount`	DECIMAL(19,2)	NOT NULL	DEFAULT 0,
                               `streak_count`	INT	NOT NULL	DEFAULT 0,
                               `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `posts` (
                         `post_id`	BIGINT	NOT NULL,
                         `round_id`	BIGINT	NOT NULL,
                         `user_id`	VARCHAR(50)	NOT NULL,
                         `photo_url`	VARCHAR(255)	NULL,
                         `content`	VARCHAR(500)	NULL,
                         `post_status`	ENUM( 'PENDING', 'APPROVED', 'REJECTED','NONE')	NOT NULL	DEFAULT 'PENDING'	COMMENT '하루가 시작되면 NONE으로 post 생성 => created_at
인증 게시물 올리면 PENDING으로 수정 => posted_at
승인/반려 당하면 각각 맞는거로 수정 => status_changed_at
인증 삭제하면 다시 NONE으로 수정하고 posted_at 값 제거',
                         `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         `posted_at`	DATETIME	NULL	COMMENT 'post_status가 NONE에서 PENDING으로 바뀐 시점',
                         `status_changed_at`	DATETIME	NULL,
                         `like_count`	INT	NOT NULL	DEFAULT 0,
                         `dislike_count`	INT	NOT NULL	DEFAULT 0,
                         `comment_count`	INT	NOT NULL	DEFAULT 0,
                         `reject_count`	INT	NOT NULL    	DEFAULT 0	COMMENT '게시글의 승인 여부를 판단할 때, reject의 수를 사용',
                         `approve_count`	INT	NOT NULL    	DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `round_history` (
                                 `round_history_id`	BIGINT	NOT NULL,
                                 `round_id`	BIGINT	NOT NULL,
                                 `user_id`	VARCHAR(50)	NOT NULL,
                                 `account_id`	VARCHAR(50)	NOT NULL	COMMENT '미래적립금 수령할 계좌(입출금 or 연금)',
                                 `moim_account_id`	VARCHAR(50)	NOT NULL	COMMENT '미래적립금 빠져나가는 모임통장',
                                 `rank_no`	INT	NULL,
                                 `success_count`	INT	NOT NULL	DEFAULT 0,
                                 `settlement_amount`	DECIMAL(19,2)	NULL,
                                 `remaining_fail_pass_count`	INT	NOT NULL,
                                 `prior_failure_response`	ENUM( 'GIVE_UP', 'EASE', 'RETRY' )	NULL,
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP	COMMENT '라운드 시작하면 만들어짐',
                                 `settlement_at`	DATETIME	NULL	COMMENT '라운드 종료되고 정산까지 완료되면 그때 채워지는 컬럼, 동시에 round_status도 바꿔줘야함'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `groups` (
                          `group_id`	VARCHAR(50)	NOT NULL,
                          `moim_account_id`	VARCHAR(50)	NOT NULL,
                          `user_id`	VARCHAR(50)	NULL,
                          `invite_code`	VARCHAR(36)	NOT NULL	COMMENT 'UUID, 참여용 초대코드',
                          `group_name`	VARCHAR(50)	NOT NULL,
                          `group_count`	INT	NOT NULL,
                          `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `custom_rule`	VARCHAR(255)	NULL,
                          `challenge_type`	ENUM( 'DRAFT','EXERCISE', 'STUDY', 'READING', 'HABIT', 'CUSTOM' )	NOT NULL,
                          `content`	VARCHAR(255)	NULL,
                          `future_deposit_ratio_rule`	VARCHAR(255)	NOT NULL,
                          `duration_days`	INT	NOT NULL	DEFAULT 7	COMMENT '일단 디폴트로 7일',
                          `min_count`	INT	NOT NULL,
                          `round_cycle_days`	INT	NOT NULL	DEFAULT 28	COMMENT '디폴트로 28일',
                          `default_fail_pass_count`	INT	NULL,
                          `base_deposit_amount`	DECIMAL(19,2)	NOT NULL,
                          `group_status`	ENUM( 'RECRUITING', 'ONGOING', 'FINISHED' )	NOT NULL,
                          `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `interests` (
                             `interest_id`	BIGINT	NOT NULL,
                             `interest_name`	VARCHAR(50)	NOT NULL,
                             `is_investment`	BOOLEAN	NOT NULL, -- true: 투자 # false: 관심사
                             `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `moim_accounts` (
                                 `moim_account_id`	VARCHAR(50)	NOT NULL,
                                 `user_id`	VARCHAR(50)	NOT NULL,
                                 `kb_account_id`	VARCHAR(50)	NOT NULL,
                                 `account_status`  ENUM ('ACTIVE', 'DEACTIVATED') NOT NULL DEFAULT 'ACTIVE',
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `synced_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 `account_name`	VARCHAR(50)	NOT NULL	COMMENT 'default로 bank_name(모임) + account_number'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rounds` (
                          `round_id`	BIGINT	NOT NULL,
                          `group_id`	VARCHAR(50)	NOT NULL,
                          `round_no`	INT	NOT NULL,
                          `start_date`	DATE	NOT NULL,
                          `end_date`	DATE	NOT NULL,
                          `round_status`	ENUM( 'ONGOING', 'WAITING_SETTLEMENT', 'SETTLED' )	NOT NULL,
                          `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 사용자별 주간 결산 결과. 승인 게시물 수를 보관하고 유니크 키로 중복 결산을 방지한다.
CREATE TABLE `weekly_settlements` (
                                      `weekly_settlement_id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `round_id` BIGINT NOT NULL,
                                      `week_no` INT NOT NULL,
                                      `user_id` VARCHAR(50) NOT NULL,
                                      `approved_post_count` INT NOT NULL DEFAULT 0,
                                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT `PK_WEEKLY_SETTLEMENTS`
                                          PRIMARY KEY (`weekly_settlement_id`),
                                      CONSTRAINT `UK_WEEKLY_SETTLEMENTS_ROUND_WEEK_USER`
                                          UNIQUE (`round_id`, `week_no`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_approvals` (
                                  `post_approval_id`	BIGINT	NOT NULL,
                                  `user_id`	VARCHAR(50)	NOT NULL,
                                  `post_id`	BIGINT	NOT NULL,
                                  `approval_status`	ENUM('APPROVE','REJECT','PENDING','AUTO_APPROVE')	NOT NULL,
                                  `reject_reason`	VARCHAR(255)	NULL,
                                  `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `notifications` (
                                 `notification_id`	BIGINT	NOT NULL,
                                 `user_id`	VARCHAR(50)	NOT NULL,
                                 `notification_type`	ENUM( 'GROUP_INVITE', 'APPROVAL_REQUEST', 'APPROVED', 'REJECTED', 'ROUND_START', 'ROUND_END', 'SETTLEMENT_COMPLETED', 'REMINDER' )	NOT NULL,
                                 `content`	VARCHAR(255)	NOT NULL,
                                 `is_read`	BOOLEAN	NOT NULL	DEFAULT FALSE,
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_history` (
                                `post_history_id`	BIGINT	NOT NULL,
                                `post_id`	BIGINT	NOT NULL,
                                `post_history_version`	BIGINT	NOT NULL,
                                `photo_url`	VARCHAR(255)	NULL,
                                `content`	VARCHAR(500)	NULL,
                                `post_status`	ENUM( 'PENDING', 'APPROVED', 'REJECTED','NONE')	NOT NULL,
                                `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_reactions` (
                                  `reaction_id`	BIGINT	NOT NULL,
                                  `post_id`	BIGINT	NOT NULL,
                                  `user_id`	VARCHAR(50)	NOT NULL,
                                  `reaction_type`	ENUM( 'PENDING', 'LIKE', 'DISLIKE')	NOT NULL	DEFAULT 'PENDING',
                                  `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `accounts` (
                            `account_id`	VARCHAR(50)	NOT NULL,
                            `user_id`	VARCHAR(50)	NOT NULL,
                            `kb_account_id`	VARCHAR(50)	NOT NULL,
                            `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `synced_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `account_status`	ENUM('INCOME','OUTCOME', 'INOUTCOME','NONE')	NOT NULL,
                            `account_name`	VARCHAR(50)	NOT NULL	COMMENT 'default로 bank_name + account_number로 설정',
                            `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `kb_accounts` (
                               `kb_account_id`	VARCHAR(50)	NOT NULL,
                               `account_type`	ENUM('DEPOSIT', 'PENSION','MOIM')	NOT NULL,
                               `account_number`	VARCHAR(50)	NOT NULL,
                               `bank_name`	VARCHAR(30)	NOT NULL DEFAULT '국민',
                               `balance`	DECIMAL(19,2)	NOT NULL,
                               `interest_rate`	DECIMAL(7,2)	NOT NULL,
                               `name`	VARCHAR(30)	NOT NULL,
                               `birthday` DATE NOT NULL,
                               `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `point_history` (
                                 `point_log_id`	BIGINT	NOT NULL,
                                 `user_id`	VARCHAR(50)	NOT NULL,
                                 `item_id`	BIGINT	NULL,
                                 `point_type`	ENUM( 'EARN', 'USE' )	NOT NULL,
                                 `amount`	INT	NOT NULL,
                                 `content`	VARCHAR(100)	NULL,
                                 `idempotency_key`	VARCHAR(100)	NULL,
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `collectible_items` (
                                     `item_id`	BIGINT	NOT NULL,
                                     `item_category`	ENUM( 'CHARACTER', 'FRAME','ACC' )	NOT NULL,
                                     `item_name`	VARCHAR(50)	NOT NULL,
                                     `image_url`	VARCHAR(255)	NOT NULL,
                                     `drop_rate`	DECIMAL(5,2)	NOT NULL,
                                     `base_character`	ENUM('KIKI','AGO','BB','LAMU','KOLLY','ETC')	NULL,
                                     `acc_part`	ENUM('HAT','GLASSES','GLOVES','SHOES','ETC')	NULL,
                                     `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `survey_questions` (
                                    `question_id`	BIGINT	NOT NULL,
                                    `question_no`	INT	NOT NULL,
                                    `question_text`	VARCHAR(200)	NOT NULL,
                                    `is_multiple`	BOOLEAN	NOT NULL DEFAULT FALSE,
                                    `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `survey_choices` (
                                  `choice_id`	BIGINT	NOT NULL,
                                  `question_id`	BIGINT	NOT NULL,
                                  `choice_text`	VARCHAR(200)	NOT NULL,
                                  `score`	INT	NOT NULL,
                                  `display_order`	INT	NOT NULL,
                                  `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `survey_results` (
                                  `survey_result_id`	BIGINT	NOT NULL,
                                  `user_id`	VARCHAR(50)	NOT NULL,
                                  `answers_json`	JSON	NOT NULL,
                                  `total_score`	INT	NOT NULL,
                                  `baseline`	VARCHAR(50)	NOT NULL,
                                  `submitted_at`	DATETIME	NOT NULL,
                                  `calculated_at`	DATETIME	NOT NULL,
                                  `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `survey_result_interest` (
                                          `survey_result_id` BIGINT NOT NULL,
                                          `interest_id` BIGINT NOT NULL,
                                          PRIMARY KEY (`survey_result_id`, `interest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `group_history` (
                                 `group_history_id`	BIGINT	NOT NULL,
                                 `moim_account_id`	VARCHAR(50)	NOT NULL,
                                 `group_id`	VARCHAR(50)	NOT NULL,
                                 `group_history_version`	BIGINT	NOT NULL,
                                 `invite_code`	VARCHAR(36)	NOT NULL,
                                 `group_name`	VARCHAR(50)	NOT NULL,
                                 `group_count`	INT	NOT NULL,
                                 `custom_rule`	VARCHAR(255)	NULL,
                                 `challenge_type`	ENUM( 'DRAFT','EXERCISE', 'STUDY', 'READING', 'HABIT', 'CUSTOM' )	NOT NULL,
                                 `content`	VARCHAR(255)	NULL,
                                 `future_deposit_ratio_rule`	VARCHAR(255)	NOT NULL,
                                 `duration_days`	INT	NOT NULL,
                                 `min_count`	INT	NOT NULL,
                                 `round_cycle_days`	INT	NOT NULL,
                                 `base_deposit_amount`	DECIMAL(19,2)	NOT NULL,
                                 `group_status`	ENUM( 'RECRUITING', 'ONGOING', 'FINISHED' )	NOT NULL,
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `users` (
                         `user_id`	VARCHAR(50)	NOT NULL,
                         `name`	VARCHAR(50)	NOT NULL,
                         `login_id`	VARCHAR(20)	NOT NULL,
                         `nickname`	VARCHAR(15)	NOT NULL,
                         `email`	VARCHAR(100)	NOT NULL,
                         `profile_image_url`	VARCHAR(255)	NULL,
                         `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         `password`	VARCHAR(255)	NOT NULL,
                         `user_status`	ENUM('ACTIVE', 'DEACTIVATED')	NOT NULL DEFAULT 'ACTIVE',
                         `point`	BIGINT	NOT NULL DEFAULT 0,
                         `birthday`	DATE	NOT NULL,
                         `is_notification_agreement`  BOOLEAN NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_comments` (
                                 `post_comment_id`	BIGINT	NOT NULL,
                                 `user_id`	VARCHAR(50)	NOT NULL,
                                 `post_id`	BIGINT	NOT NULL,
                                 `content`	VARCHAR(255)	NOT NULL,
                                 `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `interest_users` (
                                  `interest_id`	BIGINT	NOT NULL,
                                  `user_id`	VARCHAR(50)	NOT NULL,
                                  `created_at`	DATETIME	NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE market_daily_snapshot (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       market_date DATE NOT NULL,
                                       source_subject VARCHAR(500) NOT NULL,
                                       pdf_file_name VARCHAR(500) NOT NULL,
                                       pdf_url TEXT NOT NULL,
                                       raw_text LONGTEXT NULL,
                                       summary_text LONGTEXT NULL,
                                       market_headline_text TEXT NULL,
                                       market_detail_text TEXT NULL,
                                       kospi DECIMAL(12, 2) NULL,
                                       kosdaq DECIMAL(12, 2) NULL,
                                       nasdaq DECIMAL(12, 2) NULL,
                                       usdkrw DECIMAL(12, 2) NULL,
                                       treasury_3y DECIMAL(8, 4) NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       PRIMARY KEY (id),
                                       UNIQUE KEY uk_market_daily_snapshot_pdf_file_name (pdf_file_name),
                                       UNIQUE KEY uk_market_daily_snapshot_date_subject (market_date, source_subject),
                                       KEY idx_market_daily_snapshot_market_date (market_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


ALTER TABLE `user_items` ADD CONSTRAINT `PK_USER_ITEMS` PRIMARY KEY (
                                                                     `user_item_id`
    );

ALTER TABLE `account_transactions` ADD CONSTRAINT `PK_ACCOUNT_TRANSACTIONS` PRIMARY KEY (
                                                                                         `account_transaction_id`
    );

ALTER TABLE `recommendations` ADD CONSTRAINT `PK_RECOMMENDATIONS` PRIMARY KEY (
                                                                               `recommendation_id`
    );

ALTER TABLE `group_users` ADD CONSTRAINT `PK_GROUP_USERS` PRIMARY KEY (
                                                                       `group_user_id`
    );

ALTER TABLE `posts` ADD CONSTRAINT `PK_POSTS` PRIMARY KEY (
                                                           `post_id`
    );

ALTER TABLE `round_history` ADD CONSTRAINT `PK_ROUND_HISTORY` PRIMARY KEY (
                                                                           `round_history_id`
    );

ALTER TABLE `groups` ADD CONSTRAINT `PK_GROUPS` PRIMARY KEY (
                                                             `group_id`
    );

ALTER TABLE `interests` ADD CONSTRAINT `PK_INTERESTS` PRIMARY KEY (
                                                                   `interest_id`
    );

ALTER TABLE `moim_accounts` ADD CONSTRAINT `PK_MOIM_ACCOUNTS` PRIMARY KEY (
                                                                           `moim_account_id`
    );

ALTER TABLE `rounds` ADD CONSTRAINT `PK_ROUNDS` PRIMARY KEY (
                                                             `round_id`
    );

ALTER TABLE `post_approvals` ADD CONSTRAINT `PK_POST_APPROVALS` PRIMARY KEY (
                                                                             `post_approval_id`
    );

ALTER TABLE `notifications` ADD CONSTRAINT `PK_NOTIFICATIONS` PRIMARY KEY (
                                                                           `notification_id`
    );

ALTER TABLE `post_history` ADD CONSTRAINT `PK_POST_HISTORY` PRIMARY KEY (
                                                                         `post_history_id`
    );

ALTER TABLE `post_reactions` ADD CONSTRAINT `PK_POST_REACTIONS` PRIMARY KEY (
                                                                             `reaction_id`
    );

ALTER TABLE `accounts` ADD CONSTRAINT `PK_ACCOUNTS` PRIMARY KEY (
                                                                 `account_id`
    );

ALTER TABLE `kb_accounts` ADD CONSTRAINT `PK_KB_ACCOUNTS` PRIMARY KEY (
                                                                       `kb_account_id`
    );

ALTER TABLE `point_history` ADD CONSTRAINT `PK_POINT_HISTORY` PRIMARY KEY (
                                                                           `point_log_id`
    );

ALTER TABLE `collectible_items` ADD CONSTRAINT `PK_COLLECTIBLE_ITEMS` PRIMARY KEY (
                                                                                   `item_id`
    );

ALTER TABLE `survey_questions` ADD CONSTRAINT `PK_SURVEY_QUESTIONS` PRIMARY KEY (
                                                                                 `question_id`
    );

ALTER TABLE `survey_questions` ADD CONSTRAINT `UK_SURVEY_QUESTIONS_QUESTION_NO` UNIQUE (
                                                                                        `question_no`
    );

ALTER TABLE `survey_choices` ADD CONSTRAINT `PK_SURVEY_CHOICES` PRIMARY KEY (
                                                                             `choice_id`
    );

ALTER TABLE `survey_choices` ADD CONSTRAINT `UK_SURVEY_CHOICES_QUESTION_DISPLAY_ORDER` UNIQUE (
                                                                                               `question_id`,
                                                                                               `display_order`
    );

ALTER TABLE `interests` ADD CONSTRAINT `UK_INTERESTS_NAME` UNIQUE (`interest_name`);

ALTER TABLE `survey_results` ADD CONSTRAINT `PK_SURVEY_RESULTS` PRIMARY KEY (
                                                                             `survey_result_id`
    );


-- 사용자별 최신 설문 결과 조회(selectLatestResultByUserId)를 위한 복합 인덱스
CREATE INDEX `IDX_SURVEY_RESULTS_USER_SUBMITTED_AT`
    ON `survey_results` (`user_id`, `submitted_at` DESC);

CREATE INDEX `IDX_SURVEY_CHOICES_QUESTION_DISPLAY_ORDER`
    ON `survey_choices` (`question_id`, `display_order`);

ALTER TABLE `group_history` ADD CONSTRAINT `PK_GROUP_HISTORY` PRIMARY KEY (
                                                                           `group_history_id`
    );

ALTER TABLE `users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
                                                           `user_id`
    );

ALTER TABLE `post_comments` ADD CONSTRAINT `PK_POST_COMMENTS` PRIMARY KEY (
                                                                           `post_comment_id`
    );

ALTER TABLE `users` ADD CONSTRAINT `UK_USERS_LOGIN_ID` UNIQUE (
                                                               `login_id`
    );

ALTER TABLE `users` ADD CONSTRAINT `UK_USERS_EMAIL` UNIQUE (
                                                            `email`
    );

ALTER TABLE `users` ADD CONSTRAINT `UK_USERS_NICKNAME` UNIQUE (
                                                               `nickname`
    );

ALTER TABLE `groups` ADD CONSTRAINT `UK_GROUPS_INVITE_CODE` UNIQUE (
                                                                    `invite_code`
    );

ALTER TABLE `group_users` ADD CONSTRAINT `UK_GROUP_USERS_GROUP_USER` UNIQUE (
                                                                             `group_id`,
                                                                             `user_id`
    );

ALTER TABLE `user_items` ADD CONSTRAINT `UK_USER_ITEMS_USER_ITEM` UNIQUE (
                                                                          `user_id`,
                                                                          `item_id`
    );

ALTER TABLE `rounds` ADD CONSTRAINT `UK_ROUNDS_GROUP_ROUND_NO` UNIQUE (
                                                                       `group_id`,
                                                                       `round_no`
    );

ALTER TABLE `post_reactions` ADD CONSTRAINT `UK_POST_REACTIONS_POST_USER` UNIQUE (
                                                                                  `post_id`,
                                                                                  `user_id`
    );

ALTER TABLE `post_approvals` ADD CONSTRAINT `UK_POST_APPROVALS_POST_USER` UNIQUE (
                                                                                  `post_id`,
                                                                                  `user_id`
    );

ALTER TABLE `post_history` ADD CONSTRAINT `UK_POST_HISTORY_POST_VERSION` UNIQUE (
                                                                                 `post_id`,
                                                                                 `post_history_version`
    );

ALTER TABLE `account_transactions` ADD CONSTRAINT `UK_ACCOUNT_TRANSACTIONS_IDEMPOTENCY_KEY` UNIQUE (
                                                                                                    `idempotency_key`
    );

ALTER TABLE `point_history` ADD CONSTRAINT `UK_POINT_HISTORY_IDEMPOTENCY_KEY` UNIQUE (
                                                                                      `idempotency_key`
    );


ALTER TABLE `interest_users` ADD CONSTRAINT `PK_INTEREST_USERS` PRIMARY KEY (
                                                                             `interest_id`,
                                                                             `user_id`
    );

-- 숫자형 단일 기본키 자동 증가 설정
-- AUTO_INCREMENT 컬럼은 키여야 하므로 기본키 제약조건 생성 후 적용한다.
ALTER TABLE `user_items`
    MODIFY `user_item_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `account_transactions`
    MODIFY `account_transaction_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `recommendations`
    MODIFY `recommendation_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `group_users`
    MODIFY `group_user_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `posts`
    MODIFY `post_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `round_history`
    MODIFY `round_history_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `interests`
    MODIFY `interest_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `rounds`
    MODIFY `round_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_approvals`
    MODIFY `post_approval_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `notifications`
    MODIFY `notification_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_history`
    MODIFY `post_history_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_reactions`
    MODIFY `reaction_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `point_history`
    MODIFY `point_log_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `collectible_items`
    MODIFY `item_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `survey_questions`
    MODIFY `question_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `survey_choices`
    MODIFY `choice_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `survey_results`
    MODIFY `survey_result_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `group_history`
    MODIFY `group_history_id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_comments`
    MODIFY `post_comment_id` BIGINT NOT NULL AUTO_INCREMENT;

-- ============================================================================
-- 숫자 범위 CHECK 제약조건 추가
-- ============================================================================

ALTER TABLE `account_transactions`
    ADD CONSTRAINT `CK_ACCOUNT_TRANSACTIONS_AMOUNT`
        CHECK (`amount` >= 0);

ALTER TABLE `group_users`
    ADD CONSTRAINT `CK_GROUP_USERS_CURRENT_DEPOSIT_AMOUNT`
        CHECK (`current_deposit_amount` >= 0),
    ADD CONSTRAINT `CK_GROUP_USERS_STREAK_COUNT`
        CHECK (`streak_count` >= 0);

ALTER TABLE `posts`
    ADD CONSTRAINT `CK_POSTS_LIKE_COUNT`
        CHECK (`like_count` >= 0),
    ADD CONSTRAINT `CK_POSTS_DISLIKE_COUNT`
        CHECK (`dislike_count` >= 0),
    ADD CONSTRAINT `CK_POSTS_COMMENT_COUNT`
        CHECK (`comment_count` >= 0),
    ADD CONSTRAINT `CK_POSTS_REJECT_COUNT`
        CHECK (`reject_count` >= 0),
    ADD CONSTRAINT `CK_POSTS_APPROVE_COUNT`
        CHECK (`approve_count` >= 0);

ALTER TABLE `round_history`
    ADD CONSTRAINT `CK_ROUND_HISTORY_SUCCESS_COUNT`
        CHECK (`success_count` >= 0);

ALTER TABLE `groups`
    ADD CONSTRAINT `CK_GROUPS_GROUP_COUNT`
        CHECK (`group_count` > 0),
    ADD CONSTRAINT `CK_GROUPS_DURATION_DAYS`
        CHECK (`duration_days` > 0),
    ADD CONSTRAINT `CK_GROUPS_MIN_COUNT`
        CHECK (`min_count` > 0),
    ADD CONSTRAINT `CK_GROUPS_BASE_DEPOSIT_AMOUNT`
        CHECK (`base_deposit_amount` >= 0);

ALTER TABLE `kb_accounts`
    ADD CONSTRAINT `CK_KB_ACCOUNTS_BALANCE`
        CHECK (`balance` >= 0),
    ADD CONSTRAINT `CK_KB_ACCOUNTS_INTEREST_RATE`
        CHECK (`interest_rate` >= 0);

ALTER TABLE `rounds`
    ADD CONSTRAINT `CK_ROUNDS_ROUND_NO`
        CHECK (`round_no` > 0);

ALTER TABLE `point_history`
    ADD CONSTRAINT `CK_POINT_HISTORY_AMOUNT`
        CHECK (`amount` >= 0);

ALTER TABLE `collectible_items`
    ADD CONSTRAINT `CK_COLLECTIBLE_ITEMS_DROP_RATE`
        CHECK (`drop_rate` >= 0 AND `drop_rate` <= 100);

ALTER TABLE `group_history`
    ADD CONSTRAINT `CK_GROUP_HISTORY_GROUP_COUNT`
        CHECK (`group_count` > 0),
    ADD CONSTRAINT `CK_GROUP_HISTORY_DURATION_DAYS`
        CHECK (`duration_days` > 0),
    ADD CONSTRAINT `CK_GROUP_HISTORY_MIN_COUNT`
        CHECK (`min_count` > 0),
    ADD CONSTRAINT `CK_GROUP_HISTORY_BASE_DEPOSIT_AMOUNT`
        CHECK (`base_deposit_amount` >= 0);

ALTER TABLE `users`
    ADD CONSTRAINT `CK_USERS_POINT`
        CHECK (`point` >= 0);

-- ============================================================================
-- 외래키(Foreign Key) 제약조건 추가
-- ============================================================================

-- 1. user_items
ALTER TABLE `user_items`
    ADD CONSTRAINT `FK_user_items_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_user_items_item_id` FOREIGN KEY (`item_id`) REFERENCES `collectible_items` (`item_id`);

-- 2. account_transactions
ALTER TABLE `account_transactions`
    ADD CONSTRAINT `FK_account_transactions_kb_account_id` FOREIGN KEY (`kb_account_id`) REFERENCES `kb_accounts` (`kb_account_id`),
    ADD CONSTRAINT `FK_account_transactions_group_user_id` FOREIGN KEY (`group_user_id`) REFERENCES `group_users` (`group_user_id`),
    ADD CONSTRAINT `FK_account_transactions_round_id` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`);

-- 3. recommendations
ALTER TABLE `recommendations`
    ADD CONSTRAINT `FK_recommendations_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 4. group_users
ALTER TABLE `group_users`
    ADD CONSTRAINT `FK_group_users_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`),
    ADD CONSTRAINT `FK_group_users_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 5. posts
ALTER TABLE `posts`
    ADD CONSTRAINT `FK_posts_round_id` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
    ADD CONSTRAINT `FK_posts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 6. round_history
ALTER TABLE `round_history`
    ADD CONSTRAINT `FK_round_history_round_id` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
    ADD CONSTRAINT `FK_round_history_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_round_history_account_id` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`),
    ADD CONSTRAINT `FK_round_history_moim_account_id` FOREIGN KEY (`moim_account_id`) REFERENCES `moim_accounts` (`moim_account_id`);

-- 7. groups
ALTER TABLE `groups`
    ADD CONSTRAINT `FK_groups_moim_account_id` FOREIGN KEY (`moim_account_id`) REFERENCES `moim_accounts` (`moim_account_id`),
    ADD CONSTRAINT `FK_groups_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 8. moim_accounts
ALTER TABLE `moim_accounts`
    ADD CONSTRAINT `FK_moim_accounts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_moim_accounts_kb_account_id` FOREIGN KEY (`kb_account_id`) REFERENCES `kb_accounts` (`kb_account_id`);

-- 9. rounds
ALTER TABLE `rounds`
    ADD CONSTRAINT `FK_rounds_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`);

-- 10. weekly_settlements
ALTER TABLE `weekly_settlements`
    ADD CONSTRAINT `FK_weekly_settlements_round_id`
        FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
    ADD CONSTRAINT `FK_weekly_settlements_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 10. post_approvals
ALTER TABLE `post_approvals`
    ADD CONSTRAINT `FK_post_approvals_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_post_approvals_post_id` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`);

-- 11. notifications
ALTER TABLE `notifications`
    ADD CONSTRAINT `FK_notifications_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 12. post_history
ALTER TABLE `post_history`
    ADD CONSTRAINT `FK_post_history_post_id` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`);

-- 13. post_reactions
ALTER TABLE `post_reactions`
    ADD CONSTRAINT `FK_post_reactions_post_id` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`),
    ADD CONSTRAINT `FK_post_reactions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 15. accounts
ALTER TABLE `accounts`
    ADD CONSTRAINT `FK_accounts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_accounts_kb_account_id` FOREIGN KEY (`kb_account_id`) REFERENCES `kb_accounts` (`kb_account_id`);

-- 16. point_history
ALTER TABLE `point_history`
    ADD CONSTRAINT `FK_point_history_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_point_history_item_id` FOREIGN KEY (`item_id`) REFERENCES `collectible_items` (`item_id`);

-- 17. survey_results
ALTER TABLE `survey_results`
    ADD CONSTRAINT `FK_survey_results_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

-- 18. survey_questions / survey_choices
ALTER TABLE `survey_choices`
    ADD CONSTRAINT `FK_survey_choices_question_id` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`question_id`);

-- 19 survey_result_interest
ALTER TABLE `survey_result_interest`
    ADD CONSTRAINT `FK_SURVEY_RESULT_INTEREST_RESULT_ID`
        FOREIGN KEY (`survey_result_id`) REFERENCES `survey_results` (`survey_result_id`),
    ADD CONSTRAINT `FK_SURVEY_RESULT_INTEREST_INTEREST_ID`
        FOREIGN KEY (`interest_id`) REFERENCES `interests` (`interest_id`);

-- 20. group_history
ALTER TABLE `group_history`
    ADD CONSTRAINT `FK_group_history_moim_account_id` FOREIGN KEY (`moim_account_id`) REFERENCES `moim_accounts` (`moim_account_id`),
    ADD CONSTRAINT `FK_group_history_group_id` FOREIGN KEY (`group_id`) REFERENCES `groups` (`group_id`);

-- 19. post_comments
ALTER TABLE `post_comments`
    ADD CONSTRAINT `FK_post_comments_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    ADD CONSTRAINT `FK_post_comments_post_id` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`);


ALTER TABLE `interest_users`
    ADD CONSTRAINT `FK_interests_TO_interest_users_1` FOREIGN KEY (`interest_id`) REFERENCES `interests` (`interest_id`);

ALTER TABLE `interest_users`
    ADD CONSTRAINT `FK_users_TO_interest_users_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

SET FOREIGN_KEY_CHECKS = 1;
