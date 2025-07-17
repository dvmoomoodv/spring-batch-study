-- MSSQL 테스트 데이터베이스 초기화 스크립트

-- 데이터베이스 생성
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'sourceDB')
BEGIN
    CREATE DATABASE sourceDB;
END
GO

USE sourceDB;
GO

-- 사용자 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='사용자' AND xtype='U')
BEGIN
    CREATE TABLE 사용자 (
        사용자ID INT PRIMARY KEY IDENTITY(1,1),
        이름 NVARCHAR(50) NOT NULL,
        이메일 NVARCHAR(100) UNIQUE,
        성별 NVARCHAR(10),
        직업 NVARCHAR(50),
        활성여부 BIT DEFAULT 1,
        생성일시 DATETIME2 DEFAULT GETDATE(),
        수정일시 DATETIME2 DEFAULT GETDATE()
    );
END
GO

-- 카테고리 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='카테고리' AND xtype='U')
BEGIN
    CREATE TABLE 카테고리 (
        카테고리ID INT PRIMARY KEY IDENTITY(1,1),
        카테고리명 NVARCHAR(50) NOT NULL,
        상위카테고리ID INT,
        정렬순서 INT DEFAULT 0,
        활성여부 BIT DEFAULT 1,
        생성일시 DATETIME2 DEFAULT GETDATE()
    );
END
GO

-- 상품 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='상품' AND xtype='U')
BEGIN
    CREATE TABLE 상품 (
        상품ID INT PRIMARY KEY IDENTITY(1,1),
        상품명 NVARCHAR(100) NOT NULL,
        카테고리ID INT,
        가격 DECIMAL(10,2),
        판매상태 NVARCHAR(20) DEFAULT '판매중',
        재고수량 INT DEFAULT 0,
        상품설명 NTEXT,
        생성일시 DATETIME2 DEFAULT GETDATE(),
        수정일시 DATETIME2 DEFAULT GETDATE(),
        FOREIGN KEY (카테고리ID) REFERENCES 카테고리(카테고리ID)
    );
END
GO

-- 주문 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='주문' AND xtype='U')
BEGIN
    CREATE TABLE 주문 (
        주문ID INT PRIMARY KEY IDENTITY(1,1),
        사용자ID INT NOT NULL,
        주문번호 NVARCHAR(50) UNIQUE NOT NULL,
        주문상태 NVARCHAR(20) DEFAULT '주문접수',
        결제방법 NVARCHAR(20),
        총금액 DECIMAL(12,2),
        주문일시 DATETIME2 DEFAULT GETDATE(),
        배송주소 NVARCHAR(200),
        FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
    );
END
GO

-- 주문상세 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='주문상세' AND xtype='U')
BEGIN
    CREATE TABLE 주문상세 (
        주문상세ID INT PRIMARY KEY IDENTITY(1,1),
        주문ID INT NOT NULL,
        상품ID INT NOT NULL,
        수량 INT NOT NULL,
        단가 DECIMAL(10,2),
        소계 DECIMAL(12,2),
        FOREIGN KEY (주문ID) REFERENCES 주문(주문ID),
        FOREIGN KEY (상품ID) REFERENCES 상품(상품ID)
    );
END
GO

-- 리뷰 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='리뷰' AND xtype='U')
BEGIN
    CREATE TABLE 리뷰 (
        리뷰ID INT PRIMARY KEY IDENTITY(1,1),
        상품ID INT NOT NULL,
        사용자ID INT NOT NULL,
        평점 INT CHECK (평점 >= 1 AND 평점 <= 5),
        리뷰내용 NTEXT,
        작성일시 DATETIME2 DEFAULT GETDATE(),
        FOREIGN KEY (상품ID) REFERENCES 상품(상품ID),
        FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
    );
END
GO

-- 공지사항 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='공지사항' AND xtype='U')
BEGIN
    CREATE TABLE 공지사항 (
        공지ID INT PRIMARY KEY IDENTITY(1,1),
        제목 NVARCHAR(200) NOT NULL,
        내용 NTEXT,
        작성자 NVARCHAR(50),
        조회수 INT DEFAULT 0,
        중요여부 BIT DEFAULT 0,
        게시시작일 DATETIME2,
        게시종료일 DATETIME2,
        생성일시 DATETIME2 DEFAULT GETDATE()
    );
END
GO

-- 쿠폰 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='쿠폰' AND xtype='U')
BEGIN
    CREATE TABLE 쿠폰 (
        쿠폰ID INT PRIMARY KEY IDENTITY(1,1),
        쿠폰명 NVARCHAR(100) NOT NULL,
        쿠폰코드 NVARCHAR(50) UNIQUE,
        할인타입 NVARCHAR(20), -- '정액', '정률'
        할인값 DECIMAL(10,2),
        최소주문금액 DECIMAL(10,2),
        사용시작일 DATETIME2,
        사용종료일 DATETIME2,
        사용여부 BIT DEFAULT 1,
        생성일시 DATETIME2 DEFAULT GETDATE()
    );
END
GO

-- 배송 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='배송' AND xtype='U')
BEGIN
    CREATE TABLE 배송 (
        배송ID INT PRIMARY KEY IDENTITY(1,1),
        주문ID INT NOT NULL,
        배송업체 NVARCHAR(50),
        송장번호 NVARCHAR(50),
        배송상태 NVARCHAR(20) DEFAULT '배송준비',
        배송시작일 DATETIME2,
        배송완료일 DATETIME2,
        수취인명 NVARCHAR(50),
        배송주소 NVARCHAR(200),
        연락처 NVARCHAR(20),
        FOREIGN KEY (주문ID) REFERENCES 주문(주문ID)
    );
END
GO

-- 문의 테이블 생성
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='문의' AND xtype='U')
BEGIN
    CREATE TABLE 문의 (
        문의ID INT PRIMARY KEY IDENTITY(1,1),
        사용자ID INT,
        문의유형 NVARCHAR(20),
        제목 NVARCHAR(200) NOT NULL,
        내용 NTEXT,
        답변내용 NTEXT,
        처리상태 NVARCHAR(20) DEFAULT '접수',
        작성일시 DATETIME2 DEFAULT GETDATE(),
        답변일시 DATETIME2,
        FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
    );
END
GO

-- 인덱스 생성
CREATE INDEX IX_사용자_이메일 ON 사용자(이메일);
CREATE INDEX IX_상품_카테고리ID ON 상품(카테고리ID);
CREATE INDEX IX_주문_사용자ID ON 주문(사용자ID);
CREATE INDEX IX_주문_주문일시 ON 주문(주문일시);
CREATE INDEX IX_주문상세_주문ID ON 주문상세(주문ID);
CREATE INDEX IX_주문상세_상품ID ON 주문상세(상품ID);
CREATE INDEX IX_리뷰_상품ID ON 리뷰(상품ID);
CREATE INDEX IX_리뷰_사용자ID ON 리뷰(사용자ID);
CREATE INDEX IX_배송_주문ID ON 배송(주문ID);
CREATE INDEX IX_문의_사용자ID ON 문의(사용자ID);
GO

-- 기본 테스트 데이터 삽입
INSERT INTO 사용자 (이름, 이메일, 성별, 직업) VALUES
('김철수', 'kim.cs@test.com', '남성', '개발자'),
('이영희', 'lee.yh@test.com', '여성', '디자이너'),
('박민수', 'park.ms@test.com', '남성', '마케터'),
('최지영', 'choi.jy@test.com', '여성', '기획자'),
('정우성', 'jung.ws@test.com', '남성', '영업');
GO

INSERT INTO 카테고리 (카테고리명, 상위카테고리ID) VALUES
('전자제품', NULL),
('스마트폰', 1),
('노트북', 1),
('의류', NULL),
('남성의류', 4),
('여성의류', 4);
GO

INSERT INTO 상품 (상품명, 카테고리ID, 가격, 판매상태, 재고수량) VALUES
('갤럭시 S24', 2, 1200000, '판매중', 50),
('아이폰 15', 2, 1300000, '판매중', 30),
('맥북 프로', 3, 2500000, '품절', 0),
('남성 셔츠', 5, 50000, '판매중', 100),
('여성 원피스', 6, 80000, '판매중', 80);
GO

INSERT INTO 주문 (사용자ID, 주문번호, 주문상태, 결제방법, 총금액, 배송주소) VALUES
(1, 'ORD-2024-001', '배송완료', '신용카드', 1200000, '서울시 강남구'),
(2, 'ORD-2024-002', '배송중', '계좌이체', 1300000, '서울시 서초구'),
(3, 'ORD-2024-003', '주문접수', '신용카드', 50000, '부산시 해운대구'),
(4, 'ORD-2024-004', '배송준비', '무통장입금', 80000, '대구시 중구'),
(5, 'ORD-2024-005', '주문취소', '신용카드', 2500000, '광주시 서구');
GO

PRINT '테스트 데이터베이스 초기화 완료';
GO
