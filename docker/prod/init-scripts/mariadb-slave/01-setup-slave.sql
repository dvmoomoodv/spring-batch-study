-- MariaDB Slave 복제 설정
USE mysql;

-- 배치 작업용 사용자 생성 (읽기 전용)
CREATE USER IF NOT EXISTS 'batch_user'@'%' IDENTIFIED BY 'BatchPassword123!';
GRANT SELECT ON targetDB.* TO 'batch_user'@'%';

FLUSH PRIVILEGES;

-- Slave 복제 설정은 컨테이너 시작 후 별도 스크립트로 처리
-- (Master의 바이너리 로그 위치를 알아야 하므로)

SELECT 'MariaDB Slave 기본 설정 완료' as message;
