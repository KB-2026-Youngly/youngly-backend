-- ============================================================================
-- Youngly - 데이터베이스 및 계정 생성 스크립트
--
-- [INFO] 실행 순서
--   1. MySQL 8.0 에 root 로 접속한다.
--   2. 이 파일을 실행한다.
--   3. db/tables.sql (테이블 생성) -> db/data.sql (초기 데이터) 순으로 실행한다.
--
-- [WARN] 아래 비밀번호는 로컬 개발용이다. 운영 환경에서는 반드시 변경한다.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS youngly_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'youngly'@'%' IDENTIFIED BY '원하는값';
GRANT ALL PRIVILEGES ON youngly_db.* TO 'youngly'@'%';
FLUSH PRIVILEGES;

USE youngly_db;
